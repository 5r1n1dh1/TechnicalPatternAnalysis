package com.tpa.app.notify

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tpa.app.R

class TrendCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs: SharedPreferences = applicationContext.getSharedPreferences("tpa_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notif_enabled", true)) return Result.success()
        NotifChannels.create(applicationContext)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(applicationContext, NotifChannels.TREND)
            .setContentTitle("Trend update")
            .setContentText("Watchlist trend changed.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setGroup("trend_group")
            .build()
        nm.notify(1, n)
        return Result.success()
    }
}
