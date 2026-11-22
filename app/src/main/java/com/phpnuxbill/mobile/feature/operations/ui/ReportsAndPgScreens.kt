package com.phpnuxbill.mobile.feature.operations.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels
import com.phpnuxbill.mobile.feature.operations.model.PgTransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val reportState by viewModel.reportState.collectAsStateWithLifecycle()
    val startDate by viewModel.reportStartDate.collectAsStateWithLifecycle()
    val endDate by viewModel.reportEndDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.reportSearch.collectAsStateWithLifecycle()
    val gwFilter by viewModel.reportGatewayFilter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.reportTypeFilter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { OperationsLabels(strings) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(strings.menuReports, style = MaterialTheme.typography.titleLarge)
                        Text(session.effectiveCompanyName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadFinancialReports(isPullRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { viewModel.onReportDateChanged(it, endDate) },
                        label = { Text(labels.startDateLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { viewModel.onReportDateChanged(startDate, it) },
                        label = { Text(labels.endDateLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onReportSearchChanged,
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onReportSearchChanged("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { viewModel.loadFinancialReports(isPullRefresh = true) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(labels.filterBtn)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf("all" to strings.all, "Hotspot" to "Hotspot", "PPPOE" to "PPPoE", "Balance" to "Balance")
                    items(types) { (k, label) ->
                        FilterChip(
                            selected = typeFilter.equals(k, true),
                            onClick = { viewModel.onReportTypeFilterChanged(k) },
                            label = { Text(label) }
                        )
                    }
                    if (!gwFilter.equals("all", true)) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onReportGatewayFilterChanged("all") },
                                label = { Text("$gwFilter ✕") }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = reportState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadFinancialReports() })
                is UiState.Success -> {
                    val data = state.data
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadFinancialReports(isPullRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("${data.startDate}  —  ${data.endDate}", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatMoney(session.currencyCode, data.totalIncome),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text("${data.totalTransactions} ${labels.totalTrxLabel}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            if (data.breakdown.isNotEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(labels.revenueByMethod, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            data.breakdown.forEach { b ->
                                                val isActive = gwFilter.equals(b.gateway, true)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                            else MaterialTheme.colorScheme.surface
                                                        )
                                                        .clickable {
                                                            viewModel.onReportGatewayFilterChanged(
                                                                if (isActive) "all" else b.gateway
                                                            )
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("${b.gateway} (${b.count}x)", style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        text = formatMoney(session.currencyCode, b.total),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(data.transactions, key = { it.id }) { trx ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${trx.invoice} • ${trx.username}", fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${trx.planName} (${trx.method})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${trx.rechargedOn} ${trx.rechargedTime} • ${trx.routers}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = formatMoney(session.currencyCode, trx.price),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val pgState by viewModel.pgState.collectAsStateWithLifecycle()
    val statusFilter by viewModel.pgStatusFilter.collectAsStateWithLifecycle()
    val gwFilter by viewModel.pgGatewayFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.pgSearch.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { OperationsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    var approveTarget by remember { mutableStateOf<PgTransactionItem?>(null) }
    var cancelTarget by remember { mutableStateOf<PgTransactionItem?>(null) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    if (approveTarget != null) {
        val target = approveTarget!!
        AlertDialog(
            onDismissRequest = { approveTarget = null },
            title = { Text("${labels.approveManual} (#${target.id})") },
            text = { Text(labels.approveConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        approveTarget = null
                        viewModel.triggerPgAction("approve_manual", target.id)
                    }
                ) { Text(strings.confirm) }
            },
            dismissButton = { TextButton(onClick = { approveTarget = null }) { Text(strings.cancel) } }
        )
    }

    if (cancelTarget != null) {
        val target = cancelTarget!!
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("${labels.cancelBill} (#${target.id})") },
            text = { Text(labels.cancelConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelTarget = null
                        viewModel.triggerPgAction("cancel", target.id)
                    }
                ) { Text(strings.confirm, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text(strings.cancel) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(strings.menuPaymentGateway, style = MaterialTheme.typography.titleLarge)
                        Text(session.effectiveCompanyName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPgTransactions(isPullRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onPgSearchChanged,
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onPgSearchChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statuses = listOf("all" to strings.all, "1" to "Unpaid", "2" to "Paid", "3" to "Expired", "4" to "Canceled")
                    items(statuses) { (k, label) ->
                        FilterChip(
                            selected = statusFilter == k,
                            onClick = { viewModel.onPgStatusFilterChanged(k) },
                            label = { Text(label) }
                        )
                    }
                }

                val availableGws = (pgState as? UiState.Success)?.data?.gateways.orEmpty()
                if (availableGws.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = gwFilter.equals("all", true),
                                onClick = { viewModel.onPgGatewayFilterChanged("all") },
                                label = { Text("All PG") }
                            )
                        }
                        items(availableGws) { gw ->
                            FilterChip(
                                selected = gwFilter.equals(gw, true),
                                onClick = { viewModel.onPgGatewayFilterChanged(gw) },
                                label = { Text(gw.uppercase()) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = pgState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(message = state.message, onRetry = { viewModel.loadPgTransactions() })
                is UiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadPgTransactions(isPullRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ElevatedCard(
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(labels.totalPaidLabel, style = MaterialTheme.typography.labelMedium)
                                            Text(
                                                text = formatMoney(session.currencyCode, state.data.totalPaidAmount),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    ElevatedCard(
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(labels.totalUnpaidLabel, style = MaterialTheme.typography.labelMedium)
                                            Text(
                                                text = state.data.totalUnpaidCount.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            items(state.data.items, key = { it.id }) { item ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${item.username} • ${item.planName}", fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "${item.gateway.uppercase()} (${item.paymentChannel}) • ${item.createdDate}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (item.trxInvoice.isNotBlank()) {
                                                    Text(
                                                        text = item.trxInvoice,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            StatusBadge(
                                                text = item.statusLabel,
                                                tone = when (item.status) {
                                                    2 -> BadgeTone.SUCCESS
                                                    1 -> BadgeTone.WARNING
                                                    else -> BadgeTone.DANGER
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = formatMoney(session.currencyCode, item.price),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (item.status == 1) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(onClick = { cancelTarget = item }) {
                                                    Text(labels.cancelBill, color = MaterialTheme.colorScheme.error)
                                                }
                                                TextButton(onClick = { viewModel.triggerPgAction("check", item.id) }) {
                                                    Text(labels.checkStatus)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Button(onClick = { approveTarget = item }, shape = RoundedCornerShape(10.dp)) {
                                                    Text(labels.approveManual)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
