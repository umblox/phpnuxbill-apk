package com.phpnuxbill.mobile.feature.plans.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppFloatingSnackbarHost
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.plans.model.PlanBandwidthLabels
import com.phpnuxbill.mobile.feature.plans.model.PlanBandwidthMetadata
import com.phpnuxbill.mobile.feature.plans.model.PlanFormState
import com.phpnuxbill.mobile.feature.plans.model.PlanItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: PlanBandwidthViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val plansState by viewModel.plansState.collectAsStateWithLifecycle()
    val typeFilter by viewModel.planTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.planSearch.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val planForm by viewModel.planForm.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { PlanBandwidthLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteCandidate by remember { mutableStateOf<PlanItem?>(null) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    if (deleteCandidate != null) {
        val target = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("${strings.delete} (${target.namePlan})") },
            text = { Text(labels.deletePlanConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        viewModel.deletePlan(target.id)
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

    if (planForm.isOpen) {
        FullScreenPlanForm(
            form = planForm,
            metadata = metadata,
            currencyCode = session.currencyCode,
            labels = labels,
            onUpdate = viewModel::updatePlanForm,
            onTypeChanged = viewModel::onPlanFormTypeChanged,
            onRadiusToggled = viewModel::onPlanFormRadiusToggled,
            onSubmit = viewModel::submitPlanForm,
            onClose = viewModel::closePlanForm
        )
        return
    }

    val filterTypes = remember(metadata.supportedPlanTypes, strings.all) {
        buildList {
            add("All" to strings.all)
            metadata.supportedPlanTypes.forEach { t ->
                val displayLabel = if (t.equals("PPPOE", ignoreCase = true)) "PPPoE" else t
                add(t to displayLabel)
            }
        }
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
                            text = strings.menuPlans,
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
                    IconButton(onClick = { viewModel.syncPlansToRouter("all") }) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = labels.syncPlansToRouter
                        )
                    }
                    IconButton(onClick = { viewModel.loadPlansAndMetadata(isPullRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { AppFloatingSnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openAddPlanForm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = labels.addPlan)
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
                    onValueChange = viewModel::onPlanSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(labels.planNameLabel) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onPlanSearchChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterTypes) { (key, label) ->
                        FilterChip(
                            selected = typeFilter.equals(key, ignoreCase = true),
                            onClick = { viewModel.onPlanTypeFilterChanged(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = plansState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadPlansAndMetadata() }
                )
                is UiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadPlansAndMetadata(isPullRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                items(state.data, key = { it.id }) { plan ->
                                    PlanCardItem(
                                        plan = plan,
                                        currencyCode = session.currencyCode,
                                        onEdit = { viewModel.openEditPlanForm(plan) },
                                        onDelete = { deleteCandidate = plan }
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
private fun PlanCardItem(
    plan: PlanItem,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isBalance = plan.type.equals("Balance", ignoreCase = true)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
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
                        text = plan.namePlan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isBalance) {
                            "Top-Up Balance"
                        } else {
                            "${plan.type} • ${plan.bandwidthName} (${plan.rateLabel})"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(
                    text = if (plan.enabled) strings.active else "Hidden",
                    tone = if (plan.enabled) BadgeTone.SUCCESS else BadgeTone.WARNING
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBalance) {
                        plan.billingLabel
                    } else {
                        val driverSuffix = if (plan.device.isNotBlank() && !plan.isRadius) " • ${plan.device}" else ""
                        "${plan.validity} ${plan.validityUnit} • ${if (plan.isRadius) "Radius" else plan.routers}$driverSuffix"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatMoney(currencyCode, plan.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!isBalance) "${plan.activeUsers} ${strings.active}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = strings.edit, modifier = Modifier.size(18.dp))
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenPlanForm(
    form: PlanFormState,
    metadata: PlanBandwidthMetadata,
    currencyCode: String,
    labels: PlanBandwidthLabels,
    onUpdate: ((PlanFormState) -> PlanFormState) -> Unit,
    onTypeChanged: (String) -> Unit,
    onRadiusToggled: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var bwExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var routerExpanded by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }
    var deviceExpanded by remember { mutableStateOf(false) }

    val isBalance = form.type.equals("Balance", ignoreCase = true)
    val isHotspot = form.type.equals("Hotspot", ignoreCase = true)
    val isPppoeOrVpn = form.type.equals("PPPOE", ignoreCase = true) || form.type.equals("VPN", ignoreCase = true)
    val isStaticDevice = form.device.equals("MikrotikStatic", ignoreCase = true)
    val selectedBw = metadata.bandwidths.find { it.id == form.idBw }
    val formPlanTypes = remember(metadata.supportedPlanTypes) {
        metadata.supportedPlanTypes.ifEmpty { listOf("Hotspot", "PPPOE", "Balance") }
    }
    val ipPoolLabelText = remember(metadata.supportsVpn, isStaticDevice, isEn) {
        if (isStaticDevice) {
            if (isEn) "IP Pool (Optional for MikrotikStatic)" else "IP Pool (Opsional utk MikrotikStatic)"
        } else {
            labels.ipPoolLabelFor(metadata.supportsVpn)
        }
    }

    AppScreenScaffold(
        title = if (form.isEdit) labels.editPlan else labels.addPlan,
        subtitle = form.type,
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
                        enabled = !form.isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSaving && form.namePlan.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
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
                        Text(strings.save)
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

            // Tipe Paket (Dinamis mengikuti ENUM aktual tbl_plans.type di server)
            if (!form.isEdit) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    formPlanTypes.forEachIndexed { idx, t ->
                        val displayLabel = if (t.equals("PPPOE", ignoreCase = true)) "PPPoE" else t
                        SegmentedButton(
                            selected = form.type.equals(t, ignoreCase = true),
                            onClick = { onTypeChanged(t) },
                            shape = SegmentedButtonDefaults.itemShape(idx, formPlanTypes.size),
                            label = { Text(displayLabel, maxLines = 1) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = form.namePlan,
                onValueChange = { v -> onUpdate { it.copy(namePlan = v) } },
                label = { Text(labels.planNameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = form.price,
                    onValueChange = { v -> onUpdate { it.copy(price = v) } },
                    label = { Text("${labels.priceLabel} ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = form.priceOld,
                    onValueChange = { v -> onUpdate { it.copy(priceOld = v) } },
                    label = { Text(labels.oldPriceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(labels.planEnabledLabel, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = form.enabled,
                    onCheckedChange = { c -> onUpdate { it.copy(enabled = c) } }
                )
            }

            if (!isBalance) {
                // Skema Tagihan: Prepaid vs Postpaid
                Text(labels.billingTypeLabel, style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf("yes" to labels.prepaidLabel, "no" to labels.postpaidLabel)
                    modes.forEachIndexed { idx, (key, label) ->
                        SegmentedButton(
                            selected = form.prepaid == key,
                            onClick = { onUpdate { it.copy(prepaid = key) } },
                            shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                            label = { Text(label) }
                        )
                    }
                }

                if (form.prepaid == "no") {
                    OutlinedTextField(
                        value = form.expiredDate,
                        onValueChange = { v -> onUpdate { it.copy(expiredDate = v.filter { c -> c.isDigit() }) } },
                        label = { Text(labels.postpaidCycleDayLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Pilih Profil Bandwidth
                ExposedDropdownMenuBox(
                    expanded = bwExpanded,
                    onExpandedChange = { bwExpanded = !bwExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBw?.let {
                            "${it.nameBw} (${it.rateDown}${it.rateDownUnit}/${it.rateUp}${it.rateUpUnit})"
                        } ?: labels.bandwidthProfileLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(labels.bandwidthProfileLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bwExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = bwExpanded,
                        onDismissRequest = { bwExpanded = false }
                    ) {
                        metadata.bandwidths.forEach { bw ->
                            DropdownMenuItem(
                                text = {
                                    Text("${bw.nameBw} (${bw.rateDown} ${bw.rateDownUnit} / ${bw.rateUp} ${bw.rateUpUnit})")
                                },
                                onClick = {
                                    onUpdate { it.copy(idBw = bw.id) }
                                    bwExpanded = false
                                }
                            )
                        }
                    }
                }

                // Masa Aktif & Satuan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = form.validity,
                        onValueChange = { v -> onUpdate { it.copy(validity = v.filter { c -> c.isDigit() }) } },
                        label = { Text(labels.validityDurationLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = form.validityUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(labels.validityUnitLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            metadata.validityUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        onUpdate { it.copy(validityUnit = unit) }
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Radius vs Router MikroTik
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(labels.useRadiusLabel, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = form.isRadius,
                        onCheckedChange = onRadiusToggled
                    )
                }

                if (!form.isRadius) {
                    ExposedDropdownMenuBox(
                        expanded = routerExpanded,
                        onExpandedChange = { routerExpanded = !routerExpanded }
                    ) {
                        OutlinedTextField(
                            value = form.routers.ifBlank { labels.routerServerLabel },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(labels.routerServerLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = routerExpanded,
                            onDismissRequest = { routerExpanded = false }
                        ) {
                            metadata.routers.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        onUpdate { it.copy(routers = r.name) }
                                        routerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // IP Pool untuk PPPoE (dan VPN jika didukung server; opsional untuk plugin MikrotikStatic)
                if (isPppoeOrVpn) {
                    val matchingPools = remember(metadata.pools, form.routers, form.isRadius) {
                        if (form.isRadius) metadata.pools
                        else metadata.pools.filter { it.routers.equals(form.routers, ignoreCase = true) }
                            .ifEmpty { metadata.pools }
                    }
                    ExposedDropdownMenuBox(
                        expanded = poolExpanded,
                        onExpandedChange = { poolExpanded = !poolExpanded }
                    ) {
                        OutlinedTextField(
                            value = form.pool.ifBlank { ipPoolLabelText },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(ipPoolLabelText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = poolExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = poolExpanded,
                            onDismissRequest = { poolExpanded = false }
                        ) {
                            if (isStaticDevice) {
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "- None (Static IP Binding) -" else "- Tanpa Pool (IP Statis) -") },
                                    onClick = {
                                        onUpdate { it.copy(pool = "") }
                                        poolExpanded = false
                                    }
                                )
                            }
                            matchingPools.forEach { pl ->
                                DropdownMenuItem(
                                    text = { Text("${pl.poolName} (${pl.rangeIp})") },
                                    onClick = {
                                        onUpdate { it.copy(pool = pl.poolName) }
                                        poolExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Khusus Hotspot: Shared Users & Mode Kuota/Waktu
                if (isHotspot) {
                    OutlinedTextField(
                        value = form.sharedUsers,
                        onValueChange = { v -> onUpdate { it.copy(sharedUsers = v.filter { c -> c.isDigit() }) } },
                        label = { Text(labels.sharedUsersLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(labels.hotspotLimitModeLabel, style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        metadata.typebps.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = form.typebp.equals(mode, ignoreCase = true),
                                onClick = { onUpdate { it.copy(typebp = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(idx, metadata.typebps.size),
                                label = { Text(mode) }
                            )
                        }
                    }

                    if (form.typebp.equals("Limited", ignoreCase = true)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = form.timeLimit,
                                onValueChange = { v -> onUpdate { it.copy(timeLimit = v.filter { c -> c.isDigit() }) } },
                                label = { Text("${labels.timeLimitLabel} (${form.timeUnit})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = form.dataLimit,
                                onValueChange = { v -> onUpdate { it.copy(dataLimit = v) } },
                                label = { Text("${labels.dataLimitLabel} (${form.dataUnit})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Pilihan Device Driver (dinamis dari folder system/devices/ server)
                ExposedDropdownMenuBox(
                    expanded = deviceExpanded,
                    onExpandedChange = { deviceExpanded = !deviceExpanded }
                ) {
                    OutlinedTextField(
                        value = form.device,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(labels.deviceDriverLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = deviceExpanded,
                        onDismissRequest = { deviceExpanded = false }
                    ) {
                        metadata.devices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text(dev) },
                                onClick = {
                                    onUpdate { it.copy(device = dev) }
                                    deviceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Info kontekstual khusus plugin opsional MikrotikStatic (hanya tampil jika terpasang & dipilih)
                if (isStaticDevice) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isEn) {
                                    "MikrotikStatic plugin active: binds customer's Static IP (PPPoE IP) via /queue/simple and isolates expired users using firewall address-list \"Expired\"."
                                } else {
                                    "Plugin MikrotikStatic aktif: mengikat IP Statis (PPPoE IP) pelanggan lewat /queue/simple dan isolir otomatis ke firewall address-list \"Expired\"."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
