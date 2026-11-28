package com.phpnuxbill.mobile.feature.operations.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.BuildConfig
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.auth.AdminRolePolicy
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
import com.phpnuxbill.mobile.feature.operations.model.AvailableWidgetOption
import com.phpnuxbill.mobile.feature.operations.model.ChangePasswordState
import com.phpnuxbill.mobile.feature.operations.model.DashboardWidgetFormState
import com.phpnuxbill.mobile.feature.operations.model.DashboardWidgetItem
import com.phpnuxbill.mobile.feature.operations.model.DashboardWidgetsData
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels
import com.phpnuxbill.mobile.feature.operations.model.StaffFormState
import com.phpnuxbill.mobile.feature.operations.model.StaffUserItem
import com.phpnuxbill.mobile.feature.operations.model.WebServerSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSystemScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit,
    onWebSettingsChanged: () -> Unit = {}
) {
    val webSettings by viewModel.webSettings.collectAsStateWithLifecycle()
    val notifTemplates by viewModel.notifTemplates.collectAsStateWithLifecycle()
    val widgetsState by viewModel.widgetsState.collectAsStateWithLifecycle()
    val selectedWidgetRole by viewModel.selectedWidgetRole.collectAsStateWithLifecycle()
    val widgetStructureInput by viewModel.widgetStructureInput.collectAsStateWithLifecycle()
    val widgetForm by viewModel.widgetForm.collectAsStateWithLifecycle()
    val staffListState by viewModel.staffListState.collectAsStateWithLifecycle()
    val staffForm by viewModel.staffForm.collectAsStateWithLifecycle()
    val changePassState by viewModel.changePasswordState.collectAsStateWithLifecycle()
    val dbStatusState by viewModel.dbStatusState.collectAsStateWithLifecycle()
    val systemLogs by viewModel.systemLogs.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { OperationsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }

    val effectiveLevel = remember(session.userType) {
        AdminLevel.fromUserType(session.userType)
    }
    val canManageSystem = AdminRolePolicy.canManageSystemSettings(effectiveLevel)
    val allowedStaffRoles = remember(effectiveLevel) {
        when (effectiveLevel) {
            AdminLevel.SUPER_ADMIN -> listOf("SuperAdmin", "Admin", "Report", "Agent", "Sales")
            AdminLevel.ADMIN -> listOf("Report", "Agent", "Sales")
            AdminLevel.AGENT -> listOf("Sales")
            else -> emptyList()
        }
    }

    // Tab: 0=General, 1=Templates, 2=Widgets, 3=Staff & Key, 4=DB & Logs
    var selectedTab by remember(canManageSystem) { mutableIntStateOf(if (canManageSystem) 0 else 3) }
    var wasSavingWebSettings by remember { mutableStateOf(false) }
    var deleteWidgetTarget by remember { mutableStateOf<DashboardWidgetItem?>(null) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onCustomLogoSelectedFromGallery(uri)
    }

    LaunchedEffect(webSettings.isSaving) {
        if (wasSavingWebSettings && !webSettings.isSaving) {
            onWebSettingsChanged()
        }
        wasSavingWebSettings = webSettings.isSaving
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    LaunchedEffect(selectedTab, canManageSystem) {
        if (!canManageSystem) {
            viewModel.loadStaffList()
        } else {
            when (selectedTab) {
                0 -> viewModel.ensureWebSettingsLoaded()
                1 -> if (notifTemplates.isEmpty()) viewModel.loadNotificationTemplates()
                2 -> viewModel.ensureWidgetsLoaded()
                3 -> viewModel.loadStaffList()
                4 -> viewModel.loadDbStatusAndLogs()
            }
        }
    }

    // Pastikan role default form staf selalu sesuai RBAC level admin yang login (khususnya Agent -> Sales)
    LaunchedEffect(staffForm.isOpen, staffForm.isEdit, allowedStaffRoles) {
        if (staffForm.isOpen && allowedStaffRoles.isNotEmpty()) {
            val isAllowed = allowedStaffRoles.any { it.equals(staffForm.userType, ignoreCase = true) }
            if (!isAllowed) {
                val fallbackRole = if (allowedStaffRoles.contains("Sales")) "Sales" else allowedStaffRoles.first()
                viewModel.updateStaffForm { it.copy(userType = fallbackRole) }
            }
        }
    }

    BackHandler(enabled = staffForm.isOpen || widgetForm.isOpen) {
        when {
            widgetForm.isOpen -> viewModel.closeWidgetForm()
            staffForm.isOpen -> viewModel.closeStaffForm()
        }
    }

    // Sub-layar penuh: Tambah / Edit Widget Dasbor
    if (widgetForm.isOpen) {
        val loadedWidgets = (widgetsState as? UiState.Success)?.data
        FullScreenWidgetForm(
            form = widgetForm,
            availableWidgets = loadedWidgets?.availableWidgets.orEmpty(),
            availableRoles = loadedWidgets?.users ?: listOf("Admin", "Agent", "Sales", "Customer"),
            labels = labels,
            onUpdate = viewModel::updateWidgetForm,
            onSubmit = viewModel::submitWidgetForm,
            onClose = viewModel::closeWidgetForm
        )
        return
    }

    // Sub-layar penuh: Tambah / Edit Akun Staf
    if (staffForm.isOpen) {
        FullScreenStaffForm(
            form = staffForm,
            allowedRoles = allowedStaffRoles,
            labels = labels,
            onUpdate = viewModel::updateStaffForm,
            onSubmit = viewModel::submitStaffForm,
            onClose = viewModel::closeStaffForm
        )
        return
    }

    if (deleteWidgetTarget != null) {
        val target = deleteWidgetTarget!!
        AlertDialog(
            onDismissRequest = { deleteWidgetTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text("${strings.delete} (${target.title})", fontWeight = FontWeight.Bold) },
            text = { Text(labels.deleteWidgetConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteWidgetTarget = null
                        viewModel.deleteWidget(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteWidgetTarget = null }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    val screenTitle = if (!canManageSystem && effectiveLevel == AdminLevel.AGENT) {
        if (isEn) "Sales Staff" else "Staf Sales"
    } else {
        strings.menuSettings
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(screenTitle, style = MaterialTheme.typography.titleLarge)
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
                            if (!canManageSystem) {
                                viewModel.loadStaffList()
                            } else {
                                when (selectedTab) {
                                    0 -> viewModel.loadWebSettings()
                                    1 -> viewModel.loadNotificationTemplates()
                                    2 -> viewModel.loadWidgets(isPullRefresh = true)
                                    3 -> viewModel.loadStaffList()
                                    4 -> viewModel.loadDbStatusAndLogs()
                                }
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
            if (canManageSystem) {
                val tabs = listOf(
                    labels.tabGeneralSettings,
                    labels.tabNotifTemplates,
                    labels.tabWidgets,
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
            }

            val effectiveTab = if (canManageSystem) selectedTab else 3

            when (effectiveTab) {
                // =============================================================
                // TAB 0: MAINTENANCE MODE, PREFERENSI APLIKASI, WEB CONFIG & TES GATEWAY
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
                        // Kartu 1: Kontrol Khusus Maintenance Mode (selaras ui/ui/admin/settings/maintenance-mode.tpl)
                        MaintenanceModeControlCard(
                            webSettings = webSettings,
                            labels = labels,
                            isEn = isEn,
                            onUpdate = viewModel::updateWebSettings,
                            onSave = viewModel::submitWebSettings
                        )

                        // Kartu 2: Preferensi Aplikasi (Logo Galeri, Bahasa ID/EN, Tema)
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

                        // Kartu 3: Konfigurasi Server Web Billing (tbl_appconfig)
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

                        // Kartu 4: Tes Gateway Notifikasi (WA / SMS / Email / Telegram)
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
                            items(notifTemplates, key = { it.key }) { item ->
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
                                                text = item.key,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(
                                                onClick = { viewModel.resetNotifTemplateToDefault(item.key) }
                                            ) {
                                                Text("Default")
                                            }
                                        }
                                        OutlinedTextField(
                                            value = item.value,
                                            onValueChange = { newText ->
                                                viewModel.updateNotifTemplate(item.key, newText)
                                            },
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
                // TAB 2: MANAJEMEN WIDGETS DASBOR (tbl_widgets — SELARAS widgets.php)
                // =============================================================
                2 -> {
                    WidgetsTabContent(
                        widgetsState = widgetsState,
                        selectedRole = selectedWidgetRole,
                        structureInput = widgetStructureInput,
                        labels = labels,
                        onSelectRole = viewModel::onSelectWidgetRole,
                        onStructureInputChanged = viewModel::onWidgetStructureInputChanged,
                        onSaveStructure = viewModel::saveWidgetStructure,
                        onAddWidget = { viewModel.openWidgetForm(null) },
                        onEditWidget = { w -> viewModel.openWidgetForm(w) },
                        onToggleWidget = viewModel::toggleWidgetEnabled,
                        onMoveWidget = viewModel::moveWidgetOrder,
                        onDeleteWidget = { w -> deleteWidgetTarget = w },
                        onRetry = { viewModel.loadWidgets(isPullRefresh = false) }
                    )
                }

                // =============================================================
                // TAB 3: MANAJEMEN STAF & GANTI KATA SANDI ADMIN
                // =============================================================
                3 -> {
                    StaffAndSecurityTabContent(
                        staffListState = staffListState,
                        changePassState = changePassState,
                        session = session,
                        canDeleteStaff = canManageSystem,
                        sectionTitle = if (!canManageSystem) {
                            if (isEn) "Sales Team" else "Tim Sales"
                        } else {
                            "Admin / Staff"
                        },
                        labels = labels,
                        onAddStaff = { viewModel.openStaffForm(null) },
                        onEditStaff = { staff -> viewModel.openStaffForm(staff) },
                        onDeleteStaff = viewModel::deleteStaff,
                        onReloadStaff = viewModel::loadStaffList,
                        onUpdateChangePassword = viewModel::updateChangePassword,
                        onSubmitChangePassword = viewModel::submitChangePassword
                    )
                }

                // =============================================================
                // TAB 4: MONITOR STATUS DATABASE (READ-ONLY) & SYSTEM LOGS
                // =============================================================
                4 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = labels.dbBackupWebNoteTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = labels.dbBackupWebNoteBody,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        when (val st = dbStatusState) {
                            is UiState.Idle, is UiState.Loading -> item { LoadingStateView() }
                            is UiState.Success -> {
                                item {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Database: ${st.data.totalTables} Tables • ${st.data.totalRows} Rows • ${st.data.totalSize}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            st.data.tables.forEachIndexed { idx, t ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = t.name,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        if (t.engine.isNotBlank()) {
                                                            Text(
                                                                text = t.engine,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "${t.rows} rows (${t.sizeLabel})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (idx < st.data.tables.lastIndex) {
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> item { ErrorStateView(message = st.message, onRetry = viewModel::loadDbStatusAndLogs) }
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
private fun WidgetsTabContent(
    widgetsState: UiState<DashboardWidgetsData>,
    selectedRole: String,
    structureInput: String,
    labels: OperationsLabels,
    onSelectRole: (String) -> Unit,
    onStructureInputChanged: (String) -> Unit,
    onSaveStructure: () -> Unit,
    onAddWidget: () -> Unit,
    onEditWidget: (DashboardWidgetItem) -> Unit,
    onToggleWidget: (DashboardWidgetItem) -> Unit,
    onMoveWidget: (DashboardWidgetItem, Boolean) -> Unit,
    onDeleteWidget: (DashboardWidgetItem) -> Unit,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    val loaded = (widgetsState as? UiState.Success)?.data
    val roles = loaded?.users ?: listOf("Admin", "Agent", "Sales", "Customer")

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(roles) { role ->
                FilterChip(
                    selected = selectedRole.equals(role, ignoreCase = true),
                    onClick = { onSelectRole(role) },
                    label = { Text(role) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when (widgetsState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(message = widgetsState.message, onRetry = onRetry)
            is UiState.Success -> {
                val data = widgetsState.data
                val sortedWidgets = remember(data.widgets) {
                    data.widgets.sortedWith(
                        compareBy<DashboardWidgetItem> { it.position }
                            .thenBy { it.orders }
                            .thenBy { it.id }
                    )
                }
                val byPosition = remember(sortedWidgets) {
                    sortedWidgets.groupBy { it.position }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SettingsSectionBox(title = "${labels.widgetStructureTitle} ($selectedRole)") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = structureInput,
                                    onValueChange = onStructureInputChanged,
                                    label = { Text("dashboard_$selectedRole") },
                                    placeholder = { Text(labels.widgetStructureHint) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                FilledTonalButton(
                                    onClick = onSaveStructure,
                                    enabled = structureInput.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                                ) {
                                    Text(labels.saveStructureBtn, maxLines = 1)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${labels.widgetsSectionTitle} (${sortedWidgets.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = onAddWidget, shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(labels.addWidgetBtn)
                            }
                        }
                    }

                    if (sortedWidgets.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = labels.widgetEmptyMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(sortedWidgets, key = { it.id }) { item ->
                            val siblings = byPosition[item.position].orEmpty()
                            val idxInPos = siblings.indexOfFirst { it.id == item.id }
                            val canMoveUp = idxInPos > 0
                            val canMoveDown = idxInPos in 0 until siblings.lastIndex

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                StatusBadge(
                                                    text = "Pos #${item.position}",
                                                    tone = BadgeTone.NEUTRAL
                                                )
                                                StatusBadge(
                                                    text = "#${item.orders}",
                                                    tone = BadgeTone.NEUTRAL
                                                )
                                                StatusBadge(
                                                    text = if (item.enabled) strings.active else "OFF",
                                                    tone = if (item.enabled) BadgeTone.SUCCESS else BadgeTone.WARNING
                                                )
                                            }
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.widget,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Switch(
                                            checked = item.enabled,
                                            onCheckedChange = { onToggleWidget(item) }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            IconButton(
                                                onClick = { onMoveWidget(item, true) },
                                                enabled = canMoveUp,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowUpward,
                                                    contentDescription = "Up",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onMoveWidget(item, false) },
                                                enabled = canMoveDown,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowDownward,
                                                    contentDescription = "Down",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            IconButton(
                                                onClick = { onEditWidget(item) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = strings.edit,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteWidget(item) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
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
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenWidgetForm(
    form: DashboardWidgetFormState,
    availableWidgets: List<AvailableWidgetOption>,
    availableRoles: List<String>,
    labels: OperationsLabels,
    onUpdate: ((DashboardWidgetFormState) -> DashboardWidgetFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var moduleExpanded by remember { mutableStateOf(false) }

    AppScreenScaffold(
        title = if (form.isEdit) labels.editWidgetTitle else labels.addWidgetBtn,
        subtitle = form.user,
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
                        enabled = !form.isSaving && form.title.isNotBlank() && form.widget.isNotBlank(),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        if (form.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(strings.save)
                        }
                    }
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(availableRoles) { role ->
                    FilterChip(
                        selected = form.user.equals(role, ignoreCase = true),
                        onClick = { onUpdate { it.copy(user = role) } },
                        label = { Text(role) }
                    )
                }
            }

            OutlinedTextField(
                value = form.title,
                onValueChange = { v -> onUpdate { it.copy(title = v) } },
                label = { Text(labels.widgetTitleLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = moduleExpanded,
                onExpandedChange = { moduleExpanded = !moduleExpanded }
            ) {
                val matchedLabel = availableWidgets.find { it.key == form.widget }?.let { "${it.label} (${it.key})" }
                    ?: form.widget
                OutlinedTextField(
                    value = matchedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(labels.widgetModuleLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = moduleExpanded,
                    onDismissRequest = { moduleExpanded = false }
                ) {
                    availableWidgets.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(opt.label, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = opt.key,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onUpdate { cur ->
                                    cur.copy(
                                        widget = opt.key,
                                        title = if (cur.title.isBlank()) opt.label else cur.title
                                    )
                                }
                                moduleExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = form.position,
                    onValueChange = { v -> onUpdate { it.copy(position = v.filter { c -> c.isDigit() }) } },
                    label = { Text(labels.widgetPositionLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = form.orders,
                    onValueChange = { v -> onUpdate { it.copy(orders = v.filter { c -> c.isDigit() }) } },
                    label = { Text(labels.widgetOrderLabel) },
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
                Text(labels.widgetEnabledLabel, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = form.enabled,
                    onCheckedChange = { c -> onUpdate { it.copy(enabled = c) } }
                )
            }

            OutlinedTextField(
                value = form.content,
                onValueChange = { v -> onUpdate { it.copy(content = v) } },
                label = { Text(labels.widgetContentLabel) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceModeControlCard(
    webSettings: WebServerSettings,
    labels: OperationsLabels,
    isEn: Boolean,
    onUpdate: ((WebServerSettings) -> WebServerSettings) -> Unit,
    onSave: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val initialMillis = remember(webSettings.maintenanceDate) {
            parseUtcDateMillis(webSettings.maintenanceDate)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onUpdate { it.copy(maintenanceDate = formatUtcDateString(millis)) }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val borderColor = if (webSettings.maintenanceMode) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (webSettings.maintenanceMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEn) "Maintenance Mode" else "Mode Pemeliharaan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (webSettings.maintenanceMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(
                    text = if (webSettings.maintenanceMode) {
                        if (isEn) "ACTIVE" else "AKTIF"
                    } else {
                        if (isEn) "OFF" else "NONAKTIF"
                    },
                    tone = if (webSettings.maintenanceMode) BadgeTone.DANGER else BadgeTone.SUCCESS
                )
            }

            Text(
                text = if (isEn) {
                    "Blocks customer login and portal access while Admin & Staff remain able to operate normally."
                } else {
                    "Menutup akses login & portal pelanggan sementara, sedangkan Admin & Staf tetap dapat beroperasi normal."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(labels.maintenanceModeLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = webSettings.maintenanceMode,
                    onCheckedChange = { c -> onUpdate { it.copy(maintenanceMode = c) } }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(labels.forceLogoutLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isEn) {
                            "Signs out currently active customer sessions"
                        } else {
                            "Keluarkan otomatis sesi pelanggan yang sedang login"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = webSettings.maintenanceForceLogout,
                    onCheckedChange = { c -> onUpdate { it.copy(maintenanceForceLogout = c) } }
                )
            }

            // Pemilih Tanggal Estimasi Selesai (DatePickerDialog Material 3, format YYYY-MM-DD)
            OutlinedTextField(
                value = webSettings.maintenanceDate,
                onValueChange = { v -> onUpdate { it.copy(maintenanceDate = v) } },
                label = { Text(labels.maintenanceDateLabel) },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                readOnly = true,
                leadingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (webSettings.maintenanceDate.isNotBlank()) {
                            IconButton(onClick = { onUpdate { it.copy(maintenanceDate = "") } }) {
                                Icon(Icons.Outlined.Clear, contentDescription = strings.delete)
                            }
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(if (isEn) "Pick" else "Pilih")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Button(
                onClick = onSave,
                enabled = !webSettings.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (webSettings.maintenanceMode) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (webSettings.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (webSettings.maintenanceMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isEn) "Save Maintenance Status" else "Simpan Status Maintenance",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffAndSecurityTabContent(
    staffListState: UiState<List<StaffUserItem>>,
    changePassState: ChangePasswordState,
    session: SessionState,
    canDeleteStaff: Boolean,
    sectionTitle: String,
    labels: OperationsLabels,
    onAddStaff: () -> Unit,
    onEditStaff: (StaffUserItem) -> Unit,
    onDeleteStaff: (Int) -> Unit,
    onReloadStaff: () -> Unit,
    onUpdateChangePassword: ((ChangePasswordState) -> ChangePasswordState) -> Unit,
    onSubmitChangePassword: () -> Unit
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
                Text(sectionTitle, style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = onAddStaff,
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
            is UiState.Error -> item { ErrorStateView(message = st.message, onRetry = onReloadStaff) }
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
                            IconButton(onClick = { onEditStaff(staff) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                            }
                            if (canDeleteStaff && !staff.username.equals(session.username, true)) {
                                IconButton(onClick = { onDeleteStaff(staff.id) }) {
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
                    onValueChange = { v -> onUpdateChangePassword { it.copy(currentPassword = v) } },
                    label = { Text(labels.currentPassLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = changePassState.newPassword,
                    onValueChange = { v -> onUpdateChangePassword { it.copy(newPassword = v) } },
                    label = { Text(labels.newPassLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = changePassState.confirmPassword,
                    onValueChange = { v -> onUpdateChangePassword { it.copy(confirmPassword = v) } },
                    label = { Text(labels.confirmPassLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onSubmitChangePassword,
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
    allowedRoles: List<String>,
    labels: OperationsLabels,
    onUpdate: ((StaffFormState) -> StaffFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val roleChoices = allowedRoles.ifEmpty { listOf("Sales") }

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
                roleChoices.forEach { role ->
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

private fun parseUtcDateMillis(rawDate: String): Long? {
    val clean = rawDate.trim()
    if (clean.isBlank()) return null
    val dateOnly = clean.take(10)
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(dateOnly)?.time
    }.getOrNull()
}

private fun formatUtcDateString(utcMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(utcMillis))
}
