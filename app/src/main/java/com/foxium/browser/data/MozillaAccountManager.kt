package com.foxium.browser.data

import android.content.Context
import android.content.SharedPreferences

data class MozillaUserProfile(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val syncEnabled: Boolean = true
)

class MozillaAccountManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mozilla_account_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MOZILLA_AUTH_URL = "https://accounts.firefox.com/signin?context=fx_desktop_v3&service=sync"
        const val MOZILLA_SETTINGS_URL = "https://accounts.firefox.com/settings"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_SYNC_ENABLED = "key_sync_enabled"
    }

    fun getProfile(): MozillaUserProfile {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val syncEnabled = prefs.getBoolean(KEY_SYNC_ENABLED, true)
        return MozillaUserProfile(isLoggedIn, email, syncEnabled)
    }

    fun login(email: String = "firefox.user@mozilla.org") {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_SYNC_ENABLED, true)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_EMAIL, "")
            .putBoolean(KEY_SYNC_ENABLED, false)
            .apply()
    }
}
