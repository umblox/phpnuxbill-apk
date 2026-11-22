package com.phpnuxbill.mobile.core.datastore

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.ServerProfile
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val Context.billingDataStore: DataStore<Preferences> by preferencesDataStore(name = "phpnuxbill_session_prefs")

/**
 * Kontrak penyimpanan sesi & preferensi aplikasi (ISP - Interface Segregation Principle)
 */
interface SessionStorage {
    val sessionFlow: Flow<SessionState>
    suspend fun getCurrentSession(): SessionState
    suspend fun updateServerPreview(
        serverUrl: String,
        companyName: String,
        currencyCode: String,
        serverLogoUrl: String,
        serverLanguage: String? = null
    )
    suspend fun saveAuthenticatedSession(
        serverUrl: String,
        token: String,
        role: UserRole,
        username: String,
        fullname: String,
        userType: String,
        companyName: String,
        currencyCode: String,
        serverLogoUrl: String
    )
    suspend fun switchSavedServer(profileId: String): ServerProfile?
    suspend fun removeSavedServer(profileId: String)
    suspend fun setLanguage(languageCode: String)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun saveCustomLogoFromUri(uri: Uri): String?
    suspend fun clearCustomLogo()
    suspend fun clearActiveToken()
}

class DataStoreSessionStorage(
    private val context: Context,
    private val gson: Gson = Gson()
) : SessionStorage {

    private object Keys {
        val ACTIVE_SERVER_URL = stringPreferencesKey("active_server_url")
        val TOKEN = stringPreferencesKey("auth_token")
        val ROLE = stringPreferencesKey("user_role")
        val USERNAME = stringPreferencesKey("username")
        val FULLNAME = stringPreferencesKey("fullname")
        val USER_TYPE = stringPreferencesKey("user_type")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val SERVER_LOGO_URL = stringPreferencesKey("server_logo_url")
        val CUSTOM_LOCAL_LOGO = stringPreferencesKey("custom_local_logo_path")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SAVED_SERVERS_JSON = stringPreferencesKey("saved_servers_json")
    }

    override val sessionFlow: Flow<SessionState> = context.billingDataStore.data.map { prefs ->
        val rawServers = prefs[Keys.SAVED_SERVERS_JSON].orEmpty()
        val servers = parseServers(rawServers)
        val localLogo = prefs[Keys.CUSTOM_LOCAL_LOGO].orEmpty()
        val validLocalLogo = if (localLogo.isNotBlank() && File(localLogo).exists()) localLogo else ""

        SessionState(
            activeServerUrl = prefs[Keys.ACTIVE_SERVER_URL].orEmpty(),
            token = prefs[Keys.TOKEN].orEmpty(),
            role = UserRole.fromString(prefs[Keys.ROLE] ?: "admin"),
            username = prefs[Keys.USERNAME].orEmpty(),
            fullname = prefs[Keys.FULLNAME].orEmpty(),
            userType = prefs[Keys.USER_TYPE].orEmpty(),
            companyName = prefs[Keys.COMPANY_NAME].orEmpty().ifBlank { "PHPNuxBill" },
            currencyCode = prefs[Keys.CURRENCY_CODE].orEmpty().ifBlank { "Rp." },
            serverLogoUrl = prefs[Keys.SERVER_LOGO_URL].orEmpty(),
            customLocalLogoPath = validLocalLogo,
            languageCode = prefs[Keys.LANGUAGE_CODE].orEmpty().ifBlank { "id" },
            themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE] ?: "system"),
            savedServers = servers
        )
    }

    override suspend fun getCurrentSession(): SessionState = sessionFlow.first()

    override suspend fun updateServerPreview(
        serverUrl: String,
        companyName: String,
        currencyCode: String,
        serverLogoUrl: String,
        serverLanguage: String?
    ) {
        val cleanUrl = normalizeServerUrl(serverUrl)
        context.billingDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SERVER_URL] = cleanUrl
            if (companyName.isNotBlank()) {
                prefs[Keys.COMPANY_NAME] = companyName.trim()
            }
            if (currencyCode.isNotBlank()) {
                prefs[Keys.CURRENCY_CODE] = currencyCode.trim()
            }
            prefs[Keys.SERVER_LOGO_URL] = serverLogoUrl.trim()

            // Sinkronkan bahasa awal server jika pengguna belum memilih bahasa manual
            if (prefs[Keys.LANGUAGE_CODE].isNullOrBlank() && !serverLanguage.isNullOrBlank()) {
                prefs[Keys.LANGUAGE_CODE] = if (serverLanguage.startsWith("en", ignoreCase = true)) "en" else "id"
            }

            val existing = parseServers(prefs[Keys.SAVED_SERVERS_JSON].orEmpty()).toMutableList()
            val idx = existing.indexOfFirst { normalizeServerUrl(it.serverUrl) == cleanUrl }
            if (idx >= 0) {
                val old = existing[idx]
                existing[idx] = old.copy(
                    companyName = companyName.ifBlank { old.companyName },
                    currencyCode = currencyCode.ifBlank { old.currencyCode },
                    serverLogoUrl = serverLogoUrl.ifBlank { old.serverLogoUrl },
                    lastUsedAt = System.currentTimeMillis()
                )
            } else if (cleanUrl.isNotBlank()) {
                existing.add(
                    0,
                    ServerProfile(
                        id = UUID.randomUUID().toString(),
                        serverUrl = cleanUrl,
                        companyName = companyName.ifBlank { "PHPNuxBill" },
                        currencyCode = currencyCode.ifBlank { "Rp." },
                        serverLogoUrl = serverLogoUrl
                    )
                )
            }
            prefs[Keys.SAVED_SERVERS_JSON] = gson.toJson(existing)
        }
    }

    override suspend fun saveAuthenticatedSession(
        serverUrl: String,
        token: String,
        role: UserRole,
        username: String,
        fullname: String,
        userType: String,
        companyName: String,
        currencyCode: String,
        serverLogoUrl: String
    ) {
        val cleanUrl = normalizeServerUrl(serverUrl)
        context.billingDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SERVER_URL] = cleanUrl
            prefs[Keys.TOKEN] = token.trim()
            prefs[Keys.ROLE] = role.apiValue
            prefs[Keys.USERNAME] = username.trim()
            prefs[Keys.FULLNAME] = fullname.trim().ifBlank { username.trim() }
            prefs[Keys.USER_TYPE] = userType.trim()
            if (companyName.isNotBlank()) prefs[Keys.COMPANY_NAME] = companyName.trim()
            if (currencyCode.isNotBlank()) prefs[Keys.CURRENCY_CODE] = currencyCode.trim()
            if (serverLogoUrl.isNotBlank()) prefs[Keys.SERVER_LOGO_URL] = serverLogoUrl.trim()

            val existing = parseServers(prefs[Keys.SAVED_SERVERS_JSON].orEmpty()).toMutableList()
            val idx = existing.indexOfFirst {
                normalizeServerUrl(it.serverUrl) == cleanUrl &&
                    (it.username.isBlank() || it.username.equals(username, ignoreCase = true))
            }
            val updatedProfile = ServerProfile(
                id = if (idx >= 0) existing[idx].id else UUID.randomUUID().toString(),
                serverUrl = cleanUrl,
                companyName = companyName.ifBlank { prefs[Keys.COMPANY_NAME] ?: "PHPNuxBill" },
                currencyCode = currencyCode.ifBlank { prefs[Keys.CURRENCY_CODE] ?: "Rp." },
                serverLogoUrl = serverLogoUrl.ifBlank { prefs[Keys.SERVER_LOGO_URL].orEmpty() },
                username = username.trim(),
                role = role,
                token = token.trim(),
                lastUsedAt = System.currentTimeMillis()
            )
            if (idx >= 0) {
                existing[idx] = updatedProfile
            } else {
                existing.add(0, updatedProfile)
            }
            prefs[Keys.SAVED_SERVERS_JSON] = gson.toJson(existing)
        }
    }

    override suspend fun switchSavedServer(profileId: String): ServerProfile? {
        val current = getCurrentSession()
        val target = current.savedServers.find { it.id == profileId } ?: return null
        context.billingDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SERVER_URL] = normalizeServerUrl(target.serverUrl)
            prefs[Keys.COMPANY_NAME] = target.companyName.ifBlank { "PHPNuxBill" }
            prefs[Keys.CURRENCY_CODE] = target.currencyCode.ifBlank { "Rp." }
            prefs[Keys.SERVER_LOGO_URL] = target.serverLogoUrl
            prefs[Keys.USERNAME] = target.username
            prefs[Keys.ROLE] = target.role.apiValue
            prefs[Keys.TOKEN] = target.token
        }
        return target
    }

    override suspend fun removeSavedServer(profileId: String) {
        context.billingDataStore.edit { prefs ->
            val updated = parseServers(prefs[Keys.SAVED_SERVERS_JSON].orEmpty())
                .filterNot { it.id == profileId }
            prefs[Keys.SAVED_SERVERS_JSON] = gson.toJson(updated)
        }
    }

    override suspend fun setLanguage(languageCode: String) {
        val normalized = if (languageCode.equals("en", ignoreCase = true)) "en" else "id"
        context.billingDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = normalized
        }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        context.billingDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.key
        }
    }

    override suspend fun saveCustomLogoFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.filesDir, "custom_brand_logo.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (destFile.exists() && destFile.length() > 0L) {
                context.billingDataStore.edit { prefs ->
                    prefs[Keys.CUSTOM_LOCAL_LOGO] = destFile.absolutePath
                }
                destFile.absolutePath
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun clearCustomLogo() {
        withContext(Dispatchers.IO) {
            val destFile = File(context.filesDir, "custom_brand_logo.png")
            if (destFile.exists()) {
                destFile.delete()
            }
        }
        context.billingDataStore.edit { prefs ->
            prefs.remove(Keys.CUSTOM_LOCAL_LOGO)
        }
    }

    override suspend fun clearActiveToken() {
        context.billingDataStore.edit { prefs ->
            val currentUrl = prefs[Keys.ACTIVE_SERVER_URL].orEmpty()
            val currentUser = prefs[Keys.USERNAME].orEmpty()
            prefs.remove(Keys.TOKEN)

            val updatedServers = parseServers(prefs[Keys.SAVED_SERVERS_JSON].orEmpty()).map {
                if (normalizeServerUrl(it.serverUrl) == currentUrl && it.username == currentUser) {
                    it.copy(token = "")
                } else {
                    it
                }
            }
            prefs[Keys.SAVED_SERVERS_JSON] = gson.toJson(updatedServers)
        }
    }

    private fun parseServers(json: String): List<ServerProfile> {
        if (json.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<ServerProfile>>() {}.type
            gson.fromJson<List<ServerProfile>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun normalizeServerUrl(raw: String): String {
            var url = raw.trim()
            if (url.isEmpty()) return ""
            if (!url.startsWith("http://", ignoreCase = true) &&
                !url.startsWith("https://", ignoreCase = true)
            ) {
                url = "https://$url"
            }
            return url
                .replace(Regex("/system/api\\.php.*$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("/index\\.php.*$", RegexOption.IGNORE_CASE), "")
                .trimEnd('/')
        }
    }
}
