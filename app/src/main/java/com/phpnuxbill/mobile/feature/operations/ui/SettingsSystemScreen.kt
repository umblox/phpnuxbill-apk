package com.phpnuxbill.mobile.feature.operations.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.BuildConfig
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LanguageToggleChip
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels
import com.phpnuxbill.mobile.feature.operations.model.StaffFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSystemScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val webSettings by viewModel.webSettings.collectAsStateWithLifecycle()
    val notifTemplates by viewModel.notifTemplates.collectAsStateWithLifecycle()
    val staffListState by viewModel.staffListState.collectAsStateWithLifecycle()
    val staffForm by viewModel.staffForm.collectAsStateWithLifecycle()
    val changePassState by viewModel.changePasswordState.collectAsStateWithLifecycle()
    val dbStatusState by viewModel.dbStatusState.collectAsStateWithLifecycle()
    val systemLogs by viewModel.systemLogs.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { OperationsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onCustomLogoSelectedFromGallery(uri)
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> if (notifTemplates.isEmpty()) viewModel.loadNotificationTemplates()
            2 -> viewModel.loadStaffList()
            3 -> viewModel.loadDbStatusAndLogs()
        }
    }

    // Sub-layar penuh: Tambah / Edit Akun Staf
    if (staffForm.isOpen) {
        FullScreenStaffForm(
            form = staffForm,
            labels = labels,
            onUpdate = viewModel::updateStaffForm,
            onSubmit = viewModel::submitStaffForm,
            onClose = viewModel::closeStaffForm
        )
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
                        Text(strings.menuSettings, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = session.effectiveCompanyName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            when (selectedTab) {
                                0 -> viewModel.loadWebSettings()
                                1 -> viewModel.loadNotificationTemplates()
                                2 -> viewModel.loadStaffList()
                                3 -> viewModel.loadDbStatusAndLogs()
                            }
                        }
                    ) {
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
            val tabs = listOf(
                labels.tabGeneralSettings,
                labels.tabNotifTemplates,
                labels.tabStaffSecurity,
                labels.tabDbLogs
            )
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                // =============================================================
                // TAB 0: PREFERENSI APLIKASI, WEB CONFIG & TES GATEWAY
                // =============================================================
                0 -> {
                    var testChannel by remember { mutableStateOf("wa") }
                    var testTarget by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Kartu 1: Preferensi Aplikasi (Logo Galeri, Bahasa ID/EN, Tema)
                        SettingsSectionBox(title = strings.appPreferences) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.languageSetting, style = MaterialTheme.typography.bodyLarge)
                                LanguageToggleChip(
                                    currentLanguageCode = session.languageCode,
                                    onLanguageSelected = viewModel::setAppLanguage
                                )
                            }

                            Text(strings.themeSetting, style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val themes = listOf(
                                    AppThemeMode.SYSTEM to strings.themeSystem,
                                    AppThemeMode.LIGHT to strings.themeLight,
                                    AppThemeMode.DARK to strings.themeDark
                                )
                                themes.forEachIndexed { idx, (mode, label) ->
                                    SegmentedButton(
                                        selected = session.themeMode == mode,
                                        onClick = { viewModel.setAppTheme(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                                        label = { Text(label, maxLines = 1) }
                                    )
                                }
                            }

                            Text(strings.customLogoSetting, style = MaterialTheme.typography.labelLarge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BrandLogo(
                                    customLocalLogoPath = session.customLocalLogoPath,
                                    serverLogoUrl = session.serverLogoUrl,
                                    size = 64.dp,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            logoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.changeLogoGallery, maxLines = 1)
                                    }
                                    if (session.customLocalLogoPath.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = viewModel::onResetCustomLogo,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.resetLogoDefault, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // Kartu 2: Konfigurasi Server Web Billing (tbl_appconfig)
                        SettingsSectionBox(title = labels.webBillingConfigTitle) {
                            OutlinedTextField(
                                value = webSettings.companyName,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(companyName = v) } },
                                label = { Text(labels.companyNameLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = webSettings.currencyCode,
                                    onValueChange = { v -> viewModel.updateWebSettings { it.copy(currencyCode = v) } },
                                    label = { Text("Currency") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = webSettings.resetDay,
                                    onValueChange = { v -> viewModel.updateWebSettings { it.copy(resetDay = v.filter { c -> c.isDigit() }) } },
                                    label = { Text(labels.resetDayLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            OutlinedTextField(
                                value = webSettings.phone,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(phone = v) } },
                                label = { Text(strings.phoneNumber) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = webSettings.address,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(address = v) } },
                                label = { Text(strings.address) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = webSettings.waUrl,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(waUrl = v) } },
                                label = { Text(labels.waGatewayUrlLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = webSettings.smsUrl,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(smsUrl = v) } },
                                label = { Text(labels.smsGatewayUrlLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = webSettings.smtpHost,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(smtpHost = v) } },
                                label = { Text(labels.smtpHostLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = webSettings.telegramBot,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(telegramBot = v) } },
                                label = { Text(labels.telegramBotLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = webSettings.telegramTargetId,
                                onValueChange = { v -> viewModel.updateWebSettings { it.copy(telegramTargetId = v) } },
                                label = { Text(labels.telegramChatIdLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(labels.maintenanceModeLabel, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = webSettings.maintenanceMode,
                                    onCheckedChange = { c -> viewModel.updateWebSettings { it.copy(maintenanceMode = c) } }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(labels.forceLogoutLabel, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = webSettings.maintenanceForceLogout,
                                    onCheckedChange = { c -> viewModel.updateWebSettings { it.copy(maintenanceForceLogout = c) } }
                                )
                            }
                            if (webSettings.maintenanceMode) {
                                OutlinedTextField(
                                    value = webSettings.maintenanceDate,
                                    onValueChange = { v -> viewModel.updateWebSettings { it.copy(maintenanceDate = v) } },
                                    label = { Text(labels.maintenanceDateLabel) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = viewModel::submitWebSettings,
                                enabled = !webSettings.isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (webSettings.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text(strings.save)
                                }
                            }
                        }

                        // Kartu 3: Tes Gateway Notifikasi (WA / SMS / Email / Telegram)
                        SettingsSectionBox(title = labels.testGatewayTitle) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("wa" to "WA", "sms" to "SMS", "email" to "Email", "tg" to "Telegram").forEach { (ch, l) ->
                                    FilterChip(
                                        selected = testChannel == ch,
                                        onClick = { testChannel = ch },
                                        label = { Text(l) }
                                    )
                                }
                            }
                            if (testChannel != "tg") {
                                OutlinedTextField(
                                    value = testTarget,
                                    onValueChange = { testTarget = it },
                                    label = { Text(labels.testTargetHint) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { viewModel.testNotifGateway(testChannel, testTarget) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(labels.sendTestBtn)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.runSystemTool("clear_cache") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(labels.clearCacheBtn, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.runSystemTool("run_cron") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(labels.runCronBtn, maxLines = 1)
                                }
                            }
                        }

                        Text(
                            text = "${strings.appVersionLabel}: v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // =============================================================
                // TAB 1: EDITOR TEMPLATE NOTIFIKASI (notifications.json)
                // =============================================================
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val entries = notifTemplates.entries.toList()
                            items(entries, key = { it.key }) { (key, value) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(onClick = { viewModel.resetNotifTemplateToDefault(key) }) {
                                                Text("Default")
                                            }
                                        }
                                        OutlinedTextField(
                                            value = value,
                                            onValueChange = { viewModel.updateNotifTemplate(key, it) },
                                            minLines = 3,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                            Button(
                                onClick = viewModel::saveNotificationTemplates,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(strings.save)
                            }
                        }
                    }
                }

                // =============================================================
                // TAB 2: MANAJEMEN STAF & GANTI KATA SANDI ADMIN
                // =============================================================
                2 -> {
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
                                Text("Admin / Staff", style = MaterialTheme.typography.titleMedium)
                                Button(
                                    onClick = { viewModel.openStaffForm(null) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(labels.addStaffBtn)
                                }
                            }
                        }

                        when (val st = staffListState) {
                            is UiState.Idle, is UiState.Loading -> item { LoadingStateView() }
                            is UiState.Error -> item { ErrorStateView(message = st.message, onRetry = viewModel::loadStaffList) }
                            is UiState.Success -> {
                                items(st.data, key = { it.id }) { staff ->
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${staff.fullname} (@${staff.username})", fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "${staff.userType} • ${staff.phone.ifBlank { "-" }}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            StatusBadge(
                                                text = staff.status,
                                                tone = if (staff.status.equals("Active", true)) BadgeTone.SUCCESS else BadgeTone.DANGER
                                            )
                                            IconButton(onClick = { viewModel.openStaffForm(staff) }) {
                                                Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                                            }
                                            if (!staff.username.equals(session.username, true)) {
                                                IconButton(onClick = { viewModel.deleteStaff(staff.id) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SettingsSectionBox(title = labels.changePasswordTitle) {
                                OutlinedTextField(
                                    value = changePassState.currentPassword,
                                    onValueChange = { v -> viewModel.updateChangePassword { it.copy(currentPassword = v) } },
                                    label = { Text(labels.currentPassLabel) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = changePassState.newPassword,
                                    onValueChange = { v -> viewModel.updateChangePassword { it.copy(newPassword = v) } },
                                    label = { Text(labels.newPassLabel) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = changePassState.confirmPassword,
                                    onValueChange = { v -> viewModel.updateChangePassword { it.copy(confirmPassword = v) } },
                                    label = { Text(labels.confirmPassLabel) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = viewModel::submitChangePassword,
                                    enabled = !changePassState.isSaving &&
                                        changePassState.currentPassword.isNotBlank() &&
                                        changePassState.newPassword.length >= 6,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.save)
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // TAB 3: STATUS DATABASE, BACKUP JSON & SYSTEM LOGS
                // =============================================================
                3 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = viewModel::triggerDbBackup,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(labels.backupDbNowBtn)
                            }
                        }

                        when (val st = dbStatusState) {
                            is UiState.Success -> {
                                item {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Database: ${st.data.totalTables} Tables • ${st.data.totalRows} Rows • ${st.data.totalSize}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            st.data.tables.take(12).forEach { t ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(t.name, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                                    Text("${t.rows} rows (${t.sizeLabel})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> item { ErrorStateView(message = st.message, onRetry = viewModel::loadDbStatusAndLogs) }
                            else -> {}
                        }

                        item {
                            Text(labels.systemLogsTitle, style = MaterialTheme.typography.titleMedium)
                        }

                        items(systemLogs, key = { it.id }) { log ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${log.date} • ${log.type} (${log.ip})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(log.description, style = MaterialTheme.typography.bodyMedium)
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
private fun SettingsSectionBox(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun FullScreenStaffForm(
    form: StaffFormState,
    labels: OperationsLabels,
    onUpdate: ((StaffFormState) -> StaffFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppScreenScaffold(
        title = if (form.isEdit) strings.edit else labels.addStaffBtn,
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
                        enabled = !form.isSaving && form.username.isNotBlank() && form.fullname.isNotBlank(),
                        modifier = Modifier.weight(1.5f)
                    ) { Text(strings.save) }
                }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
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
                value = form.username,
                onValueChange = { v -> onUpdate { it.copy(username = v) } },
                label = { Text(strings.usernameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.fullname,
                onValueChange = { v -> onUpdate { it.copy(fullname = v) } },
                label = { Text(strings.fullName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = { v -> onUpdate { it.copy(password = v) } },
                label = { Text("${strings.passwordLabel} (Min 6 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Admin", "Report", "Agent", "Sales").forEach { role ->
                    FilterChip(
                        selected = form.userType.equals(role, true),
                        onClick = { onUpdate { it.copy(userType = role) } },
                        label = { Text(role) }
                    )
                }
            }
            OutlinedTextField(
                value = form.phone,
                onValueChange = { v -> onUpdate { it.copy(phone = v) } },
                label = { Text(strings.phoneNumber) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.email,
                onValueChange = { v -> onUpdate { it.copy(email = v) } },
                label = { Text(strings.emailAddress) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
