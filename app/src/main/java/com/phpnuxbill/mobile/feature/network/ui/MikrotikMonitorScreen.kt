package com.phpnuxbill.mobile.feature.network.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.network.model.IpPoolFormState
import com.phpnuxbill.mobile.feature.network.model.IpPoolItem
import com.phpnuxbill.mobile.feature.network.model.NetworkGenieAcsLabels
import com.phpnuxbill.mobile.feature.network.model.OnlineSessionUser
import com.phpnuxbill.mobile.feature.network.model.OnlineUsersSummary
import com.phpnuxbill.mobile.feature.network.model.RouterFormState
import com.phpnuxbill.mobile.feature.network.model.RouterItem
import com.phpnuxbill.mobile.feature.network.model.RouterLiveStatus
import com.phpnuxbill.mobile.feature.network.model.RouterLogEntry
import com.phpnuxbill.mobile.feature.network.model.TrafficSpeedSample

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikrotikMonitorScreen(
    viewModel: NetworkGenieAcsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val selectedTab by viewModel.selectedMikrotikTab.collectAsStateWithLifecycle()
    val routers by viewModel.routers.collectAsStateWithLifecycle()
    val selectedRouterId by viewModel.selectedRouterId.collectAsStateWithLifecycle()
    val selectedInterface by viewModel.selectedInterface.collectAsStateWithLifecycle()
    val liveState by viewModel.liveStatusState.collectAsStateWithLifecycle()
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val isPollingLive by viewModel.isPollingLive.collectAsStateWithLifecycle()
    val onlineUsersState by viewModel.onlineUsersState.collectAsStateWithLifecycle()
    val onlineUserSearch by viewModel.onlineUserSearch.collectAsStateWithLifecycle()
    val ipPools by viewModel.ipPools.collectAsStateWithLifecycle()
    val routerLogs by viewModel.routerLogs.collectAsStateWithLifecycle()
    val routerForm by viewModel.routerForm.collectAsStateWithLifecycle()
    val poolForm by viewModel.poolForm.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { NetworkGenieAcsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showRebootConfirm by remember { mutableStateOf(false) }
    var kickTarget by remember { mutableStateOf<OnlineSessionUser?>(null) }

    DisposableEffect(Unit) {
        if (selectedTab == 0) viewModel.startLivePolling()
        onDispose { viewModel.stopLivePolling() }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    if (routerForm.isOpen) {
        FullScreenRouterForm(
            form = routerForm,
            labels = labels,
            onUpdate = viewModel::updateRouterForm,
            onSubmit = viewModel::submitRouterForm,
            onClose = viewModel::closeRouterForm
        )
        return
    }

    if (poolForm.isOpen) {
        FullScreenPoolForm(
            form = poolForm,
            routers = routers,
            labels = labels,
            onUpdate = viewModel::updatePoolForm,
            onSubmit = viewModel::submitPoolForm,
            onClose = viewModel::closePoolForm
        )
        return
    }

    if (showRebootConfirm && selectedRouterId > 0) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text(strings.rebootRouter) },
            text = { Text(labels.rebootConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebootConfirm = false
                        viewModel.rebootSelectedRouter(selectedRouterId)
                    }
                ) {
                    Text(strings.confirm, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    if (kickTarget != null) {
        val target = kickTarget!!
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            title = { Text("${strings.disconnectUser} (${target.username})") },
            text = { Text(labels.kickConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        kickTarget = null
                        viewModel.kickUser(target)
                    }
                ) {
                    Text(strings.confirm, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { kickTarget = null }) { Text(strings.cancel) }
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
                            text = strings.menuMikrotik,
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
                    if (selectedTab == 0) {
                        IconButton(onClick = viewModel::toggleLivePolling) {
                            Icon(
                                imageVector = if (isPollingLive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = "Live Polling",
                                tint = if (isPollingLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedRouterId > 0) {
                            IconButton(onClick = { showRebootConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.PowerSettingsNew,
                                    contentDescription = strings.rebootRouter,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                when (selectedTab) {
                                    1 -> viewModel.loadOnlineUsers(isPullRefresh = true)
                                    2 -> viewModel.loadRoutersAndPools()
                                    3 -> viewModel.loadRouterLogs()
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                        }
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
            // Pemilih Router Aktif (jika lebih dari 1 router)
            if (routers.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routers, key = { it.id }) { r ->
                        FilterChip(
                            selected = selectedRouterId == r.id,
                            onClick = { viewModel.onSelectRouter(r.id) },
                            label = { Text(r.name) }
                        )
                    }
                }
            }

            // Tab Navigasi MikroTik
            val tabs = listOf(
                labels.tabLiveMonitor,
                labels.tabOnlineUsers,
                labels.tabRoutersPools,
                labels.tabRouterLogs
            )
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { viewModel.onMikrotikTabSelected(idx) },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LiveMonitorTabContent(
                    liveState = liveState,
                    speedHistory = speedHistory,
                    selectedInterface = selectedInterface,
                    labels = labels,
                    onSelectInterface = viewModel::onSelectInterface,
                    onRetry = { viewModel.fetchSingleLiveSample(silent = false) }
                )
                1 -> OnlineUsersTabContent(
                    state = onlineUsersState,
                    searchQuery = onlineUserSearch,
                    isRefreshing = isRefreshing,
                    onSearchChanged = viewModel::onOnlineUserSearchChanged,
                    onRefresh = { viewModel.loadOnlineUsers(isPullRefresh = true) },
                    onKickUser = { kickTarget = it }
                )
                2 -> RoutersAndPoolsTabContent(
                    routers = routers,
                    pools = ipPools,
                    labels = labels,
                    onAddRouter = viewModel::openAddRouterForm,
                    onEditRouter = viewModel::openEditRouterForm,
                    onDeleteRouter = viewModel::deleteRouter,
                    onAddPool = viewModel::openAddPoolForm,
                    onEditPool = viewModel::openEditPoolForm,
                    onDeletePool = viewModel::deletePool
                )
                3 -> RouterLogsTabContent(
                    logs = routerLogs,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::loadRouterLogs
                )
            }
        }
    }
}

@Composable
private fun LiveMonitorTabContent(
    liveState: UiState<RouterLiveStatus>,
    speedHistory: List<TrafficSpeedSample>,
    selectedInterface: String,
    labels: NetworkGenieAcsLabels,
    onSelectInterface: (String) -> Unit,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    when (liveState) {
        is UiState.Idle, is UiState.Loading -> LoadingStateView()
        is UiState.Error -> ErrorStateView(message = liveState.message, onRetry = onRetry)
        is UiState.Success -> {
            val status = liveState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Status & Spesifikasi Board MikroTik
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${status.name} (${status.ipAddress})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${status.boardName} • RouterOS ${status.version}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(
                                    text = if (status.online) "${strings.online} (${status.pingMs}ms)" else strings.offline,
                                    tone = if (status.online) BadgeTone.SUCCESS else BadgeTone.DANGER
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ResourcePill(
                                    label = strings.cpuLoad,
                                    value = "${status.cpuLoad}%",
                                    modifier = Modifier.weight(1f)
                                )
                                ResourcePill(
                                    label = strings.memoryUsage,
                                    value = "${status.ramPercent.toInt()}%",
                                    modifier = Modifier.weight(1f)
                                )
                                ResourcePill(
                                    label = strings.uptime,
                                    value = status.uptime,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hotspot Online: ${status.hotspotOnline}  •  PPPoE Online: ${status.pppoeOnline}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 2. Pemilih Interface & Grafik Live Speed (Tanpa Skala Minimum Hardcode)
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = labels.liveTrafficTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (status.interfacesList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(status.interfacesList) { iface ->
                                        FilterChip(
                                            selected = (selectedInterface.ifBlank { status.monitoredInterface }) == iface,
                                            onClick = { onSelectInterface(iface) },
                                            label = { Text(iface) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(strings.downloadSpeed, style = MaterialTheme.typography.labelMedium, color = BrandBlue600)
                                    Text(
                                        text = status.rxSpeedLabel,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandBlue600
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(strings.uploadSpeed, style = MaterialTheme.typography.labelMedium, color = BrandTeal500)
                                    Text(
                                        text = status.txSpeedLabel,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandTeal500
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LiveSpeedChartCanvas(
                                samples = speedHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                        }
                    }
                }

                // 3. Statistik Total Kuota per Interface
                if (status.interfacesTraffic.isNotEmpty()) {
                    item {
                        Text(
                            text = labels.interfaceStatsTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(status.interfacesTraffic, key = { it.name }) { iface ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(iface.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "RX: ${iface.rx} • TX: ${iface.tx}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(
                                    text = iface.status,
                                    tone = if (iface.running) BadgeTone.SUCCESS else BadgeTone.NEUTRAL
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grafik kecepatan real-time dengan auto-scaling penuh (tanpa batas minimum hardcode)
 */
@Composable
private fun LiveSpeedChartCanvas(
    samples: List<TrafficSpeedSample>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxPeakBps = remember(samples) {
        val peak = samples.maxOfOrNull { maxOf(it.rxBps, it.txBps) } ?: 0L
        if (peak <= 0L) 1L else peak
    }

    Column(modifier = modifier) {
        Text(
            text = "Peak: ${formatBitsPerSec(maxPeakBps)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height

            // Garis bantu horizontal (0%, 50%, 100%)
            for (step in 0..2) {
                val y = h * (step / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (samples.size < 2) return@Canvas

            val stepX = w / (samples.size - 1).coerceAtLeast(1)
            val rxPath = Path()
            val txPath = Path()

            samples.forEachIndexed { idx, s ->
                val x = idx * stepX
                val rxRatio = (s.rxBps.toFloat() / maxPeakBps.toFloat()).coerceIn(0f, 1f)
                val txRatio = (s.txBps.toFloat() / maxPeakBps.toFloat()).coerceIn(0f, 1f)
                val yRx = h - (rxRatio * h)
                val yTx = h - (txRatio * h)

                if (idx == 0) {
                    rxPath.moveTo(x, yRx)
                    txPath.moveTo(x, yTx)
                } else {
                    rxPath.lineTo(x, yRx)
                    txPath.lineTo(x, yTx)
                }
            }

            drawPath(
                path = rxPath,
                color = BrandBlue600,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = txPath,
                color = BrandTeal500,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun ResourcePill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OnlineUsersTabContent(
    state: UiState<OnlineUsersSummary>,
    searchQuery: String,
    isRefreshing: Boolean,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onKickUser: (OnlineSessionUser) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        when (state) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(message = state.message, onRetry = onRefresh)
            is UiState.Success -> {
                val filteredUsers = remember(state.data.users, searchQuery) {
                    if (searchQuery.isBlank()) state.data.users
                    else state.data.users.filter {
                        it.username.contains(searchQuery, ignoreCase = true) ||
                            it.ipAddress.contains(searchQuery, ignoreCase = true) ||
                            it.macAddress.contains(searchQuery, ignoreCase = true)
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "${strings.onlineUsers}: ${filteredUsers.size} • Total: ${state.data.totalTraffic}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(filteredUsers, key = { "${it.router}_${it.service}_${it.rosId}_${it.username}" }) { u ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = u.username,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(
                                                text = u.service,
                                                tone = if (u.service.equals("PPPoE", true)) BadgeTone.SUCCESS else BadgeTone.NEUTRAL
                                            )
                                        }
                                        Text(
                                            text = "IP: ${u.ipAddress} • MAC: ${u.macAddress}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Up: ${u.uptime} • ${u.total}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onKickUser(u) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonOff,
                                            contentDescription = strings.disconnectUser,
                                            tint = MaterialTheme.colorScheme.error
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

@Composable
private fun RoutersAndPoolsTabContent(
    routers: List<RouterItem>,
    pools: List<IpPoolItem>,
    labels: NetworkGenieAcsLabels,
    onAddRouter: () -> Unit,
    onEditRouter: (RouterItem) -> Unit,
    onDeleteRouter: (Int) -> Unit,
    onAddPool: () -> Unit,
    onEditPool: (IpPoolItem) -> Unit,
    onDeletePool: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MikroTik Routers (${routers.size})", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddRouter, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.add)
                }
            }
        }

        items(routers, key = { "r_${it.id}" }) { r ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${r.ipAddress} • User: ${r.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEditRouter(r) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                    }
                    IconButton(onClick = { onDeleteRouter(r.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("IP Pools (${pools.size})", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddPool, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.add)
                }
            }
        }

        items(pools, key = { "p_${it.id}" }) { p ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.poolName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${p.rangeIp} • ${p.routers}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEditPool(p) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                    }
                    IconButton(onClick = { onDeletePool(p.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouterLogsTabContent(
    logs: List<RouterLogEntry>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val strings = LocalAppStrings.current
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${log.time} • [${log.topics}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenRouterForm(
    form: RouterFormState,
    labels: NetworkGenieAcsLabels,
    onUpdate: ((RouterFormState) -> RouterFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppScreenScaffold(
        title = if (form.isEdit) labels.editRouter else labels.addRouter,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSaving && form.name.isNotBlank() && form.ipAddress.isNotBlank(),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        if (form.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(strings.save)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(form.error.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> onUpdate { it.copy(name = v) } },
                label = { Text(labels.routerNameLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.ipAddress,
                onValueChange = { v -> onUpdate { it.copy(ipAddress = v) } },
                label = { Text(labels.routerIpPortLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.username,
                onValueChange = { v -> onUpdate { it.copy(username = v) } },
                label = { Text(strings.usernameLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = { v -> onUpdate { it.copy(password = v) } },
                label = { Text(strings.passwordLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.coordinates,
                onValueChange = { v -> onUpdate { it.copy(coordinates = v) } },
                label = { Text("Coordinates (lat, lng)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(labels.testConnectionToggle, modifier = Modifier.weight(1f))
                Switch(
                    checked = form.testConnection,
                    onCheckedChange = { c -> onUpdate { it.copy(testConnection = c) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenPoolForm(
    form: IpPoolFormState,
    routers: List<RouterItem>,
    labels: NetworkGenieAcsLabels,
    onUpdate: ((IpPoolFormState) -> IpPoolFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var routerExpanded by remember { mutableStateOf(false) }

    AppScreenScaffold(
        title = if (form.isEdit) labels.editIpPool else labels.addIpPool,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSaving && form.poolName.isNotBlank() && form.rangeIp.isNotBlank(),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        if (form.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(strings.save)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(form.error.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
            OutlinedTextField(
                value = form.poolName,
                onValueChange = { v -> onUpdate { it.copy(poolName = v) } },
                label = { Text(labels.poolNameLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.rangeIp,
                onValueChange = { v -> onUpdate { it.copy(rangeIp = v) } },
                label = { Text(labels.rangeIpLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.localIp,
                onValueChange = { v -> onUpdate { it.copy(localIp = v) } },
                label = { Text(labels.localIpLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenuBox(
                expanded = routerExpanded,
                onExpandedChange = { routerExpanded = !routerExpanded }
            ) {
                OutlinedTextField(
                    value = form.routers,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Router") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = routerExpanded,
                    onDismissRequest = { routerExpanded = false }
                ) {
                    routers.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.name) },
                            onClick = {
                                onUpdate { it.copy(routers = r.name) }
                                routerExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("radius") },
                        onClick = {
                            onUpdate { it.copy(routers = "radius") }
                            routerExpanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatBitsPerSec(bps: Long): String {
    return when {
        bps >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.2f Gbps", bps / 1_000_000_000.0)
        bps >= 1_000_000L -> String.format(java.util.Locale.US, "%.2f Mbps", bps / 1_000_000.0)
        bps >= 1_000L -> String.format(java.util.Locale.US, "%.1f Kbps", bps / 1_000.0)
        else -> "$bps bps"
    }
}
