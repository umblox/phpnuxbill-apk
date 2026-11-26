package com.phpnuxbill.mobile.feature.auth.model

import com.phpnuxbill.mobile.core.model.ServerProfile
import com.phpnuxbill.mobile.core.model.UserRole

data class ServerInfoResult(
    val serverUrl: String,
    val companyName: String,
    val currencyCode: String,
    val language: String,
    val logoUrl: String,
    val apiVersion: String,
    val maintenanceMode: Boolean = false,
    val maintenanceForceLogout: Boolean = false,
    val maintenanceDate: String = "",
    val companyPhone: String = ""
)

data class LoginResult(
    val token: String,
    val role: UserRole,
    val username: String,
    val fullname: String,
    val userType: String,
    val companyName: String,
    val currencyCode: String,
    val logoUrl: String
)

data class LoginUiState(
    val serverUrl: String = "",
    val companyName: String = "PHPNuxBill",
    val currencyCode: String = "Rp.",
    val serverLogoUrl: String = "",
    val customLocalLogoPath: String = "",
    val selectedRole: UserRole = UserRole.ADMIN,
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isCheckingServer: Boolean = false,
    val isServerVerified: Boolean = false,
    val serverStatusMessage: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val savedServers: List<ServerProfile> = emptyList(),
    val isServerInMaintenance: Boolean = false,
    val maintenanceForceLogout: Boolean = false,
    val maintenanceDate: String = "",
    val companyPhone: String = ""
)
