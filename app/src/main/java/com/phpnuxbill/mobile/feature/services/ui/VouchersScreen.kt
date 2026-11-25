package com.phpnuxbill.mobile.feature.services.ui

import android.content.Intent
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import com.phpnuxbill.mobile.feature.services.model.ActiveServiceSheetMode
import com.phpnuxbill.mobile.feature.services.model.GenerateVoucherFormState
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherLabels
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata
import com.phpnuxbill.mobile.feature.services.model.VoucherItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    viewModel: ServiceVoucherViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val vouchersState by viewModel.vouchersState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.voucherSearch.collectAsStateWithLifecycle()
    val statusFilter by viewModel.voucherStatusFilter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val genForm by viewModel.generateVoucherForm.collectAsStateWithLifecycle()
    val generatedBatch by viewModel.generatedCodesBatch.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeServiceMode.collectAsStateWithLifecycle()
    val refillForm by viewModel.refillForm.collectAsStateWithLifecycle()
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { ServiceVoucherLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showCleanUsedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    if (activeReceipt != null) {
        ReceiptPreviewScreen(
            receipt = activeReceipt!!,
            companyName = session.effectiveCompanyName,
            viewModel = viewModel,
            onClose = viewModel::closeReceipt
        )
        return
    }

    if (activeMode == ActiveServiceSheetMode.REFILL_VOUCHER) {
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

    if (genForm.isOpen) {
        FullScreenGenerateVoucherForm(
            form = genForm,
            metadata = metadata,
            currencyCode = session.currencyCode,
            labels = labels,
            onUpdate = viewModel::updateGenerateVoucherForm,
            onSubmit = viewModel::submitGenerateVouchers,
            onClose = viewModel::closeGenerateVoucherForm
        )
        return
    }

    // Dialog hasil Generate Voucher Massal (Siap Salin / Bagikan)
    if (generatedBatch.isNotEmpty()) {
        val joinedCodes = generatedBatch.joinToString("\n")
        AlertDialog(
            onDismissRequest = viewModel::dismissGeneratedBatch,
            title = { Text("${labels.batchCodesCreated} (${generatedBatch.size})") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = joinedCodes,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(joinedCodes))
                        viewModel.dismissGeneratedBatch()
                    }
                ) {
                    Text(strings.copy)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${session.effectiveCompanyName}\n\n$joinedCodes")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, labels.batchCodesCreated))
                    }
                ) {
                    Text(strings.share)
                }
            }
        )
    }

    if (showCleanUsedDialog) {
        AlertDialog(
            onDismissRequest = { showCleanUsedDialog = false },
            title = { Text(labels.cleanUsedVouchers) },
            text = { Text(labels.cleanUsedConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanUsedDialog = false
                        viewModel.deleteUsedVouchers()
                    }
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanUsedDialog = false }) {
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
                            text = strings.menuVouchers,
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
                    IconButton(onClick = { showCleanUsedDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = labels.cleanUsedVouchers
                        )
                    }
                    IconButton(onClick = { viewModel.loadVouchers(isPullRefresh = true) }) {
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
                onClick = viewModel::openGenerateVoucherForm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = strings.generateVoucher)
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
                    onValueChange = viewModel::onVoucherSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(labels.voucherCodeLabel) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onVoucherSearchChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to strings.all,
                        "0" to labels.unusedStatus,
                        "1" to labels.usedStatus
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { viewModel.onVoucherStatusFilterChanged(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = vouchersState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadVouchers() }
                )
                is UiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadVouchers(isPullRefresh = true) },
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
                                items(state.data, key = { it.id }) { voucher ->
                                    VoucherCardItem(
                                        voucher = voucher,
                                        currencyCode = session.currencyCode,
                                        labels = labels,
                                        onCopy = { clipboard.setText(AnnotatedString(voucher.code)) },
                                        onShare = {
                                            val msg = "${session.effectiveCompanyName}\nVoucher: ${voucher.code}\nPaket: ${voucher.namePlan} (${voucher.validity})\nHarga: ${formatMoney(session.currencyCode, voucher.price)}"
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, voucher.code))
                                        },
                                        onRefill = { viewModel.openRefillForm(voucher.code) },
                                        onDelete = { viewModel.deleteVoucher(voucher.id) }
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
private fun VoucherCardItem(
    voucher: VoucherItem,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRefill: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
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
                Text(
                    text = voucher.code,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(
                    text = if (voucher.isUsed) labels.usedStatus else labels.unusedStatus,
                    tone = if (voucher.isUsed) BadgeTone.NEUTRAL else BadgeTone.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${voucher.namePlan} • ${voucher.validity} • ${voucher.routers}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatMoney(currencyCode, voucher.price),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (voucher.isUsed && voucher.user != "0" && voucher.user.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used by: @${voucher.user} (${voucher.usedDate})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (!voucher.isUsed) {
                    TextButton(onClick = onRefill) {
                        Text("Refill")
                    }
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = strings.copy, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = strings.share, modifier = Modifier.size(18.dp))
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
private fun FullScreenGenerateVoucherForm(
    form: GenerateVoucherFormState,
    metadata: ServiceVoucherMetadata,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onUpdate: ((GenerateVoucherFormState) -> GenerateVoucherFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var planExpanded by remember { mutableStateOf(false) }
    var serverExpanded by remember { mutableStateOf(false) }
    val selectedPlan = metadata.internetPlans.find { it.id == form.planId }

    AppScreenScaffold(
        title = strings.generateVoucher,
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
                        enabled = !form.isSubmitting && form.planId > 0,
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
                        Text(strings.generateVoucher)
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
                                onUpdate { it.copy(planId = p.id, server = p.routers) }
                                planExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = serverExpanded,
                onExpandedChange = { serverExpanded = !serverExpanded }
            ) {
                OutlinedTextField(
                    value = form.server,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Router / Server") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serverExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = serverExpanded,
                    onDismissRequest = { serverExpanded = false }
                ) {
                    metadata.routers.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.name) },
                            onClick = {
                                onUpdate { it.copy(server = r.name) }
                                serverExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("radius") },
                        onClick = {
                            onUpdate { it.copy(server = "radius") }
                            serverExpanded = false
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = form.count,
                    onValueChange = { c -> onUpdate { it.copy(count = c.filter { ch -> ch.isDigit() }) } },
                    label = { Text(labels.voucherCountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = form.lengthCode,
                    onValueChange = { l -> onUpdate { it.copy(lengthCode = l.filter { ch -> ch.isDigit() }) } },
                    label = { Text(labels.voucherLengthLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Text(labels.codeFormatLabel, style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val formats = listOf(
                    "up" to "ABC",
                    "low" to "abc",
                    "rand" to "AbC",
                    "numbers" to "123"
                )
                formats.forEachIndexed { idx, (key, label) ->
                    SegmentedButton(
                        selected = form.format == key,
                        onClick = { onUpdate { it.copy(format = key) } },
                        shape = SegmentedButtonDefaults.itemShape(idx, formats.size),
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = form.prefix,
                onValueChange = { p -> onUpdate { it.copy(prefix = p) } },
                label = { Text(labels.voucherPrefixLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = form.batchName,
                onValueChange = { b -> onUpdate { it.copy(batchName = b) } },
                label = { Text(labels.batchNameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
