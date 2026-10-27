package com.foxium.browser.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MozillaSyncManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mozilla_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_TOOLBAR_POSITION_TOP = "pref_toolbar_position_top"
    }

    var isToolbarAtTop: Boolean
        get() = prefs.getBoolean(PREF_TOOLBAR_POSITION_TOP, false)
        set(value) = prefs.edit().putBoolean(PREF_TOOLBAR_POSITION_TOP, value).apply()

    // Default sample synced devices and tabs from Mozilla Sync
    private val _syncedDevices = MutableStateFlow<List<SyncedDeviceTabs>>(
        listOf(
            SyncedDeviceTabs(
                deviceName = "Firefox on PC (Windows 11)",
                deviceType = "desktop",
                lastActive = "5 минут назад",
                tabs = listOf(
                    SyncedTab(title = "GitHub: Where the world builds software", url = "https://github.com"),
                    SyncedTab(title = "Habr — Все публикации", url = "https://habr.com"),
                    SyncedTab(title = "MDN Web Docs: Web technology for developers", url = "https://developer.mozilla.org"),
                    SyncedTab(title = "Android Developers Blog", url = "https://android-developers.googleblog.com")
                )
            ),
            SyncedDeviceTabs(
                deviceName = "MacBook Air (macOS Sonoma)",
                deviceType = "desktop",
                lastActive = "2 часа назад",
                tabs = listOf(
                    SyncedTab(title = "YouTube — Видеохостинг", url = "https://youtube.com"),
                    SyncedTab(title = "Reddit — Dive into anything", url = "https://reddit.com"),
                    SyncedTab(title = "DuckDuckGo Privacy Browser", url = "https://duckduckgo.com")
                )
            )
        )
    )
    val syncedDevices: StateFlow<List<SyncedDeviceTabs>> = _syncedDevices

    // Synced saved logins & passwords from Firefox Lockwise
    private val _syncedCredentials = MutableStateFlow<List<SyncedCredential>>(
        listOf(
            SyncedCredential(
                domain = "github.com",
                username = "kostya@developer.io",
                password = "ghp_secureToken123$",
                lastModified = "Вчера"
            ),
            SyncedCredential(
                domain = "accounts.firefox.com",
                username = "kostya@firefox.org",
                password = "FxSync#SecretPass2026",
                lastModified = "3 дня назад"
            ),
            SyncedCredential(
                domain = "habr.com",
                username = "xesterr76",
                password = "habr_ultra_pass_99",
                lastModified = "1 неделю назад"
            ),
            SyncedCredential(
                domain = "reddit.com",
                username = "xesterr76_reddit",
                password = "Redd!tUser2026_Secure",
                lastModified = "2 недели назад"
            ),
            SyncedCredential(
                domain = "ya.ru",
                username = "kostya.dev@yandex.ru",
                password = "Yandex#KeyWord2026",
                lastModified = "В прошлом месяце"
            )
        )
    )
    val syncedCredentials: StateFlow<List<SyncedCredential>> = _syncedCredentials

    fun triggerSync() {
        // Simulates instant fresh sync update with timestamp
        val current = _syncedDevices.value.toMutableList()
        if (current.isNotEmpty()) {
            val updatedFirst = current[0].copy(lastActive = "Только что")
            current[0] = updatedFirst
            _syncedDevices.value = current
        }
    }

    fun removeCredential(id: String) {
        _syncedCredentials.value = _syncedCredentials.value.filter { it.id != id }
    }
}
