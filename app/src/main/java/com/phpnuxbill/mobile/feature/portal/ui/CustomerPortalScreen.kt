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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemStatusResult
import com.phpnuxbill.mobile.feature.portal.model.CustomerPackageItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import com.phpnuxbill.mobile.feature.portal.model.ModemSupportStatus
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
    val inboxState by viewModel.inboxState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val voucherCode by viewModel.voucherCodeInput.collectAsStateWithLifecycle()
    val checkoutPlan by viewModel.checkoutPlan.collectAsStateWithLifecycle()
    val activePaymentBill by viewModel.activePaymentBill.collectAsStateWithLifecycle()
    val isCheckingPayment by viewModel.isCheckingPayment.collectAsStateWithLifecycle()
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
    var orderSubTab by remember { mutableIntStateOf(0) } // 0 = Paket Internet, 1 = Isi Saldo
    var showProfileScreen by remember { mutableStateOf(false) }
    var showInboxScreen by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showSendPlanDialog by remember { mutableStateOf(false) }
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    val activeDash = (dashboardState as? UiState.Success)?.data
    val plansData = (plansState as? UiState.Success)?.data
    val effectiveBrandName = activeDash?.companyName?.ifBlank { session.effectiveCompanyName }
        ?: session.effectiveCompanyName

    // Strategi GenieACS: Sembunyikan tab Modem untuk user Hotspot; tampilkan untuk PPPoE & Others
    val showModemTab = activeDash != null && !activeDash.serviceType.equals("Hotspot", ignoreCase = true)

    LaunchedEffect(showModemTab) {
        if (!showModemTab && selectedTab == 3) {
            selectedTab = 0
        }
    }

    LaunchedEffect(selectedTab, showModemTab) {
        when (selectedTab) {
            1 -> viewModel.ensurePlansLoaded()
            2 -> viewModel.ensureTransactionsLoaded()
            3 -> if (showModemTab) viewModel.ensureModemLoaded()
        }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    BackHandler(enabled = true) {
        when {
            activePaymentBill != null -> viewModel.closePaymentBill()
            checkoutPlan != null -> viewModel.closeCheckout()
            showProfileScreen -> showProfileScreen = false
            showInboxScreen -> showInboxScreen = false
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

    // 1. LAYAR PEMBAYARAN PENUH
    if (activePaymentBill != null) {
        CustomerPaymentScreen(
            bill = activePaymentBill!!,
            brandName = effectiveBrandName,
            isChecking = isCheckingPayment,
            onCheckStatus = { silent -> viewModel.checkBillStatus(activePaymentBill!!.id, silent = silent) },
            onCancelBill = { viewModel.cancelBill(activePaymentBill!!.id) },
            onBackToHome = {
                viewModel.closePaymentBill()
                selectedTab = 0
            }
        )
        return
    }

    // 2. LAYAR PROFIL AKUN LENGKAP
    if (showProfileScreen && activeDash != null) {
        CustomerProfileScreen(
            dashboard = activeDash,
            session = session,
            isSubmitting = isSubmitting,
            onSaveProfile = { full, addr, ph, em, b64 ->
                viewModel.saveProfile(full, addr, ph, em, b64) {}
            },
            onChangePassword = { oldP, newP, confP, onDone ->
                viewModel.changePassword(oldP, newP, confP, onDone)
            },
            onLanguageSelected = viewModel::setAppLanguage,
            onThemeSelected = viewModel::setAppThemeMode,
            onLogout = onLogout,
            onBack = { showProfileScreen = false }
        )
        return
    }

    // 3. LAYAR KOTAK MASUK (INBOX)
    if (showInboxScreen) {
        CustomerInboxScreen(
            inboxState = inboxState,
            onRefresh = viewModel::loadInbox,
            onMarkRead = viewModel::markInboxRead,
            onDelete = viewModel::deleteInbox,
            onBack = { showInboxScreen = false }
        )
        return
    }

    // 4. LAYAR CHECKOUT PINTAR
    val mergedGateways = remember(activeDash?.paymentGateways, plansData?.paymentGateways) {
        val raw = activeDash?.paymentGateways.orEmpty().ifEmpty { plansData?.paymentGateways.orEmpty() }
        raw.filter { gw -> !gw.requiresChannel || gw.channels.isNotEmpty() }
    }

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
                viewModel.confirmCheckoutOrder(onBalancePaidSuccess = { selectedTab = 0 })
            },
            onClose = viewModel::closeCheckout
        )
        return
    }

    // Dialog Transfer Saldo & Belikan Paket Teman
    if (showTransferDialog && activeDash != null) {
        TransferBalanceDialog(
            currency = activeDash.currency,
            currentBalance = activeDash.balance,
            minimumTransfer = activeDash.minimumTransfer,
            isSubmitting = isSubmitting,
            onDismiss = { showTransferDialog = false },
            onConfirm = { targetUser, amt ->
                viewModel.transferBalance(targetUser, amt) {
                    showTransferDialog = false
                }
            }
        )
    }

    if (showSendPlanDialog && activeDash != null) {
        SendPlanToFriendDialog(
            currency = activeDash.currency,
            currentBalance = activeDash.balance,
            availablePlans = plansData?.internetPlans.orEmpty(),
            isSubmitting = isSubmitting,
            onDismiss = { showSendPlanDialog = false },
            onConfirm = { friendUser, pId ->
                viewModel.sendPlanToFriend(friendUser, pId) {
                    showSendPlanDialog = false
                }
            }
        )
    }

    // 5. SHELL UTAMA PORTAL PELANGGAN
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
                                text = effectiveBrandName,
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
                    // Ikon Kotak Masuk + Badge Unread
                    IconButton(
                        onClick = {
                            viewModel.loadInbox()
                            showInboxScreen = true
                        }
                    ) {
                        val unread = activeDash?.inboxUnreadCount ?: 0
                        BadgedBox(
                            badge = {
                                if (unread > 0) {
                                    Badge { Text(if (unread > 9) "9+" else unread.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.MailOutline, contentDescription = if (isEn) "Inbox" else "Kotak Masuk")
                        }
                    }

                    // Avatar Foto Profil Asli Web Pelanggan -> Buka Akun Saya
                    Box(modifier = Modifier.padding(end = 12.dp, start = 4.dp)) {
                        CustomerAvatarCircle(
                            photoUrl = activeDash?.photoUrl.orEmpty(),
                            fullname = activeDash?.fullname?.ifBlank { session.username } ?: session.username,
                            size = 36.dp,
                            onClick = {
                                if (activeDash != null) showProfileScreen = true
                            }
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
            val tabs = buildList {
                add(if (isEn) "Home" else "Beranda")
                add(if (isEn) "Plans" else "Paket")
                add(if (isEn) "History" else "Riwayat")
                if (showModemTab) add("Modem")
            }

            PrimaryTabRow(
                selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
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
                // TAB 0: BERANDA PELANGGAN (BERSIH TANPA DUPLIKASI + FITUR WEB)
                // =============================================================
                0 -> when (val st = dashboardState) {
                    is UiState.Idle, is UiState.Loading -> LoadingStateView()
                    is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadDashboard() })
                    is UiState.Success -> {
                        val data = st.data

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
                                // 1. Tagihan Tertunda
                                if (data.unpaidBill != null) {
                                    item(key = "unpaid_banner") {
                                        UnpaidBillSummaryBanner(
                                            bill = data.unpaidBill,
                                            isEn = isEn,
                                            onOpenPayment = { viewModel.openPaymentBill(data.unpaidBill) }
                                        )
                                    }
                                }

                                // 2. Pengumuman Resmi dari Server (pages/Announcement.html)
                                if (data.announcement.isNotBlank()) {
                                    item(key = "announcement") {
                                        AnnouncementCard(text = data.announcement, isEn = isEn)
                                    }
                                }

                                // 3. Hero Wallet Card (+ Aksi Cepat: Isi Saldo, Beli Paket, Transfer, Belikan Teman)
                                item(key = "hero_wallet") {
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
                                        },
                                        onOpenTransfer = { showTransferDialog = true },
                                        onOpenSendPlan = {
                                            viewModel.ensurePlansLoaded()
                                            showSendPlanDialog = true
                                        }
                                    )
                                }

                                // 4. Paket Layanan Aktif (Satu Bagian Tunggal Tanpa Duplikasi!)
                                item(key = "active_pkg_header") {
                                    Text(
                                        text = if (isEn) "Active Subscription" else "Paket Layanan Aktif",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (data.activePackages.isEmpty()) {
                                    item(key = "empty_pkg") {
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
                                        val canExtend = !pkg.status.equals("on", true) &&
                                            data.extendExpired &&
                                            !data.extendedThisMonth
                                        ActivePackageRichCard(
                                            pkg = pkg,
                                            isEn = isEn,
                                            isSubmitting = isSubmitting,
                                            canExtend = canExtend,
                                            extendDays = data.extendDays,
                                            onRenewClick = {
                                                viewModel.openRenewForPackage(pkg) {
                                                    orderSubTab = 0
                                                    selectedTab = 1
                                                }
                                            },
                                            onSyncClick = viewModel::syncPlanToRouter,
                                            onExtendClick = { viewModel.extendExpiredPlan(pkg.id) }
                                        )
                                    }
                                }

                                // 5. Aktivasi Voucher (Sembunyi bila disable_voucher = yes)
                                if (!data.disableVoucher) {
                                    item(key = "redeem_voucher") {
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
                                }

                                // 6. Bantuan CS
                                if (data.companyPhone.isNotBlank() || data.companyAddress.isNotBlank()) {
                                    item(key = "support_cs") {
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
                // TAB 1: PAKET / PLANS (CAROUSEL FOKUS-REDUP + LENCANA TIER)
                // =============================================================
                1 -> CustomerPlansTabContent(
                    plansState = plansState,
                    currency = activeDash?.currency ?: session.currencyCode,
                    enableBalance = activeDash?.enableBalance ?: true,
                    orderSubTab = orderSubTab,
                    onSubTabChange = { orderSubTab = it },
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshActiveTab(1) },
                    onSelectPlan = viewModel::openCheckoutForPlan
                )

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
                                                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
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
                // TAB 3: MODEM TR-069 (KHUSUS PPPOE & OTHERS — INFORMATIF)
                // =============================================================
                3 -> {
                    when (val st = modemState) {
                        is UiState.Idle, is UiState.Loading -> LoadingStateView()
                        is UiState.Error -> ErrorStateView(message = st.message, onRetry = viewModel::loadModem)
                        is UiState.Success -> {
                            CustomerModemTabContent(
                                result = st.data,
                                companyPhone = activeDash?.companyPhone.orEmpty(),
                                wifiSsid = wifiSsid,
                                wifiPass = wifiPass,
                                isEn = isEn,
                                onWifiChanged = viewModel::onWifiInputsChanged,
                                onSaveWifi = viewModel::saveWifi,
                                onSummon = viewModel::summonModem,
                                onReboot = viewModel::rebootModem,
                                onReload = viewModel::loadModem
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(text: String, isEn: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEn) "Announcement" else "Pengumuman",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun UnpaidBillSummaryBanner(
    bill: CustomerBillItem,
    isEn: Boolean,
    onOpenPayment: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPayment)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEn) "Unpaid Transaction" else "Menunggu Pembayaran",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "${bill.planName} • ${formatMoney(bill.currency, bill.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "${bill.paymentChannel} • ${if (isEn) "Tap to pay" else "Ketuk untuk bayar"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = onOpenPayment,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (isEn) "Pay" else "Bayar")
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
    onQuickBuyPlan: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenSendPlan: () -> Unit
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
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

            // Aksi Utama: Isi Saldo + Beli Paket
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

            // Aksi Saldo Tambahan (Transfer Saldo & Belikan Paket Teman) sesuai setting web
            if (data.enableBalance) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (data.allowBalanceTransfer) {
                        OutlinedButton(
                            onClick = onOpenTransfer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Transfer" else "Transfer",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenSendPlan,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Gift Plan" else "Kirim Paket",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1
                        )
                    }
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
private fun ActivePackageRichCard(
    pkg: CustomerPackageItem,
    isEn: Boolean,
    isSubmitting: Boolean,
    canExtend: Boolean,
    extendDays: Int,
    onRenewClick: () -> Unit,
    onSyncClick: () -> Unit,
    onExtendClick: () -> Unit
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

    // Format sisa masa aktif yang rapi (hindari tulisan aneh seperti "Sisa 13635 Hari")
    val remainingLabel = remember(daysLeft, isOn, isEn) {
        when {
            !isOn -> if (isEn) "Expired" else "Masa aktif berakhir"
            daysLeft == null -> ""
            daysLeft < 0 -> if (isEn) "Expired" else "Kedaluwarsa"
            daysLeft == 0 -> if (isEn) "Expires Today" else "Berakhir Hari Ini"
            daysLeft <= 365 -> if (isEn) "$daysLeft Days Left" else "Sisa $daysLeft Hari"
            else -> if (isEn) "Long-term Active" else "Aktif Jangka Panjang"
        }
    }

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
                if (remainingLabel.isNotBlank()) {
                    Text(
                        text = remainingLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Baris Aksi Paket: Perpanjang + Sync Router / Extend Masa Tenggang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onRenewClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEn) "Renew" else "Perpanjang", maxLines = 1)
                }

                if (isOn) {
                    OutlinedButton(
                        onClick = onSyncClick,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", maxLines = 1)
                    }
                } else if (canExtend) {
                    OutlinedButton(
                        onClick = onExtendClick,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Outlined.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Extend +${extendDays}d" else "Tenggang +${extendDays}h", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerModemTabContent(
    result: CustomerModemStatusResult,
    companyPhone: String,
    wifiSsid: String,
    wifiPass: String,
    isEn: Boolean,
    onWifiChanged: (String, String) -> Unit,
    onSaveWifi: () -> Unit,
    onSummon: () -> Unit,
    onReboot: () -> Unit,
    onReload: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (result.status) {
            ModemSupportStatus.SUPPORTED_ONLINE -> {
                val modem = result.info!!
                CustomerModemOnlineCard(
                    modem = modem,
                    wifiSsid = wifiSsid,
                    wifiPass = wifiPass,
                    isEn = isEn,
                    onWifiChanged = onWifiChanged,
                    onSaveWifi = onSaveWifi,
                    onSummon = onSummon,
                    onReboot = onReboot
                )
            }

            ModemSupportStatus.SUPPORTED_OFFLINE -> {
                val modem = result.info!!
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = if (isEn) "Modem Offline" else "Modem Sedang Offline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            StatusBadge(text = strings.offline, tone = BadgeTone.DANGER)
                        }
                        Text(
                            text = "${modem.model} • SN: ${modem.serialNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${if (isEn) "Last seen online" else "Terakhir terhubung"}: ${modem.lastInform}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEn) {
                                "Your modem is registered but currently unreachable. Please check the power adapter and fiber/LAN cable, then tap Check Again."
                            } else {
                                "Modem Anda terdaftar tetapi sedang tidak terhubung ke server. Periksa adaptor listrik dan kabel fiber/LAN, lalu ketuk Cek Ulang."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onReload, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Check Again" else "Cek Ulang")
                            }
                            if (companyPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { openWhatsAppSupport(context, companyPhone) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isEn) "Contact CS" else "Hubungi CS")
                                }
                            }
                        }
                    }
                }
            }

            ModemSupportStatus.NOT_SUPPORTED_OR_UNLINKED,
            ModemSupportStatus.SERVER_UNAVAILABLE -> {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (result.status == ModemSupportStatus.SERVER_UNAVAILABLE) {
                                if (isEn) "Modem Management Unavailable" else "Layanan Kontrol Modem Belum Aktif"
                            } else {
                                if (isEn) "Router Not Linked Yet" else "Router Belum Mendukung / Belum Terhubung"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (result.status == ModemSupportStatus.SERVER_UNAVAILABLE) {
                                if (isEn) {
                                    "Remote modem management (TR-069) is currently not active on this server. Your internet service remains unaffected."
                                } else {
                                    "Sistem kontrol modem jarak jauh (TR-069) belum diaktifkan oleh pengelola. Koneksi internet Anda tetap berjalan normal."
                                }
                            } else {
                                if (isEn) {
                                    "Your router/ONT does not yet support TR-069 remote control or has not been linked to your account. Contact support if you need help changing WiFi settings."
                                } else {
                                    "Perangkat router/modem Anda saat ini belum mendukung kontrol jarak jauh (TR-069) atau belum terhubung ke akun ini. Hubungi CS jika Anda ingin dibantu mengubah nama/sandi WiFi."
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onReload,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Check Again" else "Cek Ulang")
                            }
                            if (companyPhone.isNotBlank()) {
                                Button(
                                    onClick = { openWhatsAppSupport(context, companyPhone) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isEn) "Contact CS" else "Hubungi CS")
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
private fun CustomerModemOnlineCard(
    modem: CustomerModemInfo,
    wifiSsid: String,
    wifiPass: String,
    isEn: Boolean,
    onWifiChanged: (String, String) -> Unit,
    onSaveWifi: () -> Unit,
    onSummon: () -> Unit,
    onReboot: () -> Unit
) {
    val strings = LocalAppStrings.current
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(modem.model, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("SN: ${modem.serialNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(text = strings.online, tone = BadgeTone.SUCCESS)
            }
            Text("Uptime: ${modem.uptime} • IP: ${modem.wanIp}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${if (isEn) "Last sync" else "Sinkron terakhir"}: ${modem.lastInform}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (modem.isOptical) {
                    StatusBadge(text = "RX: ${modem.rxPower}", tone = BadgeTone.SUCCESS)
                }
                if (modem.temperature.isNotBlank() && modem.temperature != "N/A") {
                    StatusBadge(text = modem.temperature, tone = BadgeTone.NEUTRAL)
                }
                StatusBadge(
                    text = "${maxOf(modem.connectedDevices, modem.connectedHostsCount)} ${if (isEn) "Devices" else "Perangkat"}",
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
        shape = RoundedCornerShape(18.dp),
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
                label = { Text(if (isEn) "WiFi Password (Min 8 chars)" else "Password WiFi (Min 8 karakter)") },
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
                    onClick = { openWhatsAppSupport(context, companyPhone) },
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
private fun FullScreenCustomerCheckout(
    plan: CustomerBuyPlanItem,
    dashboard: CustomerPortalDashboard,
    gateways: List<PortalPaymentGateway>,
    selectedGatewayId: String,
    selectedChannelId: String,
    couponCode: String,
    isSubmitting: Boolean,
    onSelectGateway: (String) -> Unit,
    onSelectChannel: (String) -> Unit,
    onCouponChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val isBalancePlan = plan.type.equals("Balance", true)
    val canShowBalance = !isBalancePlan && dashboard.enableBalance
    val isBalanceSufficient = dashboard.balance >= plan.price

    val selectedPg = gateways.find { it.id == selectedGatewayId }
    val isChannelValid = when {
        selectedGatewayId == "balance" -> canShowBalance && isBalanceSufficient
        selectedPg == null -> false
        selectedPg.requiresChannel -> {
            val ch = selectedPg.channels.find { it.id == selectedChannelId }
            ch != null && plan.price >= ch.minAmount
        }
        else -> true
    }

    AppScreenScaffold(
        title = if (isEn) "Checkout" else "Konfirmasi Pesanan",
        subtitle = plan.namePlan,
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isSubmitting && isChannelValid,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isEn) "Pay Now" else "Bayar Sekarang")
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(plan.namePlan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (!isBalancePlan && plan.validity.isNotBlank()) {
                            Text(
                                text = "${if (isEn) "Validity" else "Masa Aktif"}: ${plan.validity}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isBalancePlan) {
                            Text(
                                text = if (isEn) "Account Balance Top-Up" else "Penambahan Saldo Akun",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = formatMoney(dashboard.currency, plan.price),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = strings.paymentMethod,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (canShowBalance) {
                val selected = selectedGatewayId == "balance" && isBalanceSufficient
                val deficit = (plan.price - dashboard.balance).coerceAtLeast(0.0)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isBalanceSufficient) 1f else 0.55f)
                        .clickable(enabled = isBalanceSufficient) {
                            onSelectGateway("balance")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = if (isBalanceSufficient) { { onSelectGateway("balance") } } else null,
                            enabled = isBalanceSufficient
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Account Balance (${formatMoney(dashboard.currency, dashboard.balance)})"
                                else "Saldo Akun (${formatMoney(dashboard.currency, dashboard.balance)})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBalanceSufficient) {
                                    if (isEn) "Instant activation without admin fee" else "Aktivasi instan tanpa biaya admin"
                                } else {
                                    if (isEn) "Insufficient balance (short ${formatMoney(dashboard.currency, deficit)})"
                                    else "Saldo tidak mencukupi (kurang ${formatMoney(dashboard.currency, deficit)})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBalanceSufficient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (gateways.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "No active online payment gateway configured by Admin." else "Belum ada Payment Gateway online yang diaktifkan oleh Admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                gateways.forEach { pg ->
                    val isSelected = selectedGatewayId == pg.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGateway(pg.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectGateway(pg.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pg.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    if (pg.description.isNotBlank()) {
                                        Text(
                                            text = pg.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (isSelected && pg.requiresChannel && pg.channels.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isEn) "Select Payment Channel:" else "Pilih Channel Pembayaran:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pg.channels.forEach { ch ->
                                        val meetsMin = plan.price >= ch.minAmount
                                        val chSelected = selectedChannelId == ch.id && meetsMin
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (chSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
                                            border = BorderStroke(
                                                width = if (chSelected) 1.5.dp else 1.dp,
                                                color = if (chSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .alpha(if (meetsMin) 1f else 0.5f)
                                                .clickable(enabled = meetsMin) { onSelectChannel(ch.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ch.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (chSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    if (!meetsMin) {
                                                        Text(
                                                            text = if (isEn) "Min. ${formatMoney(dashboard.currency, ch.minAmount)}"
                                                            else "Minimal transaksi ${formatMoney(dashboard.currency, ch.minAmount)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                if (chSelected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
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

            if (!isBalancePlan) {
                OutlinedTextField(
                    value = couponCode,
                    onValueChange = onCouponChanged,
                    label = { Text(if (isEn) "Promo Coupon Code (Optional)" else "Kode Kupon Promo (Opsional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun openWhatsAppSupport(context: android.content.Context, rawPhone: String) {
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.isBlank()) return
    val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum")))
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
