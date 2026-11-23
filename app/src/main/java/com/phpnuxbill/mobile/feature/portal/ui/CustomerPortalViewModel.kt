package com.phpnuxbill.mobile.feature.portal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.portal.data.CustomerPortalRepository
import com.phpnuxbill.mobile.feature.portal.model.CustomerBillItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerInboxItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemStatusResult
import com.phpnuxbill.mobile.feature.portal.model.CustomerPackageItem
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

    private val _modemState = MutableStateFlow<UiState<CustomerModemStatusResult>>(UiState.Idle)
    val modemState: StateFlow<UiState<CustomerModemStatusResult>> = _modemState.asStateFlow()

    private val _inboxState = MutableStateFlow<UiState<List<CustomerInboxItem>>>(UiState.Idle)
    val inboxState: StateFlow<UiState<List<CustomerInboxItem>>> = _inboxState.asStateFlow()

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
    private var isCheckRequestInFlight = false

    private val _selectedGatewayId = MutableStateFlow("")
    val selectedGatewayId: StateFlow<String> = _selectedGatewayId.asStateFlow()

    private val _selectedChannelId = MutableStateFlow("")
    val selectedChannelId: StateFlow<String> = _selectedChannelId.asStateFlow()

    private val _couponCodeInput = MutableStateFlow("")
    val couponCodeInput: StateFlow<String> = _couponCodeInput.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        loadDashboard()
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

    fun setAppLanguage(langCode: String) {
        viewModelScope.launch {
            sessionStorage.setLanguage(langCode)
        }
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            sessionStorage.setThemeMode(mode)
        }
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

    fun ensureModemLoaded() {
        if (_modemState.value is UiState.Idle) loadModem()
    }

    fun loadModem() {
        viewModelScope.launch {
            _modemState.value = UiState.Loading
            when (val res = repository.getCustomerModem()) {
                is ApiResult.Success -> _modemState.value = UiState.Success(res.data)
                is ApiResult.Error -> _modemState.value = UiState.Error(res.message)
            }
        }
    }

    fun loadInbox() {
        viewModelScope.launch {
            if (_inboxState.value !is UiState.Success) _inboxState.value = UiState.Loading
            when (val res = repository.getInbox()) {
                is ApiResult.Success -> _inboxState.value = UiState.Success(res.data)
                is ApiResult.Error -> _inboxState.value = UiState.Error(res.message)
            }
        }
    }

    fun markInboxRead(item: CustomerInboxItem) {
        if (item.isRead) return
        viewModelScope.launch {
            when (repository.markInboxRead(item.id)) {
                is ApiResult.Success -> {
                    val curr = (_inboxState.value as? UiState.Success)?.data
                    if (curr != null) {
                        _inboxState.value = UiState.Success(
                            curr.map { if (it.id == item.id) it.copy(isRead = true) else it }
                        )
                    }
                    loadDashboard(isPullRefresh = true)
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun deleteInbox(id: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteInbox(id)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadInbox()
                    loadDashboard(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun transferBalance(username: String, amount: Double, onSuccess: () -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.transferBalance(username, amount)) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    loadDashboard(isPullRefresh = true)
                    loadTransactions(isPullRefresh = true)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun sendPlanToFriend(username: String, planId: Int, onSuccess: () -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.sendPlanToFriend(username, planId)) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    loadDashboard(isPullRefresh = true)
                    loadTransactions(isPullRefresh = true)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun syncPlanToRouter() {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.syncPlanToRouter()) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
            _isSubmitting.value = false
        }
    }

    fun extendExpiredPlan(rechargeId: Int) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.extendExpiredPlan(rechargeId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadDashboard(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
            _isSubmitting.value = false
        }
    }

    fun saveProfile(
        fullname: String,
        address: String,
        phonenumber: String,
        email: String,
        photoBase64: String,
        onSuccess: () -> Unit
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.saveProfile(fullname, address, phonenumber, email, photoBase64)) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    loadDashboard(isPullRefresh = true)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.changePassword(oldPassword, newPassword, confirmPassword)) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun openRenewForPackage(pkg: CustomerPackageItem, onFallbackToPlansTab: () -> Unit) {
        if (pkg.planId > 0 && pkg.planEnabled && pkg.price > 0.0) {
            val cachedMatch = (_plansState.value as? UiState.Success)?.data?.internetPlans
                ?.find { it.id == pkg.planId }
            val targetPlan = cachedMatch ?: CustomerBuyPlanItem(
                id = pkg.planId,
                namePlan = pkg.namebp,
                price = pkg.price,
                type = pkg.type,
                validity = pkg.validity,
                routers = pkg.routers
            )
            openCheckoutForPlan(targetPlan)
            return
        }

        viewModelScope.launch {
            var plans = (_plansState.value as? UiState.Success)?.data
            if (plans == null) {
                when (val res = repository.getPortalPlans()) {
                    is ApiResult.Success -> {
                        plans = res.data
                        _plansState.value = UiState.Success(res.data)
                    }
                    is ApiResult.Error -> {
                        _bannerMessage.value = res.message
                        onFallbackToPlansTab()
                        return@launch
                    }
                }
            }
            val matched = plans.internetPlans.find { it.id == pkg.planId && pkg.planId > 0 }
                ?: plans.internetPlans.find { it.namePlan.equals(pkg.namebp, ignoreCase = true) }

            if (matched != null) {
                openCheckoutForPlan(matched)
            } else {
                _bannerMessage.value = "Paket ${pkg.namebp} sudah tidak aktif di katalog. Silakan pilih paket lain."
                onFallbackToPlansTab()
            }
        }
    }

    fun refreshActiveTab(tabIndex: Int) {
        when (tabIndex) {
            0 -> loadDashboard(isPullRefresh = true)
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
                        _activePaymentBill.value = createdBill
                        loadDashboard(isPullRefresh = true)
                    } else {
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
        if (isCheckRequestInFlight) return
        viewModelScope.launch {
            isCheckRequestInFlight = true
            if (!silent) _isCheckingPayment.value = true
            try {
                when (val res = repository.checkBillStatus(billId)) {
                    is ApiResult.Success -> {
                        val (isPaid, updatedBill) = res.data
                        if (updatedBill != null && _activePaymentBill.value?.id == updatedBill.id) {
                            _activePaymentBill.value = updatedBill
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
            } finally {
                isCheckRequestInFlight = false
                if (!silent) _isCheckingPayment.value = false
            }
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

    fun saveWifi(
        wlanIndex: Int,
        ssid: String,
        password: String,
        source: String,
        onSuccess: () -> Unit
    ) {
        if (ssid.isBlank() || _isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (
                val res = repository.updateCustomerWifi(
                    wlanIndex = wlanIndex,
                    ssid = ssid.trim(),
                    password = password.trim(),
                    source = source
                )
            ) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    onSuccess()
                    loadModem()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun rebootModem(onSuccess: () -> Unit = {}) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.rebootCustomerModem()) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun summonModem() {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val res = repository.summonCustomerModem()) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                    loadModem()
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _bannerMessage.value = res.message
                }
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
