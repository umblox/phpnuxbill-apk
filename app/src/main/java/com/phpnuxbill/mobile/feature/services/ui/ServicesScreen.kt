package com.phpnuxbill.mobile.feature.services.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.services.model.ActiveServiceSheetMode
import com.phpnuxbill.mobile.feature.services.model.CustomerOption
import com.phpnuxbill.mobile.feature.services.model.DepositFormState
import com.phpnuxbill.mobile.feature.services.model.PrepaidServiceItem
import com.phpnuxbill.mobile.feature.services.model.RechargeFormState
import com.phpnuxbill.mobile.feature.services.model.RefillFormState
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherLabels
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

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
    val typeFilter by viewModel.serviceTypeFilter.collectAsStateWithLifecycle()
    val selectedActiveService by viewModel.selectedActiveService.collectAsStateWithLifecycle()
    val activeDetailState by viewModel.activeDetailState.collectAsStateWithLifecycle()
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val isLivePolling by viewModel.isLivePollingEnabled.collectAsStateWithLifecycle()
    val isDetailActionRunning by viewModel.isDetailActionRunning.collectAsStateWithLifecycle()
    val editPrepaidForm by viewModel.editPrepaidForm.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeServiceMode.collectAsStateWithLifecycle()
    val rechargeForm by viewModel.rechargeForm.collectAsStateWithLifecycle()
    val depositForm by viewModel.depositForm.collectAsStateWithLifecycle()
    val refillForm by viewModel.refillForm.collectAsStateWithLifecycle()
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { ServiceVoucherLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }

    val adminLevel = remember(session.userType) { AdminLevel.fromUserType(session.userType) }
    val canRecharge = AdminRolePolicy.canRechargeOrDeposit(adminLevel)
    val canExtend = AdminRolePolicy.canExtendService(adminLevel)
    val canEditOrDelete = AdminRolePolicy.canEditOrDeleteService(adminLevel)

    var extendTarget by remember { mutableStateOf<PrepaidServiceItem?>(null) }
    var extendDaysInput by remember { mutableStateOf("3") }
    var deleteTarget by remember { mutableStateOf<PrepaidServiceItem?>(null) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    // 1. Jika ada Struk Invoice aktif, tampilkan layar penuh ReceiptPreviewScreen
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
                onSelectCustomer = viewModel::selectRechargeCustomer,
                onUnlockCustomer = viewModel::unlockRechargeCustomer,
                onSelectServiceType = viewModel::selectRechargeServiceType,
                onSelectServer = viewModel::selectRechargeServer,
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
                onSelectCustomer = viewModel::selectDepositCustomer,
                onUnlockCustomer = viewModel::unlockDepositCustomer,
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

    // 3. Jika salah satu Pelanggan Aktif diklik -> Layar Penuh Detail MikroTik + Live Speed + Billing
    if (selectedActiveService != null) {
        ActiveCustomerDetailScreen(
            selectedItem = selectedActiveService!!,
            detailState = activeDetailState,
            speedHistory = speedHistory,
            isLivePollingEnabled = isLivePolling,
            isActionRunning = isDetailActionRunning,
            editForm = editPrepaidForm,
            metadata = metadata,
            adminLevel = adminLevel,
            currencyCode = session.currencyCode,
            labels = labels,
            onToggleLivePolling = viewModel::toggleLivePolling,
            onPollLive = { silent -> viewModel.pollActiveCustomerLiveDetail(silent = silent) },
            onRecharge = { cid, pid -> viewModel.openRechargeForm(cid, pid) },
            onDeposit = { cid -> viewModel.openDepositForm(cid) },
            onExtend = { rid, days -> viewModel.extendService(rid, days) },
            onSyncRouter = viewModel::syncSelectedCustomerToRouter,
            onKickSession = viewModel::kickSelectedActiveUser,
            onOpenEditPrepaid = viewModel::openEditPrepaidForm,
            onUpdateEditPrepaid = viewModel::updateEditPrepaidForm,
            onSubmitEditPrepaid = viewModel::submitEditPrepaidForm,
            onCloseEditPrepaid = viewModel::closeEditPrepaidForm,
            onViewReceipt = {
                viewModel.openReceiptForService(
                    item = selectedActiveService!!,
                    companyName = session.effectiveCompanyName,
                    currencyCode = session.currencyCode
                )
            },
            onDeleteService = { rid -> viewModel.deleteService(rid, closeDetailOnDone = true) },
            onBack = viewModel::closeActiveCustomerDetail
        )
        return
    }

    // Dialog Extend Hari
    if (extendTarget != null && canExtend) {
        val target = extendTarget!!
        AlertDialog(
            onDismissRequest = { extendTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text("${strings.extendExpiry} (@${target.username})", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = extendDaysInput,
                    onValueChange = { extendDaysInput = it.filter { c -> c.isDigit() } },
                    label = { Text(labels.extendDaysPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = extendDaysInput.toIntOrNull()?.coerceAtLeast(1) ?: 3
                        extendTarget = null
                        viewModel.extendService(target.id, d)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { extendTarget = null }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Layanan
    if (deleteTarget != null && canEditOrDelete) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text("${labels.deactivateConfirmTitle} (@${target.username})", fontWeight = FontWeight.Bold) },
            text = { Text(labels.deactivateConfirmMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTarget = null
                        viewModel.deleteService(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    val allItems = (servicesState as? UiState.Success)?.data.orEmpty()
    val filteredItems = remember(allItems, typeFilter) {
        if (typeFilter.equals("all", ignoreCase = true)) allItems
        else allItems.filter { it.type.equals(typeFilter, ignoreCase = true) }
    }
    val onCount = remember(allItems) { allItems.count { it.status.equals("on", true) } }
    val offCount = remember(allItems) { allItems.count { !it.status.equals("on", true) } }
    val hotspotCount = remember(allItems) { allItems.count { it.type.equals("Hotspot", true) } }
    val pppoeCount = remember(allItems) { allItems.count { it.type.equals("PPPoE", true) } }

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
                            text = "${labels.activeCustomersTitle} (${filteredItems.size})",
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
            // Bar Tombol Aksi Utama (sesuai izin role)
            if (canRecharge) {
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
            }

            // Strip Ringkasan KPI Pelanggan Aktif
            if (servicesState is UiState.Success && allItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactSummaryPill(
                        label = strings.active,
                        count = onCount,
                        accent = BrandTeal500,
                        modifier = Modifier.weight(1f)
                    )
                    CompactSummaryPill(
                        label = if (isEn) "Expired" else "Expired",
                        count = offCount,
                        accent = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    CompactSummaryPill(
                        label = "Hotspot",
                        count = hotspotCount,
                        accent = BrandBlue600,
                        modifier = Modifier.weight(1f)
                    )
                    CompactSummaryPill(
                        label = "PPPoE",
                        count = pppoeCount,
                        accent = Color(0xFF4F46E5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pencarian & Filter Ganda (Status + Tipe Layanan)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusOptions = listOf(
                        "all" to strings.all,
                        "on" to strings.active,
                        "off" to strings.expiredServices
                    )
                    items(statusOptions) { (key, label) ->
                        FilterChip(
                            selected = statusFilter.equals(key, ignoreCase = true),
                            onClick = { viewModel.onServiceStatusFilterChanged(key) },
                            label = { Text(label) }
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .width(1.dp)
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    val typeOptions = listOf(
                        "all" to (if (isEn) "All Types" else "Semua Tipe"),
                        "Hotspot" to "Hotspot",
                        "PPPoE" to "PPPoE",
                        "VPN" to "VPN"
                    )
                    items(typeOptions) { (key, label) ->
                        FilterChip(
                            selected = typeFilter.equals(key, ignoreCase = true),
                            onClick = { viewModel.onServiceTypeFilterChanged(key) },
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
                        if (filteredItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    PrepaidServiceCard(
                                        item = item,
                                        currencyCode = session.currencyCode,
                                        labels = labels,
                                        isEn = isEn,
                                        canRecharge = canRecharge,
                                        canExtend = canExtend,
                                        canDelete = canEditOrDelete,
                                        onOpenDetail = { viewModel.openActiveCustomerDetail(item) },
                                        onRecharge = {
                                            viewModel.openRechargeForm(
                                                prefillCustomerId = item.customerId,
                                                prefillPlanId = item.planId,
                                                prefillServer = item.routers,
                                                prefillServiceType = item.type,
                                                prefillFullname = item.fullname,
                                                prefillUsername = item.username,
                                                lockCustomer = true
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
private fun CompactSummaryPill(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrepaidServiceCard(
    item: PrepaidServiceItem,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    isEn: Boolean,
    canRecharge: Boolean,
    canExtend: Boolean,
    canDelete: Boolean,
    onOpenDetail: () -> Unit,
    onRecharge: () -> Unit,
    onExtend: () -> Unit,
    onViewReceipt: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isOn = item.status.equals("on", ignoreCase = true)
    val isPppoe = item.type.equals("PPPoE", ignoreCase = true)
    val daysLeft = remember(item.expiration) { calcServiceDaysRemaining(item.expiration) }
    val daysLabel = remember(daysLeft, isOn, isEn) { formatRemainingDaysLabel(daysLeft, isOn, isEn) }
    val progress = remember(daysLeft, isOn) {
        when {
            !isOn || daysLeft == null || daysLeft <= 0 -> 0.05f
            daysLeft >= 30 -> 1f
            else -> (daysLeft / 30f).coerceIn(0.08f, 1f)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Baris 1: Ikon Layanan + Nama + Username + Status ON/OFF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isOn) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPppoe) Icons.Outlined.Router else Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.fullname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${item.username} • ${item.type} • ${item.routers}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(
                    text = if (isOn) "ON" else "OFF",
                    tone = if (isOn) BadgeTone.SUCCESS else BadgeTone.DANGER
                )
            }

            // Baris 2: Paket + Harga + Sisa Hari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.namebp,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMoney(currencyCode, item.price),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Baris 3: Progress Bar Masa Aktif + Info Jatuh Tempo & Metode
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isOn && (daysLeft == null || daysLeft > 3)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exp: ${item.expiration} ${item.time.take(5)} • ${item.method}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (daysLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = daysLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOn && (daysLeft == null || daysLeft > 3)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Baris 4: Petunjuk Tap Detail Live Speed + Aksi Cepat Role-Aware
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = labels.tapForLiveDetail,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canRecharge) {
                        TextButton(
                            onClick = onRecharge,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Recharge")
                        }
                    }
                    if (!isOn && canExtend) {
                        TextButton(
                            onClick = onExtend,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Extend")
                        }
                    }
                    if (item.phonenumber.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val digits = item.phonenumber.replace(Regex("[^0-9]"), "")
                                if (digits.isNotBlank()) {
                                    val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum")))
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = "WhatsApp",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    IconButton(onClick = onViewReceipt, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Receipt",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun calcServiceDaysRemaining(expirationDate: String): Int? {
    val clean = expirationDate.trim().take(10)
    if (clean.length < 10) return null
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val expDate = sdf.parse(clean) ?: return null
        val today = sdf.parse(sdf.format(System.currentTimeMillis())) ?: return null
        val diffMs = expDate.time - today.time
        ceil(diffMs / 86_400_000.0).toInt()
    }.getOrNull()
}

internal fun formatRemainingDaysLabel(daysLeft: Int?, isOn: Boolean, isEn: Boolean): String {
    return when {
        !isOn -> if (isEn) "Expired" else "Kedaluwarsa"
        daysLeft == null -> ""
        daysLeft < 0 -> if (isEn) "Expired" else "Kedaluwarsa"
        daysLeft == 0 -> if (isEn) "Today" else "Hari Ini"
        else -> if (isEn) "${daysLeft}d left" else "Sisa $daysLeft Hari"
    }
}
