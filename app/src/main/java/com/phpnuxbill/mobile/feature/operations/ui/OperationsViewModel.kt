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
import com.phpnuxbill.mobile.feature.operations.model.ChangePasswordState
import com.phpnuxbill.mobile.feature.operations.model.CouponFormState
import com.phpnuxbill.mobile.feature.operations.model.CouponItem
import com.phpnuxbill.mobile.feature.operations.model.DashboardWidgetFormState
import com.phpnuxbill.mobile.feature.operations.model.DashboardWidgetItem
import com.phpnuxbill.mobile.feature.operations.model.DashboardWidgetsData
import com.phpnuxbill.mobile.feature.operations.model.DbStatusData
import com.phpnuxbill.mobile.feature.operations.model.FinancialReportData
import com.phpnuxbill.mobile.feature.operations.model.MessageLogsData
import com.phpnuxbill.mobile.feature.operations.model.MessageSendFormState
import com.phpnuxbill.mobile.feature.operations.model.NetworkMapData
import com.phpnuxbill.mobile.feature.operations.model.NotificationTemplateItem
import com.phpnuxbill.mobile.feature.operations.model.PgAuditData
import com.phpnuxbill.mobile.feature.operations.model.StaffFormState
import com.phpnuxbill.mobile.feature.operations.model.StaffUserItem
import com.phpnuxbill.mobile.feature.operations.model.StaticPageItem
import com.phpnuxbill.mobile.feature.operations.model.SystemLogItem
import com.phpnuxbill.mobile.feature.operations.model.WebServerSettings
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
class OperationsViewModel(
    private val repository: OperationsRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    // Laporan Keuangan
    private val _reportState = MutableStateFlow<UiState<FinancialReportData>>(UiState.Idle)
    val reportState: StateFlow<UiState<FinancialReportData>> = _reportState.asStateFlow()
    private val _reportStartDate = MutableStateFlow("")
    val reportStartDate: StateFlow<String> = _reportStartDate.asStateFlow()
    private val _reportEndDate = MutableStateFlow("")
    val reportEndDate: StateFlow<String> = _reportEndDate.asStateFlow()
    private val _reportSearch = MutableStateFlow("")
    val reportSearch: StateFlow<String> = _reportSearch.asStateFlow()
    private val _reportGatewayFilter = MutableStateFlow("all")
    val reportGatewayFilter: StateFlow<String> = _reportGatewayFilter.asStateFlow()
    private val _reportTypeFilter = MutableStateFlow("all")
    val reportTypeFilter: StateFlow<String> = _reportTypeFilter.asStateFlow()

    // Payment Gateway Audit
    private val _pgState = MutableStateFlow<UiState<PgAuditData>>(UiState.Idle)
    val pgState: StateFlow<UiState<PgAuditData>> = _pgState.asStateFlow()
    private val _pgStatusFilter = MutableStateFlow("all")
    val pgStatusFilter: StateFlow<String> = _pgStatusFilter.asStateFlow()
    private val _pgGatewayFilter = MutableStateFlow("all")
    val pgGatewayFilter: StateFlow<String> = _pgGatewayFilter.asStateFlow()
    private val _pgSearch = MutableStateFlow("")
    val pgSearch: StateFlow<String> = _pgSearch.asStateFlow()

    // Kupon & Aset
    private val _couponsState = MutableStateFlow<UiState<List<CouponItem>>>(UiState.Idle)
    val couponsState: StateFlow<UiState<List<CouponItem>>> = _couponsState.asStateFlow()
    private val _couponSearch = MutableStateFlow("")
    val couponSearch: StateFlow<String> = _couponSearch.asStateFlow()
    private val _couponForm = MutableStateFlow(CouponFormState())
    val couponForm: StateFlow<CouponFormState> = _couponForm.asStateFlow()

    private val _assetsState = MutableStateFlow<UiState<Pair<Double, List<AssetItem>>>>(UiState.Idle)
    val assetsState: StateFlow<UiState<Pair<Double, List<AssetItem>>>> = _assetsState.asStateFlow()
    private val _assetForm = MutableStateFlow(AssetFormState())
    val assetForm: StateFlow<AssetFormState> = _assetForm.asStateFlow()

    // Peta Jaringan
    private val _mapState = MutableStateFlow<UiState<NetworkMapData>>(UiState.Idle)
    val mapState: StateFlow<UiState<NetworkMapData>> = _mapState.asStateFlow()

    // Pesan, Riwayat Pesan (tbl_message_logs) & Halaman Statis
    private val _messageForm = MutableStateFlow(MessageSendFormState())
    val messageForm: StateFlow<MessageSendFormState> = _messageForm.asStateFlow()
    private val _messageLogsState = MutableStateFlow<UiState<MessageLogsData>>(UiState.Idle)
    val messageLogsState: StateFlow<UiState<MessageLogsData>> = _messageLogsState.asStateFlow()
    private val _messageLogTypeFilter = MutableStateFlow("all")
    val messageLogTypeFilter: StateFlow<String> = _messageLogTypeFilter.asStateFlow()
    private val _messageLogStatusFilter = MutableStateFlow("all")
    val messageLogStatusFilter: StateFlow<String> = _messageLogStatusFilter.asStateFlow()
    private val _messageLogSearch = MutableStateFlow("")
    val messageLogSearch: StateFlow<String> = _messageLogSearch.asStateFlow()
    private val _staticPagesState = MutableStateFlow<UiState<List<StaticPageItem>>>(UiState.Idle)
    val staticPagesState: StateFlow<UiState<List<StaticPageItem>>> = _staticPagesState.asStateFlow()
    private val _editingPage = MutableStateFlow<StaticPageItem?>(null)
    val editingPage: StateFlow<StaticPageItem?> = _editingPage.asStateFlow()

    // Pengaturan, Template Notifikasi, Widgets, Staf, DB & Logs
    private val _webSettings = MutableStateFlow(WebServerSettings())
    val webSettings: StateFlow<WebServerSettings> = _webSettings.asStateFlow()
    private var webSettingsLoaded = false
    private val _notifTemplates = MutableStateFlow(emptyList<NotificationTemplateItem>())
    val notifTemplates: StateFlow<List<NotificationTemplateItem>> = _notifTemplates.asStateFlow()

    private val _widgetsState = MutableStateFlow<UiState<DashboardWidgetsData>>(UiState.Idle)
    val widgetsState: StateFlow<UiState<DashboardWidgetsData>> = _widgetsState.asStateFlow()
    private val _selectedWidgetRole = MutableStateFlow("Admin")
    val selectedWidgetRole: StateFlow<String> = _selectedWidgetRole.asStateFlow()
    private val _widgetStructureInput = MutableStateFlow("")
    val widgetStructureInput: StateFlow<String> = _widgetStructureInput.asStateFlow()
    private val _widgetForm = MutableStateFlow(DashboardWidgetFormState())
    val widgetForm: StateFlow<DashboardWidgetFormState> = _widgetForm.asStateFlow()

    private val _staffListState = MutableStateFlow<UiState<List<StaffUserItem>>>(UiState.Loading)
    val staffListState: StateFlow<UiState<List<StaffUserItem>>> = _staffListState.asStateFlow()
    private val _staffForm = MutableStateFlow(StaffFormState())
    val staffForm: StateFlow<StaffFormState> = _staffForm.asStateFlow()
    private val _changePasswordState = MutableStateFlow(ChangePasswordState())
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()
    private val _dbStatusState = MutableStateFlow<UiState<DbStatusData>>(UiState.Loading)
    val dbStatusState: StateFlow<UiState<DbStatusData>> = _dbStatusState.asStateFlow()
    private val _systemLogs = MutableStateFlow<List<SystemLogItem>>(emptyList())
    val systemLogs: StateFlow<List<SystemLogItem>> = _systemLogs.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _reportSearch.drop(1).debounce(250L).distinctUntilChanged().collect { loadFinancialReports() }
        }
        viewModelScope.launch {
            _pgSearch.drop(1).debounce(250L).distinctUntilChanged().collect { loadPgTransactions() }
        }
        viewModelScope.launch {
            _couponSearch.drop(1).debounce(250L).distinctUntilChanged().collect { loadCoupons() }
        }
        viewModelScope.launch {
            _messageLogSearch.drop(1).debounce(250L).distinctUntilChanged().collect { loadMessageLogs() }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun ensureReportsLoaded() {
        if (_reportState.value is UiState.Idle) loadFinancialReports()
    }

    fun ensurePgLoaded() {
        if (_pgState.value is UiState.Idle) loadPgTransactions()
    }

    fun ensureCouponsLoaded() {
        if (_couponsState.value is UiState.Idle) loadCoupons()
    }

    fun ensureAssetsLoaded() {
        if (_assetsState.value is UiState.Idle) loadAssets()
    }

    fun ensureMapLoaded() {
        if (_mapState.value is UiState.Idle) loadNetworkMap()
    }

    fun ensureMessageLogsLoaded() {
        if (_messageLogsState.value is UiState.Idle) loadMessageLogs()
    }

    fun ensureStaticPagesLoaded() {
        if (_staticPagesState.value is UiState.Idle) loadStaticPages()
    }

    fun ensureWidgetsLoaded() {
        if (_widgetsState.value is UiState.Idle) loadWidgets()
    }

    fun ensureWebSettingsLoaded() {
        if (!webSettingsLoaded) {
            webSettingsLoaded = true
            loadWebSettings()
        }
    }

    // -------------------------------------------------------------------------
    // LAPORAN KEUANGAN
    // -------------------------------------------------------------------------
    fun onReportDateChanged(start: String, end: String) {
        _reportStartDate.value = start
        _reportEndDate.value = end
    }

    fun onReportSearchChanged(q: String) {
        _reportSearch.value = q
    }

    fun onReportGatewayFilterChanged(gw: String) {
        _reportGatewayFilter.value = gw
        loadFinancialReports()
    }

    fun onReportTypeFilterChanged(type: String) {
        _reportTypeFilter.value = type
        loadFinancialReports()
    }

    fun loadFinancialReports(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_reportState.value !is UiState.Success) _reportState.value = UiState.Loading

            when (
                val res = repository.getFinancialReports(
                    startDate = _reportStartDate.value,
                    endDate = _reportEndDate.value,
                    search = _reportSearch.value,
                    gateway = _reportGatewayFilter.value,
                    type = _reportTypeFilter.value
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

    // -------------------------------------------------------------------------
    // AUDIT PAYMENT GATEWAY
    // -------------------------------------------------------------------------
    fun onPgStatusFilterChanged(status: String) {
        _pgStatusFilter.value = status
        loadPgTransactions()
    }

    fun onPgGatewayFilterChanged(gw: String) {
        _pgGatewayFilter.value = gw
        loadPgTransactions()
    }

    fun onPgSearchChanged(q: String) {
        _pgSearch.value = q
    }

    fun loadPgTransactions(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_pgState.value !is UiState.Success) _pgState.value = UiState.Loading

            when (
                val res = repository.getPgTransactions(
                    status = _pgStatusFilter.value,
                    gateway = _pgGatewayFilter.value,
                    search = _pgSearch.value
                )
            ) {
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

    // -------------------------------------------------------------------------
    // KUPON PROMO & ASET JARINGAN
    // -------------------------------------------------------------------------
    fun onCouponSearchChanged(q: String) {
        _couponSearch.value = q
    }

    fun loadCoupons(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_couponsState.value !is UiState.Success) _couponsState.value = UiState.Loading

            when (val res = repository.getCoupons(search = _couponSearch.value)) {
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

    fun closeCouponForm() = _couponForm.update { it.copy(isOpen = false, error = null) }
    fun updateCouponForm(block: (CouponFormState) -> CouponFormState) = _couponForm.update { block(it).copy(error = null) }

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

    fun toggleCouponStatus(item: CouponItem) {
        val next = if (item.status == "active") "inactive" else "active"
        viewModelScope.launch {
            when (val res = repository.toggleCouponStatus(item.id, next)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadCoupons(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
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
                assetTag = item.assetTag,
                name = item.name,
                category = item.category.ifBlank { "Network Equipment" },
                brand = item.brand,
                modelName = item.modelName,
                serialNumber = item.serialNumber,
                cost = item.cost.toLong().toString(),
                purchaseDate = item.purchaseDate,
                warrantyExpiry = item.warrantyExpiry,
                location = item.location,
                coordinates = item.coordinates,
                status = item.status.ifBlank { "Active" },
                conditionStatus = item.conditionStatus.ifBlank { "Good" },
                notes = item.notes
            )
        }
    }

    fun closeAssetForm() = _assetForm.update { it.copy(isOpen = false, error = null) }
    fun updateAssetForm(block: (AssetFormState) -> AssetFormState) = _assetForm.update { block(it).copy(error = null) }

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

    fun loadNetworkMap() {
        viewModelScope.launch {
            _mapState.value = UiState.Loading
            when (val res = repository.getNetworkMapData()) {
                is ApiResult.Success -> _mapState.value = UiState.Success(res.data)
                is ApiResult.Error -> _mapState.value = UiState.Error(res.message)
            }
        }
    }

    // -------------------------------------------------------------------------
    // PESAN, RIWAYAT PESAN (tbl_message_logs) & HALAMAN STATIS
    // -------------------------------------------------------------------------
    fun updateMessageForm(block: (MessageSendFormState) -> MessageSendFormState) = _messageForm.update(block)

    fun submitSendMessage() {
        val f = _messageForm.value
        if (f.isSending) return
        viewModelScope.launch {
            _messageForm.update { it.copy(isSending = true) }
            when (val res = repository.sendMessage(f)) {
                is ApiResult.Success -> {
                    _messageForm.update { it.copy(isSending = false, message = "") }
                    _bannerMessage.value = res.message
                    if (_messageLogsState.value !is UiState.Idle) {
                        loadMessageLogs(isPullRefresh = true)
                    }
                }
                is ApiResult.Error -> {
                    _messageForm.update { it.copy(isSending = false) }
                    _bannerMessage.value = res.message
                    if (_messageLogsState.value !is UiState.Idle) {
                        loadMessageLogs(isPullRefresh = true)
                    }
                }
            }
        }
    }

    fun onMessageLogTypeFilterChanged(type: String) {
        _messageLogTypeFilter.value = type
        loadMessageLogs()
    }

    fun onMessageLogStatusFilterChanged(status: String) {
        _messageLogStatusFilter.value = status
        loadMessageLogs()
    }

    fun onMessageLogSearchChanged(q: String) {
        _messageLogSearch.value = q
    }

    fun loadMessageLogs(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_messageLogsState.value !is UiState.Success) _messageLogsState.value = UiState.Loading

            when (
                val res = repository.getMessageLogs(
                    messageType = _messageLogTypeFilter.value,
                    status = _messageLogStatusFilter.value,
                    search = _messageLogSearch.value
                )
            ) {
                is ApiResult.Success -> _messageLogsState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _messageLogsState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun loadStaticPages() {
        viewModelScope.launch {
            _staticPagesState.value = UiState.Loading
            when (val res = repository.getStaticPages()) {
                is ApiResult.Success -> _staticPagesState.value = UiState.Success(res.data)
                is ApiResult.Error -> _staticPagesState.value = UiState.Error(res.message)
            }
        }
    }

    fun openPageEditor(page: StaticPageItem) {
        _editingPage.value = page
    }

    fun updateEditingPageContent(newContent: String) {
        _editingPage.update { it?.copy(content = newContent) }
    }

    fun closePageEditor() {
        _editingPage.value = null
    }

    fun saveEditingPage() {
        val page = _editingPage.value ?: return
        viewModelScope.launch {
            when (val res = repository.saveStaticPage(page.name, page.content)) {
                is ApiResult.Success -> {
                    _editingPage.value = null
                    _bannerMessage.value = res.message
                    loadStaticPages()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // -------------------------------------------------------------------------
    // PENGATURAN, NOTIFIKASI, WIDGETS, STAF, DB & LOGS
    // -------------------------------------------------------------------------
    fun loadWebSettings() {
        viewModelScope.launch {
            _webSettings.update { it.copy(isLoading = true) }
            when (val res = repository.getWebSettings()) {
                is ApiResult.Success -> _webSettings.value = res.data.copy(isLoading = false)
                is ApiResult.Error -> _webSettings.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateWebSettings(block: (WebServerSettings) -> WebServerSettings) = _webSettings.update(block)

    fun submitWebSettings() {
        val s = _webSettings.value
        if (s.isSaving) return
        viewModelScope.launch {
            _webSettings.update { it.copy(isSaving = true) }
            when (val res = repository.saveWebSettings(s)) {
                is ApiResult.Success -> {
                    _webSettings.update { it.copy(isSaving = false) }
                    val sess = sessionStorage.getCurrentSession()
                    if (s.companyName.isNotBlank() && sess.activeServerUrl.isNotBlank()) {
                        sessionStorage.updateServerPreview(
                            serverUrl = sess.activeServerUrl,
                            companyName = s.companyName,
                            currencyCode = s.currencyCode.ifBlank { sess.currencyCode },
                            serverLogoUrl = sess.serverLogoUrl
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

    fun testNotifGateway(channel: String, target: String) {
        viewModelScope.launch {
            when (val res = repository.testNotificationGateway(channel, target)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun loadNotificationTemplates() {
        viewModelScope.launch {
            when (val res = repository.getNotificationTemplates()) {
                is ApiResult.Success -> _notifTemplates.value = res.data
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun updateNotifTemplate(key: String, value: String) {
        _notifTemplates.update { list ->
            list.map { item ->
                if (item.key == key) item.copy(value = value) else item
            }
        }
    }

    fun resetNotifTemplateToDefault(key: String) {
        _notifTemplates.update { list ->
            list.map { item ->
                if (item.key == key) item.copy(value = item.defaultValue) else item
            }
        }
    }

    fun saveNotificationTemplates() {
        viewModelScope.launch {
            when (val res = repository.saveNotificationTemplates(_notifTemplates.value)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // -------------------------------------------------------------------------
    // DASHBOARD WIDGETS (tbl_widgets — SELARAS widgets.php)
    // -------------------------------------------------------------------------
    fun onSelectWidgetRole(role: String) {
        if (_selectedWidgetRole.value.equals(role, ignoreCase = true)) return
        _selectedWidgetRole.value = role
        loadWidgets(isPullRefresh = false)
    }

    fun onWidgetStructureInputChanged(value: String) {
        _widgetStructureInput.value = value
    }

    fun loadWidgets(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else _widgetsState.value = UiState.Loading

            when (val res = repository.getWidgets(userRole = _selectedWidgetRole.value)) {
                is ApiResult.Success -> {
                    _widgetStructureInput.value = res.data.dashboardStructure
                    _widgetsState.value = UiState.Success(res.data)
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _widgetsState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun openWidgetForm(item: DashboardWidgetItem? = null) {
        val loaded = (_widgetsState.value as? UiState.Success)?.data
        val activeRole = _selectedWidgetRole.value
        _widgetForm.value = if (item == null) {
            val defaultMod = loaded?.availableWidgets?.firstOrNull()
            val nextOrder = (loaded?.widgets?.maxOfOrNull { it.orders } ?: 0) + 1
            DashboardWidgetFormState(
                isOpen = true,
                isEdit = false,
                user = activeRole,
                title = defaultMod?.label.orEmpty(),
                widget = defaultMod?.key.orEmpty(),
                position = "1",
                orders = nextOrder.toString(),
                enabled = true
            )
        } else {
            DashboardWidgetFormState(
                isOpen = true,
                isEdit = true,
                id = item.id,
                user = item.user.ifBlank { activeRole },
                title = item.title,
                widget = item.widget,
                position = item.position.toString(),
                orders = item.orders.toString(),
                enabled = item.enabled,
                content = item.content
            )
        }
    }

    fun closeWidgetForm() = _widgetForm.update { it.copy(isOpen = false, error = null) }
    fun updateWidgetForm(block: (DashboardWidgetFormState) -> DashboardWidgetFormState) =
        _widgetForm.update { block(it).copy(error = null) }

    fun submitWidgetForm() {
        val f = _widgetForm.value
        if (f.isSaving) return
        viewModelScope.launch {
            _widgetForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveWidget(f)) {
                is ApiResult.Success -> {
                    _widgetForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadWidgets(isPullRefresh = true)
                }
                is ApiResult.Error -> _widgetForm.update { it.copy(isSaving = false, error = res.message) }
            }
        }
    }

    fun toggleWidgetEnabled(item: DashboardWidgetItem) {
        viewModelScope.launch {
            when (val res = repository.toggleWidget(item.id, !item.enabled)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadWidgets(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun moveWidgetOrder(item: DashboardWidgetItem, moveUp: Boolean) {
        val data = (_widgetsState.value as? UiState.Success)?.data ?: return
        val samePos = data.widgets
            .filter { it.position == item.position }
            .sortedWith(compareBy<DashboardWidgetItem> { it.orders }.thenBy { it.id })
            .toMutableList()
        val idx = samePos.indexOfFirst { it.id == item.id }
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (idx < 0 || targetIdx !in samePos.indices) return

        val swapped = samePos[targetIdx]
        samePos[targetIdx] = samePos[idx]
        samePos[idx] = swapped

        val updates = samePos.mapIndexed { orderIdx, w ->
            Triple(w.id, orderIdx + 1, w.position)
        }
        viewModelScope.launch {
            when (val res = repository.reorderWidgets(updates)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadWidgets(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun deleteWidget(widgetId: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteWidget(widgetId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadWidgets(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun saveWidgetStructure() {
        val structVal = _widgetStructureInput.value.trim()
        if (structVal.isBlank()) return
        viewModelScope.launch {
            when (val res = repository.saveWidgetStructure(_selectedWidgetRole.value, structVal)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadWidgets(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun loadStaffList() {
        viewModelScope.launch {
            _staffListState.value = UiState.Loading
            when (val res = repository.getStaffList()) {
                is ApiResult.Success -> _staffListState.value = UiState.Success(res.data)
                is ApiResult.Error -> _staffListState.value = UiState.Error(res.message)
            }
        }
    }

    fun openStaffForm(item: StaffUserItem? = null) {
        _staffForm.value = if (item == null) {
            StaffFormState(isOpen = true, isEdit = false)
        } else {
            StaffFormState(
                isOpen = true,
                isEdit = true,
                id = item.id,
                username = item.username,
                fullname = item.fullname,
                userType = item.userType,
                phone = item.phone,
                email = item.email,
                city = item.city,
                status = item.status,
                rootAgentId = item.root
            )
        }
    }

    fun closeStaffForm() = _staffForm.update { it.copy(isOpen = false, error = null) }
    fun updateStaffForm(block: (StaffFormState) -> StaffFormState) = _staffForm.update { block(it).copy(error = null) }

    fun submitStaffForm() {
        val f = _staffForm.value
        if (f.isSaving) return
        viewModelScope.launch {
            _staffForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveStaff(f)) {
                is ApiResult.Success -> {
                    _staffForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadStaffList()
                }
                is ApiResult.Error -> _staffForm.update { it.copy(isSaving = false, error = res.message) }
            }
        }
    }

    fun deleteStaff(id: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteStaff(id)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadStaffList()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun updateChangePassword(block: (ChangePasswordState) -> ChangePasswordState) = _changePasswordState.update(block)

    fun submitChangePassword() {
        val st = _changePasswordState.value
        if (st.isSaving) return
        viewModelScope.launch {
            _changePasswordState.update { it.copy(isSaving = true) }
            when (val res = repository.changeAdminPassword(st)) {
                is ApiResult.Success -> {
                    _changePasswordState.value = ChangePasswordState()
                    _bannerMessage.value = res.message
                }
                is ApiResult.Error -> {
                    _changePasswordState.update { it.copy(isSaving = false) }
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun loadDbStatusAndLogs() {
        viewModelScope.launch {
            _dbStatusState.value = UiState.Loading
            when (val res = repository.getDatabaseStatus()) {
                is ApiResult.Success -> _dbStatusState.value = UiState.Success(res.data)
                is ApiResult.Error -> _dbStatusState.value = UiState.Error(res.message)
            }
            when (val lRes = repository.getSystemLogs(limit = 100)) {
                is ApiResult.Success -> _systemLogs.value = lRes.data
                is ApiResult.Error -> {}
            }
        }
    }

    fun triggerDbBackup() {
        viewModelScope.launch {
            when (val res = repository.backupDatabase()) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
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

    fun setAppLanguage(lang: AppLanguage) = viewModelScope.launch { sessionStorage.setLanguage(lang.code) }
    fun setAppTheme(mode: AppThemeMode) = viewModelScope.launch { sessionStorage.setThemeMode(mode) }

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
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OperationsViewModel(repository, sessionStorage) as T
    }
}
