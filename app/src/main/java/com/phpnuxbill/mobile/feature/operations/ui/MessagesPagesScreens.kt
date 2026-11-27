package com.phpnuxbill.mobile.feature.operations.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.operations.model.MessageLogItem
import com.phpnuxbill.mobile.feature.operations.model.MessageLogsData
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesAndPagesScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val form by viewModel.messageForm.collectAsStateWithLifecycle()
    val messageLogsState by viewModel.messageLogsState.collectAsStateWithLifecycle()
    val logTypeFilter by viewModel.messageLogTypeFilter.collectAsStateWithLifecycle()
    val logStatusFilter by viewModel.messageLogStatusFilter.collectAsStateWithLifecycle()
    val logSearch by viewModel.messageLogSearch.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pagesState by viewModel.staticPagesState.collectAsStateWithLifecycle()
    val editingPage by viewModel.editingPage.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { OperationsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> viewModel.ensureMessageLogsLoaded()
            2 -> viewModel.ensureStaticPagesLoaded()
        }
    }

    BackHandler(enabled = editingPage != null) {
        viewModel.closePageEditor()
    }

    // Sub-layar Editor Halaman Statis HTML (pages/*.html)
    if (editingPage != null) {
        val page = editingPage!!
        AppScreenScaffold(
            title = "${labels.editPageTitle}:${page.title}",
            subtitle = "pages/${page.name}.html",
            onBack = viewModel::closePageEditor,
            bottomBar = {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = viewModel::closePageEditor, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
                        Button(onClick = viewModel::saveEditingPage, modifier = Modifier.weight(1.5f)) { Text(strings.save) }
                    }
                }
            }
        ) { pad ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = page.content,
                    onValueChange = viewModel::updateEditingPageContent,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(strings.menuMessages, style = MaterialTheme.typography.titleLarge)
                        Text(session.effectiveCompanyName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (selectedTab == 1 || selectedTab == 2) {
                        IconButton(
                            onClick = {
                                if (selectedTab == 1) viewModel.loadMessageLogs(isPullRefresh = true)
                                else viewModel.loadStaticPages()
                            }
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(labels.tabBroadcastMsg, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(labels.tabMessageLogs, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(labels.tabStaticPages, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }

            when (selectedTab) {
                // =============================================================
                // TAB 0: KIRIM PESAN PERSONAL / BROADCAST
                // =============================================================
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("broadcast" to labels.sendBroadcast, "single" to labels.sendSingle).forEachIndexed { idx, (k, l) ->
                                SegmentedButton(
                                    selected = form.mode == k,
                                    onClick = { viewModel.updateMessageForm { it.copy(mode = k) } },
                                    shape = SegmentedButtonDefaults.itemShape(idx, 2),
                                    label = { Text(l) }
                                )
                            }
                        }

                        if (form.mode == "single") {
                            OutlinedTextField(
                                value = form.targetUsername,
                                onValueChange = { v -> viewModel.updateMessageForm { it.copy(targetUsername = v) } },
                                label = { Text(labels.targetUsernameHint) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val groups = listOf("all" to "All", "active" to "Active", "expired" to "Expired", "hotspot" to "Hotspot", "pppoe" to "PPPoE")
                                items(groups) { (g, l) ->
                                    FilterChip(
                                        selected = form.group == g,
                                        onClick = { viewModel.updateMessageForm { it.copy(group = g) } },
                                        label = { Text(l) }
                                    )
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val vias = listOf("wa" to "WhatsApp", "sms" to "SMS", "inbox" to "Inbox", "email" to "Email", "both" to "WA + Inbox", "all" to "All Channels")
                            items(vias) { (v, l) ->
                                FilterChip(
                                    selected = form.via == v,
                                    onClick = { viewModel.updateMessageForm { it.copy(via = v) } },
                                    label = { Text(l) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = form.subject,
                            onValueChange = { v -> viewModel.updateMessageForm { it.copy(subject = v) } },
                            label = { Text("Subject / Judul") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(labels.quickTagsLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val tags = listOf("[[name]]", "[[username]]", "[[balance]]", "[[company_name]]", "[[payment_link]]")
                            items(tags) { tag ->
                                AssistChip(
                                    onClick = { viewModel.updateMessageForm { it.copy(message = it.message + " $tag") } },
                                    label = { Text(tag) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = form.message,
                            onValueChange = { v -> viewModel.updateMessageForm { it.copy(message = v) } },
                            label = { Text(labels.messageBodyLabel) },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = viewModel::submitSendMessage,
                            enabled = !form.isSending && form.message.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (form.isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(labels.sendNow)
                        }
                    }
                }

                // =============================================================
                // TAB 1: RIWAYAT PENGIRIMAN PESAN (tbl_message_logs)
                // =============================================================
                1 -> {
                    MessageLogsTabContent(
                        logsState = messageLogsState,
                        typeFilter = logTypeFilter,
                        statusFilter = logStatusFilter,
                        searchQuery = logSearch,
                        isRefreshing = isRefreshing,
                        labels = labels,
                        onTypeFilterChanged = viewModel::onMessageLogTypeFilterChanged,
                        onStatusFilterChanged = viewModel::onMessageLogStatusFilterChanged,
                        onSearchChanged = viewModel::onMessageLogSearchChanged,
                        onRefresh = { viewModel.loadMessageLogs(isPullRefresh = true) },
                        onRetry = { viewModel.loadMessageLogs(isPullRefresh = false) }
                    )
                }

                // =============================================================
                // TAB 2: HALAMAN STATIS HTML (pages/*.html)
                // =============================================================
                else -> {
                    when (val st = pagesState) {
                        is UiState.Idle, is UiState.Loading -> LoadingStateView()
                        is UiState.Error -> ErrorStateView(message = st.message, onRetry = viewModel::loadStaticPages)
                        is UiState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(st.data, key = { it.name }) { page ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openPageEditor(page) },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(page.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("pages/${page.name}.html", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageLogsTabContent(
    logsState: UiState<MessageLogsData>,
    typeFilter: String,
    statusFilter: String,
    searchQuery: String,
    isRefreshing: Boolean,
    labels: OperationsLabels,
    onTypeFilterChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    val loadedData = (logsState as? UiState.Success)?.data

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Bar (Ringkasan + Pencarian + Channel + Status)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loadedData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MessageLogStatPill(
                        label = labels.msgLogsTotalLabel,
                        value = loadedData.total.toString(),
                        accent = BrandBlue600,
                        modifier = Modifier.weight(1f)
                    )
                    MessageLogStatPill(
                        label = labels.msgLogsSentLabel,
                        value = loadedData.sentCount.toString(),
                        accent = BrandTeal500,
                        modifier = Modifier.weight(1f)
                    )
                    MessageLogStatPill(
                        label = labels.msgLogsFailedLabel,
                        value = loadedData.failedCount.toString(),
                        accent = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text(labels.msgLogsSearchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = strings.cancel)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val channels = listOf(
                    "all" to strings.all,
                    "wa" to "WhatsApp",
                    "sms" to "SMS",
                    "email" to "Email",
                    "inbox" to "Inbox"
                )
                items(channels) { (key, title) ->
                    FilterChip(
                        selected = typeFilter.equals(key, ignoreCase = true),
                        onClick = { onTypeFilterChanged(key) },
                        label = { Text(title) }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val statuses = listOf(
                    "all" to strings.all,
                    "sent" to labels.msgLogsSentLabel,
                    "failed" to labels.msgLogsFailedLabel
                )
                items(statuses) { (key, title) ->
                    FilterChip(
                        selected = statusFilter.equals(key, ignoreCase = true),
                        onClick = { onStatusFilterChanged(key) },
                        label = { Text(title) }
                    )
                }
            }
        }

        when (logsState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(message = logsState.message, onRetry = onRetry)
            is UiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (logsState.data.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MarkEmailRead,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = labels.msgLogsEmpty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(logsState.data.items, key = { it.id }) { item ->
                                MessageLogCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageLogStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
private fun MessageLogCard(item: MessageLogItem) {
    var expanded by remember(item.id) { mutableStateOf(false) }
    val statusLower = item.status.trim().lowercase()
    val isFailed = statusLower.contains("fail") || statusLower.contains("error")
    val isSent = statusLower.contains("sent") || statusLower.contains("success")
    val badgeTone = when {
        isFailed -> BadgeTone.DANGER
        isSent -> BadgeTone.SUCCESS
        else -> BadgeTone.INFO
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = item.messageType.uppercase(),
                        tone = BadgeTone.INFO
                    )
                    StatusBadge(
                        text = item.status.ifBlank { "-" }.uppercase(),
                        tone = badgeTone
                    )
                }
                Text(
                    text = item.sentAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.recipient,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.messageContent.isNotBlank()) {
                Text(
                    text = item.messageContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.errorMessage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
