package com.sk.autotrader.data.repository

import android.util.Log
import com.sk.autotrader.core.engine.TradingEngine
import com.sk.autotrader.core.market.MarketSession
import com.sk.autotrader.core.model.Candle
import com.sk.autotrader.core.model.Decision
import com.sk.autotrader.core.model.OrderSide
import com.sk.autotrader.core.risk.RiskEngine
import com.sk.autotrader.core.strategy.StrategyRegistry
import com.sk.autotrader.data.local.AutoTraderDatabase
import com.sk.autotrader.data.local.OrderCooldownEntity
import com.sk.autotrader.data.local.SecureStore
import com.sk.autotrader.data.local.SettingsStore
import com.sk.autotrader.data.local.TradeLogEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 자동매매 1회분 실행.
 *
 * 서비스는 이 클래스를 주기적으로 부르기만 한다. "언제 도는가"(서비스)와
 * "무엇을 하는가"(러너)를 분리해두면 러너를 화면에서 수동 실행하거나 테스트하기 쉽다.
 *
 * 봉 구성 방식: 일봉 이력에 **현재가로 만든 진행 중인 봉**을 덧붙여 판단한다.
 * 장중에 신호가 나오되, 마지막 봉은 아직 확정되지 않았으므로 신호가 붙었다 떨어질 수 있다.
 * 이 떨림은 재주문 쿨다운으로 억제한다. 확정 봉만으로 매매하려면
 * [buildCandles]에서 현재가 덧붙이기를 빼면 된다(그 대신 하루 한 번만 신호가 난다).
 */
class AutoTradeRunner(
    private val marketData: MarketDataRepository,
    private val trading: TradingRepository,
    private val db: AutoTraderDatabase,
    private val settingsStore: SettingsStore,
    private val secureStore: SecureStore,
    private val marketSession: MarketSession = MarketSession(),
) {

    data class CycleResult(
        val evaluated: Int,
        val ordersPlaced: Int,
        val skippedReason: String? = null,
        val errors: List<String> = emptyList(),
    )

    /**
     * 한 사이클을 실행한다. 예외를 밖으로 던지지 않고 매매일지에 남긴다 —
     * 종목 하나에서 난 오류로 자동매매 전체가 멈추면 안 되기 때문이다.
     */
    suspend fun runCycle(force: Boolean = false): CycleResult {
        if (!force && !marketSession.isTradable()) {
            return CycleResult(0, 0, skippedReason = "매매 시간 아님 (${marketSession.describe()})")
        }

        val watchList = db.watchItemDao().enabledOnce()
        if (watchList.isEmpty()) {
            return CycleResult(0, 0, skippedReason = "대상 종목이 없습니다")
        }

        val settings = settingsStore.settings.first()
        val policy = settings.riskPolicy
        val mode = secureStore.tradingMode
        val riskEngine = RiskEngine(policy)

        val account = try {
            trading.accountSnapshot()
        } catch (e: Exception) {
            logError("ACCOUNT", "잔고 조회 실패: ${e.message}", mode.name)
            return CycleResult(0, 0, errors = listOf("잔고 조회 실패: ${e.message}"))
        }

        // 킬 스위치는 계좌 단위이므로 종목 루프 밖에서 한 번만 판정하고 기록한다.
        if (riskEngine.isKillSwitchOn(account) && !trading.isKillSwitchTripped()) {
            trading.markKillSwitch()
            logError(
                "KILL_SWITCH",
                "일일 손실 한도 도달 (당일 ${"%.2f".format(account.dayPnlRate * 100)}%) — 신규 매수를 중단합니다",
                mode.name,
            )
        }

        var evaluated = 0
        var placed = 0
        val errors = mutableListOf<String>()

        for (item in watchList) {
            try {
                val candles = buildCandles(item.symbol)
                if (candles.isEmpty()) {
                    errors += "${item.symbol}: 시세 없음"
                    continue
                }

                val engine = TradingEngine(
                    strategy = StrategyRegistry.create(settings.strategyKind, settings.strategyParams),
                    riskEngine = riskEngine,
                )
                val minutesSince = db.orderCooldownDao().lastOrderAt(item.symbol)?.let {
                    TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - it).toInt()
                }

                val decision = engine.evaluate(item.symbol, candles, account, minutesSince)
                evaluated++

                if (handle(decision, item.symbol, item.name, policy.dryRun, mode.name)) placed++
            } catch (e: Exception) {
                Log.w(TAG, "종목 ${item.symbol} 처리 실패", e)
                errors += "${item.symbol}: ${e.message}"
                logError(item.symbol, "처리 실패: ${e.message}", mode.name)
            }
        }

        return CycleResult(evaluated, placed, errors = errors)
    }

    /**
     * 일봉 이력 + 현재가로 만든 진행 중 봉.
     *
     * 현재가 조회가 실패하면 확정된 일봉만으로 판단한다 — 오래된 값으로 매매하느니
     * 신호가 늦게 나는 편이 안전하다.
     */
    private suspend fun buildCandles(symbol: String): List<Candle> {
        val history = marketData.dailyCandles(symbol, days = HISTORY_DAYS)
        if (history.isEmpty()) return emptyList()

        val quote = runCatching { marketData.quote(symbol) }.getOrNull() ?: return history
        // 거래 정지 등으로 현재가가 0으로 내려오면 봉을 만들 수 없다. 확정 일봉만으로 판단한다.
        if (quote.price <= 0.0) return history

        val last = history.last()
        val liveBar = Candle(
            epochMillis = System.currentTimeMillis(),
            open = if (quote.open > 0) quote.open else quote.price,
            high = maxOf(quote.high, quote.price),
            low = if (quote.low > 0) minOf(quote.low, quote.price) else quote.price,
            close = quote.price,
            volume = quote.volume,
        )
        // 오늘 일봉이 이미 응답에 들어 있으면 중복되므로 마지막 봉을 교체한다.
        return if (isSameDay(last.epochMillis, liveBar.epochMillis)) {
            history.dropLast(1) + liveBar
        } else {
            history + liveBar
        }
    }

    private fun isSameDay(a: Long, b: Long): Boolean =
        TimeUnit.MILLISECONDS.toDays(a + KST_OFFSET) == TimeUnit.MILLISECONDS.toDays(b + KST_OFFSET)

    /** @return 실제로 브로커에 주문을 낸 경우 true. */
    private suspend fun handle(
        decision: Decision,
        symbol: String,
        name: String,
        dryRun: Boolean,
        mode: String,
    ): Boolean {
        val intent = decision.intent
        if (intent == null) {
            // 거절 사유가 있을 때만 기록한다. HOLD를 전부 남기면 로그가 의미를 잃는다.
            if (decision.blockedReasons.isNotEmpty()) {
                db.tradeLogDao().insert(
                    TradeLogEntity(
                        timestamp = System.currentTimeMillis(),
                        symbol = symbol,
                        symbolName = name,
                        action = "BLOCKED",
                        reason = "${decision.signal.reason} → ${decision.blockedReasons.joinToString("; ")}",
                        mode = mode,
                    ),
                )
            }
            return false
        }

        val action = if (intent.side == OrderSide.BUY) "BUY" else "SELL"

        if (dryRun) {
            db.tradeLogDao().insert(
                TradeLogEntity(
                    timestamp = System.currentTimeMillis(),
                    symbol = symbol,
                    symbolName = name,
                    action = action,
                    quantity = intent.quantity,
                    price = intent.price,
                    strategyId = intent.strategyId,
                    reason = "[기록 전용] ${intent.reason}",
                    executed = false,
                    mode = mode,
                ),
            )
            return false
        }

        return try {
            val receipt = trading.placeOrder(intent)
            db.orderCooldownDao().mark(OrderCooldownEntity(symbol, System.currentTimeMillis()))
            db.tradeLogDao().insert(
                TradeLogEntity(
                    timestamp = System.currentTimeMillis(),
                    symbol = symbol,
                    symbolName = name,
                    action = action,
                    quantity = intent.quantity,
                    price = intent.price,
                    strategyId = intent.strategyId,
                    reason = intent.reason,
                    executed = true,
                    orderNo = receipt.orderNo,
                    mode = mode,
                ),
            )
            true
        } catch (e: Exception) {
            db.tradeLogDao().insert(
                TradeLogEntity(
                    timestamp = System.currentTimeMillis(),
                    symbol = symbol,
                    symbolName = name,
                    action = "ERROR",
                    quantity = intent.quantity,
                    price = intent.price,
                    strategyId = intent.strategyId,
                    reason = "주문 실패: ${e.message} (원인 신호: ${intent.reason})",
                    mode = mode,
                ),
            )
            false
        }
    }

    private suspend fun logError(symbol: String, message: String, mode: String) {
        db.tradeLogDao().insert(
            TradeLogEntity(
                timestamp = System.currentTimeMillis(),
                symbol = symbol,
                action = "ERROR",
                reason = message,
                mode = mode,
            ),
        )
    }

    private companion object {
        const val TAG = "AutoTradeRunner"

        /** 지표 워밍업에 충분한 이력. 장기 이동평균 60일도 커버한다. */
        const val HISTORY_DAYS = 180

        /** 한국 표준시 오프셋(+9시간). 날짜 비교용. */
        const val KST_OFFSET = 9 * 60 * 60 * 1000L
    }
}
