package com.phpnuxbill.mobile.feature.auth.data

import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.MaintenanceInfo
import com.phpnuxbill.mobile.core.model.ServerProfile
import com.phpnuxbill.mobile.core.model.UserRole
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.core.network.asObjOrNull
import com.phpnuxbill.mobile.core.network.boolVal
import com.phpnuxbill.mobile.core.network.str
import com.phpnuxbill.mobile.feature.auth.model.LoginResult
import com.phpnuxbill.mobile.feature.auth.model.ServerInfoResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Kontrak Repository Autentikasi (SOLID - ISP & DIP)
 */
interface AuthRepository {
    val maintenanceState: StateFlow<MaintenanceInfo?>
    fun clearMaintenanceState()

    suspend fun probeServer(serverUrl: String): ApiResult<ServerInfoResult>
    suspend fun login(
        serverUrl: String,
        role: UserRole,
        username: String,
        password: String
    ): ApiResult<LoginResult>

    suspend fun switchSavedServer(profileId: String): ServerProfile?
    suspend fun removeSavedServer(profileId: String)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun logout()
}

class RemoteAuthRepository(
    private val networkGateway: NetworkGateway,
    private val sessionStorage: SessionStorage
) : AuthRepository {

    override val maintenanceState: StateFlow<MaintenanceInfo?> = networkGateway.maintenanceState

    override fun clearMaintenanceState() {
        networkGateway.clearMaintenanceState()
    }

    override suspend fun probeServer(serverUrl: String): ApiResult<ServerInfoResult> {
        val cleanUrl = DataStoreSessionStorage.normalizeServerUrl(serverUrl)
        if (cleanUrl.isBlank()) {
            val isEn = sessionStorage.getCurrentSession().languageCode == "en"
            return ApiResult.Error(
                if (isEn) "Please enter the billing server URL first."
                else "Masukkan URL server billing terlebih dahulu."
            )
        }

        return when (val res = networkGateway.get(action = "index", overrideServerUrl = cleanUrl)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val obj = res.data.asObjOrNull()
                val compObj = obj?.getAsJsonObject("company")

                val compName = obj.str("company_name")
                    .ifBlank { compObj.str("name") }
                    .ifBlank { "PHPNuxBill" }
                val currCode = obj.str("currency_code")
                    .ifBlank { compObj.str("currency") }
                    .ifBlank { "Rp." }
                val srvLang = obj.str("language")
                    .ifBlank { compObj.str("language") }
                    .ifBlank { "indonesia" }
                val logoUrl = obj.str("logo_url")
                    .ifBlank { compObj.str("logo_url") }
                val version = obj.str("version").ifBlank { "3.1.0" }
                val maintMode = obj.boolVal("maintenance_mode") || compObj.boolVal("maintenance_mode")
                val maintLogout = obj.boolVal("maintenance_mode_logout") || compObj.boolVal("maintenance_mode_logout")
                val maintDate = obj.str("maintenance_date").ifBlank { compObj.str("maintenance_date") }
                val compPhone = compObj.str("phone").ifBlank { obj.str("phone") }

                sessionStorage.updateServerPreview(
                    serverUrl = cleanUrl,
                    companyName = compName,
                    currencyCode = currCode,
                    serverLogoUrl = logoUrl,
                    serverLanguage = srvLang
                )

                ApiResult.Success(
                    data = ServerInfoResult(
                        serverUrl = cleanUrl,
                        companyName = compName,
                        currencyCode = currCode,
                        language = srvLang,
                        logoUrl = logoUrl,
                        apiVersion = version,
                        maintenanceMode = maintMode,
                        maintenanceForceLogout = maintLogout,
                        maintenanceDate = maintDate,
                        companyPhone = compPhone
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun login(
        serverUrl: String,
        role: UserRole,
        username: String,
        password: String
    ): ApiResult<LoginResult> {
        val cleanUrl = DataStoreSessionStorage.normalizeServerUrl(serverUrl)
        val currentSession = sessionStorage.getCurrentSession()
        val isEn = currentSession.languageCode == "en"

        if (cleanUrl.isBlank()) {
            return ApiResult.Error(
                if (isEn) "Server URL is required." else "URL Server wajib diisi."
            )
        }
        if (username.isBlank() || password.isBlank()) {
            return ApiResult.Error(
                if (isEn) "Username and password are required." else "Username dan kata sandi wajib diisi."
            )
        }

        // Ambil metadata server (logo_url & CompanyName) jika belum ter-cache untuk server ini
        var cachedLogoUrl = if (DataStoreSessionStorage.normalizeServerUrl(currentSession.activeServerUrl) == cleanUrl) {
            currentSession.serverLogoUrl
        } else {
            currentSession.savedServers.find {
                DataStoreSessionStorage.normalizeServerUrl(it.serverUrl) == cleanUrl
            }?.serverLogoUrl.orEmpty()
        }

        if (cachedLogoUrl.isBlank()) {
            val probe = probeServer(cleanUrl)
            if (probe is ApiResult.Success) {
                cachedLogoUrl = probe.data.logoUrl
            }
        }

        val response = networkGateway.postForm(
            action = "auth",
            fields = mapOf(
                "role" to role.apiValue,
                "username" to username.trim(),
                "password" to password
            ),
            overrideServerUrl = cleanUrl
        )

        return when (response) {
            is ApiResult.Error -> response
            is ApiResult.Success -> {
                val obj = response.data.asObjOrNull()
                val token = obj.str("token").ifBlank { obj.str("native_token") }
                if (token.isBlank()) {
                    return ApiResult.Error(
                        if (isEn) "Invalid token response from server."
                        else "Server tidak mengembalikan token sesi yang valid."
                    )
                }

                val compObj = obj?.getAsJsonObject("company")
                val compName = obj.str("company_name")
                    .ifBlank { compObj.str("name") }
                    .ifBlank { "PHPNuxBill" }
                val currCode = obj.str("currency_code")
                    .ifBlank { compObj.str("currency") }
                    .ifBlank { "Rp." }
                val logoUrl = obj.str("logo_url")
                    .ifBlank { compObj.str("logo_url") }
                    .ifBlank { cachedLogoUrl }

                val userObj = if (role == UserRole.ADMIN) {
                    obj?.getAsJsonObject("admin")
                } else {
                    obj?.getAsJsonObject("customer")
                }

                val resolvedUsername = userObj.str("username").ifBlank { username.trim() }
                val resolvedFullname = userObj.str("fullname").ifBlank { resolvedUsername }
                val resolvedUserType = if (role == UserRole.ADMIN) {
                    userObj.str("user_type").ifBlank { "Admin" }
                } else {
                    "Customer"
                }

                sessionStorage.saveAuthenticatedSession(
                    serverUrl = cleanUrl,
                    token = token,
                    role = role,
                    username = resolvedUsername,
                    fullname = resolvedFullname,
                    userType = resolvedUserType,
                    companyName = compName,
                    currencyCode = currCode,
                    serverLogoUrl = logoUrl
                )

                ApiResult.Success(
                    data = LoginResult(
                        token = token,
                        role = role,
                        username = resolvedUsername,
                        fullname = resolvedFullname,
                        userType = resolvedUserType,
                        companyName = compName,
                        currencyCode = currCode,
                        logoUrl = logoUrl
                    ),
                    message = response.message
                )
            }
        }
    }

    override suspend fun switchSavedServer(profileId: String): ServerProfile? {
        networkGateway.clearMaintenanceState()
        return sessionStorage.switchSavedServer(profileId)
    }

    override suspend fun removeSavedServer(profileId: String) {
        sessionStorage.removeSavedServer(profileId)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        sessionStorage.setLanguage(language.code)
    }

    override suspend fun logout() {
        networkGateway.clearMaintenanceState()
        sessionStorage.clearActiveToken()
    }
}
