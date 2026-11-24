package com.phpnuxbill.mobile.feature.customers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.feature.customers.data.CustomerRepository
import com.phpnuxbill.mobile.feature.customers.model.CustomerCustomField
import com.phpnuxbill.mobile.feature.customers.model.CustomerFormMetadata
import com.phpnuxbill.mobile.feature.customers.model.CustomerFormState
import com.phpnuxbill.mobile.feature.customers.model.CustomerItem
import com.phpnuxbill.mobile.feature.services.model.InvoiceReceiptData
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
class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<CustomerItem>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<CustomerItem>>> = _listState.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedServiceFilter = MutableStateFlow("All")
    val selectedServiceFilter: StateFlow<String> = _selectedServiceFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionBannerMessage = MutableStateFlow<String?>(null)
    val actionBannerMessage: StateFlow<String?> = _actionBannerMessage.asStateFlow()

    private val _metadata = MutableStateFlow(CustomerFormMetadata())
    val metadata: StateFlow<CustomerFormMetadata> = _metadata.asStateFlow()

    private val _formState = MutableStateFlow(CustomerFormState())
    val formState: StateFlow<CustomerFormState> = _formState.asStateFlow()

    private val _showMapPicker = MutableStateFlow(false)
    val showMapPicker: StateFlow<Boolean> = _showMapPicker.asStateFlow()

    private val _createdReceipt = MutableStateFlow<InvoiceReceiptData?>(null)
    val createdReceipt: StateFlow<InvoiceReceiptData?> = _createdReceipt.asStateFlow()

    init {
        loadCustomers(isPullRefresh = false)
        loadMetadataIfNeeded()

        viewModelScope.launch {
            _searchQuery
                .drop(1)
                .debounce(250L)
                .distinctUntilChanged()
                .collect {
                    loadCustomers(isPullRefresh = false)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onServiceFilterChanged(serviceType: String) {
        if (_selectedServiceFilter.value == serviceType) return
        _selectedServiceFilter.value = serviceType
        loadCustomers(isPullRefresh = false)
    }

    fun clearBannerMessage() {
        _actionBannerMessage.value = null
    }

    fun clearCreatedReceipt() {
        _createdReceipt.value = null
    }

    fun loadCustomers(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullRefresh) {
                _isRefreshing.value = true
            } else if (_listState.value !is UiState.Success) {
                _listState.value = UiState.Loading
            }

            when (
                val res = customerRepository.getCustomers(
                    search = _searchQuery.value,
                    serviceType = _selectedServiceFilter.value
                )
            ) {
                is ApiResult.Success -> {
                    _totalCount.value = res.data.first
                    _listState.value = UiState.Success(res.data.second)
                }
                is ApiResult.Error -> {
                    if (res.isUnauthorized) {
                        sessionStorage.clearActiveToken()
                    } else {
                        _listState.value = UiState.Error(res.message)
                    }
                }
            }
            _isRefreshing.value = false
        }
    }

    private fun loadMetadataIfNeeded() {
        if (_metadata.value.plans.isNotEmpty()) return
        viewModelScope.launch {
            _formState.update { it.copy(isLoadingMeta = true) }
            when (val res = customerRepository.getFormMetadata()) {
                is ApiResult.Success -> {
                    _metadata.value = res.data
                    _formState.update { current ->
                        val defaultPlan = if (!current.isEdit && current.selectedPlanId == 0) {
                            res.data.plans.firstOrNull {
                                it.type.equals(current.serviceType, ignoreCase = true)
                            }?.id ?: res.data.plans.firstOrNull()?.id ?: 0
                        } else {
                            current.selectedPlanId
                        }
                        val defaultUsing = res.data.paymentUsings.firstOrNull() ?: "Cash"
                        current.copy(
                            isLoadingMeta = false,
                            selectedPlanId = defaultPlan,
                            selectedUsing = if (current.selectedUsing.isBlank()) defaultUsing else current.selectedUsing
                        )
                    }
                }
                is ApiResult.Error -> {
                    _formState.update { it.copy(isLoadingMeta = false) }
                }
            }
        }
    }

    fun openAddCustomerForm() {
        loadMetadataIfNeeded()
        val meta = _metadata.value
        val defaultPlanId = meta.plans.firstOrNull { it.type.equals("Hotspot", ignoreCase = true) }?.id
            ?: meta.plans.firstOrNull()?.id
            ?: 0
        val defaultUsing = meta.paymentUsings.firstOrNull() ?: "Cash"

        _formState.value = CustomerFormState(
            isOpen = true,
            isEdit = false,
            serviceType = "Hotspot",
            selectedPlanId = defaultPlanId,
            selectedUsing = defaultUsing,
            sendWelcomeMessage = true,
            welcomeViaWa = true,
            welcomeViaInbox = true
        )
    }

    fun openEditCustomerForm(item: CustomerItem) {
        loadMetadataIfNeeded()
        val defaultUsing = _metadata.value.paymentUsings.firstOrNull() ?: "Cash"
        _formState.value = CustomerFormState(
            isOpen = true,
            isEdit = true,
            id = item.id,
            username = item.username,
            fullname = item.fullname,
            password = "",
            phonenumber = item.phonenumber,
            email = item.email,
            address = item.address,
            city = item.city,
            district = item.district,
            state = item.state,
            zip = item.zip,
            serviceType = item.serviceType.ifBlank { "Hotspot" },
            accountType = item.accountType.ifBlank { "Personal" },
            status = item.status.ifBlank { "Active" },
            autoRenewal = item.autoRenewal == 1,
            pppoeUsername = item.pppoeUsername,
            pppoePassword = item.pppoePassword,
            pppoeIp = item.pppoeIp,
            coordinates = item.coordinates,
            selectedPlanId = 0,
            selectedUsing = defaultUsing,
            sendWelcomeMessage = false,
            customFields = item.customFields
        )
    }

    fun closeCustomerForm() {
        _formState.update { it.copy(isOpen = false, formError = null) }
    }

    fun updateForm(transform: (CustomerFormState) -> CustomerFormState) {
        _formState.update { transform(it).copy(formError = null) }
    }

    fun onFormServiceTypeChanged(newType: String) {
        val meta = _metadata.value
        _formState.update { current ->
            val autoPlanId = if (!current.isEdit) {
                if (newType.equals("Others", ignoreCase = true)) {
                    meta.plans.firstOrNull()?.id ?: 0
                } else {
                    meta.plans.firstOrNull { it.type.equals(newType, ignoreCase = true) }?.id ?: 0
                }
            } else {
                current.selectedPlanId
            }
            current.copy(
                serviceType = newType,
                selectedPlanId = autoPlanId,
                formError = null
            )
        }
    }

    fun addCustomFieldRow() {
        _formState.update {
            it.copy(customFields = it.customFields + CustomerCustomField())
        }
    }

    fun updateCustomFieldRow(index: Int, name: String, value: String) {
        _formState.update { current ->
            val updated = current.customFields.toMutableList()
            if (index in updated.indices) {
                updated[index] = CustomerCustomField(name = name, value = value)
            }
            current.copy(customFields = updated)
        }
    }

    fun removeCustomFieldRow(index: Int) {
        _formState.update { current ->
            val updated = current.customFields.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            current.copy(customFields = updated)
        }
    }

    fun openMapPicker() {
        _showMapPicker.value = true
    }

    fun closeMapPicker() {
        _showMapPicker.value = false
    }

    fun applyPickedCoordinates(lat: Double, lng: Double) {
        val formatted = String.format(java.util.Locale.US, "%.6f, %.6f", lat, lng)
        _formState.update { it.copy(coordinates = formatted) }
        _showMapPicker.value = false
    }

    fun submitCustomerForm() {
        val current = _formState.value
        if (current.isSaving) return

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, formError = null) }
            when (val res = customerRepository.saveCustomer(current)) {
                is ApiResult.Success -> {
                    _formState.update { it.copy(isSaving = false, isOpen = false) }
                    _actionBannerMessage.value = res.message
                    if (res.data != null) {
                        _createdReceipt.value = res.data
                    }
                    loadCustomers(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _formState.update { it.copy(isSaving = false, formError = res.message) }
                }
            }
        }
    }

    fun deleteCustomer(customerId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = customerRepository.deleteCustomer(customerId)) {
                is ApiResult.Success -> {
                    _actionBannerMessage.value = res.message
                    loadCustomers(isPullRefresh = true)
                }
                is ApiResult.Error -> {
                    _isRefreshing.value = false
                    _actionBannerMessage.value = res.message
                }
            }
        }
    }

    fun syncSingleCustomer(customerId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = customerRepository.syncCustomer(customerId)) {
                is ApiResult.Success -> _actionBannerMessage.value = res.message
                is ApiResult.Error -> _actionBannerMessage.value = res.message
            }
            _isRefreshing.value = false
        }
    }

    fun syncAllCustomers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = customerRepository.syncAllCustomers()) {
                is ApiResult.Success -> _actionBannerMessage.value = res.message
                is ApiResult.Error -> _actionBannerMessage.value = res.message
            }
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val customerRepository: CustomerRepository,
        private val sessionStorage: SessionStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomerViewModel(
                customerRepository = customerRepository,
                sessionStorage = sessionStorage
            ) as T
        }
    }
}
