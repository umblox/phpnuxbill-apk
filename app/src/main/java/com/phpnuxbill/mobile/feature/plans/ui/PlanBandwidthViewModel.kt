package com.phpnuxbill.mobile.feature.plans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.plans.data.PlanBandwidthRepository
import com.phpnuxbill.mobile.feature.plans.model.BandwidthFormState
import com.phpnuxbill.mobile.feature.plans.model.BandwidthItem
import com.phpnuxbill.mobile.feature.plans.model.BurstPresetItem
import com.phpnuxbill.mobile.feature.plans.model.PlanBandwidthMetadata
import com.phpnuxbill.mobile.feature.plans.model.PlanFormState
import com.phpnuxbill.mobile.feature.plans.model.PlanItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(FlowPreview::class)
class PlanBandwidthViewModel(
    private val repository: PlanBandwidthRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _plansState = MutableStateFlow<UiState<List<PlanItem>>>(UiState.Loading)
    val plansState: StateFlow<UiState<List<PlanItem>>> = _plansState.asStateFlow()

    private val _planTypeFilter = MutableStateFlow("All")
    val planTypeFilter: StateFlow<String> = _planTypeFilter.asStateFlow()

    private val _planSearch = MutableStateFlow("")
    val planSearch: StateFlow<String> = _planSearch.asStateFlow()

    private val _bandwidthsState = MutableStateFlow<UiState<List<BandwidthItem>>>(UiState.Loading)
    val bandwidthsState: StateFlow<UiState<List<BandwidthItem>>> = _bandwidthsState.asStateFlow()

    private val _bwSearch = MutableStateFlow("")
    val bwSearch: StateFlow<String> = _bwSearch.asStateFlow()

    private val _metadata = MutableStateFlow(PlanBandwidthMetadata())
    val metadata: StateFlow<PlanBandwidthMetadata> = _metadata.asStateFlow()

    private val _planForm = MutableStateFlow(PlanFormState())
    val planForm: StateFlow<PlanFormState> = _planForm.asStateFlow()

    private val _bwForm = MutableStateFlow(BandwidthFormState())
    val bwForm: StateFlow<BandwidthFormState> = _bwForm.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private var cachedAllPlans: List<PlanItem> = emptyList()

    init {
        loadPlansAndMetadata()
        loadBandwidths()

        viewModelScope.launch {
            _bwSearch.drop(1).debounce(250L).distinctUntilChanged().collect {
                loadBandwidths()
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    // =========================================================================
    // MANAJEMEN PAKET INTERNET & SALDO
    // =========================================================================

    fun onPlanTypeFilterChanged(type: String) {
        _planTypeFilter.value = type
        loadPlansAndMetadata()
    }

    fun onPlanSearchChanged(q: String) {
        _planSearch.value = q
        applyPlanSearchFilter()
    }

    private fun applyPlanSearchFilter() {
        val q = _planSearch.value.trim()
        val filtered = if (q.isEmpty()) {
            cachedAllPlans
        } else {
            cachedAllPlans.filter {
                it.namePlan.contains(q, ignoreCase = true) ||
                    it.routers.contains(q, ignoreCase = true) ||
                    it.bandwidthName.contains(q, ignoreCase = true)
            }
        }
        _plansState.value = UiState.Success(filtered)
    }

    fun loadPlansAndMetadata(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_plansState.value !is UiState.Success) _plansState.value = UiState.Loading

            when (val res = repository.getPlansAndMetadata(typeFilter = _planTypeFilter.value)) {
                is ApiResult.Success -> {
                    cachedAllPlans = res.data.first
                    _metadata.value = res.data.second
                    applyPlanSearchFilter()
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _plansState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun openAddPlanForm() {
        val meta = _metadata.value
        val defaultBw = meta.bandwidths.firstOrNull()?.id ?: 0
        val defaultRouter = meta.routers.firstOrNull()?.name.orEmpty()
        val defaultPool = meta.pools.firstOrNull()?.poolName.orEmpty()

        _planForm.value = PlanFormState(
            isOpen = true,
            isEdit = false,
            type = "Hotspot",
            idBw = defaultBw,
            routers = defaultRouter,
            pool = defaultPool,
            device = "MikrotikHotspot"
        )
    }

    fun openEditPlanForm(item: PlanItem) {
        _planForm.value = PlanFormState(
            isOpen = true,
            isEdit = true,
            id = item.id,
            type = item.type,
            namePlan = item.namePlan,
            price = formatCleanNumber(item.price),
            priceOld = item.priceOld,
            enabled = item.enabled,
            prepaid = item.prepaid,
            planType = item.planType,
            idBw = item.idBw,
            validity = item.validity.toString(),
            validityUnit = item.validityUnit,
            routers = item.routers,
            isRadius = item.isRadius,
            pool = item.pool,
            device = item.device.ifBlank { defaultDeviceFor(item.type, item.isRadius) },
            typebp = item.typebp,
            limitType = item.limitType,
            timeLimit = item.timeLimit.toString(),
            timeUnit = item.timeUnit,
            dataLimit = formatCleanNumber(item.dataLimit),
            dataUnit = item.dataUnit,
            sharedUsers = item.sharedUsers.toString(),
            expiredDate = item.expiredDate.toString(),
            planExpired = item.planExpired
        )
    }

    fun closePlanForm() {
        _planForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updatePlanForm(block: (PlanFormState) -> PlanFormState) {
        _planForm.update { block(it).copy(error = null) }
    }

    fun onPlanFormTypeChanged(newType: String) {
        _planForm.update { current ->
            val nextDevice = defaultDeviceFor(newType, current.isRadius)
            val nextPrepaid = if (newType.equals("PPPOE", ignoreCase = true) && !current.isEdit) "no" else "yes"
            current.copy(
                type = newType,
                device = nextDevice,
                prepaid = nextPrepaid,
                error = null
            )
        }
    }

    fun onPlanFormRadiusToggled(isRadius: Boolean) {
        _planForm.update { current ->
            val nextDevice = defaultDeviceFor(current.type, isRadius)
            current.copy(
                isRadius = isRadius,
                device = nextDevice,
                error = null
            )
        }
    }

    fun submitPlanForm() {
        val form = _planForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _planForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.savePlan(form)) {
                is ApiResult.Success -> {
                    _planForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadPlansAndMetadata(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _planForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    fun deletePlan(planId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.deletePlan(planId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadPlansAndMetadata(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    fun syncPlansToRouter(type: String = "all") {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.syncPlans(type)) {
                is ApiResult.Success -> _bannerMessage.value = res.message
                is ApiResult.Error -> _bannerMessage.value = res.message
            }
            _isRefreshing.value = false
        }
    }

    // =========================================================================
    // MANAJEMEN PROFIL BANDWIDTH & PRESET BURST
    // =========================================================================

    fun onBwSearchChanged(q: String) {
        _bwSearch.value = q
    }

    fun loadBandwidths(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) _isRefreshing.value = true
            else if (_bandwidthsState.value !is UiState.Success) _bandwidthsState.value = UiState.Loading

            when (val res = repository.getBandwidths(search = _bwSearch.value)) {
                is ApiResult.Success -> _bandwidthsState.value = UiState.Success(res.data)
                is ApiResult.Error -> {
                    if (res.isUnauthorized) sessionStorage.clearActiveToken()
                    else _bandwidthsState.value = UiState.Error(res.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun openAddBandwidthForm() {
        _bwForm.value = BandwidthFormState(
            isOpen = true,
            isEdit = false
        )
    }

    fun openEditBandwidthForm(item: BandwidthItem) {
        val parts = item.burstParts
        _bwForm.value = BandwidthFormState(
            isOpen = true,
            isEdit = true,
            id = item.id,
            nameBw = item.nameBw,
            rateDown = formatCleanNumber(item.rateDown),
            rateDownUnit = item.rateDownUnit,
            rateUp = formatCleanNumber(item.rateUp),
            rateUpUnit = item.rateUpUnit,
            burstLimit = parts.getOrElse(0) { "" },
            burstThreshold = parts.getOrElse(1) { "" },
            burstTime = parts.getOrElse(2) { "" },
            priority = parts.getOrElse(3) { "" },
            limitAt = parts.getOrElse(4) { "" }
        )
    }

    fun closeBandwidthForm() {
        _bwForm.update { it.copy(isOpen = false, error = null) }
    }

    fun updateBandwidthForm(block: (BandwidthFormState) -> BandwidthFormState) {
        _bwForm.update { block(it).copy(error = null) }
    }

    /**
     * Menerapkan Preset Burst resmi dari server PHPNuxBill (2x atau UpTo)
     */
    fun applyServerBurstPreset(preset: BurstPresetItem) {
        val b = preset.burst
        _bwForm.update { current ->
            val autoName = current.nameBw.ifBlank {
                preset.label.replace(" ", "_")
            }
            current.copy(
                nameBw = autoName,
                rateDown = preset.rateDown.toString(),
                rateDownUnit = preset.rateDownUnit,
                rateUp = preset.rateUp.toString(),
                rateUpUnit = preset.rateUpUnit,
                burstLimit = b.getOrElse(0) { "" },
                burstThreshold = b.getOrElse(1) { "" },
                burstTime = b.getOrElse(2) { "16/16" },
                priority = b.getOrElse(3) { "8" },
                limitAt = b.getOrElse(4) { "" },
                error = null
            )
        }
    }

    /**
     * Tombol Cepat Kalkulator Burst 1.5x / 2.0x berdasarkan rateDown & rateUp yang sedang diisi
     */
    fun applyMultiplierBurst(multiplier: Double) {
        val form = _bwForm.value
        val downVal = form.rateDown.toDoubleOrNull() ?: return
        val upVal = form.rateUp.toDoubleOrNull() ?: downVal
        if (downVal <= 0.0 || upVal <= 0.0) return

        val downKbps = if (form.rateDownUnit == "Mbps") downVal * 1024.0 else downVal
        val upKbps = if (form.rateUpUnit == "Mbps") upVal * 1024.0 else upVal

        val burstDownK = (downKbps * multiplier).roundToInt()
        val burstUpK = (upKbps * multiplier).roundToInt()
        val threshDownK = (downKbps * 0.75).roundToInt()
        val threshUpK = (upKbps * 0.75).roundToInt()
        val limitAtDownK = (downKbps * 0.50).roundToInt()
        val limitAtUpK = (upKbps * 0.50).roundToInt()

        _bwForm.update {
            it.copy(
                burstLimit = "${formatRateToken(burstUpK)}/${formatRateToken(burstDownK)}",
                burstThreshold = "${formatRateToken(threshUpK)}/${formatRateToken(threshDownK)}",
                burstTime = "16/16",
                priority = "8",
                limitAt = "${formatRateToken(limitAtUpK)}/${formatRateToken(limitAtDownK)}",
                error = null
            )
        }
    }

    fun clearBurstFields() {
        _bwForm.update {
            it.copy(
                burstLimit = "",
                burstThreshold = "",
                burstTime = "",
                priority = "",
                limitAt = "",
                error = null
            )
        }
    }

    fun submitBandwidthForm() {
        val form = _bwForm.value
        if (form.isSaving) return
        viewModelScope.launch {
            _bwForm.update { it.copy(isSaving = true, error = null) }
            when (val res = repository.saveBandwidth(form)) {
                is ApiResult.Success -> {
                    _bwForm.update { it.copy(isSaving = false, isOpen = false) }
                    _bannerMessage.value = res.message
                    loadBandwidths(isPullRefresh = true)
                    loadPlansAndMetadata(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _bwForm.update { it.copy(isSaving = false, error = res.message) }
                }
            }
        }
    }

    fun deleteBandwidth(bwId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = repository.deleteBandwidth(bwId)) {
                is ApiResult.Success -> {
                    _bannerMessage.value = res.message
                    loadBandwidths(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _bannerMessage.value = res.message
                }
            }
        }
    }

    private fun defaultDeviceFor(type: String, isRadius: Boolean): String {
        if (isRadius) return "Radius"
        return when (type.uppercase()) {
            "PPPOE" -> "MikrotikPppoe"
            "VPN" -> "MikrotikVpn"
            "BALANCE" -> ""
            else -> "MikrotikHotspot"
        }
    }

    private fun formatCleanNumber(v: Double): String {
        return if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
    }

    private fun formatRateToken(kbps: Int): String {
        return if (kbps >= 1024 && kbps % 1024 == 0) {
            "${kbps / 1024}M"
        } else {
            "${kbps}k"
        }
    }

    class Factory(
        private val repository: PlanBandwidthRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlanBandwidthViewModel(
                repository = repository,
                sessionStorage = sessionStorage
            ) as T
        }
    }
}
