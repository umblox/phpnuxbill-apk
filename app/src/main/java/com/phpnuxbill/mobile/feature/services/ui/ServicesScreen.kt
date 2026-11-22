package com.phpnuxbill.mobile.feature.services.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.services.model.ActiveServiceSheetMode
import com.phpnuxbill.mobile.feature.services.model.CustomerOption
import com.phpnuxbill.mobile.feature.services.model.DepositFormState
import com.phpnuxbill.mobile.feature.services.model.PrepaidServiceItem
import com.phpnuxbill.mobile.feature.services.model.RechargeFormState
import com.phpnuxbill.mobile.feature.services.model.RefillFormState
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherLabels
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    viewModel: ServiceVoucherViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val servicesState by viewModel.servicesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.serviceSearch.collectAsStateWithLifecycle()
    val statusFilter by viewModel.serviceStatusFilter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeServiceMode.collectAsStateWithLifecycle()
    val rechargeForm by viewModel.rechargeForm.collectAsStateWithLifecycle()
    val depositForm by viewModel.depositForm.collectAsStateWithLifecycle()
    val refillForm by viewModel.refillForm.collectAsStateWithLifecycle()
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { ServiceVoucherLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }

    var extendTarget by remember { mutableStateOf<PrepaidServiceItem?>(null) }
    var extendDaysInput by remember { mutableStateOf("3") }
    var deleteTarget by remember { mutableStateOf<PrepaidServiceItem?>(null) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    // 1. Jika ada Struk Invoice aktif, tampilkan ReceiptPreviewScreen
    if (activeReceipt != null) {
        ReceiptPreviewScreen(
            receipt = activeReceipt!!,
            companyName = session.effectiveCompanyName,
            viewModel = viewModel,
            onClose = viewModel::closeReceipt
        )
        return
    }

    // 2. Jika mode Form Layar Penuh (Recharge / Deposit / Refill) aktif
    when (activeMode) {
        ActiveServiceSheetMode.RECHARGE_PLAN -> {
            FullScreenRechargeForm(
                form = rechargeForm,
                metadata = metadata,
                currencyCode = session.currencyCode,
                labels = labels,
                onUpdate = viewModel::updateRechargeForm,
                onSubmit = viewModel::submitRecharge,
                onClose = viewModel::closeServiceMode
            )
            return
        }
        ActiveServiceSheetMode.DEPOSIT_BALANCE -> {
            FullScreenDepositForm(
                form = depositForm,
                metadata = metadata,
                currencyCode = session.currencyCode,
                labels = labels,
                onUpdate = viewModel::updateDepositForm,
                onSubmit = viewModel::submitDeposit,
                onClose = viewModel::closeServiceMode
            )
            return
        }
        ActiveServiceSheetMode.REFILL_VOUCHER -> {
            FullScreenRefillForm(
                form = refillForm,
                metadata = metadata,
                labels = labels,
                onUpdate = viewModel::updateRefillForm,
                onSubmit = viewModel::submitRefillVoucher,
                onClose = viewModel::closeServiceMode
            )
            return
        }
        ActiveServiceSheetMode.NONE -> {}
    }

    // Dialog Extend Hari
    if (extendTarget != null) {
        val target = extendTarget!!
        AlertDialog(
            onDismissRequest = { extendTarget = null },
            title = { Text("${strings.extendExpiry} (${target.username})") },
            text = {
                OutlinedTextField(
                    value = extendDaysInput,
                    onValueChange = { extendDaysInput = it.filter { c -> c.isDigit() } },
                    label = { Text(labels.extendDaysPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = extendDaysInput.toIntOrNull()?.coerceAtLeast(1) ?: 3
                        extendTarget = null
                        viewModel.extendService(target.id, d)
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { extendTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Layanan
    if (deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("${labels.deactivateConfirmTitle} (${target.username})") },
            text = { Text(labels.deactivateConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        viewModel.deleteService(target.id)
                    }
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = strings.menuServices,
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
                    IconButton(onClick = { viewModel.loadPrepaidServices(isPullRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Bar Tombol Aksi Utama: Isi Ulang Paket | Top-Up Saldo | Redeem Voucher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.openRechargeForm() },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.rechargeNow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = { viewModel.openDepositForm() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deposit", maxLines = 1)
                }

                OutlinedButton(
                    onClick = { viewModel.openRefillForm() },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refill", maxLines = 1)
                }
            }

            // Pencarian & Filter Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onServiceSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onServiceSearchChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to strings.all,
                        "on" to strings.active,
                        "off" to strings.expiredServices
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter.equals(key, ignoreCase = true),
                            onClick = { viewModel.onServiceStatusFilterChanged(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = servicesState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadPrepaidServices() }
                )
                is UiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadPrepaidServices(isPullRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.data, key = { it.id }) { item ->
                                    PrepaidServiceCard(
                                        item = item,
                                        currencyCode = session.currencyCode,
                                        onRecharge = {
                                            viewModel.openRechargeForm(
                                                prefillCustomerId = item.customerId,
                                                prefillPlanId = item.planId
                                            )
                                        },
                                        onExtend = {
                                            extendDaysInput = "3"
                                            extendTarget = item
                                        },
                                        onViewReceipt = {
                                            viewModel.openReceiptForService(
                                                item = item,
                                                companyName = session.effectiveCompanyName,
                                                currencyCode = session.currencyCode
                                            )
                                        },
                                        onDelete = { deleteTarget = item }
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
private fun PrepaidServiceCard(
    item: PrepaidServiceItem,
    currencyCode: String,
    onRecharge: () -> Unit,
    onExtend: () -> Unit,
    onViewReceipt: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isOn = item.status.equals("on", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fullname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${item.username} • ${item.namebp} (${item.type})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(
                    text = if (isOn) "ON" else "OFF",
                    tone = if (isOn) BadgeTone.SUCCESS else BadgeTone.DANGER
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.routers} • Exp: ${item.expiration}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(currencyCode, item.price),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRecharge) {
                    Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recharge")
                }
                if (!isOn) {
                    TextButton(onClick = onExtend) {
                        Icon(Icons.Outlined.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Extend")
                    }
                }
                IconButton(onClick = onViewReceipt, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = "Receipt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelectorField(
    customers: List<CustomerOption>,
    selectedCustomerId: Int,
    filterText: String,
    labels: ServiceVoucherLabels,
    onFilterChanged: (String) -> Unit,
    onSelected: (CustomerOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedObj = customers.find { it.id == selectedCustomerId }
    val filteredList = remember(customers, filterText) {
        if (filterText.isBlank()) customers.take(40)
        else customers.filter {
            it.fullname.contains(filterText, ignoreCase = true) ||
                it.username.contains(filterText, ignoreCase = true) ||
                it.phonenumber.contains(filterText, ignoreCase = true)
        }.take(40)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = filterText,
            onValueChange = {
                onFilterChanged(it)
                expanded = true
            },
            label = { Text(labels.filterCustomerHint) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedObj?.let { "${it.fullname} (@${it.username})" } ?: labels.selectCustomer,
                onValueChange = {},
                readOnly = true,
                label = { Text(labels.selectCustomer) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredList.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.fullname} (@${c.username}) • ${c.serviceType}") },
                        onClick = {
                            onSelected(c)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenRechargeForm(
    form: RechargeFormState,
    metadata: ServiceVoucherMetadata,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onUpdate: ((RechargeFormState) -> RechargeFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var planExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }
    val selectedPlan = metadata.internetPlans.find { it.id == form.planId }

    AppScreenScaffold(
        title = strings.rechargeNow,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !form.isSubmitting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSubmitting && form.customerId > 0 && form.planId > 0,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (form.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(strings.rechargeNow)
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
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            CustomerSelectorField(
                customers = metadata.customers,
                selectedCustomerId = form.customerId,
                filterText = form.customerQuery,
                labels = labels,
                onFilterChanged = { q -> onUpdate { it.copy(customerQuery = q) } },
                onSelected = { c -> onUpdate { it.copy(customerId = c.id) } }
            )

            ExposedDropdownMenuBox(
                expanded = planExpanded,
                onExpandedChange = { planExpanded = !planExpanded }
            ) {
                OutlinedTextField(
                    value = selectedPlan?.let {
                        "${it.namePlan} (${formatMoney(currencyCode, it.price)})"
                    } ?: strings.selectPlan,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.selectPlan) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = planExpanded,
                    onDismissRequest = { planExpanded = false }
                ) {
                    metadata.internetPlans.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.namePlan} • ${p.type} (${formatMoney(currencyCode, p.price)})") },
                            onClick = {
                                onUpdate { it.copy(planId = p.id) }
                                planExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = !methodExpanded }
            ) {
                OutlinedTextField(
                    value = form.usingMethod,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.paymentMethod) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = methodExpanded,
                    onDismissRequest = { methodExpanded = false }
                ) {
                    metadata.paymentUsings.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                onUpdate { it.copy(usingMethod = m) }
                                methodExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = { n -> onUpdate { it.copy(note = n) } },
                label = { Text(labels.noteLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenDepositForm(
    form: DepositFormState,
    metadata: ServiceVoucherMetadata,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onUpdate: ((DepositFormState) -> DepositFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var balPlanExpanded by remember { mutableStateOf(false) }
    val selectedBalPlan = metadata.balancePlans.find { it.id == form.planId }

    AppScreenScaffold(
        title = strings.topUpBalance,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !form.isSubmitting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSubmitting && form.customerId > 0 &&
                            (form.planId > 0 || (form.customAmount.toDoubleOrNull() ?: 0.0) != 0.0),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (form.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(strings.topUpBalance)
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
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            CustomerSelectorField(
                customers = metadata.customers,
                selectedCustomerId = form.customerId,
                filterText = form.customerQuery,
                labels = labels,
                onFilterChanged = { q -> onUpdate { it.copy(customerQuery = q) } },
                onSelected = { c -> onUpdate { it.copy(customerId = c.id) } }
            )

            if (metadata.balancePlans.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = balPlanExpanded,
                    onExpandedChange = { balPlanExpanded = !balPlanExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBalPlan?.let {
                            "${it.namePlan} (${formatMoney(currencyCode, it.price)})"
                        } ?: strings.selectPlan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectPlan) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = balPlanExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = balPlanExpanded,
                        onDismissRequest = { balPlanExpanded = false }
                    ) {
                        metadata.balancePlans.forEach { bp ->
                            DropdownMenuItem(
                                text = { Text("${bp.namePlan} (${formatMoney(currencyCode, bp.price)})") },
                                onClick = {
                                    onUpdate { it.copy(planId = bp.id, customAmount = "") }
                                    balPlanExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = form.customAmount,
                onValueChange = { amt -> onUpdate { it.copy(customAmount = amt) } },
                label = { Text("${labels.customNominalLabel} ($currencyCode)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = form.note,
                onValueChange = { n -> onUpdate { it.copy(note = n) } },
                label = { Text(labels.noteLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun FullScreenRefillForm(
    form: RefillFormState,
    metadata: ServiceVoucherMetadata,
    labels: ServiceVoucherLabels,
    onUpdate: ((RefillFormState) -> RefillFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current

    AppScreenScaffold(
        title = labels.refillVoucherTitle,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !form.isSubmitting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSubmitting && form.customerId > 0 && form.voucherCode.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (form.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(strings.confirm)
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
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            CustomerSelectorField(
                customers = metadata.customers,
                selectedCustomerId = form.customerId,
                filterText = form.customerQuery,
                labels = labels,
                onFilterChanged = { q -> onUpdate { it.copy(customerQuery = q) } },
                onSelected = { c -> onUpdate { it.copy(customerId = c.id) } }
            )

            OutlinedTextField(
                value = form.voucherCode,
                onValueChange = { c -> onUpdate { it.copy(voucherCode = c) } },
                label = { Text(labels.voucherCodeLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
