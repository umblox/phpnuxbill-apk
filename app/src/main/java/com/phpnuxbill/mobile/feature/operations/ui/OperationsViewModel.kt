package com.phpnuxbill.mobile.feature.operations.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.operations.data.OperationsRepository
import com.phpnuxbill.mobile.feature.operations.model.AssetFormState
import com.phpnuxbill.mobile.feature.operations.model.AssetItem
import com.phpnuxbill.mobile.feature.operations.model.CouponFormState
import com.phpnuxbill.mobile.feature.operations.model.CouponItem
import com.phpnuxbill.mobile.feature.operations.model.FinancialReportData
import com.phpnuxbill.mobile.feature.operations.model.MessageSendFormState
import com.phpnuxbill.mobile.feature.operations.model.NetworkMapData
import com.phpnuxbill.mobile.feature.operations.model.PgAuditData
import com.phpnuxbill.mobile.feature.operations.model.WebServerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OperationsViewModel(
    private val repository: OperationsRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    // Laporan Keuangan
    private val _reportState = MutableStateFlow<UiState<FinancialReportData>>(UiState.Loading)
    val reportState: StateFlow<UiState<FinancialReportData>> = _reportState.asStateFlow()

    private val _reportStartDate = MutableStateFlow("")
    val reportStartDate: StateFlow<String> = _reportStartDate.asStateFlow()

    private val _reportEndDate = MutableStateFlow("")
    val reportEndDate: StateFlow<String> = _reportEndDate.asStateFlow()

    // Payment Gateway Audit
    private val _pgState = MutableStateFlow<UiState<PgAuditData>>(UiState.Loading)
    val pgState: StateFlow<UiState<PgAuditData>> = _pgState.asStateFlow()

    private val _pgStatusFilter = MutableStateFlow("all")
    val pgStatusFilter: StateFlow<String> = _pgStatusFilter.asStateFlow()

    // Kupon & Aset
    private val _couponsState = MutableStateFlow<UiState<List<CouponItem>>>(UiState.Loading)
    val couponsState: StateFlow<UiState<List<CouponItem>>> = _couponsState.asStateFlow()

    private val _couponForm = MutableStateFlow(CouponFormState())
    val couponForm: StateFlow<CouponFormState> = _couponForm.asStateFlow()

    private val _assetsState = MutableStateFlow<UiState<Pair<Double, List<AssetItem>>>>(UiState.Loading)
    val assetsState: StateFlow<UiState<Pair<Double, List<AssetItem>>>> = _assetsState.asStateFlow()

    private val _assetForm = MutableStateFlow(AssetFormState())
    val assetForm: StateFlow<AssetFormState> = _assetForm.asStateFlow()

    // Peta Jaringan
    private val _mapState = MutableStateFlow<UiState<NetworkMapData>>(UiState.Loading)
    val mapState: StateFlow<UiState<NetworkMapData>> = _mapState.asStateFlow()

    // Kirim Pesan / Broadcast
    private val _messageForm = MutableStateFlow(MessageSendFormState())
    val messageForm: StateFlow<MessageSendFormState> = _messageForm.asStateFlow()

    // Pengaturan Web & Aplikasi
    private val _webSettings = MutableStateFlow(WebServerSettings())
    val webSettings: StateFlow<WebServerSettings> = _webSettings.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        loadFinancialReports()
        loadPgTransactions()
        loadCoupons()
        loadAssets()
        loadNetworkMap()
        loadWebSettings()
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    // Laporan Keuangan
    fun onReportDateChanged(start: String, end: String) {
        _reportStartDate.value = start
        _reportEndDate.value = end
    }

    fun loadFinancialReports(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_reportState.value !is UiState.Success) _reportState.value = UiState.Loading

            when (
                val res = repository.getFinancialReports(
                    startDate = _reportStartDate.value,
                    endDate = _reportEndDate.value
                )
            ) {
                is ApiResult.Success -> {
                    if (_reportStartDate.value.isBlank()) _reportStartDate.value = res.data.startDate
                    if (_reportEndDate.value.isBlank()) _reportEndDate.value = res.data.endDate
                    _reportState.value = UiState.Success(res.data)
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _reportState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    // Payment Gateway
    fun onPgStatusFilterChanged(status: String) {
        _pgStatusFilter.value = status
        loadPgTransactions()
    }

    fun loadPgTransactions(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_pgState.value !is UiState.Success) _pgState.value = UiState.Loading

            when (val res = repository.getPgTransactions(status = _pgStatusFilter.value)) {
                is ApiResult.Success -> _pgState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _pgState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun triggerPgAction(subAction: String, trxId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.executePgAction(subAction, trxId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPgTransactions(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    // Kupon
    fun loadCoupons(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_couponsState.value !is UiState.Success) _couponsState.value = UiState.Loading

            when (val res = repository.getCoupons()) {
                is ApiResult.Success -> _couponsState.value = UiState.Success(res.data)
                is ApiResult.Error -> _couponsState.value = UiState.Error(res.message)
            }
            _isRefreshing.value = false
        }
    }

    fun openCouponForm(item: CouponItem? = null) {
        _couponForm.value = if (item == null) {
            CouponFormState(isOpen = true)
        } else {
            CouponFormState(
                isOpen = true,
                id = item.id,
                code = item.code,
                type = item.type,
                value = item.value.toLong().toString(),
                description = item.description,
                maxUsage = item.maxUsage.toString(),
                minOrderAmount = item.minOrderAmount.toLong().toString(),
                maxDiscountAmount = item.maxDiscountAmount.toLong().toString(),
                status = item.status,
                startDate = item.startDate,
                endDate = item.endDate
            )
        }
    }

    fun closeCouponForm() {
        _couponForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateCouponForm(block: (CouponFormState) -> CouponFormState) {
        _couponForm.update { block(it).copy(error = null) }
    }

    fun submitCouponForm() {
        val f = _couponForm.value
        if (f.isSaving) return
        viewModelScope.launch {
            _couponForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveCoupon(f)) {
                is ApiResult.Success -> {
                    _couponForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadCoupons(isPullRefresh = true)
                }
                is ApiResult.Error -> _couponForm.update { it.copy(isSaving = false, error = res.message) }
            }
        }
    }

    fun deleteCoupon(id: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteCoupon(id)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadCoupons(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // Aset Jaringan
    fun loadAssets(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_assetsState.value !is UiState.Success) _assetsState.value = UiState.Loading

            when (val res = repository.getAssets()) {
                is ApiResult.Success -> _assetsState.value = UiState.Success(res.data)
                is ApiResult.Error -> _assetsState.value = UiState.Error(res.message)
            }
            _isRefreshing.value = false
        }
    }

    fun openAssetForm(item: AssetItem? = null) {
        _assetForm.value = if (item == null) {
            AssetFormState(isOpen = true)
        } else {
            AssetFormState(
                isOpen = true,
                id = item.id,
                name = item.name,
                category = item.category,
                brand = item.brand,
                serialNumber = item.serialNumber,
                cost = item.cost.toLong().toString(),
                location = item.location,
                coordinates = item.coordinates,
                conditionStatus = item.conditionStatus,
                notes = item.notes
            )
        }
    }

    fun closeAssetForm() {
        _assetForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateAssetForm(block: (AssetFormState) -> AssetFormState) {
        _assetForm.update { block(it).copy(error = null) }
    }

    fun submitAssetForm() {
        val f = _assetForm.value
        if (f.isSaving) return
        viewModelScope.launch {
            _assetForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveAsset(f)) {
                is ApiResult.Success -> {
                    _assetForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadAssets(isPullRefresh = true)
                }
                is ApiResult.Error -> _assetForm.update { it.copy(isSaving = false, error = res.message) }
            }
        }
    }

    fun deleteAsset(id: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteAsset(id)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadAssets(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // Peta Jaringan
    fun loadNetworkMap() {
        viewModelScope.launch {
            _mapState.value = UiState.Loading
            when (val res = repository.getNetworkMapData()) {
                is ApiResult.Success -> _mapState.value = UiState.Success(res.data)
                is ApiResult.Error -> _mapState.value = UiState.Error(res.message)
            }
        }
    }

    // Kirim Pesan / Broadcast
    fun updateMessageForm(block: (MessageSendFormState) -> MessageSendFormState) {
        _messageForm.update(block)
    }

    fun submitSendMessage() {
        val f = _messageForm.value
        if (f.isSending) return
        viewModelScope.launch {
            _messageForm.update { it.copy(isSending = true) }
            when (val res = repository.sendMessage(f)) {
                is ApiResult.Success -> {
                    _messageForm.update { it.copy(isSending = false, message = "") }
                    _bannerMessage.value = res.message
                }
                is ApiResult.Error -> {
                    _messageForm.update { it.copy(isSending = false) }
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    // Pengaturan Web & Preferensi Aplikasi (Logo Galeri, Bahasa ID/EN, Tema)
    fun loadWebSettings() {
        viewModelScope.launch {
            _webSettings.update { it.copy(isLoading = true) }
            when (val res = repository.getWebSettings()) {
                is ApiResult.Success -> _webSettings.value = res.data.copy(isLoading = false)
                is ApiResult.Error -> _webSettings.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateWebSettings(block: (WebServerSettings) -> WebServerSettings) {
        _webSettings.update(block)
    }

    fun submitWebSettings() {
        val s = _webSettings.value
        if (s.isSaving) return
        viewModelScope.launch {
            _webSettings.update { it.copy(isSaving = true) }
            when (val res = repository.saveWebSettings(s)) {
                is ApiResult.Success -> {
                    _webSettings.update { it.copy(isSaving = false) }
                    val currentSess = sessionStorage.getCurrentSession()
                    if (s.companyName.isNotBlank() && currentSess.activeServerUrl.isNotBlank()) {
                        sessionStorage.updateServerPreview(
                            serverUrl = currentSess.activeServerUrl,
                            companyName = s.companyName,
                            currencyCode = s.currencyCode.ifBlank { currentSess.currencyCode },
                            serverLogoUrl = currentSess.serverLogoUrl
                        )
                    }
                    _bannerMessage.value = res.message
                }
                is ApiResult.Error -> {
                    _webSettings.update { it.copy(isSaving = false) }
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun runSystemTool(tool: String) {
        viewModelScope.launch {
            when (val res = repository.runSystemTool(tool)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun setAppLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            sessionStorage.setLanguage(lang.code)
        }
    }

    fun setAppTheme(mode: AppThemeMode) {
        viewModelScope.launch {
            sessionStorage.setThemeMode(mode)
        }
    }

    fun onCustomLogoSelectedFromGallery(uri: Uri) {
        viewModelScope.launch {
            val savedPath = sessionStorage.saveCustomLogoFromUri(uri)
            if (savedPath != null) {
                val isEn = sessionStorage.getCurrentSession().languageCode == "en"
                _bannerMessage.value = if (isEn) "Custom logo updated." else "Logo perusahaan di aplikasi diperbarui."
            }
        }
    }

    fun onResetCustomLogo() {
        viewModelScope.launch {
            sessionStorage.clearCustomLogo()
            val isEn = sessionStorage.getCurrentSession().languageCode == "en"
            _bannerMessage.value = if (isEn) "Default logo restored." else "Logo dikembalikan ke bawaan."
        }
    }

    class Factory(
        private val repository: OperationsRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OperationsViewModel(
                repository = repository,
                sessionStorage = sessionStorage
            ) as T
        }
    }
}
