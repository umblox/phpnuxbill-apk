package com.phpnuxbill.mobile.feature.network.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.network.data.NetworkGenieAcsRepository
import com.phpnuxbill.mobile.feature.network.model.GenieAcsDeviceDetail
import com.phpnuxbill.mobile.feature.network.model.GenieAcsDeviceItem
import com.phpnuxbill.mobile.feature.network.model.GenieAcsHost
import com.phpnuxbill.mobile.feature.network.model.GenieAcsHostFormState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsStats
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWanEditState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWanItem
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWlanEditState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWlanItem
import com.phpnuxbill.mobile.feature.network.model.IpPoolFormState
import com.phpnuxbill.mobile.feature.network.model.IpPoolItem
import com.phpnuxbill.mobile.feature.network.model.OnlineSessionUser
import com.phpnuxbill.mobile.feature.network.model.OnlineUsersSummary
import com.phpnuxbill.mobile.feature.network.model.RouterFormState
import com.phpnuxbill.mobile.feature.network.model.RouterItem
import com.phpnuxbill.mobile.feature.network.model.RouterLiveStatus
import com.phpnuxbill.mobile.feature.network.model.RouterLogEntry
import com.phpnuxbill.mobile.feature.network.model.TrafficSpeedSample
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class NetworkGenieAcsViewModel(
    private val repository: NetworkGenieAcsRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    // =========================================================================
    // MIKROTIK & NETWORK STATES
    // =========================================================================

    private val _selectedMikrotikTab = MutableStateFlow(0) // 0=Live, 1=Online, 2=Routers/Pools, 3=Logs
    val selectedMikrotikTab: StateFlow<Int> = _selectedMikrotikTab.asStateFlow()

    private val _routers = MutableStateFlow<List<RouterItem>>(emptyList())
    val routers: StateFlow<List<RouterItem>> = _routers.asStateFlow()

    private val _selectedRouterId = MutableStateFlow(0)
    val selectedRouterId: StateFlow<Int> = _selectedRouterId.asStateFlow()

    private val _selectedInterface = MutableStateFlow("")
    val selectedInterface: StateFlow<String> = _selectedInterface.asStateFlow()

    private val _liveStatusState = MutableStateFlow<UiState<RouterLiveStatus>>(UiState.Loading)
    val liveStatusState: StateFlow<UiState<RouterLiveStatus>> = _liveStatusState.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<TrafficSpeedSample>>(emptyList())
    val speedHistory: StateFlow<List<TrafficSpeedSample>> = _speedHistory.asStateFlow()

    private val _isPollingLive = MutableStateFlow(true)
    val isPollingLive: StateFlow<Boolean> = _isPollingLive.asStateFlow()

    private val _onlineUsersState = MutableStateFlow<UiState<OnlineUsersSummary>>(UiState.Loading)
    val onlineUsersState: StateFlow<UiState<OnlineUsersSummary>> = _onlineUsersState.asStateFlow()

    private val _onlineUserSearch = MutableStateFlow("")
    val onlineUserSearch: StateFlow<String> = _onlineUserSearch.asStateFlow()

    private val _ipPools = MutableStateFlow<List<IpPoolItem>>(emptyList())
    val ipPools: StateFlow<List<IpPoolItem>> = _ipPools.asStateFlow()

    private val _routerLogs = MutableStateFlow<List<RouterLogEntry>>(emptyList())
    val routerLogs: StateFlow<List<RouterLogEntry>> = _routerLogs.asStateFlow()

    private val _routerForm = MutableStateFlow(RouterFormState())
    val routerForm: StateFlow<RouterFormState> = _routerForm.asStateFlow()

    private val _poolForm = MutableStateFlow(IpPoolFormState())
    val poolForm: StateFlow<IpPoolFormState> = _poolForm.asStateFlow()

    // =========================================================================
    // GENIEACS TR-069 STATES
    // =========================================================================

    private val _genieHosts = MutableStateFlow<List<GenieAcsHost>>(emptyList())
    val genieHosts: StateFlow<List<GenieAcsHost>> = _genieHosts.asStateFlow()

    private val _selectedGenieHostId = MutableStateFlow("")
    val selectedGenieHostId: StateFlow<String> = _selectedGenieHostId.asStateFlow()

    private val _genieStats = MutableStateFlow(GenieAcsStats())
    val genieStats: StateFlow<GenieAcsStats> = _genieStats.asStateFlow()

    private val _genieDevicesState = MutableStateFlow<UiState<List<GenieAcsDeviceItem>>>(UiState.Loading)
    val genieDevicesState: StateFlow<UiState<List<GenieAcsDeviceItem>>> = _genieDevicesState.asStateFlow()

    private val _genieSearch = MutableStateFlow("")
    val genieSearch: StateFlow<String> = _genieSearch.asStateFlow()

    private val _genieStatusFilter = MutableStateFlow("all")
    val genieStatusFilter: StateFlow<String> = _genieStatusFilter.asStateFlow()

    private val _deviceDetailState = MutableStateFlow<UiState<GenieAcsDeviceDetail>?>(null)
    val deviceDetailState: StateFlow<UiState<GenieAcsDeviceDetail>?> = _deviceDetailState.asStateFlow()

    private val _showHostsManager = MutableStateFlow(false)
    val showHostsManager: StateFlow<Boolean> = _showHostsManager.asStateFlow()

    private val _hostForm = MutableStateFlow(GenieAcsHostFormState())
    val hostForm: StateFlow<GenieAcsHostFormState> = _hostForm.asStateFlow()

    private val _wlanEditForm = MutableStateFlow(GenieAcsWlanEditState())
    val wlanEditForm: StateFlow<GenieAcsWlanEditState> = _wlanEditForm.asStateFlow()

    private val _wanEditForm = MutableStateFlow(GenieAcsWanEditState())
    val wanEditForm: StateFlow<GenieAcsWanEditState> = _wanEditForm.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadRoutersAndPools()
        loadGenieAcsDevices()

        viewModelScope.launch {
            _genieSearch.drop(1).debounce(250L).distinctUntilChanged().collect {
                loadGenieAcsDevices()
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    // =========================================================================
    // MIKROTIK LIVE POLLING & OPERATIONS
    // =========================================================================

    fun onMikrotikTabSelected(index: Int) {
        _selectedMikrotikTab.value = index
        when (index) {
            0 -> startLivePolling()
            1 -> {
                stopLivePolling()
                loadOnlineUsers()
            }
            2 -> {
                stopLivePolling()
                loadRoutersAndPools()
            }
            3 -> {
                stopLivePolling()
                loadRouterLogs()
            }
        }
    }

    fun startLivePolling() {
        if (pollingJob?.isActive == true) return
        _isPollingLive.value = true
        pollingJob = viewModelScope.launch {
            while (isActive && _isPollingLive.value) {
                fetchSingleLiveSample(silent = (_liveStatusState.value is UiState.Success))
                delay(2_500L)
            }
        }
    }

    fun stopLivePolling() {
        _isPollingLive.value = false
        pollingJob?.cancel()
        pollingJob = null
    }

    fun toggleLivePolling() {
        if (_isPollingLive.value) stopLivePolling() else startLivePolling()
    }

    fun onSelectRouter(routerId: Int) {
        if (_selectedRouterId.value == routerId) return
        _selectedRouterId.value = routerId
        _selectedInterface.value = ""
        _speedHistory.value = emptyList()
        fetchSingleLiveSample(silent = false)
        if (_selectedMikrotikTab.value == 1) loadOnlineUsers()
        if (_selectedMikrotikTab.value == 3) loadRouterLogs()
    }

    fun onSelectInterface(ifaceName: String) {
        if (_selectedInterface.value == ifaceName) return
        _selectedInterface.value = ifaceName
        _speedHistory.value = emptyList()
        fetchSingleLiveSample(silent = false)
    }

    fun fetchSingleLiveSample(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent && _liveStatusState.value !is UiState.Success) {
                _liveStatusState.value = UiState.Loading
            }
            when (
                val res = repository.getMikrotikLive(
                    routerId = _selectedRouterId.value,
                    interfaceName = _selectedInterface.value
                )
            ) {
                is ApiResult.Success -> {
                    val status = res.data.firstOrNull()
                    if (status != null) {
                        if (_selectedRouterId.value == 0) {
                            _selectedRouterId.value = status.id
                        }
                        if (_selectedInterface.value.isBlank() && status.monitoredInterface.isNotBlank()) {
                            _selectedInterface.value = status.monitoredInterface
                        }
                        _liveStatusState.value = UiState.Success(status)
                        val newSample = TrafficSpeedSample(
                            timestampMs = System.currentTimeMillis(),
                            rxBps = status.rxBps,
                            txBps = status.txBps
                        )
                        _speedHistory.update { old -> (old + newSample).takeLast(30) }
                    } else {
                        _liveStatusState.value = UiState.Error("Router belum dikonfigurasi.")
                    }
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) {
                        stopLivePolling()
                        sessionStorage.clearActiveToken()
                    } else if (!silent) {
                        _liveStatusState.value = UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun onOnlineUserSearchChanged(q: String) {
        _onlineUserSearch.value = q
    }

    fun loadOnlineUsers(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_onlineUsersState.value !is UiState.Success) _onlineUsersState.value = UiState.Loading

            when (val res = repository.getOnlineUsers(routerId = _selectedRouterId.value)) {
                is ApiResult.Success -> _onlineUsersState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _onlineUsersState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun kickUser(user: OnlineSessionUser) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (
                val res = repository.kickOnlineUser(
                    routerName = user.router,
                    service = user.service,
                    rosId = user.rosId,
                    username = user.username
                )
            ) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadOnlineUsers(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun rebootSelectedRouter(routerId: Int) {
        viewModelScope.launch {
            when (val res = repository.rebootRouter(routerId)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    fun loadRouterLogs() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.getRouterLogs(_selectedRouterId.value)) {
                is ApiResult.Success -> _routerLogs.value = res.data
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
            _isRefreshing.value = false
        }
    }

    fun loadRoutersAndPools() {
        viewModelScope.launch {
            when (val rRes = repository.getRouters()) {
                is ApiResult.Success -> {
                    _routers.value = rRes.data
                    if (_selectedRouterId.value == 0 && rRes.data.isNotEmpty()) {
                        _selectedRouterId.value = rRes.data.first().id
                    }
                }
                is ApiResult.Error -> {}
            }
            when (val pRes = repository.getIpPools()) {
                is ApiResult.Success -> _ipPools.value = pRes.data
                is ApiResult.Error -> {}
            }
        }
    }

    // CRUD Router
    fun openAddRouterForm() {
        _routerForm.value = RouterFormState(isOpen = true, isEdit = false)
    }

    fun openEditRouterForm(r: RouterItem) {
        _routerForm.value = RouterFormState(
            isOpen = true,
            isEdit = true,
            id = r.id,
            name = r.name,
            ipAddress = r.ipAddress,
            username = r.username,
            description = r.description,
            coordinates = r.coordinates,
            coverage = r.coverage,
            enabled = r.enabled
        )
    }

    fun closeRouterForm() {
        _routerForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateRouterForm(block: (RouterFormState) -> RouterFormState) {
        _routerForm.update { block(it).copy(error = null) }
    }

    fun submitRouterForm() {
        val form = _routerForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _routerForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveRouter(form)) {
                is ApiResult.Success -> {
                    _routerForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadRoutersAndPools()
                }
                is ApiResult.Error -> {
                    _routerForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    fun deleteRouter(routerId: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteRouter(routerId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadRoutersAndPools()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // CRUD IP Pool
    fun openAddPoolForm() {
        _poolForm.value = IpPoolFormState(
            isOpen = true,
            isEdit = false,
            routers = _routers.value.firstOrNull()?.name.orEmpty()
        )
    }

    fun openEditPoolForm(p: IpPoolItem) {
        _poolForm.value = IpPoolFormState(
            isOpen = true,
            isEdit = true,
            id = p.id,
            poolName = p.poolName,
            localIp = p.localIp,
            rangeIp = p.rangeIp,
            routers = p.routers
        )
    }

    fun closePoolForm() {
        _poolForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updatePoolForm(block: (IpPoolFormState) -> IpPoolFormState) {
        _poolForm.update { block(it).copy(error = null) }
    }

    fun submitPoolForm() {
        val form = _poolForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _poolForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveIpPool(form)) {
                is ApiResult.Success -> {
                    _poolForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadRoutersAndPools()
                }
                is ApiResult.Error -> {
                    _poolForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    fun deletePool(poolId: Int) {
        viewModelScope.launch {
            when (val res = repository.deleteIpPool(poolId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadRoutersAndPools()
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // =========================================================================
    // GENIEACS TR-069 OPERATIONS
    // =========================================================================

    fun onGenieSearchChanged(q: String) {
        _genieSearch.value = q
    }

    fun onGenieStatusFilterChanged(status: String) {
        _genieStatusFilter.value = status
        loadGenieAcsDevices()
    }

    fun onSelectGenieHost(hostId: String) {
        _selectedGenieHostId.value = hostId
        loadGenieAcsDevices()
    }

    fun loadGenieAcsDevices(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_genieDevicesState.value !is UiState.Success) _genieDevicesState.value = UiState.Loading

            when (
                val res = repository.getGenieAcsDashboardAndDevices(
                    hostId = _selectedGenieHostId.value,
                    search = _genieSearch.value,
                    statusFilter = _genieStatusFilter.value
                )
            ) {
                is ApiResult.Success -> {
                    val (hosts, stats, devices) = res.data
                    _genieHosts.value = hosts
                    if (_selectedGenieHostId.value.isBlank()) {
                        val active = hosts.find { it.active } ?: hosts.firstOrNull()
                        if (active != null) _selectedGenieHostId.value = active.id
                    }
                    _genieStats.value = stats
                    _genieDevicesState.value = UiState.Success(devices)
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) {
                        sessionStorage.clearActiveToken()
                    } else {
                        // Tetap muat daftar host agar tombol kelola host tetap tampil jika server belum diset
                        when (val hRes = repository.getGenieAcsHosts()) {
                            is ApiResult.Success -> _genieHosts.value = hRes.data.first
                            else -> {}
                        }
                        _genieDevicesState.value = UiState.Error(res.message)
                    }
                }
            }
            _isRefreshing.value = false
        }
    }

    fun openDeviceDetail(deviceId: String) {
        viewModelScope.launch {
            _deviceDetailState.value = UiState.Loading
            when (
                val res = repository.getGenieAcsDeviceDetail(
                    hostId = _selectedGenieHostId.value,
                    deviceId = deviceId
                )
            ) {
                is ApiResult.Success -> _deviceDetailState.value = UiState.Success(res.data)
                is ApiResult.Error -> _deviceDetailState.value = UiState.Error(res.message)
            }
        }
    }

    fun closeDeviceDetail() {
        _deviceDetailState.value = null
    }

    fun triggerDeviceAction(deviceId: String, subAction: String, tag: String = "") {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (
                val res = repository.executeDeviceAction(
                    hostId = _selectedGenieHostId.value,
                    deviceId = deviceId,
                    subAction = subAction,
                    tag = tag
                )
            ) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    if (subAction == "delete_device" || subAction == "delete") {
                        _deviceDetailState.value = null
                        loadGenieAcsDevices(isPullRefresh = true)
                    } else if (_deviceDetailState.value != null && deviceId.isNotBlank()) {
                        delay(1_200L)
                        openDeviceDetail(deviceId)
                    } else {
                        loadGenieAcsDevices(isPullRefresh = true)
                    }
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
            _isRefreshing.value = false
        }
    }

    // Host Manager
    fun openHostsManager() {
        _showHostsManager.value = true
    }

    fun closeHostsManager() {
        _showHostsManager.value = false
    }

    fun openAddHostForm() {
        _hostForm.value = GenieAcsHostFormState(isOpen = true)
    }

    fun openEditHostForm(h: GenieAcsHost) {
        _hostForm.value = GenieAcsHostFormState(
            isOpen = true,
            id = h.id,
            name = h.name,
            host = h.host,
            port = h.port.toString(),
            protocol = h.protocol,
            username = h.username
        )
    }

    fun closeHostForm() {
        _hostForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateHostForm(block: (GenieAcsHostFormState) -> GenieAcsHostFormState) {
        _hostForm.update { block(it).copy(error = null) }
    }

    fun submitHostForm() {
        val form = _hostForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _hostForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveGenieAcsHost(form)) {
                is ApiResult.Success -> {
                    _hostForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadGenieAcsDevices(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _hostForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    fun triggerHostAction(subAction: String, hostId: String) {
        viewModelScope.launch {
            when (val res = repository.executeHostAction(subAction, hostId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    if (subAction == "setactive") _selectedGenieHostId.value = hostId
                    loadGenieAcsDevices(isPullRefresh = true)
                }
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
        }
    }

    // WLAN & WAN Editors
    fun openWlanEditor(deviceId: String, wlan: GenieAcsWlanItem) {
        _wlanEditForm.value = GenieAcsWlanEditState(
            isOpen = true,
            deviceId = deviceId,
            wlanIndex = wlan.index,
            ssid = wlan.ssid,
            password = wlan.password,
            securityMode = wlan.securityMode,
            channel = wlan.channel,
            enable = wlan.enable,
            source = wlan.source
        )
    }

    fun closeWlanEditor() {
        _wlanEditForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateWlanEditor(block: (GenieAcsWlanEditState) -> GenieAcsWlanEditState) {
        _wlanEditForm.update { block(it).copy(error = null) }
    }

    fun submitWlanEdit() {
        val form = _wlanEditForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _wlanEditForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveGenieAcsWlan(_selectedGenieHostId.value, form)) {
                is ApiResult.Success -> {
                    _wlanEditForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    openDeviceDetail(form.deviceId)
                }
                is ApiResult.Error -> {
                    _wlanEditForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    fun openWanEditor(deviceId: String, wan: GenieAcsWanItem?, defaultIndex: Int = 1) {
        _wanEditForm.value = GenieAcsWanEditState(
            isOpen = true,
            deviceId = deviceId,
            connectionIndex = wan?.index ?: defaultIndex,
            connectionType = wan?.type ?: "ppp",
            username = wan?.username.orEmpty(),
            password = wan?.password.orEmpty(),
            vlan = wan?.vlan.orEmpty(),
            nat = wan?.nat ?: true,
            enable = wan?.enable ?: true
        )
    }

    fun closeWanEditor() {
        _wanEditForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateWanEditor(block: (GenieAcsWanEditState) -> GenieAcsWanEditState) {
        _wanEditForm.update { block(it).copy(error = null) }
    }

    fun submitWanEdit() {
        val form = _wanEditForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _wanEditForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveGenieAcsWan(_selectedGenieHostId.value, form)) {
                is ApiResult.Success -> {
                    _wanEditForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    openDeviceDetail(form.deviceId)
                }
                is ApiResult.Error -> {
                    _wanEditForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    override fun onCleared() {
        stopLivePolling()
        super.onCleared()
    }

    class Factory(
        private val repository: NetworkGenieAcsRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NetworkGenieAcsViewModel(
                repository = repository,
                sessionStorage = sessionStorage
            ) as T
        }
    }
}
