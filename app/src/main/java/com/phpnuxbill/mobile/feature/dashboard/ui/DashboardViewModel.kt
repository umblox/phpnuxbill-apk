package com.phpnuxbill.mobile.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.dashboard.data.DashboardRepository
import com.phpnuxbill.mobile.feature.dashboard.model.DashboardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardData>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadDashboard(isPullRefresh = false)
    }

    fun loadDashboard(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) {
                _isRefreshing.value = true
            } else if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Loading
            }

            when (val res = dashboardRepository.fetchAdminDashboard()) {
                is ApiResult.Success -> {
                    _uiState.value = UiState.Success(res.data)
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) {
                        sessionStorage.clearActiveToken()
                    } else {
                        _uiState.value = UiState.Error(res.message)
                    }
                }
            }
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val dashboardRepository: DashboardRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                dashboardRepository = dashboardRepository,
                sessionStorage = sessionStorage
            ) as T
        }
    }
}
