package com.tpa.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class ThemeViewModel(context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("tpa_settings", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(
        when (prefs.getString("theme_mode", "system")) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    )
    val mode: StateFlow<ThemeMode> = _mode

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name.lowercase()).apply()
        _mode.value = mode
    }
}
