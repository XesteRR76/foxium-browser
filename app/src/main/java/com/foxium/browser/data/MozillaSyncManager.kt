package com.foxium.browser.data

import android.content.Context
import android.content.SharedPreferences
import com.foxium.browser.sync.FirefoxSyncClient
import com.foxium.browser.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MozillaSyncManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mozilla_sync_prefs", Context.MODE_PRIVATE)
    private val syncClient = FirefoxSyncClient()

    companion object {
        const val PREF_TOOLBAR_POSITION_TOP = "pref_toolbar_position_top"
    }

    var isToolbarAtTop: Boolean
        get() = prefs.getBoolean(PREF_TOOLBAR_POSITION_TOP, false)
        set(value) = prefs.edit().putBoolean(PREF_TOOLBAR_POSITION_TOP, value).apply()

    // Real synced devices and tabs (Starts EMPTY — no fake data!)
    private val _syncedDevices = MutableStateFlow<List<SyncedDeviceTabs>>(emptyList())
    val syncedDevices: StateFlow<List<SyncedDeviceTabs>> = _syncedDevices

    // Real synced saved credentials (Starts EMPTY — no fake data!)
    private val _syncedCredentials = MutableStateFlow<List<SyncedCredential>>(emptyList())
    val syncedCredentials: StateFlow<List<SyncedCredential>> = _syncedCredentials

    suspend fun performRealSync(
        email: String,
        password: String,
        twoFactorCode: String? = null
    ): SyncResult {
        val result = syncClient.authenticateAndSync(email, password, twoFactorCode)
        if (result is SyncResult.Success) {
            _syncedDevices.value = result.devices
            _syncedCredentials.value = result.credentials
        }
        return result
    }

    fun removeCredential(id: String) {
        _syncedCredentials.value = _syncedCredentials.value.filter { it.id != id }
    }

    fun clearAll() {
        _syncedDevices.value = emptyList()
        _syncedCredentials.value = emptyList()
    }
}
