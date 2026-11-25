package com.phpnuxbill.mobile.feature.network.ui

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.phpnuxbill.mobile.feature.network.model.GenieAcsDeviceDetail
import com.phpnuxbill.mobile.feature.network.model.GenieAcsDeviceItem
import com.phpnuxbill.mobile.feature.network.model.GenieAcsHost
import com.phpnuxbill.mobile.feature.network.model.GenieAcsHostFormState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWanEditState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWlanEditState
import com.phpnuxbill.mobile.feature.network.model.NetworkGenieAcsLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenieAcsScreen(
    viewModel: NetworkGenieAcsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val hosts by viewModel.genieHosts.collectAsStateWithLifecycle()
    val selectedHostId by viewModel.selectedGenieHostId.collectAsStateWithLifecycle()
    val stats by viewModel.genieStats.collectAsStateWithLifecycle()
    val devicesState by viewModel.genieDevicesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.genieSearch.collectAsStateWithLifecycle()
    val statusFilter by viewModel.genieStatusFilter.collectAsStateWithLifecycle()
    val detailState by viewModel.deviceDetailState.collectAsStateWithLifecycle()
    val showHostsManager by viewModel.showHostsManager.collectAsStateWithLifecycle()
    val hostForm by viewModel.hostForm.collectAsStateWithLifecycle()
    val wlanForm by viewModel.wlanEditForm.collectAsStateWithLifecycle()
    val wanForm by viewModel.wanEditForm.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { NetworkGenieAcsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    // Sub-layar 1: Form Tambah/Edit Host GenieACS
    if (hostForm.isOpen) {
        FullScreenGenieHostForm(
            form = hostForm,
            labels = labels,
            onUpdate = viewModel::updateHostForm,
            onSubmit = viewModel::submitHostForm,
            onClose = viewModel::closeHostForm
        )
        return
    }

    // Sub-layar 2: Kelola Multi-Host GenieACS
    if (showHostsManager) {
        FullScreenHostsManager(
            hosts = hosts,
            labels = labels,
            onAddHost = viewModel::openAddHostForm,
            onEditHost = viewModel::openEditHostForm,
            onTestHost = { viewModel.triggerHostAction("test", it) },
            onSetActive = { viewModel.triggerHostAction("setactive", it) },
            onDeleteHost = { viewModel.triggerHostAction("delete", it) },
            onClose = viewModel::closeHostsManager
        )
        return
    }

    // Sub-layar 3: Editor WiFi / WLAN
    if (wlanForm.isOpen) {
        FullScreenWlanEditForm(
            form = wlanForm,
            labels = labels,
            onUpdate = viewModel::updateWlanEditor,
            onSubmit = viewModel::submitWlanEdit,
            onClose = viewModel::closeWlanEditor
        )
        return
    }

    // Sub-layar 4: Editor WAN PPPoE / IP
    if (wanForm.isOpen) {
        FullScreenWanEditForm(
            form = wanForm,
            labels = labels,
            onUpdate = viewModel::updateWanEditor,
            onSubmit = viewModel::submitWanEdit,
            onClose = viewModel::closeWanEditor
        )
        return
    }

    // Sub-layar 5: Detail & Kontrol Perangkat TR-069
    if (detailState != null) {
        FullScreenDeviceDetail(
            state = detailState!!,
            labels = labels,
            onSummon = { id -> viewModel.triggerDeviceAction(id, "summon") },
            onReboot = { id -> viewModel.triggerDeviceAction(id, "reboot") },
            onDelete = { id -> viewModel.triggerDeviceAction(id, "delete_device") },
            onEditWlan = { id, w -> viewModel.openWlanEditor(id, w) },
            onEditWan = { id, wan, idx -> viewModel.openWanEditor(id, wan, idx) },
            onClose = viewModel::closeDeviceDetail
        )
        return
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
                            text = "${strings.menuGenieAcs} (${stats.total})",
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
                    IconButton(onClick = { viewModel.triggerDeviceAction("", "clean_junk") }) {
                        Icon(Icons.Outlined.CleaningServices, contentDescription = labels.cleanJunkBots)
                    }
                    IconButton(onClick = viewModel::openHostsManager) {
                        Icon(Icons.Outlined.Dns, contentDescription = labels.manageHosts)
                    }
                    IconButton(onClick = { viewModel.loadGenieAcsDevices(isPullRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { AppFloatingSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Pemilih Server GenieACS (Multi-Host)
            if (hosts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hosts, key = { it.id }) { h ->
                        FilterChip(
                            selected = selectedHostId == h.id,
                            onClick = { viewModel.onSelectGenieHost(h.id) },
                            label = { Text(h.name) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onGenieSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onGenieSearchChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf(
                        "all" to "${strings.all} (${stats.total})",
                        "online" to "${strings.online} (${stats.online})",
                        "offline" to "${strings.offline} (${stats.offline})",
                        "bad_rx" to "${labels.badRxFilter} (${stats.badRxCount})"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { viewModel.onGenieStatusFilterChanged(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = devicesState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ErrorStateView(
                            message = state.message,
                            onRetry = { viewModel.loadGenieAcsDevices() },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Button(onClick = viewModel::openHostsManager) {
                            Text(labels.manageHosts)
                        }
                    }
                }
                is UiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadGenieAcsDevices(isPullRefresh = true) },
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
                                items(state.data, key = { it.id }) { dev ->
                                    GenieDeviceCard(
                                        dev = dev,
                                        onClick = { viewModel.openDeviceDetail(dev.id) },
                                        onSummon = { viewModel.triggerDeviceAction(dev.id, "summon") }
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
private fun GenieDeviceCard(
    dev: GenieAcsDeviceItem,
    onClick: () -> Unit,
    onSummon: () -> Unit
) {
    val strings = LocalAppStrings.current
    val titleLabel = dev.fullname.ifBlank {
        dev.pppoeUsername.takeIf { it != "N/A" } ?: dev.serialNumber
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
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
                        text = titleLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${dev.manufacturer} ${dev.model} • SN: ${dev.serialNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(
                    text = if (dev.online) strings.online else strings.offline,
                    tone = if (dev.online) BadgeTone.SUCCESS else BadgeTone.DANGER
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tampilkan indikator optik HANYA pada perangkat ONT PON (isOptical == true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dev.isOptical) {
                    val rxTone = when (dev.rxPowerStatus) {
                        "critical", "los" -> BadgeTone.DANGER
                        "warning" -> BadgeTone.WARNING
                        else -> BadgeTone.SUCCESS
                    }
                    StatusBadge(text = "RX: ${dev.rxPower}", tone = rxTone)
                    if (dev.temperature != "N/A") {
                        StatusBadge(text = dev.temperature, tone = BadgeTone.NEUTRAL)
                    }
                }
                StatusBadge(text = "Hosts: ${dev.connectedDevices}", tone = BadgeTone.NEUTRAL)
                if (dev.uptime != "-") {
                    StatusBadge(text = "Up: ${dev.uptime}", tone = BadgeTone.NEUTRAL)
                }
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
                    text = "IP: ${dev.wanIp.takeIf { it != "N/A" } ?: dev.tr069Ip}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onSummon) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Summon")
                }
            }
        }
    }
}

@Composable
private fun FullScreenDeviceDetail(
    state: UiState<GenieAcsDeviceDetail>,
    labels: NetworkGenieAcsLabels,
    onSummon: (String) -> Unit,
    onReboot: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditWlan: (String, com.phpnuxbill.mobile.feature.network.model.GenieAcsWlanItem) -> Unit,
    onEditWan: (String, com.phpnuxbill.mobile.feature.network.model.GenieAcsWanItem?, Int) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showRebootDialog by remember { mutableStateOf(false) }

    val detail = (state as? UiState.Success)?.data
    if (showRebootDialog && detail != null) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text(strings.rebootRouter) },
            text = { Text(labels.rebootModemConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebootDialog = false
                        onReboot(detail.device.id)
                    }
                ) {
                    Text(strings.confirm, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    AppScreenScaffold(
        title = labels.deviceDetailTitle,
        subtitle = detail?.device?.serialNumber,
        onBack = onClose,
        bottomBar = {
            if (detail != null) {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSummon(detail.device.id) },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.summonModem, maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { showRebootDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reboot")
                        }
                        IconButton(onClick = { onDelete(detail.device.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (state) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView(modifier = Modifier.padding(innerPadding))
            is UiState.Error -> ErrorStateView(message = state.message, modifier = Modifier.padding(innerPadding))
            is UiState.Success -> {
                val dev = state.data.device
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Ringkasan Informasi Perangkat
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${dev.manufacturer} ${dev.model}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusBadge(
                                        text = if (dev.online) strings.online else strings.offline,
                                        tone = if (dev.online) BadgeTone.SUCCESS else BadgeTone.DANGER
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("SN: ${dev.serialNumber} • FW: ${dev.softwareVersion}", style = MaterialTheme.typography.bodyMedium)
                                Text("PPPoE: ${dev.pppoeUsername} • WAN IP: ${dev.wanIp}", style = MaterialTheme.typography.bodyMedium)
                                Text("Uptime: ${dev.uptime} • Last Inform: ${dev.lastInform}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (dev.isOptical) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusBadge(text = "RX: ${dev.rxPower}", tone = BadgeTone.SUCCESS)
                                        StatusBadge(text = "TX: ${dev.txPower}", tone = BadgeTone.NEUTRAL)
                                        StatusBadge(text = "Temp: ${dev.temperature}", tone = BadgeTone.NEUTRAL)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Konfigurasi WiFi / WLAN
                    item {
                        Text(labels.wlanConfigsTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    items(state.data.wlanConfigs, key = { "wlan_${it.source}_${it.index}" }) { w ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                    Text("WLAN #${w.index} (${w.band}): ${w.ssid}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Pass: ${w.password.ifBlank { "-" }} • Ch: ${w.channel} • Clients: ${w.connectedDevices}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onEditWlan(dev.id, w) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                                }
                            }
                        }
                    }

                    // 3. Koneksi WAN
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(labels.wanConnectionsTitle, style = MaterialTheme.typography.titleMedium)
                            val nextIdx = state.data.availWanIndex
                            if (nextIdx != null) {
                                TextButton(onClick = { onEditWan(dev.id, null, nextIdx) }) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(labels.addWanBtn)
                                }
                            }
                        }
                    }
                    items(state.data.wanConnections, key = { "wan_${it.index}" }) { wan ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                    Text("WAN #${wan.index} (${wan.type.uppercase()}) • VLAN: ${wan.vlan.ifBlank { "-" }}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "User: ${wan.username.ifBlank { "-" }} • IP: ${wan.externalIp} (${wan.connectionStatus})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onEditWan(dev.id, wan, wan.index) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                                }
                            }
                        }
                    }

                    // 4. Perangkat Terhubung (Connected Hosts)
                    item {
                        Text(
                            text = "${labels.connectedClientsTitle} (${state.data.connectedHosts.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(state.data.connectedHosts) { host ->
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
                                    Text(host.hostname, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${host.ipAddress} • ${host.macAddress} (${host.interfaceType})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (host.signal.isNotBlank()) {
                                    StatusBadge(text = "${host.signal} dBm", tone = BadgeTone.NEUTRAL)
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
private fun FullScreenHostsManager(
    hosts: List<GenieAcsHost>,
    labels: NetworkGenieAcsLabels,
    onAddHost: () -> Unit,
    onEditHost: (GenieAcsHost) -> Unit,
    onTestHost: (String) -> Unit,
    onSetActive: (String) -> Unit,
    onDeleteHost: (String) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppScreenScaffold(
        title = labels.manageHosts,
        onBack = onClose,
        actions = {
            IconButton(onClick = onAddHost) {
                Icon(Icons.Outlined.Add, contentDescription = labels.addHost)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hosts, key = { it.id }) { h ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(h.fullUrl, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (h.active) {
                                StatusBadge(text = strings.active, tone = BadgeTone.SUCCESS)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onTestHost(h.id) }) { Text(labels.testHost) }
                            if (!h.active) {
                                FilledTonalButton(onClick = { onSetActive(h.id) }) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.active)
                                }
                            }
                            IconButton(onClick = { onEditHost(h) }) { Icon(Icons.Outlined.Edit, contentDescription = strings.edit) }
                            IconButton(onClick = { onDeleteHost(h.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenGenieHostForm(
    form: GenieAcsHostFormState,
    labels: NetworkGenieAcsLabels,
    onUpdate: ((GenieAcsHostFormState) -> GenieAcsHostFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppScreenScaffold(
        title = labels.addHost,
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
                        enabled = !form.isSaving && form.name.isNotBlank() && form.host.isNotBlank(),
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
                label = { Text(labels.hostNameLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("http", "https").forEachIndexed { idx, proto ->
                    SegmentedButton(
                        selected = form.protocol == proto,
                        onClick = {
                            onUpdate {
                                it.copy(
                                    protocol = proto,
                                    port = if (proto == "https" && it.port == "7557") "443" else it.port
                                )
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, 2),
                        label = { Text(proto.uppercase()) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.host,
                    onValueChange = { v -> onUpdate { it.copy(host = v) } },
                    label = { Text(labels.hostAddressLabel) },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.port,
                    onValueChange = { v -> onUpdate { it.copy(port = v.filter { c -> c.isDigit() }) } },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.7f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = form.username,
                onValueChange = { v -> onUpdate { it.copy(username = v) } },
                label = { Text("${strings.usernameLabel} (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = { v -> onUpdate { it.copy(password = v) } },
                label = { Text("${strings.passwordLabel} (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun FullScreenWlanEditForm(
    form: GenieAcsWlanEditState,
    labels: NetworkGenieAcsLabels,
    onUpdate: ((GenieAcsWlanEditState) -> GenieAcsWlanEditState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppScreenScaffold(
        title = "${labels.editWifiTitle} #${form.wlanIndex}",
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
                        enabled = !form.isSaving && form.ssid.isNotBlank(),
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
                value = form.ssid,
                onValueChange = { v -> onUpdate { it.copy(ssid = v) } },
                label = { Text(labels.ssidLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = { v -> onUpdate { it.copy(password = v) } },
                label = { Text(labels.wifiPassLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("WPA2PSK", "WPA/WPA2PSK", "None").forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = form.securityMode == mode,
                        onClick = { onUpdate { it.copy(securityMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(idx, 3),
                        label = { Text(mode, maxLines = 1) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SSID Enabled")
                Switch(checked = form.enable, onCheckedChange = { c -> onUpdate { it.copy(enable = c) } })
            }
        }
    }
}

@Composable
private fun FullScreenWanEditForm(
    form: GenieAcsWanEditState,
    labels: NetworkGenieAcsLabels,
    onUpdate: ((GenieAcsWanEditState) -> GenieAcsWanEditState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppScreenScaffold(
        title = "${labels.editWanTitle} #${form.connectionIndex}",
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
                        enabled = !form.isSaving,
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("ppp" to "PPPoE", "ip" to "DHCP / IP").forEachIndexed { idx, (key, label) ->
                    SegmentedButton(
                        selected = form.connectionType == key,
                        onClick = { onUpdate { it.copy(connectionType = key) } },
                        shape = SegmentedButtonDefaults.itemShape(idx, 2),
                        label = { Text(label) }
                    )
                }
            }
            if (form.connectionType == "ppp") {
                OutlinedTextField(
                    value = form.username,
                    onValueChange = { v -> onUpdate { it.copy(username = v) } },
                    label = { Text("PPPoE Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.password,
                    onValueChange = { v -> onUpdate { it.copy(password = v) } },
                    label = { Text("PPPoE Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = form.vlan,
                onValueChange = { v -> onUpdate { it.copy(vlan = v.filter { c -> c.isDigit() }) } },
                label = { Text(labels.vlanIdLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NAT Enabled")
                Switch(checked = form.nat, onCheckedChange = { c -> onUpdate { it.copy(nat = c) } })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Connection Enabled")
                Switch(checked = form.enable, onCheckedChange = { c -> onUpdate { it.copy(enable = c) } })
            }
        }
    }
}
