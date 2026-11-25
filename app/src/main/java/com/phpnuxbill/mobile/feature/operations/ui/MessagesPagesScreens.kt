package com.phpnuxbill.mobile.feature.operations.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppFloatingSnackbarHost
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesAndPagesScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val form by viewModel.messageForm.collectAsStateWithLifecycle()
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
    if (selectedTab == 1) viewModel.ensureStaticPagesLoaded()
}
BackHandler(enabled = editingPage != null) {
    viewModel.closePageEditor()
}

    // Sub-layar Editor Halaman Statis HTML (pages/*.html)
    if (editingPage != null) {
        val page = editingPage!!
        AppScreenScaffold(
            title = "${labels.editPageTitle}: ${page.title}",
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
                    if (selectedTab == 1) {
                        IconButton(onClick = viewModel::loadStaticPages) {
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(labels.tabBroadcastMsg) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(labels.tabStaticPages) })
            }

            if (selectedTab == 0) {
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
            } else {
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
