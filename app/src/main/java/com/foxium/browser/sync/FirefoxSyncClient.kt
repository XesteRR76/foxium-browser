package com.foxium.browser.sync

import android.util.Base64
import com.foxium.browser.data.SyncedCredential
import com.foxium.browser.data.SyncedDeviceTabs
import com.foxium.browser.data.SyncedTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(
        val devices: List<SyncedDeviceTabs>,
        val credentials: List<SyncedCredential>
    ) : SyncResult()

    data class Error(val message: String) : SyncResult()
}

class FirefoxSyncClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val FXA_AUTH_URL = "https://api.accounts.firefox.com/v1"
        private const val TOKEN_SERVER_URL = "https://token.services.mozilla.com/1.0/sync/1.5"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun authenticateAndSync(
        email: String,
        password: String,
        twoFactorCode: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Derive authPW & unwrapBKey using quickStretch
            val stretched = CryptoUtils.quickStretch(password, email.lowercase().trim())
            val authPW = CryptoUtils.deriveAuthPW(stretched)
            val unwrapBKey = CryptoUtils.deriveUnwrapBKey(stretched)

            // 2. Request FxA Login
            val loginPayload = JSONObject().apply {
                put("email", email.lowercase().trim())
                put("authPW", authPW)
                if (!twoFactorCode.isNullOrBlank()) {
                    put("code", twoFactorCode.trim())
                }
            }

            val loginRequest = Request.Builder()
                .url("$FXA_AUTH_URL/account/login?keys=true")
                .post(loginPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val loginResponse = client.newCall(loginRequest).execute()
            val loginBody = loginResponse.body?.string() ?: ""

            if (!loginResponse.isSuccessful) {
                return@withContext parseLoginError(loginBody, loginResponse.code)
            }

            val loginJson = JSONObject(loginBody)
            val uid = loginJson.optString("uid")
            val sessionToken = loginJson.optString("sessionToken")
            val keyFetchToken = loginJson.optString("keyFetchToken")
            val verified = loginJson.optBoolean("verified", true)

            if (!verified) {
                return@withContext SyncResult.Error("Email аккаунта Mozilla не подтвержден. Проверьте почту для подтверждения.")
            }

            // 3. Obtain Sync Keys
            val rawKeyFetchToken = CryptoUtils.hexToBytes(keyFetchToken)
            val hawkAuth = CryptoUtils.generateHawkHeader(
                id = keyFetchToken,
                key = rawKeyFetchToken,
                method = "GET",
                path = "/v1/account/keys",
                host = "api.accounts.firefox.com"
            )

            val keysRequest = Request.Builder()
                .url("$FXA_AUTH_URL/account/keys")
                .header("Authorization", hawkAuth)
                .get()
                .build()

            val keysResponse = client.newCall(keysRequest).execute()
            val keysBody = keysResponse.body?.string() ?: ""

            if (!keysResponse.isSuccessful) {
                return@withContext SyncResult.Error("Не удалось получить ключи шифрования Mozilla: HTTP ${keysResponse.code}")
            }

            val keysJson = JSONObject(keysBody)
            val bundleHex = keysJson.optString("bundle")
            val rawBundle = CryptoUtils.hexToBytes(bundleHex)

            // Unwrap kB with unwrapBKey using AES-256-CBC
            val iv = rawBundle.copyOfRange(0, 16)
            val ciphertext = rawBundle.copyOfRange(16, rawBundle.size)
            val unwrappedKeys = CryptoUtils.decryptAes256Cbc(ciphertext, iv, unwrapBKey)

            // kB is the second 32-byte chunk of the 64-byte unwrapped bundle
            val kB = unwrappedKeys.copyOfRange(32, 64)

            // 4. Derive oldsync syncKey from kB using HKDF
            val syncKey = CryptoUtils.hkdf(
                ikm = kB,
                salt = ByteArray(0),
                info = "identity.mozilla.com/picl/v1/oldsync".toByteArray(StandardCharsets.UTF_8),
                length = 64
            )
            val syncCryptKey = syncKey.copyOfRange(0, 32)
            val syncHmacKey = syncKey.copyOfRange(32, 64)

            // 5. Contact Token Server for Storage endpoint
            val tokenHawkAuth = CryptoUtils.generateHawkHeader(
                id = sessionToken,
                key = CryptoUtils.hexToBytes(sessionToken),
                method = "GET",
                path = "/1.0/sync/1.5",
                host = "token.services.mozilla.com"
            )

            val tokenRequest = Request.Builder()
                .url(TOKEN_SERVER_URL)
                .header("Authorization", tokenHawkAuth)
                .get()
                .build()

            val tokenResponse = client.newCall(tokenRequest).execute()
            val tokenBody = tokenResponse.body?.string() ?: ""

            if (!tokenResponse.isSuccessful) {
                return@withContext SyncResult.Error("Ошибка сервера токенов Mozilla Sync: HTTP ${tokenResponse.code}")
            }

            val tokenJson = JSONObject(tokenBody)
            val storageEndpoint = tokenJson.optString("endpoint")
            val tokenKey = tokenJson.optString("key")
            val rawTokenKey = tokenKey.toByteArray(StandardCharsets.UTF_8)

            val parsedEndpoint = URL(storageEndpoint)

            // 6. Fetch and decrypt Bulk Crypto Keys
            val cryptoKeysHawk = CryptoUtils.generateHawkHeader(
                id = tokenJson.optString("id"),
                key = rawTokenKey,
                method = "GET",
                path = parsedEndpoint.path + "/storage/crypto/keys",
                host = parsedEndpoint.host
            )

            val cryptoKeysRequest = Request.Builder()
                .url("$storageEndpoint/storage/crypto/keys")
                .header("Authorization", cryptoKeysHawk)
                .get()
                .build()

            val cryptoKeysResponse = client.newCall(cryptoKeysRequest).execute()
            val cryptoKeysBody = cryptoKeysResponse.body?.string() ?: ""

            var defaultBulkCryptKey = syncCryptKey
            if (cryptoKeysResponse.isSuccessful && cryptoKeysBody.isNotBlank()) {
                val keysObj = JSONObject(cryptoKeysBody)
                val payloadStr = keysObj.optString("payload")
                if (payloadStr.isNotBlank()) {
                    val decryptedBulkKeys = decryptBsoPayload(payloadStr, syncCryptKey)
                    val bulkJson = JSONObject(decryptedBulkKeys)
                    val defaultKeysArray = bulkJson.optJSONArray("default")
                    if (defaultKeysArray != null && defaultKeysArray.length() >= 2) {
                        defaultBulkCryptKey = Base64.decode(defaultKeysArray.getString(0), Base64.DEFAULT)
                    }
                }
            }

            // 7. Fetch Tabs
            val tabsHawk = CryptoUtils.generateHawkHeader(
                id = tokenJson.optString("id"),
                key = rawTokenKey,
                method = "GET",
                path = parsedEndpoint.path + "/storage/tabs",
                host = parsedEndpoint.host
            )
            val tabsRequest = Request.Builder()
                .url("$storageEndpoint/storage/tabs")
                .header("Authorization", tabsHawk)
                .get()
                .build()

            val tabsResponse = client.newCall(tabsRequest).execute()
            val tabsBody = tabsResponse.body?.string() ?: "[]"
            val devicesList = parseTabsResponse(tabsBody, defaultBulkCryptKey)

            // 8. Fetch Passwords
            val passwordsHawk = CryptoUtils.generateHawkHeader(
                id = tokenJson.optString("id"),
                key = rawTokenKey,
                method = "GET",
                path = parsedEndpoint.path + "/storage/passwords",
                host = parsedEndpoint.host
            )
            val passwordsRequest = Request.Builder()
                .url("$storageEndpoint/storage/passwords")
                .header("Authorization", passwordsHawk)
                .get()
                .build()

            val passwordsResponse = client.newCall(passwordsRequest).execute()
            val passwordsBody = passwordsResponse.body?.string() ?: "[]"
            val credentialsList = parsePasswordsResponse(passwordsBody, defaultBulkCryptKey)

            SyncResult.Success(devicesList, credentialsList)

        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Неизвестная ошибка синхронизации с Mozilla")
        }
    }

    private fun decryptBsoPayload(payloadJsonStr: String, bulkKey: ByteArray): String {
        val payloadObj = JSONObject(payloadJsonStr)
        val ciphertextBase64 = payloadObj.optString("ciphertext")
        val ivBase64 = payloadObj.optString("IV")

        val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)

        val decrypted = CryptoUtils.decryptAes256Cbc(ciphertext, iv, bulkKey)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    private fun parseTabsResponse(body: String, bulkKey: ByteArray): List<SyncedDeviceTabs> {
        val result = mutableListOf<SyncedDeviceTabs>()
        try {
            val array = JSONArray(body)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val payloadEncrypted = item.optString("payload")
                if (payloadEncrypted.isNotBlank()) {
                    val decrypted = decryptBsoPayload(payloadEncrypted, bulkKey)
                    val deviceObj = JSONObject(decrypted)
                    val clientName = deviceObj.optString("clientName", "Firefox Device")
                    val rawTabs = deviceObj.optJSONArray("tabs") ?: JSONArray()
                    val tabsList = mutableListOf<SyncedTab>()

                    for (j in 0 until rawTabs.length()) {
                        val t = rawTabs.getJSONObject(j)
                        val title = t.optString("title", "Вкладка")
                        val urlHistory = t.optJSONArray("urlHistory")
                        val url = if (urlHistory != null && urlHistory.length() > 0) {
                            urlHistory.getString(urlHistory.length() - 1)
                        } else {
                            t.optString("url", "about:blank")
                        }
                        tabsList.add(SyncedTab(title = title, url = url))
                    }

                    if (tabsList.isNotEmpty()) {
                        result.add(
                            SyncedDeviceTabs(
                                deviceName = clientName,
                                lastActive = "Синхронизировано",
                                tabs = tabsList
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun parsePasswordsResponse(body: String, bulkKey: ByteArray): List<SyncedCredential> {
        val result = mutableListOf<SyncedCredential>()
        try {
            val array = JSONArray(body)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val payloadEncrypted = item.optString("payload")
                if (payloadEncrypted.isNotBlank()) {
                    val decrypted = decryptBsoPayload(payloadEncrypted, bulkKey)
                    val passObj = JSONObject(decrypted)
                    val hostname = passObj.optString("hostname")
                    val username = passObj.optString("username")
                    val password = passObj.optString("password")

                    if (hostname.isNotBlank() && (username.isNotBlank() || password.isNotBlank())) {
                        result.add(
                            SyncedCredential(
                                domain = hostname.removePrefix("https://").removePrefix("http://"),
                                username = username,
                                password = password,
                                lastModified = "Mozilla Sync"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun parseLoginError(body: String, code: Int): SyncResult.Error {
        try {
            val json = JSONObject(body)
            val msg = json.optString("message")
            val errno = json.optInt("errno")

            if (errno == 105 || msg.contains("two-factor", ignoreCase = true)) {
                return SyncResult.Error("Для аккаунта требуется код двухфакторной аутентификации (2FA).")
            }
            if (msg.isNotBlank()) {
                return SyncResult.Error("Mozilla: $msg")
            }
        } catch (_: Exception) {}

        return SyncResult.Error(
            when (code) {
                400 -> "Неверный email или пароль аккаунта Mozilla."
                401 -> "Ошибка авторизации в Mozilla Accounts."
                429 -> "Слишком много попыток входа. Попробуйте через пару минут."
                else -> "Ошибка сервера Mozilla: HTTP $code"
            }
        )
    }
}
