package com.sk.autotrader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sk.autotrader.MainActivity
import com.sk.autotrader.R

/** 포그라운드 알림과 매매 알림을 만든다. */
object TradingNotifications {

    const val CHANNEL_SERVICE = "trading_service"
    const val CHANNEL_ALERT = "trading_alert"
    const val NOTIFICATION_ID_SERVICE = 1001
    private var alertId = 2000

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.channel_trading_name),
                // 상시 표시되는 알림이라 소리가 나면 방해가 된다.
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_trading_desc) },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                context.getString(R.string.channel_alert_name),
                // 주문 체결과 손절은 즉시 알아야 한다.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_alert_desc) },
        )
    }

    fun serviceNotification(context: Context, status: String, detail: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            context,
            1,
            Intent(context, TradingService::class.java).setAction(TradingService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(status)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "중지", stop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun alert(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(alertId++, notification)
    }
}
