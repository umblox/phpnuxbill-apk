package com.phpnuxbill.mobile.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.ServerProfile
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UserRole
import com.phpnuxbill.mobile.feature.auth.data.AuthRepository
import com.phpnuxbill.mobile.feature.auth.model.LoginUiState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    val bootstrapSession: StateFlow<SessionState?> = sessionStorage.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val sessionState: StateFlow<SessionState> = bootstrapSession
        .map { it ?: SessionState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState()
        )

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bootstrapSession.filterNotNull().collect { session ->
                _uiState.update { current ->
                    val shouldFillUrl = current.serverUrl.isBlank() && session.activeServerUrl.isNotBlank()
                    val shouldFillUser = current.username.isBlank() && session.username.isNotBlank()
                    current.copy(
                        serverUrl = if (shouldFillUrl) session.activeServerUrl else current.serverUrl,
                        companyName = session.effectiveCompanyName,
                        currencyCode = session.currencyCode,
                        serverLogoUrl = session.serverLogoUrl,
                        customLocalLogoPath = session.customLocalLogoPath,
                        username = if (shouldFillUser) session.username else current.username,
                        selectedRole = if (shouldFillUser) session.role else current.selectedRole,
                        savedServers = session.savedServers
                    )
                }
            }
        }
    }

    fun onServerUrlChanged(newUrl: String) {
        _uiState.update {
            it.copy(
                serverUrl = newUrl,
                isServerVerified = false,
                serverStatusMessage = null,
                errorMessage = null
            )
        }
    }

    fun onRoleChanged(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            authRepository.setLanguage(language)
        }
    }

    fun onCheckServer() {
        val rawUrl = _uiState.value.serverUrl
        if (_uiState.value.isCheckingServer) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingServer = true,
                    serverStatusMessage = null,
                    errorMessage = null
                )
            }

            when (val res = authRepository.probeServer(rawUrl)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingServer = false,
                            isServerVerified = true,
                            serverUrl = res.data.serverUrl,
                            companyName = res.data.companyName,
                            currencyCode = res.data.currencyCode,
                            serverLogoUrl = res.data.logoUrl,
                            serverStatusMessage = res.message.ifBlank { res.data.companyName }
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingServer = false,
                            isServerVerified = false,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun onSelectSavedServer(profile: ServerProfile) {
        viewModelScope.launch {
            if (profile.token.isNotBlank()) {
                authRepository.switchSavedServer(profile.id)
            } else {
                val cleanUrl = DataStoreSessionStorage.normalizeServerUrl(profile.serverUrl)
                sessionStorage.updateServerPreview(
                    serverUrl = cleanUrl,
                    companyName = profile.companyName,
                    currencyCode = profile.currencyCode,
                    serverLogoUrl = profile.serverLogoUrl
                )
                _uiState.update {
                    it.copy(
                        serverUrl = cleanUrl,
                        companyName = profile.companyName.ifBlank { "PHPNuxBill" },
                        currencyCode = profile.currencyCode,
                        serverLogoUrl = profile.serverLogoUrl,
                        username = profile.username,
                        selectedRole = profile.role,
                        isServerVerified = true,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onRemoveSavedServer(profileId: String) {
        viewModelScope.launch {
            authRepository.removeSavedServer(profileId)
        }
    }

    fun onSubmitLogin() {
        val state = _uiState.value
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            when (
                val res = authRepository.login(
                    serverUrl = state.serverUrl,
                    role = state.selectedRole,
                    username = state.username,
                    password = state.password
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            password = "",
                            companyName = res.data.companyName,
                            serverLogoUrl = res.data.logoUrl,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(
                authRepository = authRepository,
                sessionStorage = sessionStorage
            ) as T
        }
    }
}
