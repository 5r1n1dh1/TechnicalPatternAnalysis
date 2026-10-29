package com.tpa.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val TREND_WORK = "trend-check"
private const val PREFS = "tpa_settings"
private const val KEY_NOTIFICATIONS = "notif_enabled"

fun notificationsEnabled(ctx: Context): Boolean =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_NOTIFICATIONS, true)

/** Persists the preference and keeps the periodic worker in sync with it. */
fun setNotificationsEnabled(ctx: Context, enabled: Boolean) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    if (enabled) setupTrendWorker(ctx) else cancelTrendWorker(ctx)
}

fun setupTrendWorker(ctx: Context) {
    if (!notificationsEnabled(ctx)) return
    val work = PeriodicWorkRequestBuilder<TrendCheckWorker>(60, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
        TREND_WORK,
        ExistingPeriodicWorkPolicy.KEEP,
        work
    )
}

fun cancelTrendWorker(ctx: Context) {
    WorkManager.getInstance(ctx).cancelUniqueWork(TREND_WORK)
}
