package com.phpnuxbill.mobile.feature.customers.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.auth.AdminRolePolicy
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.customers.model.CustomerFormMetadata
import com.phpnuxbill.mobile.feature.customers.model.CustomerFormState
import com.phpnuxbill.mobile.feature.customers.model.CustomerItem
import com.phpnuxbill.mobile.feature.customers.model.CustomerLabels
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.services.model.InvoiceReceiptData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit,
    onReceiptCreated: (InvoiceReceiptData) -> Unit = {}
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val serviceFilter by viewModel.selectedServiceFilter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.actionBannerMessage.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val showMapPicker by viewModel.showMapPicker.collectAsStateWithLifecycle()
    val createdReceipt by viewModel.createdReceipt.collectAsStateWithLifecycle()

    LaunchedEffect(createdReceipt) {
        val receipt = createdReceipt ?: return@LaunchedEffect
        onReceiptCreated(receipt)
        viewModel.clearCreatedReceipt()
    }

    val strings = LocalAppStrings.current
    val labels = remember(strings) { CustomerLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteCandidate by remember { mutableStateOf<CustomerItem?>(null) }

    val adminLevel = remember(session.userType) { AdminLevel.fromUserType(session.userType) }
    val canAdd = AdminRolePolicy.canAddCustomer(adminLevel)
    val canEditOrDelete = AdminRolePolicy.canEditOrDeleteCustomer(adminLevel)
    val canSyncAll = AdminRolePolicy.canSyncAllCustomers(adminLevel)

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBannerMessage()
    }

    if (showMapPicker) {
        CoordinatePickerDialog(
            initialCoordinates = formState.coordinates,
            routerHints = metadata.routerHints,
            onConfirm = viewModel::applyPickedCoordinates,
            onDismiss = viewModel::closeMapPicker
        )
    }

    if (deleteCandidate != null && canEditOrDelete) {
        val target = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text("${labels.deleteConfirmTitle} (${target.username})") },
            text = { Text(labels.deleteConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        viewModel.deleteCustomer(target.id)
                    }
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (formState.isOpen && (if (formState.isEdit) canEditOrDelete else canAdd)) {
        CustomerFullScreenForm(
            form = formState,
            metadata = metadata,
            currencyCode = session.currencyCode,
            labels = labels,
            onUpdateForm = viewModel::updateForm,
            onServiceTypeChanged = viewModel::onFormServiceTypeChanged,
            onAddCustomField = viewModel::addCustomFieldRow,
            onUpdateCustomField = viewModel::updateCustomFieldRow,
            onRemoveCustomField = viewModel::removeCustomFieldRow,
            onOpenMapPicker = viewModel::openMapPicker,
            onSubmit = viewModel::submitCustomerForm,
            onClose = viewModel::closeCustomerForm
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "${strings.menuCustomers} ($totalCount)",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = session.effectiveCompanyName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (canSyncAll) {
                        IconButton(onClick = viewModel::syncAllCustomers) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = labels.syncAllRouters
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.loadCustomers(isPullRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = strings.refresh
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = viewModel::openAddCustomerForm,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = strings.addCustomer)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All" to strings.all, "Hotspot" to "Hotspot", "PPPoE" to "PPPoE").forEach { (key, label) ->
                        FilterChip(
                            selected = serviceFilter.equals(key, ignoreCase = true),
                            onClick = { viewModel.onServiceFilterChanged(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = listState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadCustomers(isPullRefresh = false) }
                )
                is UiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadCustomers(isPullRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.noData,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = 88.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.data, key = { it.id }) { customer ->
                                    CustomerCardItem(
                                        customer = customer,
                                        currencyCode = session.currencyCode,
                                        labels = labels,
                                        canSync = canAdd,
                                        canEditOrDelete = canEditOrDelete,
                                        onEdit = { viewModel.openEditCustomerForm(customer) },
                                        onSync = { viewModel.syncSingleCustomer(customer.id) },
                                        onDelete = { deleteCandidate = customer }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCardItem(
    customer: CustomerItem,
    currencyCode: String,
    labels: CustomerLabels,
    canSync: Boolean,
    canEditOrDelete: Boolean,
    onEdit: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val pkg = customer.activePackage
    val isPkgOn = pkg?.status.equals("on", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .let { if (canEditOrDelete) it.clickable(onClick = onEdit) else it },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.fullname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${customer.username} • ${customer.serviceType}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = if (pkg != null) "${pkg.namebp} (${if (isPkgOn) "ON" else "OFF"})" else labels.noPackageLabel,
                    tone = when {
                        pkg == null -> BadgeTone.NEUTRAL
                        isPkgOn -> BadgeTone.SUCCESS
                        else -> BadgeTone.DANGER
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (customer.phonenumber.isNotBlank()) {
                        Text(
                            text = customer.phonenumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (pkg != null) {
                        Text(
                            text = "${pkg.routers} • Exp: ${pkg.expiration}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = formatMoney(currencyCode, customer.balance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            val hasActions = customer.phonenumber.isNotBlank() || canSync || canEditOrDelete
            if (hasActions) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (customer.phonenumber.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val digits = customer.phonenumber.replace(Regex("[^0-9]"), "")
                                if (digits.isNotBlank()) {
                                    val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum"))
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = "WhatsApp",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (canSync) {
                        IconButton(
                            onClick = onSync,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = labels.syncRouter,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (canEditOrDelete) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = strings.edit,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerFullScreenForm(
    form: CustomerFormState,
    metadata: CustomerFormMetadata,
    currencyCode: String,
    labels: CustomerLabels,
    onUpdateForm: ((CustomerFormState) -> CustomerFormState) -> Unit,
    onServiceTypeChanged: (String) -> Unit,
    onAddCustomField: () -> Unit,
    onUpdateCustomField: (Int, String, String) -> Unit,
    onRemoveCustomField: (Int) -> Unit,
    onOpenMapPicker: () -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var planDropdownExpanded by remember { mutableStateOf(false) }
    var usingDropdownExpanded by remember { mutableStateOf(false) }

    // Saring paket otomatis sesuai tipe layanan (Hotspot / PPPoE) seperti plugin quickadd.php
    val filteredPlans = remember(metadata.plans, form.serviceType) {
        if (form.serviceType.equals("Others", ignoreCase = true)) {
            metadata.plans
        } else {
            metadata.plans.filter { it.type.equals(form.serviceType, ignoreCase = true) }
        }
    }
    val selectedPlanObj = filteredPlans.find { it.id == form.selectedPlanId }
        ?: metadata.plans.find { it.id == form.selectedPlanId }
    val isZeroMethod = form.selectedUsing.equals("Recharge Zero", ignoreCase = true)

    val isFormValid = if (form.isEdit) {
        form.username.trim().length >= 3 && form.fullname.trim().length >= 2
    } else {
        form.username.trim().length >= 3 &&
            form.fullname.trim().length >= 2 &&
            form.password.length in 3..35 &&
            form.selectedPlanId > 0
    }

    AppScreenScaffold(
        title = if (form.isEdit) strings.editCustomer else (if (isEn) "Quick Add Customer" else "Tambah Pelanggan Cepat"),
        subtitle = if (form.isEdit) "@${form.username}" else "${form.serviceType} • ${selectedPlanObj?.namePlan ?: strings.selectPlan}",
        onBack = onClose,
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !form.isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSaving && isFormValid,
                        modifier = Modifier.weight(1.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (form.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (form.isEdit) strings.save
                            else (if (isEn) "Register & Activate" else "Daftar & Aktifkan"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(visible = !form.formError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.formError.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Bagian 1 (Quick Add): Pilih Layanan & Paket Aktivasi Langsung
            FormSectionCard(
                title = if (form.isEdit) labels.optionalPlanSection else labels.initialPlanSection
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf("Hotspot", "PPPoE", "Others")
                    types.forEachIndexed { index, t ->
                        SegmentedButton(
                            selected = form.serviceType.equals(t, ignoreCase = true),
                            onClick = { onServiceTypeChanged(t) },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                            label = { Text(t) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = planDropdownExpanded,
                    onExpandedChange = { planDropdownExpanded = !planDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPlanObj?.let {
                            val displayPrice = if (isZeroMethod) 0.0 else it.price
                            "${it.namePlan} (${formatMoney(currencyCode, displayPrice)})"
                        } ?: strings.selectPlan,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                if (form.isEdit) strings.selectPlan
                                else "${strings.selectPlan} (${form.serviceType}) *"
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = planDropdownExpanded,
                        onDismissRequest = { planDropdownExpanded = false }
                    ) {
                        if (form.isEdit) {
                            DropdownMenuItem(
                                text = { Text("-") },
                                onClick = {
                                    onUpdateForm { it.copy(selectedPlanId = 0) }
                                    planDropdownExpanded = false
                                }
                            )
                        }
                        if (filteredPlans.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isEn) "No active ${form.serviceType} plans"
                                        else "Belum ada paket ${form.serviceType} aktif"
                                    )
                                },
                                onClick = { planDropdownExpanded = false }
                            )
                        } else {
                            filteredPlans.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${p.namePlan} • ${p.validityLabel} (${formatMoney(currencyCode, p.price)})")
                                    },
                                    onClick = {
                                        onUpdateForm { it.copy(selectedPlanId = p.id) }
                                        planDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (form.selectedPlanId > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = usingDropdownExpanded,
                        onExpandedChange = { usingDropdownExpanded = !usingDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = form.selectedUsing,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.paymentMethod) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = usingDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = usingDropdownExpanded,
                            onDismissRequest = { usingDropdownExpanded = false }
                        ) {
                            metadata.paymentUsings.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        onUpdateForm { it.copy(selectedUsing = method) }
                                        usingDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isZeroMethod) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusBadge(
                            text = if (isEn) "Zero Cost Activation (Rp 0)" else "Aktivasi Gratis / Zero (Rp 0)",
                            tone = BadgeTone.WARNING
                        )
                    }
                }
            }

            // Bagian 2: Informasi Akun Pelanggan
            FormSectionCard(title = labels.accountInfoSection) {
                OutlinedTextField(
                    value = form.username,
                    onValueChange = { v -> onUpdateForm { it.copy(username = v) } },
                    label = { Text("${strings.usernameLabel} *") },
                    isError = form.username.isNotEmpty() && form.username.trim().length < 3,
                    supportingText = {
                        if (form.username.isNotEmpty() && form.username.trim().length < 3) {
                            Text(if (isEn) "Minimum 3 characters" else "Minimal 3 karakter")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = form.fullname,
                    onValueChange = { v -> onUpdateForm { it.copy(fullname = v) } },
                    label = { Text("${strings.fullName} *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = form.password,
                    onValueChange = { if (it.length <= 35) onUpdateForm { f -> f.copy(password = it) } },
                    label = { Text(if (form.isEdit) strings.passwordLabel else "${strings.passwordLabel} (3–35) *") },
                    placeholder = if (form.isEdit) {
                        { Text(labels.passwordEditHint) }
                    } else null,
                    isError = !form.isEdit && form.password.isNotEmpty() && form.password.length < 3,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labels.autoRenewalLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = form.autoRenewal,
                        onCheckedChange = { checked -> onUpdateForm { it.copy(autoRenewal = checked) } }
                    )
                }
            }

            // Bagian 3: Kredensial PPPoE (Jika PPPoE atau diisi opsional)
            if (form.serviceType.equals("PPPoE", ignoreCase = true) || form.pppoeUsername.isNotBlank()) {
                FormSectionCard(title = labels.pppoeSection) {
                    OutlinedTextField(
                        value = form.pppoeUsername,
                        onValueChange = { v -> onUpdateForm { it.copy(pppoeUsername = v) } },
                        label = { Text(labels.pppoeUserLabel) },
                        placeholder = {
                            Text(if (isEn) "Leave empty to use Username" else "Kosongkan untuk samakan dgn Username")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = form.pppoePassword,
                        onValueChange = { v -> onUpdateForm { it.copy(pppoePassword = v) } },
                        label = { Text(labels.pppoePassLabel) },
                        placeholder = {
                            Text(if (isEn) "Leave empty to use Password" else "Kosongkan untuk samakan dgn Password")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = form.pppoeIp,
                        onValueChange = { v -> onUpdateForm { it.copy(pppoeIp = v) } },
                        label = { Text(labels.pppoeIpLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Bagian 4: Kontak & Lokasi Pemasangan + Coordinate Picker
            FormSectionCard(title = labels.contactLocationSection) {
                OutlinedTextField(
                    value = form.phonenumber,
                    onValueChange = { v -> onUpdateForm { it.copy(phonenumber = v) } },
                    label = { Text(strings.phoneNumber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.email,
                    onValueChange = { v -> onUpdateForm { it.copy(email = v) } },
                    label = { Text(strings.emailAddress) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.address,
                    onValueChange = { v -> onUpdateForm { it.copy(address = v) } },
                    label = { Text(strings.address) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = form.city,
                        onValueChange = { v -> onUpdateForm { it.copy(city = v) } },
                        label = { Text(labels.cityLabel) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = form.district,
                        onValueChange = { v -> onUpdateForm { it.copy(district = v) } },
                        label = { Text(labels.districtLabel) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.coordinates,
                    onValueChange = { v -> onUpdateForm { it.copy(coordinates = v) } },
                    label = { Text(labels.coordinatesLabel) },
                    trailingIcon = {
                        IconButton(onClick = onOpenMapPicker) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = strings.pickCoordinates,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Bagian 5: Welcome Message (Saat Registrasi Baru)
            if (!form.isEdit) {
                FormSectionCard(title = labels.welcomeSection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(labels.sendWelcomeToggle, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = form.sendWelcomeMessage,
                            onCheckedChange = { c -> onUpdateForm { it.copy(sendWelcomeMessage = c) } }
                        )
                    }
                    if (form.sendWelcomeMessage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = form.welcomeViaWa,
                                onClick = { onUpdateForm { it.copy(welcomeViaWa = !it.welcomeViaWa) } },
                                label = { Text("WhatsApp") }
                            )
                            FilterChip(
                                selected = form.welcomeViaSms,
                                onClick = { onUpdateForm { it.copy(welcomeViaSms = !it.welcomeViaSms) } },
                                label = { Text("SMS") }
                            )
                            FilterChip(
                                selected = form.welcomeViaMail,
                                onClick = { onUpdateForm { it.copy(welcomeViaMail = !it.welcomeViaMail) } },
                                label = { Text("Email") }
                            )
                            FilterChip(
                                selected = form.welcomeViaInbox,
                                onClick = { onUpdateForm { it.copy(welcomeViaInbox = !it.welcomeViaInbox) } },
                                label = { Text("Inbox") }
                            )
                        }
                    }
                }
            }

            // Bagian 6: Custom Fields (tbl_customers_fields)
            FormSectionCard(title = labels.customFieldsSection) {
                form.customFields.forEachIndexed { idx, cf ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cf.name,
                            onValueChange = { n -> onUpdateCustomField(idx, n, cf.value) },
                            placeholder = { Text(labels.attrNameHint) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = cf.value,
                            onValueChange = { v -> onUpdateCustomField(idx, cf.name, v) },
                            placeholder = { Text(labels.attrValueHint) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        IconButton(onClick = { onRemoveCustomField(idx) }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAddCustomField,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(labels.addAttribute)
                }
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
