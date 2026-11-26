package com.phpnuxbill.mobile.core.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Informasi resmi Mode Maintenance (HTTP 503 JSON dari helpers_common.php v3.1.0)
 */
data class MaintenanceInfo(
    val forceLogout: Boolean = false,
    val maintenanceDate: String = "",
    val companyName: String = "PHPNuxBill",
    val companyPhone: String = "",
    val logoUrl: String = "",
    val message: String = "",
    val serverUrl: String = ""
)

/**
 * Envelope respons standar dari PHPNuxBill Mobile API (api_out)
 */
data class RawApiEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("result") val result: JsonElement? = null,
    @SerializedName("data") val data: JsonElement? = null
) {
    val isSuccess: Boolean
        get() = success || status

    val payload: JsonElement?
        get() = result ?: data
}

/**
 * Pembungkus hasil operasi Repository (SRP & Type-Safe)
 */
sealed class ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val message: String = ""
    ) : ApiResult<T>()

    data class Error(
        val message: String,
        val code: Int = 0,
        val isUnauthorized: Boolean = (code == 401),
        val maintenance: MaintenanceInfo? = null
    ) : ApiResult<Nothing>() {
        val isMaintenance: Boolean
            get() = maintenance != null || code == 503
    }
}

/**
 * State reaktif standar untuk seluruh ViewModel -> Compose UI
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole(val apiValue: String) {
    ADMIN("admin"),
    CUSTOMER("customer");

    companion object {
        fun fromString(raw: String): UserRole =
            if (raw.equals("customer", ignoreCase = true)) CUSTOMER else ADMIN
    }
}

enum class AppThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String): AppThemeMode =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

/**
 * Profil server tersimpan (mendukung multi-server & identitas white-label dinamis)
 * Seluruh properti memiliki default agar Gson selalu mengisi nilai aman pada JSON lama.
 */
data class ServerProfile(
    val id: String = "",
    val serverUrl: String = "",
    val companyName: String = "PHPNuxBill",
    val currencyCode: String = "Rp.",
    val serverLogoUrl: String = "",
    val username: String = "",
    val role: UserRole = UserRole.ADMIN,
    val token: String = "",
    val lastUsedAt: Long = System.currentTimeMillis(),
    val fullname: String = "",
    val userType: String = ""
)

/**
 * State sesi aktif yang diamati secara reaktif oleh Root App & TopBar
 */
data class SessionState(
    val activeServerUrl: String = "",
    val token: String = "",
    val role: UserRole = UserRole.ADMIN,
    val username: String = "",
    val fullname: String = "",
    val userType: String = "",
    val companyName: String = "PHPNuxBill",
    val currencyCode: String = "Rp.",
    val serverLogoUrl: String = "",
    val customLocalLogoPath: String = "",
    val languageCode: String = "id",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val savedServers: List<ServerProfile> = emptyList()
) {
    val isLoggedIn: Boolean
        get() = activeServerUrl.isNotBlank() && token.isNotBlank()

    val effectiveCompanyName: String
        get() = companyName.ifBlank { "PHPNuxBill" }
}
