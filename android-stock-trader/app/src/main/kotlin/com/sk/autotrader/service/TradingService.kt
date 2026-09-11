package com.sk.autotrader.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.sk.autotrader.AutoTraderApp
import com.sk.autotrader.data.local.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 장중 내내 매매 루프를 돌리는 포그라운드 서비스.
 *
 * 포그라운드 서비스를 쓰는 이유: 안드로이드는 백그라운드 작업을 공격적으로 중단시키므로,
 * 화면이 꺼진 상태에서 "손절 주문이 나가지 않는" 사고를 막으려면 상시 알림을 띄운
 * 포그라운드 서비스가 사실상 유일한 안정적 방법이다. 그래도 제조사 배터리 최적화가
 * 서비스를 죽일 수 있으므로, **앱만 믿고 방치하지 말 것**(가이드 문서 참고).
 */
class TradingService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var loopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TradingNotifications.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopLoop()
                return START_NOT_STICKY
            }
            else -> startLoop()
        }
        // 시스템이 서비스를 죽여도 다시 살려서 장중 공백을 줄인다.
        return START_STICKY
    }

    private fun startLoop() {
        if (loopJob?.isActive == true) return

        startForeground(
            TradingNotifications.NOTIFICATION_ID_SERVICE,
            TradingNotifications.serviceNotification(this, "자동매매 시작 중", "준비하고 있습니다"),
        )
        acquireWakeLock()
        _state.value = State.RUNNING

        val app = applicationContext as AutoTraderApp
        loopJob = scope.launch {
            while (isActive) {
                val intervalSeconds = try {
                    app.container.settingsStore.settings.first().loopIntervalSeconds
                        .coerceAtLeast(SettingsStore.MIN_LOOP_SECONDS)
                } catch (e: Exception) {
                    SettingsStore.MIN_LOOP_SECONDS
                }

                try {
                    val result = app.container.autoTradeRunner.runCycle()
                    _lastCycle.value = result.toStatusLine()
                    if (result.ordersPlaced > 0) {
                        TradingNotifications.alert(
                            this@TradingService,
                            "주문 ${result.ordersPlaced}건 전송",
                            "매매일지에서 상세 내역을 확인하세요.",
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "매매 사이클 실패", e)
                    _lastCycle.value = "오류: ${e.message}"
                }

                updateNotification()
                delay(intervalSeconds * 1_000L)
            }
        }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
        _state.value = State.STOPPED
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val time = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date())
        TradingNotifications.createChannels(this)
        getSystemService(android.app.NotificationManager::class.java).notify(
            TradingNotifications.NOTIFICATION_ID_SERVICE,
            TradingNotifications.serviceNotification(
                this,
                "자동매매 실행 중",
                "$time · ${_lastCycle.value}",
            ),
        )
    }

    /**
     * 도즈 모드에서 루프가 멈추지 않도록 부분 웨이크락을 잡는다.
     * 배터리를 쓰는 대신 매매 공백을 막는 트레이드오프이며, 장 마감 후에는 반드시 해제한다.
     */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoTrader::TradingLoop").apply {
            setReferenceCounted(false)
            acquire(MAX_WAKELOCK_MILLIS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        _state.value = State.STOPPED
        super.onDestroy()
    }

    enum class State { STOPPED, RUNNING }

    companion object {
        private const val TAG = "TradingService"
        const val ACTION_STOP = "com.sk.autotrader.action.STOP"

        /** 장 시간(약 6.5시간)을 덮되 무한정 잡지 않도록 상한을 둔다. */
        private const val MAX_WAKELOCK_MILLIS = 8 * 60 * 60 * 1000L

        private val _state = MutableStateFlow(State.STOPPED)
        val state: StateFlow<State> = _state.asStateFlow()

        private val _lastCycle = MutableStateFlow("대기 중")
        val lastCycle: StateFlow<String> = _lastCycle.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, TradingService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TradingService::class.java).setAction(ACTION_STOP))
        }
    }
}

private fun com.sk.autotrader.data.repository.AutoTradeRunner.CycleResult.toStatusLine(): String = when {
    skippedReason != null -> skippedReason
    errors.isNotEmpty() -> "${evaluated}종목 평가, 주문 ${ordersPlaced}건, 오류 ${errors.size}건"
    else -> "${evaluated}종목 평가, 주문 ${ordersPlaced}건"
}
