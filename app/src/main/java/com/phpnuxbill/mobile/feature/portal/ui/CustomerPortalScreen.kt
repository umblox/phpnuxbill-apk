@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.portal.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBillItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemInfo
import com.phpnuxbill.mobile.feature.portal.model.CustomerPackageItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import com.phpnuxbill.mobile.feature.portal.model.PortalPaymentGateway
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

@Composable
fun CustomerPortalScreen(
    viewModel: CustomerPortalViewModel,
    session: SessionState,
    onLogout: () -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val plansState by viewModel.plansState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val modemState by viewModel.modemState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val voucherCode by viewModel.voucherCodeInput.collectAsStateWithLifecycle()
    val checkoutPlan by viewModel.checkoutPlan.collectAsStateWithLifecycle()
    val selectedGatewayId by viewModel.selectedGatewayId.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val couponCode by viewModel.couponCodeInput.collectAsStateWithLifecycle()
    val wifiSsid by viewModel.wifiSsidInput.collectAsStateWithLifecycle()
    val wifiPass by viewModel.wifiPassInput.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var orderSubTab by remember { mutableIntStateOf(0) } // 0 = Paket Internet, 1 = Top-Up Saldo
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> viewModel.ensurePlansLoaded()
            2 -> viewModel.ensureTransactionsLoaded()
            3 -> if (modemState is UiState.Idle) viewModel.loadModem()
        }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    // BackHandler berjenjang: Checkout -> Tutup; Tab != Home -> Home; Home -> Double-back keluar
    BackHandler(enabled = true) {
        when {
            checkoutPlan != null -> viewModel.closeCheckout()
            selectedTab != 0 -> selectedTab = 0
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedAt <= 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedAt = now
                    val msg = if (isEn) "Press back again to exit" else "Tekan kembali sekali lagi untuk keluar"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val activeDash = (dashboardState as? UiState.Success)?.data
    val plansData = (plansState as? UiState.Success)?.data
    val mergedGateways = activeDash?.paymentGateways.orEmpty().ifEmpty { plansData?.paymentGateways.orEmpty() }

    if (checkoutPlan != null && activeDash != null) {
        FullScreenCustomerCheckout(
            plan = checkoutPlan!!,
            dashboard = activeDash,
            gateways = mergedGateways,
            selectedGatewayId = selectedGatewayId,
            selectedChannelId = selectedChannelId,
            couponCode = couponCode,
            isSubmitting = isSubmitting,
            onSelectGateway = viewModel::selectCheckoutGateway,
            onSelectChannel = viewModel::selectCheckoutChannel,
            onCouponChanged = viewModel::onCouponCodeChanged,
            onConfirm = {
                viewModel.confirmCheckoutOrder(onOrderCreated = { selectedTab = 0 })
            },
            onClose = viewModel::closeCheckout
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogo(
                            customLocalLogoPath = session.customLocalLogoPath,
                            serverLogoUrl = session.serverLogoUrl,
                            size = 36.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeDash?.companyName?.ifBlank { session.effectiveCompanyName }
                                    ?: session.effectiveCompanyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${session.username}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshActiveTab(selectedTab) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = strings.logoutButton,
                            tint = MaterialTheme.colorScheme.error
                        )
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
                if (isEn) "Home" else "Beranda",
                if (isEn) "Order" else "Belanja",
                if (isEn) "History" else "Riwayat",
                if (isEn) "Modem" else "Modem"
            )
            // PrimaryTabRow memenuhi 100% lebar layar tanpa celah kosong di sisi kanan
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            when (selectedTab) {
                // =============================================================
                // TAB 0: BERANDA PELANGGAN (ESTETIK & INFORMATIF)
                // =============================================================
                0 -> when (val st = dashboardState) {
                    is UiState.Idle, is UiState.Loading -> LoadingStateView()
                    is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadDashboard() })
                    is UiState.Success -> {
                        val data = st.data
                        val primaryPkg = data.activePackages.firstOrNull { it.status.equals("on", true) }
                            ?: data.activePackages.firstOrNull()

                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshActiveTab(0) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Hero Wallet & Profil Card
                                item {
                                    CustomerHeroWalletCard(
                                        data = data,
                                        isEn = isEn,
                                        onToggleRenewal = viewModel::toggleAutoRenewal,
                                        onQuickTopUp = {
                                            orderSubTab = 1
                                            selectedTab = 1
                                        },
                                        onQuickBuyPlan = {
                                            orderSubTab = 0
                                            selectedTab = 1
                                        }
                                    )
                                }

                                // 2. Quick Insights 2 Kolom (Sisa Masa Aktif + Status Modem)
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val daysLeft = primaryPkg?.let { calcDaysRemaining(it.expiration) }
                                        val serviceSubtitle = when {
                                            primaryPkg == null -> if (isEn) "No Active Plan" else "Belum Ada Paket"
                                            !primaryPkg.status.equals("on", true) -> if (isEn) "Expired / Off" else "Nonaktif"
                                            daysLeft == null -> primaryPkg.expiration
                                            daysLeft < 0 -> if (isEn) "Expired" else "Kedaluwarsa"
                                            daysLeft == 0 -> if (isEn) "Expires Today" else "Berakhir Hari Ini"
                                            else -> if (isEn) "$daysLeft Days Left" else "Sisa $daysLeft Hari"
                                        }
                                        val isServiceOn = primaryPkg?.status.equals("on", true) == true

                                        QuickStatCard(
                                            modifier = Modifier.weight(1f),
                                            icon = Icons.Outlined.Wifi,
                                            title = if (isEn) "Internet" else "Layanan",
                                            value = primaryPkg?.namebp ?: "-",
                                            subtitle = serviceSubtitle,
                                            badgeText = if (isServiceOn) "ON" else "OFF",
                                            badgeTone = if (isServiceOn) BadgeTone.SUCCESS else BadgeTone.DANGER,
                                            onClick = {
                                                orderSubTab = 0
                                                selectedTab = 1
                                            }
                                        )

                                        val modemLoaded = (modemState as? UiState.Success)?.data
                                        val modemLoading = modemState is UiState.Loading
                                        QuickStatCard(
                                            modifier = Modifier.weight(1f),
                                            icon = Icons.Outlined.Router,
                                            title = if (isEn) "WiFi Modem" else "Modem WiFi",
                                            value = when {
                                                modemLoading -> if (isEn) "Checking..." else "Memeriksa..."
                                                modemLoaded == null -> if (isEn) "Not Linked" else "Belum Terhubung"
                                                else -> modemLoaded.currentSsid.ifBlank { modemLoaded.model }
                                            },
                                            subtitle = when {
                                                modemLoading -> "TR-069"
                                                modemLoaded == null -> if (isEn) "Tap to check" else "Ketuk untuk cek"
                                                else -> "${maxOf(modemLoaded.connectedDevices, modemLoaded.connectedHostsCount)} ${if (isEn) "Devices" else "Perangkat"}"
                                            },
                                            badgeText = when {
                                                modemLoading -> "..."
                                                modemLoaded?.online == true -> "ON"
                                                else -> "OFF"
                                            },
                                            badgeTone = when {
                                                modemLoading -> BadgeTone.NEUTRAL
                                                modemLoaded?.online == true -> BadgeTone.SUCCESS
                                                else -> BadgeTone.WARNING
                                            },
                                            onClick = { selectedTab = 3 }
                                        )
                                    }
                                }

                                // 3. Tagihan Tertunda (Unpaid Bill)
                                if (data.unpaidBill != null) {
                                    item {
                                        UnpaidBillDetailCard(
                                            bill = data.unpaidBill,
                                            onCheckStatus = { viewModel.checkBillStatus(data.unpaidBill.id) },
                                            onCancel = { viewModel.cancelBill(data.unpaidBill.id) }
                                        )
                                    }
                                }

                                // 4. Daftar Paket Aktif Lengkap
                                item {
                                    Text(
                                        text = if (isEn) "Active Subscription" else "Paket Layanan Aktif",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (data.activePackages.isEmpty()) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isEn) "You don't have an active internet package yet." else "Anda belum memiliki paket internet aktif.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Button(
                                                    onClick = {
                                                        orderSubTab = 0
                                                        selectedTab = 1
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(if (isEn) "Browse Plans" else "Pilih Paket Sekarang")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(data.activePackages, key = { it.id }) { pkg ->
                                        ActivePackageRichCard(
                                            pkg = pkg,
                                            isEn = isEn,
                                            onRenewClick = {
                                                orderSubTab = 0
                                                selectedTab = 1
                                            }
                                        )
                                    }
                                }

                                // 5. Kartu Aktivasi Voucher Mandiri
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ConfirmationNumber,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isEn) "Redeem Voucher" else "Aktivasi Kode Voucher",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = voucherCode,
                                                    onValueChange = viewModel::onVoucherCodeChanged,
                                                    placeholder = { Text(if (isEn) "Enter voucher code" else "Masukkan kode voucher") },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(
                                                    onClick = viewModel::redeemVoucher,
                                                    enabled = voucherCode.isNotBlank(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.height(54.dp)
                                                ) {
                                                    Text("Redeem")
                                                }
                                            }
                                        }
                                    }
                                }

                                // 6. Kartu Layanan & Bantuan CS
                                if (data.companyPhone.isNotBlank() || data.companyAddress.isNotBlank()) {
                                    item {
                                        CustomerSupportCard(
                                            companyName = data.companyName,
                                            companyPhone = data.companyPhone,
                                            companyAddress = data.companyAddress,
                                            isEn = isEn
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // TAB 1: BELANJA (DIPISAH TEGAS: PAKET INTERNET VS TOP-UP SALDO)
                // =============================================================
                1 -> {
                    val currency = activeDash?.currency ?: session.currencyCode
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                val subTabs = listOf(
                                    if (isEn) "Internet Plans" else "Paket Internet",
                                    if (isEn) "Top-Up Balance" else "Isi Saldo"
                                )
                                subTabs.forEachIndexed { idx, label ->
                                    SegmentedButton(
                                        selected = orderSubTab == idx,
                                        onClick = { orderSubTab = idx },
                                        shape = SegmentedButtonDefaults.itemShape(idx, subTabs.size),
                                        label = { Text(label, maxLines = 1) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        when (val st = plansState) {
                            is UiState.Idle, is UiState.Loading -> LoadingStateView()
                            is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadPlans() })
                            is UiState.Success -> {
                                PullToRefreshBox(
                                    isRefreshing = isRefreshing,
                                    onRefresh = { viewModel.refreshActiveTab(1) },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (orderSubTab == 0) {
                                        val inetPlans = st.data.internetPlans
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (inetPlans.isEmpty()) {
                                                item {
                                                    Text(
                                                        text = strings.noData,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                items(inetPlans, key = { "inet_${it.id}" }) { plan ->
                                                    InternetPlanCard(
                                                        plan = plan,
                                                        currency = currency,
                                                        isEn = isEn,
                                                        onSelect = { viewModel.openCheckoutForPlan(plan) }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val balPlans = st.data.balancePlans
                                        val balRows = remember(balPlans) { balPlans.chunked(2) }
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            item {
                                                Text(
                                                    text = if (isEn) "Select top-up amount" else "Pilih nominal isi saldo akun",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (balRows.isEmpty()) {
                                                item {
                                                    Text(
                                                        text = strings.noData,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                items(balRows) { pair ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        pair.forEach { bp ->
                                                            TopUpGridCard(
                                                                plan = bp,
                                                                currency = currency,
                                                                isEn = isEn,
                                                                onSelect = { viewModel.openCheckoutForPlan(bp) },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                        if (pair.size == 1) {
                                                            Spacer(modifier = Modifier.weight(1f))
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

                // =============================================================
                // TAB 2: RIWAYAT TRANSAKSI
                // =============================================================
                2 -> {
                    val currency = activeDash?.currency ?: session.currencyCode
                    when (val st = transactionsState) {
                        is UiState.Idle, is UiState.Loading -> LoadingStateView()
                        is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadTransactions() })
                        is UiState.Success -> {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refreshActiveTab(2) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (st.data.isEmpty()) {
                                        item {
                                            Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        items(st.data, key = { it.id }) { tr ->
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ReceiptLong,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(tr.planName, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = "${tr.invoice} • ${tr.rechargedOn} ${tr.rechargedTime}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = tr.method,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = formatMoney(currency, tr.price),
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

                // =============================================================
                // TAB 3: KONTROL MODEM TR-069 PELANGGAN
                // =============================================================
                3 -> {
                    when (val st = modemState) {
                        is UiState.Idle, is UiState.Loading -> LoadingStateView()
                        is UiState.Error -> ErrorStateView(message = st.message, onRetry = viewModel::loadModem)
                        is UiState.Success -> {
                            val modem = st.data
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (modem == null) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (isEn) "Modem Not Detected" else "Modem Belum Terdeteksi",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isEn) {
                                                    "Your TR-069 router/ONT is not yet linked to this account or GenieACS is currently offline."
                                                } else {
                                                    "Perangkat modem/router TR-069 belum terhubung ke akun Anda atau server manajemen modem sedang offline."
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedButton(onClick = viewModel::loadModem) {
                                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(strings.refresh)
                                            }
                                        }
                                    }
                                } else {
                                    CustomerModemDetailCard(
                                        modem = modem,
                                        wifiSsid = wifiSsid,
                                        wifiPass = wifiPass,
                                        onWifiChanged = viewModel::onWifiInputsChanged,
                                        onSaveWifi = viewModel::saveWifi,
                                        onSummon = viewModel::summonModem,
                                        onReboot = viewModel::rebootModem
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
private fun CustomerHeroWalletCard(
    data: CustomerPortalDashboard,
    isEn: Boolean,
    onToggleRenewal: () -> Unit,
    onQuickTopUp: () -> Unit,
    onQuickBuyPlan: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )
    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) "Hello, ${data.fullname}" else "Halo, ${data.fullname}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEn) "Account Balance" else "Saldo Akun Anda",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = data.serviceType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatMoney(data.currency, data.balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (data.enableBalance) {
                    Button(
                        onClick = onQuickTopUp,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Top-Up" else "Isi Saldo", maxLines = 1)
                    }
                }
                FilledTonalButton(
                    onClick = onQuickBuyPlan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEn) "Buy Plan" else "Beli Paket", maxLines = 1)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) "Auto-Renewal" else "Perpanjangan Otomatis",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (data.autoRenewal) {
                            if (isEn) "Active using account balance" else "Aktif memakai saldo otomatis"
                        } else {
                            if (isEn) "Manual renewal" else "Perpanjangan manual"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                Switch(checked = data.autoRenewal, onCheckedChange = { onToggleRenewal() })
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    badgeText: String,
    badgeTone: BadgeTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clickable(onClick = onClick)
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                StatusBadge(text = badgeText, tone = badgeTone)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActivePackageRichCard(
    pkg: CustomerPackageItem,
    isEn: Boolean,
    onRenewClick: () -> Unit
) {
    val isOn = pkg.status.equals("on", true)
    val daysLeft = remember(pkg.expiration) { calcDaysRemaining(pkg.expiration) }
    val progress = remember(daysLeft, isOn) {
        when {
            !isOn || daysLeft == null || daysLeft <= 0 -> 0.05f
            daysLeft >= 30 -> 1f
            else -> (daysLeft / 30f).coerceIn(0.08f, 1f)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.namebp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = listOf(pkg.type, pkg.routers).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = if (isOn) "ACTIVE" else "EXPIRED",
                    tone = if (isOn) BadgeTone.SUCCESS else BadgeTone.DANGER
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isOn && (daysLeft == null || daysLeft > 3)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEn) "Valid Until" else "Berlaku Sampai",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${pkg.expiration} ${pkg.time}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FilledTonalButton(
                    onClick = onRenewClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(if (isEn) "Renew" else "Perpanjang")
                }
            }
        }
    }
}

@Composable
private fun CustomerSupportCard(
    companyName: String,
    companyPhone: String,
    companyAddress: String,
    isEn: Boolean
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEn) "Need Assistance?" else "Butuh Bantuan?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = companyAddress.ifBlank { companyName },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (companyPhone.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        val digits = companyPhone.filter { it.isDigit() }
                        val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum")))
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (isEn) "Contact CS" else "Hubungi CS")
                }
            }
        }
    }
}

@Composable
private fun InternetPlanCard(
    plan: CustomerBuyPlanItem,
    currency: String,
    isEn: Boolean,
    onSelect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plan.namePlan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.type.isNotBlank()) {
                        StatusBadge(text = plan.type.uppercase(), tone = BadgeTone.NEUTRAL)
                    }
                }
                Text(
                    text = if (isEn) "Validity: ${plan.validity}" else "Masa Aktif: ${plan.validity}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(currency, plan.price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEn) "Select" else "Pilih")
            }
        }
    }
}

@Composable
private fun TopUpGridCard(
    plan: CustomerBuyPlanItem,
    currency: String,
    isEn: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clickable(onClick = onSelect)
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
                Icon(
                    imageVector = Icons.Outlined.AddCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isEn) "TOP-UP" else "SALDO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = formatMoney(currency, plan.price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = plan.namePlan,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerModemDetailCard(
    modem: CustomerModemInfo,
    wifiSsid: String,
    wifiPass: String,
    onWifiChanged: (String, String) -> Unit,
    onSaveWifi: () -> Unit,
    onSummon: () -> Unit,
    onReboot: () -> Unit
) {
    val strings = LocalAppStrings.current
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(modem.model, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusBadge(
                    text = if (modem.online) strings.online else strings.offline,
                    tone = if (modem.online) BadgeTone.SUCCESS else BadgeTone.DANGER
                )
            }
            Text("SN: ${modem.serialNumber} • Uptime: ${modem.uptime}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (modem.isOptical) {
                    StatusBadge(text = "RX: ${modem.rxPower}", tone = BadgeTone.SUCCESS)
                }
                StatusBadge(
                    text = "Devices: ${maxOf(modem.connectedDevices, modem.connectedHostsCount)}",
                    tone = BadgeTone.NEUTRAL
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onSummon, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh")
                }
                OutlinedButton(onClick = onReboot, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reboot")
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.wifiSettings, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = wifiSsid,
                onValueChange = { onWifiChanged(it, wifiPass) },
                label = { Text("SSID WiFi") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = wifiPass,
                onValueChange = { onWifiChanged(wifiSsid, it) },
                label = { Text("Password WiFi (Min 8 chars)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onSaveWifi,
                enabled = wifiSsid.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.save)
            }
        }
    }
}

@Composable
private fun UnpaidBillDetailCard(
    bill: CustomerBillItem,
    onCheckStatus: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${bill.planName} (${bill.paymentChannel})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusBadge(text = bill.statusLabel, tone = BadgeTone.WARNING)
            }
            Text(
                text = formatMoney(bill.currency, bill.price),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Exp: ${bill.expiredDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (bill.uiMode == "qris" && bill.qrisImage.isNotBlank()) {
                AsyncImage(
                    model = bill.qrisImage,
                    contentDescription = "QRIS Payment",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            if (bill.payCode.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bill.payCode,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { clipboard.setText(AnnotatedString(bill.payCode)) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = strings.copy)
                        }
                    }
                }
            }

            bill.instructions.forEach { group ->
                Text(group.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                group.steps.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bill.checkoutUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bill.checkoutUrl))) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Bayar") }
                }
                FilledTonalButton(onClick = onCheckStatus, modifier = Modifier.weight(1f)) { Text("Cek Status") }
                OutlinedButton(onClick = onCancel) { Text(strings.cancel) }
            }
        }
    }
}

@Composable
private fun FullScreenCustomerCheckout(
    plan: CustomerBuyPlanItem,
    dashboard: CustomerPortalDashboard,
    gateways: List<PortalPaymentGateway>,
    selectedGatewayId: String,
    selectedChannelId: String,
    couponCode: String,
    isSubmitting: Boolean,
    onSelectGateway: (String, String) -> Unit,
    onSelectChannel: (String) -> Unit,
    onCouponChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isBalancePlan = plan.type.equals("Balance", true)
    val selectedPg = gateways.find { it.id == selectedGatewayId }

    AppScreenScaffold(
        title = plan.namePlan,
        subtitle = formatMoney(dashboard.currency, plan.price),
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
                        onClick = onConfirm,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(strings.confirm)
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
            Text(strings.paymentMethod, style = MaterialTheme.typography.titleMedium)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isBalancePlan && dashboard.enableBalance) {
                    item {
                        FilterChip(
                            selected = selectedGatewayId == "balance",
                            onClick = { onSelectGateway("balance", "") },
                            label = { Text("Saldo (${formatMoney(dashboard.currency, dashboard.balance)})") }
                        )
                    }
                }
                items(gateways, key = { it.id }) { pg ->
                    FilterChip(
                        selected = selectedGatewayId == pg.id,
                        onClick = { onSelectGateway(pg.id, pg.channels.firstOrNull()?.id.orEmpty()) },
                        label = { Text(pg.name) }
                    )
                }
            }

            if (selectedPg != null && selectedPg.channels.isNotEmpty()) {
                Text("Channel ${selectedPg.name}", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedPg.channels, key = { it.id }) { ch ->
                        FilterChip(
                            selected = selectedChannelId == ch.id,
                            onClick = { onSelectChannel(ch.id) },
                            label = { Text(ch.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = couponCode,
                onValueChange = onCouponChanged,
                label = { Text("Kode Kupon Promo (Opsional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun calcDaysRemaining(expirationDate: String): Int? {
    val clean = expirationDate.trim().take(10)
    if (clean.length < 10) return null
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val expDate = sdf.parse(clean) ?: return null
        val today = sdf.parse(sdf.format(System.currentTimeMillis())) ?: return null
        val diffMs = expDate.time - today.time
        ceil(diffMs / 86_400_000.0).toInt()
    }.getOrNull()
}
