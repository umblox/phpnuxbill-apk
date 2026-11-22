package com.phpnuxbill.mobile.feature.portal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.portal.data.CustomerPortalRepository
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerPortalViewModel(
    private val repository: CustomerPortalRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<CustomerPortalDashboard>>(UiState.Loading)
    val uiState: StateFlow<UiState<CustomerPortalDashboard>> = _uiState.asStateFlow()

    private val _voucherCodeInput = MutableStateFlow("")
    val voucherCodeInput: StateFlow<String> = _voucherCodeInput.asStateFlow()

    private val _wifiSsidInput = MutableStateFlow("")
    val wifiSsidInput: StateFlow<String> = _wifiSsidInput.asStateFlow()

    private val _wifiPassInput = MutableStateFlow("")
    val wifiPassInput: StateFlow<String> = _wifiPassInput.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        loadPortal()
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun onVoucherCodeChanged(v: String) {
        _voucherCodeInput.value = v
    }

    fun onWifiInputsChanged(ssid: String, pass: String) {
        _wifiSsidInput.value = ssid
        _wifiPassInput.value = pass
    }

    fun loadPortal() {
        viewModelScope.launch {
            if (_uiState.value !is UiState.Success) _uiState.value = UiState.Loading
            when (val res = repository.getPortalHome()) {
                is ApiResult.Success -> _uiState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _uiState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun toggleAutoRenewal() {
        viewModelScope.launch {
            when (val res = repository.toggleAutoRenewal()) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun redeemVoucher() {
        val code = _voucherCodeInput.value.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            when (val res = repository.redeemVoucher(code)) {
                is ApiResult.Success -> {
                    _voucherCodeInput.value = ""
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun buyPlanWithBalance(planId: Int) {
        viewModelScope.launch {
            when (val res = repository.buyWithBalance(planId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun checkBillStatus(billId: Int) {
        viewModelScope.launch {
            when (val res = repository.checkBillStatus(billId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun cancelBill(billId: Int) {
        viewModelScope.launch {
            when (val res = repository.cancelBill(billId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun saveWifi() {
        val ssid = _wifiSsidInput.value.trim()
        val pass = _wifiPassInput.value.trim()
        if (ssid.isBlank()) return
        viewModelScope.launch {
            when (val res = repository.updateCustomerWifi(ssid, pass)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    _wifiPassInput.value = ""
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    class Factory(
        private val repository: CustomerPortalRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomerPortalViewModel(repository, sessionStorage) as T
        }
    }
}
