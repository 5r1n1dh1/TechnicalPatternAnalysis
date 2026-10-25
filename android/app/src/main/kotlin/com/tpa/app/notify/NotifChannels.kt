package com.tpa.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotifChannels {
    const val TREND = "trend"
    fun create(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(TREND, "Trend changes", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
