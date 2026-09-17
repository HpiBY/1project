package com.example.ozontracker.data

import android.content.Context

object SettingsRepository {
    private const val PREFS_NAME = "settings"
    private const val KEY_INTERVAL_HOURS = "interval_hours"
    private const val DEFAULT_INTERVAL_HOURS = 3L

    fun getIntervalHours(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS)
    }

    fun setIntervalHours(context: Context, hours: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_INTERVAL_HOURS, hours).apply()
    }
}
