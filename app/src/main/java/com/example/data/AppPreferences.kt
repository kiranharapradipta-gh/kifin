package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kifin_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "LIGHT", "DARK"
    }

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun isPinConfigured(): Boolean {
        val pin = prefs.getString(KEY_PIN_CODE, "") ?: ""
        return pin.isNotEmpty()
    }

    fun verifyPin(input: String): Boolean {
        val pin = prefs.getString(KEY_PIN_CODE, "") ?: ""
        return pin == input
    }

    fun setPin(pin: String) {
        prefs.edit()
            .putString(KEY_PIN_CODE, pin)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
        _isPinEnabled.value = true
    }

    fun disablePin() {
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
        _isPinEnabled.value = false
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
        _isPinEnabled.value = enabled
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }
}
