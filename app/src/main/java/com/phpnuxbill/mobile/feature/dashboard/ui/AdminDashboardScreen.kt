@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.auth.AdminRolePolicy
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.dashboard.model.ActiveServiceBreakdown
import com.phpnuxbill.mobile.feature.dashboard.model.AdminActivityLogItem
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDashboardLabels
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDestination
import com.phpnuxbill.mobile.feature.dashboard.model.DashboardData
import com.phpnuxbill.mobile.feature.dashboard.model.ExpiringUserItem
import com.phpnuxbill.mobile.feature.dashboard.model.PlanStockItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: DashboardViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit,
    onNavigate: (AdminDestination) -> Unit,
    onSwitchServerClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { AdminDashboardLabels(strings) }

    val loadedData = (uiState as? UiState.Success)?.data
    val effectiveLevel = remember(loadedData?.adminProfile?.userType, session.userType) {
        AdminLevel.fromUserType(loadedData?.adminProfile?.userType ?: session.userType)
    }

    var chartTab by remember { mutableIntStateOf(0) } // 0 = Sales, 1 = New Customers

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogo(
                            customLocalLogoPath = session.customLocalLogoPath,
                            serverLogoUrl = session.serverLogoUrl,
                            size = 34.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = loadedData?.companyName?.ifBlank { session.effectiveCompanyName }
                                    ?: session.effectiveCompanyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${loadedData?.adminProfile?.fullname?.ifBlank { session.fullname } ?: session.username} • ${effectiveLevel.displayLabel(isEn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard(isPullRefresh = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                    Box(modifier = Modifier.padding(end = 10.dp)) {
                        AdminAvatarCircle(
                            photoUrl = loadedData?.adminProfile?.photoUrl.orEmpty(),
                            fullname = loadedData?.adminProfile?.fullname?.ifBlank { session.username } ?: session.username,
                            size = 32.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView(modifier = Modifier.padding(innerPadding))
            is UiState.Error -> ErrorStateView(
                message = state.message,
                onRetry = { viewModel.loadDashboard() },
                modifier = Modifier.padding(innerPadding)
            )
            is UiState.Success -> {
                val data = state.data
                val flags = data.featureFlags

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.loadDashboard(isPullRefresh = true) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Peringatan Mode Maintenance Server (jika aktif)
                        if (flags.maintenanceMode) {
                            item(key = "maint_banner") {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = labels.maintenanceWarning,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Banner Ringkas Mode Level (khusus Report / Agent / Sales)
                        if (!effectiveLevel.isFullAdmin) {
                            item(key = "role_banner") {
                                val desc = when (effectiveLevel) {
                                    AdminLevel.REPORT -> labels.roleSummaryReport
                                    AdminLevel.AGENT -> labels.roleSummaryAgent
                                    AdminLevel.SALES -> labels.roleSummarySales
                                    else -> ""
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(text = effectiveLevel.displayLabel(isEn).uppercase(), tone = BadgeTone.NEUTRAL)
                                    }
                                }
                            }
                        }

                        // 3. Hero Card Pendapatan / Penjualan
                        item(key = "hero_revenue") {
                            AdminHeroRevenueCard(
                                data = data,
                                level = effectiveLevel,
                                labels = labels
                            )
                        }

                        // 4. Grid KPI Adaptif per Level Admin
                        item(key = "kpi_grid") {
                            AdminKpiGrid(
                                data = data,
                                level = effectiveLevel,
                                labels = labels,
                                onNavigate = onNavigate
                            )
                        }

                        // 5. Rincian Pelanggan Aktif (Hotspot / PPPoE / VPN) + Pintasan 1 Ketuk
                        item(key = "active_breakdown") {
                            ActiveCustomersBreakdownCard(
                                totalActive = data.cards.activeUsers,
                                breakdown = data.activeBreakdown,
                                canOpenActiveMenu = AdminRolePolicy.canAccessActiveCustomers(effectiveLevel),
                                labels = labels,
                                onOpenActive = { onNavigate(AdminDestination.SERVICES) }
                            )
                        }

                        // 6. Aksi Cepat Dinamis (hanya tampilkan menu yang diizinkan oleh AdminRolePolicy)
                        item(key = "quick_actions") {
                            AdminQuickActionsRow(
                                level = effectiveLevel,
                                flags = flags,
                                labels = labels,
                                isEn = isEn,
                                onNavigate = onNavigate
                            )
                        }

                        // 7. Grafik Performa 12 Bulan
                        if (data.monthlySales.isNotEmpty() || data.monthlyRegistered.isNotEmpty()) {
                            item(key = "monthly_chart") {
                                AdminMonthlyChartCard(
                                    data = data,
                                    selectedTab = chartTab,
                                    onTabChange = { chartTab = it },
                                    labels = labels
                                )
                            }
                        }

                        // 8. Stok Voucher & Paket Aktif (sembunyi jika voucher dimatikan atau Report)
                        if (data.stocks.isNotEmpty() && (!flags.disableVoucher || effectiveLevel.isFullAdmin)) {
                            item(key = "voucher_stocks") {
                                VoucherStockOverviewCard(
                                    stocks = data.stocks,
                                    labels = labels,
                                    canOpenVouchers = AdminRolePolicy.canAccessVouchers(effectiveLevel, flags),
                                    onOpenVouchers = { onNavigate(AdminDestination.VOUCHERS) }
                                )
                            }
                        }

                        // 9. Pelanggan Segera Jatuh Tempo
                        item(key = "expiring_users") {
                            ExpiringUsersCard(
                                users = data.expiringUsers,
                                labels = labels,
                                canOpenActive = AdminRolePolicy.canAccessActiveCustomers(effectiveLevel),
                                onOpenActive = { onNavigate(AdminDestination.SERVICES) }
                            )
                        }

                        // 10. Log Aktivitas Sistem Terbaru (khusus SuperAdmin & Admin)
                        if (effectiveLevel.isFullAdmin && data.recentLogs.isNotEmpty()) {
                            item(key = "recent_logs") {
                                RecentActivityLogCard(
                                    logs = data.recentLogs,
                                    labels = labels
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminHeroRevenueCard(
    data: DashboardData,
    level: AdminLevel,
    labels: AdminDashboardLabels
) {
    val isGlobal = AdminRolePolicy.canViewGlobalFinance(level)
    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(BrandNavy900, BrandBlue600, BrandTeal500)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGlobal) labels.incomeMonth else labels.mySalesMonth,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (data.periodLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = data.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = formatMoney(data.currencyCode, data.cards.incomeMonth),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isGlobal) labels.incomeToday else labels.mySalesToday,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Text(
                        text = formatMoney(data.currencyCode, data.cards.incomeToday),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5EEAD4)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${labels.activeCustomers} / ${labels.expiredLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Text(
                        text = data.cards.activeExpiredLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiGrid(
    data: DashboardData,
    level: AdminLevel,
    labels: AdminDashboardLabels,
    onNavigate: (AdminDestination) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiStatTile(
                icon = Icons.Outlined.WifiTethering,
                title = labels.activeCustomers,
                value = "${data.cards.activeUsers}",
                subtitle = "${labels.expiredLabel}: ${data.cards.expiredUsers}",
                accent = BrandTeal500,
                onClick = if (AdminRolePolicy.canAccessActiveCustomers(level)) {
                    { onNavigate(AdminDestination.SERVICES) }
                } else null,
                modifier = Modifier.weight(1f)
            )
            KpiStatTile(
                icon = Icons.Outlined.Group,
                title = labels.totalCustomers,
                value = "${data.cards.totalCustomers}",
                subtitle = null,
                accent = BrandBlue600,
                onClick = { onNavigate(AdminDestination.CUSTOMERS) },
                modifier = Modifier.weight(1f)
            )
        }

        if (AdminRolePolicy.canViewGlobalFinance(level)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiStatTile(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = labels.customerBalance,
                    value = formatMoney(data.currencyCode, data.cards.totalCustomerBalance),
                    subtitle = null,
                    accent = Color(0xFF0369A1),
                    onClick = { onNavigate(AdminDestination.CUSTOMERS) },
                    modifier = Modifier.weight(1f)
                )
                if (AdminRolePolicy.canAccessPaymentGatewayAudit(level)) {
                    KpiStatTile(
                        icon = Icons.Outlined.Payments,
                        title = labels.pendingGateway,
                        value = "${data.cards.unpaidBillsCount}",
                        subtitle = "Payment Gateway",
                        accent = if (data.cards.unpaidBillsCount > 0) Color(0xFFD97706) else BrandTeal500,
                        onClick = { onNavigate(AdminDestination.PAYMENT_GATEWAY) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    KpiStatTile(
                        icon = Icons.Outlined.Assessment,
                        title = labels.tabSales,
                        value = formatMoney(data.currencyCode, data.cards.incomeToday),
                        subtitle = labels.incomeToday,
                        accent = BrandTeal500,
                        onClick = { onNavigate(AdminDestination.REPORTS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatTile(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String?,
    accent: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ActiveCustomersBreakdownCard(
    totalActive: Int,
    breakdown: ActiveServiceBreakdown,
    canOpenActiveMenu: Boolean,
    labels: AdminDashboardLabels,
    onOpenActive: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = labels.activeBreakdownTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${labels.activeCustomers}: $totalActive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canOpenActiveMenu) {
                    TextButton(onClick = onOpenActive, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(labels.openActiveCustomers, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServiceCountChip(label = "Hotspot", count = breakdown.hotspot, color = BrandBlue600, modifier = Modifier.weight(1f))
                ServiceCountChip(label = "PPPoE", count = breakdown.pppoe, color = BrandTeal500, modifier = Modifier.weight(1f))
                ServiceCountChip(label = "VPN", count = breakdown.vpn, color = Color(0xFF4F46E5), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ServiceCountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminQuickActionsRow(
    level: AdminLevel,
    flags: com.phpnuxbill.mobile.feature.dashboard.model.AdminServerFlags,
    labels: AdminDashboardLabels,
    isEn: Boolean,
    onNavigate: (AdminDestination) -> Unit
) {
    val actions = remember(level, flags, isEn) {
        buildList {
            if (AdminRolePolicy.canAccessActiveCustomers(level)) {
                add(Triple(AdminDestination.SERVICES, if (isEn) "Active\nCustomers" else "Pelanggan\nAktif", Icons.Outlined.WifiTethering))
            }
            add(Triple(AdminDestination.CUSTOMERS, if (isEn) "Customers" else "Pelanggan", Icons.Outlined.Group))
            if (AdminRolePolicy.canAccessVouchers(level, flags)) {
                add(Triple(AdminDestination.VOUCHERS, "Voucher", Icons.Outlined.ConfirmationNumber))
            }
            if (AdminRolePolicy.canManagePlansAndNetwork(level)) {
                add(Triple(AdminDestination.MIKROTIK, "MikroTik", Icons.Outlined.Router))
                add(Triple(AdminDestination.GENIEACS, "GenieACS", Icons.Outlined.SettingsInputAntenna))
            }
            if (AdminRolePolicy.canAccessReports(level)) {
                add(Triple(AdminDestination.REPORTS, if (isEn) "Reports" else "Laporan", Icons.Outlined.Assessment))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = labels.quickActions,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(actions, key = { it.first.route }) { (dest, label, icon) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .width(96.dp)
                        .clickable { onNavigate(dest) }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMonthlyChartCard(
    data: DashboardData,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    labels: AdminDashboardLabels
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = labels.monthlyPerformance,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(labels.tabSales, labels.tabRegistered).forEachIndexed { idx, title ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { onTabChange(idx) },
                        shape = SegmentedButtonDefaults.itemShape(idx, 2),
                        icon = {},
                        label = { Text(title, maxLines = 1) }
                    )
                }
            }

            val points: List<Pair<String, Double>> = if (selectedTab == 0) {
                data.monthlySales.map { it.month to it.total }
            } else {
                data.monthlyRegistered.map { it.month to it.count.toDouble() }
            }
            val maxVal = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { (monthLabel, value) ->
                    val ratio = (value / maxVal).toFloat().coerceIn(0.04f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .height(110.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (value > 0.0) {
                                            if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = monthLabel.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoucherStockOverviewCard(
    stocks: List<PlanStockItem>,
    labels: AdminDashboardLabels,
    canOpenVouchers: Boolean,
    onOpenVouchers: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                Text(
                    text = labels.voucherStocksTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (canOpenVouchers) {
                    TextButton(onClick = onOpenVouchers, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Voucher")
                    }
                }
            }

            stocks.take(6).forEachIndexed { idx, s ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.namePlan, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${s.type} • ${labels.activeCustomers}: ${s.activeUsers}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(text = "${labels.availableLabel}: ${s.unused}", tone = BadgeTone.SUCCESS)
                        StatusBadge(text = "${labels.usedLabel}: ${s.used}", tone = BadgeTone.NEUTRAL)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiringUsersCard(
    users: List<ExpiringUserItem>,
    labels: AdminDashboardLabels,
    canOpenActive: Boolean,
    onOpenActive: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = labels.expiringTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (canOpenActive) {
                    TextButton(onClick = onOpenActive, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(labels.openActiveCustomers)
                    }
                }
            }

            if (users.isEmpty()) {
                Text(
                    text = labels.noExpiring,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                users.take(6).forEachIndexed { idx, u ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "@${u.username} • ${u.planName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${u.type} • ${u.routers}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = u.expiration,
                            tone = if (u.status.equals("on", true)) BadgeTone.WARNING else BadgeTone.DANGER
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivityLogCard(
    logs: List<AdminActivityLogItem>,
    labels: AdminDashboardLabels
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = labels.recentLogsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            logs.forEachIndexed { idx, lg ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = lg.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${lg.date} • ${lg.type}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatMoney(currencyCode: String, amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val df = DecimalFormat("#,##0", symbols)
    val prefix = currencyCode.ifBlank { "Rp." }.trim()
    return "$prefix ${df.format(amount)}"
}
