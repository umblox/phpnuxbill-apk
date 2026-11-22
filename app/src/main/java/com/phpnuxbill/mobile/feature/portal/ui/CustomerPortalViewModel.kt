package com.phpnuxbill.mobile.feature.portal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.portal.data.CustomerPortalRepository
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
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

    private val _checkoutPlan = MutableStateFlow<CustomerBuyPlanItem?>(null)
    val checkoutPlan: StateFlow<CustomerBuyPlanItem?> = _checkoutPlan.asStateFlow()

    private val _selectedGatewayId = MutableStateFlow("balance")
    val selectedGatewayId: StateFlow<String> = _selectedGatewayId.asStateFlow()

    private val _selectedChannelId = MutableStateFlow("")
    val selectedChannelId: StateFlow<String> = _selectedChannelId.asStateFlow()

    private val _couponCodeInput = MutableStateFlow("")
    val couponCodeInput: StateFlow<String> = _couponCodeInput.asStateFlow()

    private val _wifiSsidInput = MutableStateFlow("")
    val wifiSsidInput: StateFlow<String> = _wifiSsidInput.asStateFlow()

    private val _wifiPassInput = MutableStateFlow("")
    val wifiPassInput: StateFlow<String> = _wifiPassInput.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

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

    fun onCouponCodeChanged(v: String) {
        _couponCodeInput.value = v.uppercase()
    }

    fun onWifiInputsChanged(ssid: String, pass: String) {
        _wifiSsidInput.value = ssid
        _wifiPassInput.value = pass
    }

    fun loadPortal() {
        viewModelScope.launch {
            if (_uiState.value !is UiState.Success) _uiState.value = UiState.Loading
            when (val res = repository.getPortalHome()) {
                is ApiResult.Success -> {
                    _uiState.value = UiState.Success(res.data)
                    val modem = res.data.modemInfo
                    if (modem != null && _wifiSsidInput.value.isBlank()) {
                        _wifiSsidInput.value = modem.currentSsid
                        _wifiPassInput.value = modem.currentPassword
                    }
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _uiState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun openCheckoutForPlan(plan: CustomerBuyPlanItem) {
        val data = (_uiState.value as? UiState.Success)?.data
        val isBalancePlan = plan.type.equals("Balance", true)
        val firstPg = data?.paymentGateways?.firstOrNull()
        _checkoutPlan.value = plan
        _couponCodeInput.value = ""
        if (!isBalancePlan && data?.enableBalance == true && (data.balance >= plan.price)) {
            _selectedGatewayId.value = "balance"
            _selectedChannelId.value = ""
        } else if (firstPg != null) {
            _selectedGatewayId.value = firstPg.id
            _selectedChannelId.value = firstPg.channels.firstOrNull()?.id.orEmpty()
        } else {
            _selectedGatewayId.value = "balance"
            _selectedChannelId.value = ""
        }
    }

    fun closeCheckout() {
        _checkoutPlan.value = null
    }

    fun selectCheckoutGateway(gwId: String, firstChannelId: String = "") {
        _selectedGatewayId.value = gwId
        _selectedChannelId.value = firstChannelId
    }

    fun selectCheckoutChannel(channelId: String) {
        _selectedChannelId.value = channelId
    }

    fun confirmCheckoutOrder() {
        val plan = _checkoutPlan.value ?: return
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val res = repository.buyPlan(
                    planId = plan.id,
                    gateway = _selectedGatewayId.value,
                    channel = _selectedChannelId.value,
                    couponCode = _couponCodeInput.value
                )
            ) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _checkoutPlan.value = null
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
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
        val data = (_uiState.value as? UiState.Success)?.data
        val modem = data?.modemInfo
        val ssid = _wifiSsidInput.value.trim()
        if (ssid.isBlank()) return
        viewModelScope.launch {
            when (
                val res = repository.updateCustomerWifi(
                    deviceId = modem?.deviceId.orEmpty(),
                    wlanIndex = modem?.wlanIndex ?: 1,
                    ssid = ssid,
                    password = _wifiPassInput.value.trim(),
                    source = modem?.wlanSource ?: "tr098"
                )
            ) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPortal()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun rebootModem() {
        val deviceId = (_uiState.value as? UiState.Success)?.data?.modemInfo?.deviceId.orEmpty()
        viewModelScope.launch {
            when (val res = repository.rebootCustomerModem(deviceId)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun summonModem() {
        val deviceId = (_uiState.value as? UiState.Success)?.data?.modemInfo?.deviceId.orEmpty()
        viewModelScope.launch {
            when (val res = repository.summonCustomerModem(deviceId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPortal()
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CustomerPortalViewModel(repository, sessionStorage) as T
    }
}
