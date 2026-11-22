package com.phpnuxbill.mobile.feature.services.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.printer.BluetoothPrinterHelper
import com.phpnuxbill.mobile.core.printer.PairedPrinterDevice
import com.phpnuxbill.mobile.core.printer.PrintResult
import com.phpnuxbill.mobile.feature.services.data.ServiceVoucherRepository
import com.phpnuxbill.mobile.feature.services.model.ActiveServiceSheetMode
import com.phpnuxbill.mobile.feature.services.model.DepositFormState
import com.phpnuxbill.mobile.feature.services.model.GenerateVoucherFormState
import com.phpnuxbill.mobile.feature.services.model.InvoiceReceiptData
import com.phpnuxbill.mobile.feature.services.model.PrepaidServiceItem
import com.phpnuxbill.mobile.feature.services.model.RechargeFormState
import com.phpnuxbill.mobile.feature.services.model.RefillFormState
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata
import com.phpnuxbill.mobile.feature.services.model.VoucherItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ServiceVoucherViewModel(
    private val repository: ServiceVoucherRepository,
    private val sessionStorage: SessionStorage,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    // State Layanan Aktif
    private val _servicesState = MutableStateFlow<UiState<List<PrepaidServiceItem>>>(UiState.Loading)
    val servicesState: StateFlow<UiState<List<PrepaidServiceItem>>> = _servicesState.asStateFlow()

    private val _serviceSearch = MutableStateFlow("")
    val serviceSearch: StateFlow<String> = _serviceSearch.asStateFlow()

    private val _serviceStatusFilter = MutableStateFlow("all")
    val serviceStatusFilter: StateFlow<String> = _serviceStatusFilter.asStateFlow()

    // State Voucher
    private val _vouchersState = MutableStateFlow<UiState<List<VoucherItem>>>(UiState.Loading)
    val vouchersState: StateFlow<UiState<List<VoucherItem>>> = _vouchersState.asStateFlow()

    private val _voucherSearch = MutableStateFlow("")
    val voucherSearch: StateFlow<String> = _voucherSearch.asStateFlow()

    private val _voucherStatusFilter = MutableStateFlow("all")
    val voucherStatusFilter: StateFlow<String> = _voucherStatusFilter.asStateFlow()

    private val _generatedCodesBatch = MutableStateFlow<List<String>>(emptyList())
    val generatedCodesBatch: StateFlow<List<String>> = _generatedCodesBatch.asStateFlow()

    // Metadata Bersama & Form States
    private val _metadata = MutableStateFlow(ServiceVoucherMetadata())
    val metadata: StateFlow<ServiceVoucherMetadata> = _metadata.asStateFlow()

    private val _activeServiceMode = MutableStateFlow(ActiveServiceSheetMode.NONE)
    val activeServiceMode: StateFlow<ActiveServiceSheetMode> = _activeServiceMode.asStateFlow()

    private val _rechargeForm = MutableStateFlow(RechargeFormState())
    val rechargeForm: StateFlow<RechargeFormState> = _rechargeForm.asStateFlow()

    private val _depositForm = MutableStateFlow(DepositFormState())
    val depositForm: StateFlow<DepositFormState> = _depositForm.asStateFlow()

    private val _refillForm = MutableStateFlow(RefillFormState())
    val refillForm: StateFlow<RefillFormState> = _refillForm.asStateFlow()

    private val _generateVoucherForm = MutableStateFlow(GenerateVoucherFormState())
    val generateVoucherForm: StateFlow<GenerateVoucherFormState> = _generateVoucherForm.asStateFlow()

    // Struk & Thermal Printer
    private val _activeReceipt = MutableStateFlow<InvoiceReceiptData?>(null)
    val activeReceipt: StateFlow<InvoiceReceiptData?> = _activeReceipt.asStateFlow()

    private val _pairedPrinters = MutableStateFlow<List<PairedPrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PairedPrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _showPrinterDialog = MutableStateFlow(false)
    val showPrinterDialog: StateFlow<Boolean> = _showPrinterDialog.asStateFlow()

    private val _requestBluetoothPermissionEvent = MutableStateFlow(false)
    val requestBluetoothPermissionEvent: StateFlow<Boolean> = _requestBluetoothPermissionEvent.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        loadPrepaidServices()
        loadVouchers()
        loadMetadata()

        viewModelScope.launch {
            _serviceSearch.drop(1).debounce(250L).distinctUntilChanged().collect {
                loadPrepaidServices()
            }
        }
        viewModelScope.launch {
            _voucherSearch.drop(1).debounce(250L).distinctUntilChanged().collect {
                loadVouchers()
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun dismissGeneratedBatch() {
        _generatedCodesBatch.value = emptyList()
    }

    fun loadMetadata(force: Boolean = false) {
        if (!force && _metadata.value.customers.isNotEmpty()) return
        viewModelScope.launch {
            when (val res = repository.getMetadata()) {
                is ApiResult.Success -> _metadata.value = res.data
                is ApiResult.Error -> {}
            }
        }
    }

    // =========================================================================
    // LAYANAN AKTIF, RECHARGE, DEPOSIT & EXTEND
    // =========================================================================

    fun onServiceSearchChanged(q: String) {
        _serviceSearch.value = q
    }

    fun onServiceStatusFilterChanged(status: String) {
        _serviceStatusFilter.value = status
        loadPrepaidServices()
    }

    fun loadPrepaidServices(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_servicesState.value !is UiState.Success) _servicesState.value = UiState.Loading

            when (
                val res = repository.getPrepaidServices(
                    search = _serviceSearch.value,
                    status = _serviceStatusFilter.value
                )
            ) {
                is ApiResult.Success -> _servicesState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _servicesState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun openRechargeForm(prefillCustomerId: Int = 0, prefillPlanId: Int = 0) {
        loadMetadata()
        val meta = _metadata.value
        val cust = meta.customers.find { it.id == prefillCustomerId } ?: meta.customers.firstOrNull()
        val plan = meta.internetPlans.find { it.id == prefillPlanId } ?: meta.internetPlans.firstOrNull()
        _rechargeForm.value = RechargeFormState(
            customerId = cust?.id ?: 0,
            planId = plan?.id ?: 0,
            usingMethod = meta.paymentUsings.firstOrNull() ?: "Cash"
        )
        _activeServiceMode.value = ActiveServiceSheetMode.RECHARGE_PLAN
    }

    fun openDepositForm(prefillCustomerId: Int = 0) {
        loadMetadata()
        val meta = _metadata.value
        val cust = meta.customers.find { it.id == prefillCustomerId } ?: meta.customers.firstOrNull()
        _depositForm.value = DepositFormState(
            customerId = cust?.id ?: 0,
            planId = meta.balancePlans.firstOrNull()?.id ?: 0
        )
        _activeServiceMode.value = ActiveServiceSheetMode.DEPOSIT_BALANCE
    }

    fun openRefillForm(prefillCode: String = "") {
        loadMetadata()
        val meta = _metadata.value
        _refillForm.value = RefillFormState(
            customerId = meta.customers.firstOrNull()?.id ?: 0,
            voucherCode = prefillCode
        )
        _activeServiceMode.value = ActiveServiceSheetMode.REFILL_VOUCHER
    }

    fun closeServiceMode() {
        _activeServiceMode.value = ActiveServiceSheetMode.NONE
    }

    fun updateRechargeForm(block: (RechargeFormState) -> RechargeFormState) {
        _rechargeForm.update { block(it).copy(error = null) }
    }

    fun updateDepositForm(block: (DepositFormState) -> DepositFormState) {
        _depositForm.update { block(it).copy(error = null) }
    }

    fun updateRefillForm(block: (RefillFormState) -> RefillFormState) {
        _refillForm.update { block(it).copy(error = null) }
    }

    fun submitRecharge() {
        val form = _rechargeForm.value
        if (form.isSubmitting) return
        viewModelScope.launch {
            _rechargeForm.update { it.copy(isSubmitting = true, error = null) }
            when (val res = repository.rechargeCustomer(form)) {
                is ApiResult.Success -> {
                    _rechargeForm.update { it.copy(isSubmitting = false) }
                    _activeServiceMode.value = ActiveServiceSheetMode.NONE
                    _bannerMessage.value = res.message
                    if (res.data != null) {
                        _activeReceipt.value = res.data
                    }
                    loadPrepaidServices(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _rechargeForm.update { it.copy(isSubmitting = false, error = res.message) }
                }
            }
        }
    }

    fun submitDeposit() {
        val form = _depositForm.value
        if (form.isSubmitting) return
        viewModelScope.launch {
            _depositForm.update { it.copy(isSubmitting = true, error = null) }
            when (val res = repository.depositBalance(form)) {
                is ApiResult.Success -> {
                    _depositForm.update { it.copy(isSubmitting = false) }
                    _activeServiceMode.value = ActiveServiceSheetMode.NONE
                    _bannerMessage.value = res.message
                    if (res.data != null) {
                        _activeReceipt.value = res.data
                    }
                    loadMetadata(force = true)
                }
                is ApiResult.Error -> {
                    _depositForm.update { it.copy(isSubmitting = false, error = res.message) }
                }
            }
        }
    }

    fun extendService(rechargeId: Int, days: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.extendService(rechargeId, days)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPrepaidServices(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun deleteService(rechargeId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.deleteService(rechargeId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPrepaidServices(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    // =========================================================================
    // MANAJEMEN VOUCHER
    // =========================================================================

    fun onVoucherSearchChanged(q: String) {
        _voucherSearch.value = q
    }

    fun onVoucherStatusFilterChanged(status: String) {
        _voucherStatusFilter.value = status
        loadVouchers()
    }

    fun loadVouchers(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_vouchersState.value !is UiState.Success) _vouchersState.value = UiState.Loading

            when (
                val res = repository.getVouchers(
                    search = _voucherSearch.value,
                    status = _voucherStatusFilter.value
                )
            ) {
                is ApiResult.Success -> {
                    val (prefix, list) = res.data
                    if (prefix.isNotBlank()) {
                        _metadata.update { it.copy(defaultVoucherPrefix = prefix) }
                    }
                    _vouchersState.value = UiState.Success(list)
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _vouchersState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun openGenerateVoucherForm() {
        loadMetadata()
        val meta = _metadata.value
        val firstPlan = meta.internetPlans.firstOrNull { it.type.equals("Hotspot", ignoreCase = true) }
            ?: meta.internetPlans.firstOrNull()
        _generateVoucherForm.value = GenerateVoucherFormState(
            isOpen = true,
            planId = firstPlan?.id ?: 0,
            server = firstPlan?.routers ?: meta.routers.firstOrNull()?.name.orEmpty(),
            prefix = meta.defaultVoucherPrefix
        )
    }

    fun closeGenerateVoucherForm() {
        _generateVoucherForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateGenerateVoucherForm(block: (GenerateVoucherFormState) -> GenerateVoucherFormState) {
        _generateVoucherForm.update { block(it).copy(error = null) }
    }

    fun submitGenerateVouchers() {
        val form = _generateVoucherForm.value
        if (form.isSubmitting) return
        viewModelScope.launch {
            _generateVoucherForm.update { it.copy(isSubmitting = true, error = null) }
            when (val res = repository.generateVouchers(form)) {
                is ApiResult.Success -> {
                    _generateVoucherForm.update { it.copy(isSubmitting = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    _generatedCodesBatch.value = res.data
                    loadVouchers(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _generateVoucherForm.update { it.copy(isSubmitting = false, error = res.message) }
                }
            }
        }
    }

    fun submitRefillVoucher() {
        val form = _refillForm.value
        if (form.isSubmitting) return
        viewModelScope.launch {
            _refillForm.update { it.copy(isSubmitting = true, error = null) }
            when (val res = repository.refillVoucher(form)) {
                is ApiResult.Success -> {
                    _refillForm.update { it.copy(isSubmitting = false) }
                    _activeServiceMode.value = ActiveServiceSheetMode.NONE
                    _bannerMessage.value = res.message
                    if (res.data != null) {
                        _activeReceipt.value = res.data
                    }
                    loadVouchers(isPullRefresh = true)
                    loadPrepaidServices(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _refillForm.update { it.copy(isSubmitting = false, error = res.message) }
                }
            }
        }
    }

    fun deleteVoucher(voucherId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.deleteVoucher(voucherId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadVouchers(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun deleteUsedVouchers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.deleteUsedVouchers()) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadVouchers(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    // =========================================================================
    // STRUK INVOICE, WHATSAPP & THERMAL PRINTER BLUETOOTH
    // =========================================================================

    fun openReceiptForService(item: PrepaidServiceItem, companyName: String, currencyCode: String) {
        val receiptText = buildString {
            appendLine("*$companyName*")
            appendLine("========================")
            appendLine("No. Invoice : ${item.invoice}")
            appendLine("Tanggal     : ${item.rechargedOn} ${item.rechargedTime}")
            appendLine("Pelanggan   : ${item.fullname} (@${item.username})")
            appendLine("Paket       : ${item.namebp}")
            appendLine("Metode      : ${item.method}")
            appendLine("Aktif s/d   : *${item.expiration} ${item.time}*")
            appendLine("========================")
        }
        _activeReceipt.value = InvoiceReceiptData(
            trxId = 0,
            invoice = item.invoice,
            customerId = item.customerId,
            username = item.username,
            fullname = item.fullname,
            phonenumber = item.phonenumber,
            planName = item.namebp,
            type = item.type,
            price = item.price,
            priceFormatted = "$currencyCode ${item.price.toLong()}",
            method = item.method,
            routers = item.routers,
            rechargedAt = "${item.rechargedOn} ${item.rechargedTime}",
            expiration = "${item.expiration} ${item.time}",
            expirationPretty = "${item.expiration} ${item.time}",
            balanceAfter = 0.0,
            balanceFormatted = "",
            note = "",
            waWebText = receiptText.trim()
        )
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    fun sendReceiptViaGateway(receipt: InvoiceReceiptData) {
        viewModelScope.launch {
            when (
                val res = repository.resendInvoiceWhatsApp(
                    trxId = receipt.trxId,
                    invoice = receipt.invoice,
                    username = receipt.username,
                    phonenumber = receipt.phonenumber
                )
            ) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun onTriggerPrintReceipt() {
        val isEn = sessionStorage.sessionFlow.let { false }
        if (!printerHelper.hasBluetoothPermission()) {
            _requestBluetoothPermissionEvent.value = true
            return
        }
        loadPairedPrintersAndShowDialog(isEn)
    }

    fun onBluetoothPermissionResult(granted: Boolean, isEnglish: Boolean) {
        _requestBluetoothPermissionEvent.value = false
        if (granted) {
            loadPairedPrintersAndShowDialog(isEnglish)
        } else {
            _bannerMessage.value = if (isEnglish) {
                "Bluetooth permission denied. Cannot connect to thermal printer."
            } else {
                "Izin Bluetooth ditolak. Tidak dapat terhubung ke printer thermal."
            }
        }
    }

    private fun loadPairedPrintersAndShowDialog(isEnglish: Boolean) {
        printerHelper.getPairedPrinters(isEnglish).fold(
            onSuccess = { list ->
                _pairedPrinters.value = list
                _showPrinterDialog.value = true
            },
            onFailure = { err ->
                _bannerMessage.value = err.message
            }
        )
    }

    fun dismissPrinterDialog() {
        _showPrinterDialog.value = false
    }

    fun printToSelectedDevice(device: PairedPrinterDevice, textToPrint: String, companyHeader: String, isEnglish: Boolean) {
        _showPrinterDialog.value = false
        viewModelScope.launch {
            when (val res = printerHelper.printReceiptText(device.address, textToPrint, companyHeader, isEnglish)) {
                is PrintResult.Success -> _bannerMessage.value = res.message
                is PrintResult.Error -> _bannerMessage.value = res.message
                is PrintResult.PermissionRequired -> _requestBluetoothPermissionEvent.value = true
            }
        }
    }

    class Factory(
        private val repository: ServiceVoucherRepository,
        private val sessionStorage: SessionStorage,
        private val printerHelper: BluetoothPrinterHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServiceVoucherViewModel(
                repository = repository,
                sessionStorage = sessionStorage,
                printerHelper = printerHelper
            ) as T
        }
    }
}
