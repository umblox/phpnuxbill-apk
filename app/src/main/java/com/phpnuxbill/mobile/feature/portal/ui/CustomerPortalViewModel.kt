package com.phpnuxbill.mobile.feature.portal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.portal.data.CustomerPortalRepository
import com.phpnuxbill.mobile.feature.portal.model.CustomerBillItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemInfo
import com.phpnuxbill.mobile.feature.portal.model.CustomerPlansData
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import com.phpnuxbill.mobile.feature.portal.model.CustomerTransactionItem
import com.phpnuxbill.mobile.feature.portal.model.PortalPaymentGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerPortalViewModel(
    private val repository: CustomerPortalRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<UiState<CustomerPortalDashboard>>(UiState.Loading)
    val dashboardState: StateFlow<UiState<CustomerPortalDashboard>> = _dashboardState.asStateFlow()

    private val _plansState = MutableStateFlow<UiState<CustomerPlansData>>(UiState.Idle)
    val plansState: StateFlow<UiState<CustomerPlansData>> = _plansState.asStateFlow()

    private val _transactionsState = MutableStateFlow<UiState<List<CustomerTransactionItem>>>(UiState.Idle)
    val transactionsState: StateFlow<UiState<List<CustomerTransactionItem>>> = _transactionsState.asStateFlow()

    private val _modemState = MutableStateFlow<UiState<CustomerModemInfo?>>(UiState.Idle)
    val modemState: StateFlow<UiState<CustomerModemInfo?>> = _modemState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _voucherCodeInput = MutableStateFlow("")
    val voucherCodeInput: StateFlow<String> = _voucherCodeInput.asStateFlow()

    private val _checkoutPlan = MutableStateFlow<CustomerBuyPlanItem?>(null)
    val checkoutPlan: StateFlow<CustomerBuyPlanItem?> = _checkoutPlan.asStateFlow()

    private val _activePaymentBill = MutableStateFlow<CustomerBillItem?>(null)
    val activePaymentBill: StateFlow<CustomerBillItem?> = _activePaymentBill.asStateFlow()

    private val _isCheckingPayment = MutableStateFlow(false)
    val isCheckingPayment: StateFlow<Boolean> = _isCheckingPayment.asStateFlow()

    private val _selectedGatewayId = MutableStateFlow("")
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
        loadDashboard()
        loadModem()
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

    fun openPaymentBill(bill: CustomerBillItem) {
        _activePaymentBill.value = bill
    }

    fun closePaymentBill() {
        _activePaymentBill.value = null
        loadDashboard(isPullRefresh = true)
    }

    fun loadDashboard(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_dashboardState.value !is UiState.Success) _dashboardState.value = UiState.Loading

            when (val res = repository.getPortalDashboard()) {
                is ApiResult.Success -> _dashboardState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else if (_dashboardState.value !is UiState.Success) _dashboardState.value = UiState.Error(res.message)
                    else _bannerMessage.value = res.message
                }
            }
            _isRefreshing.value = false
        }
    }

    fun ensurePlansLoaded() {
        if (_plansState.value is UiState.Idle) loadPlans()
    }

    fun loadPlans(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_plansState.value !is UiState.Success) _plansState.value = UiState.Loading

            when (val res = repository.getPortalPlans()) {
                is ApiResult.Success -> _plansState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _plansState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun ensureTransactionsLoaded() {
        if (_transactionsState.value is UiState.Idle) loadTransactions()
    }

    fun loadTransactions(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_transactionsState.value !is UiState.Success) _transactionsState.value = UiState.Loading

            when (val res = repository.getPortalTransactions()) {
                is ApiResult.Success -> _transactionsState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _transactionsState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun loadModem() {
        viewModelScope.launch {
            if (_modemState.value !is UiState.Success) _modemState.value = UiState.Loading
            when (val res = repository.getCustomerModem()) {
                is ApiResult.Success -> {
                    _modemState.value = UiState.Success(res.data)
                    val modem = res.data
                    if (modem != null && _wifiSsidInput.value.isBlank()) {
                        _wifiSsidInput.value = modem.currentSsid
                        _wifiPassInput.value = modem.currentPassword
                    }
                }
                is ApiResult.Error -> _modemState.value = UiState.Success(null)
            }
        }
    }

    fun refreshActiveTab(tabIndex: Int) {
        when (tabIndex) {
            0 -> {
                loadDashboard(isPullRefresh = true)
                loadModem()
            }
            1 -> loadPlans(isPullRefresh = true)
            2 -> loadTransactions(isPullRefresh = true)
            3 -> loadModem()
        }
    }

    private fun getAvailableGateways(): List<PortalPaymentGateway> {
        val dash = (_dashboardState.value as? UiState.Success)?.data
        val plans = (_plansState.value as? UiState.Success)?.data
        val raw = dash?.paymentGateways.orEmpty().ifEmpty { plans?.paymentGateways.orEmpty() }
        return raw.filter { gw -> !gw.requiresChannel || gw.channels.isNotEmpty() }
    }

    fun openCheckoutForPlan(plan: CustomerBuyPlanItem) {
        val dash = (_dashboardState.value as? UiState.Success)?.data
        val gateways = getAvailableGateways()
        val isBalancePlan = plan.type.equals("Balance", true)
        val canUseBalance = !isBalancePlan && dash?.enableBalance == true && dash.balance >= plan.price

        _checkoutPlan.value = plan
        _couponCodeInput.value = ""

        if (canUseBalance) {
            _selectedGatewayId.value = "balance"
            _selectedChannelId.value = ""
        } else {
            val firstPg = gateways.firstOrNull()
            if (firstPg != null) {
                _selectedGatewayId.value = firstPg.id
                _selectedChannelId.value = if (firstPg.requiresChannel) {
                    firstPg.channels.firstOrNull { plan.price >= it.minAmount }?.id.orEmpty()
                } else ""
            } else {
                _selectedGatewayId.value = ""
                _selectedChannelId.value = ""
            }
        }
    }

    fun closeCheckout() {
        _checkoutPlan.value = null
    }

    fun selectCheckoutGateway(gwId: String) {
        val plan = _checkoutPlan.value
        _selectedGatewayId.value = gwId
        if (gwId == "balance") {
            _selectedChannelId.value = ""
            return
        }
        val pg = getAvailableGateways().find { it.id == gwId }
        _selectedChannelId.value = if (pg != null && pg.requiresChannel) {
            val price = plan?.price ?: 0.0
            pg.channels.firstOrNull { price >= it.minAmount }?.id.orEmpty()
        } else ""
    }

    fun selectCheckoutChannel(channelId: String) {
        _selectedChannelId.value = channelId
    }

    fun confirmCheckoutOrder(onBalancePaidSuccess: () -> Unit) {
        val plan = _checkoutPlan.value ?: return
        if (_isSubmitting.value || _selectedGatewayId.value.isBlank()) return
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
                    val createdBill = res.data
                    if (createdBill != null && createdBill.id > 0) {
                        // Langsung buka Layar Pembayaran Penuh
                        _activePaymentBill.value = createdBill
                        loadDashboard(isPullRefresh = true)
                    } else {
                        // Pembayaran via Saldo (lunas instan)
                        _bannerMessage.value = res.message
                        loadDashboard(isPullRefresh = true)
                        loadTransactions(isPullRefresh = true)
                        onBalancePaidSuccess()
                    }
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
                    loadDashboard(isPullRefresh = true)
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
                    loadDashboard(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun checkBillStatus(billId: Int, silent: Boolean = false) {
        if (_isCheckingPayment.value) return
        viewModelScope.launch {
            _isCheckingPayment.value = true
            when (val res = repository.checkBillStatus(billId)) {
                is ApiResult.Success -> {
                    val (isPaid, updatedBill) = res.data
                    if (updatedBill != null) {
                        if (_activePaymentBill.value?.id == updatedBill.id) {
                            _activePaymentBill.value = updatedBill
                        }
                    }
                    if (isPaid) {
                        _bannerMessage.value = res.message
                        loadDashboard(isPullRefresh = true)
                        loadTransactions(isPullRefresh = true)
                    } else if (!silent) {
                        _bannerMessage.value = res.message
                    }
                }
                is ApiResult.Error -> {
                    if (!silent) _bannerMessage.value = res.message
                }
            }
            _isCheckingPayment.value = false
        }
    }

    fun cancelBill(billId: Int) {
        viewModelScope.launch {
            when (val res = repository.cancelBill(billId)) {
                is ApiResult.Success -> {
                    if (_activePaymentBill.value?.id == billId) {
                        _activePaymentBill.value = null
                    }
                    _bannerMessage.value = res.message
                    loadDashboard(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun saveWifi() {
        val modem = (_modemState.value as? UiState.Success)?.data
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
                    loadModem()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun rebootModem() {
        val deviceId = (_modemState.value as? UiState.Success)?.data?.deviceId.orEmpty()
        viewModelScope.launch {
            when (val res = repository.rebootCustomerModem(deviceId)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun summonModem() {
        val deviceId = (_modemState.value as? UiState.Success)?.data?.deviceId.orEmpty()
        viewModelScope.launch {
            when (val res = repository.summonCustomerModem(deviceId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadModem()
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
