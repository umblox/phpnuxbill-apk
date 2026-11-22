package com.phpnuxbill.mobile.feature.portal.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    viewModel: CustomerPortalViewModel,
    session: SessionState,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    // Layar Checkout Pembelian Paket / Top-Up (Saldo atau Payment Gateway + Channel + Kupon)
    val activeData = (uiState as? UiState.Success)?.data
    if (checkoutPlan != null && activeData != null) {
        FullScreenCustomerCheckout(
            plan = checkoutPlan!!,
            dashboard = activeData,
            selectedGatewayId = selectedGatewayId,
            selectedChannelId = selectedChannelId,
            couponCode = couponCode,
            isSubmitting = isSubmitting,
            onSelectGateway = viewModel::selectCheckoutGateway,
            onSelectChannel = viewModel::selectCheckoutChannel,
            onCouponChanged = viewModel::onCouponCodeChanged,
            onConfirm = viewModel::confirmCheckoutOrder,
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
                            size = 34.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeData?.companyName?.ifBlank { session.effectiveCompanyName }
                                    ?: session.effectiveCompanyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
                    IconButton(onClick = viewModel::loadPortal) {
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
        when (val st = uiState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView(modifier = Modifier.padding(innerPadding))
            is UiState.Error -> ErrorStateView(message = st.message, onRetry = viewModel::loadPortal, modifier = Modifier.padding(innerPadding))
            is UiState.Success -> {
                val data = st.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val tabs = listOf(
                        if (isEn) "Home" else "Beranda",
                        if (isEn) "Order Plan" else "Beli Paket",
                        if (isEn) "History" else "Riwayat",
                        if (isEn) "My Modem" else "Modem Saya"
                    )
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEachIndexed { idx, title ->
                            Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                        }
                    }

                    when (selectedTab) {
                        // TAB 0: BERANDA, TAGIHAN QRIS/VA & REDEEM VOUCHER
                        0 -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(data.fullname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatMoney(data.currency, data.balance),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Auto-Renewal", style = MaterialTheme.typography.bodyMedium)
                                            Switch(checked = data.autoRenewal, onCheckedChange = { viewModel.toggleAutoRenewal() })
                                        }
                                    }
                                }
                            }

                            if (data.unpaidBill != null) {
                                item {
                                    UnpaidBillDetailCard(
                                        bill = data.unpaidBill,
                                        onCheckStatus = { viewModel.checkBillStatus(data.unpaidBill.id) },
                                        onCancel = { viewModel.cancelBill(data.unpaidBill.id) }
                                    )
                                }
                            }

                            items(data.activePackages, key = { it.id }) { pkg ->
                                val isOn = pkg.status.equals("on", true)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pkg.namebp, fontWeight = FontWeight.Bold)
                                            Text("Exp: ${pkg.expiration} ${pkg.time}", style = MaterialTheme.typography.labelMedium)
                                        }
                                        StatusBadge(text = if (isOn) "ON" else "OFF", tone = if (isOn) BadgeTone.SUCCESS else BadgeTone.DANGER)
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Redeem Voucher", style = MaterialTheme.typography.titleMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = voucherCode,
                                                onValueChange = viewModel::onVoucherCodeChanged,
                                                label = { Text("Kode Voucher") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(onClick = viewModel::redeemVoucher, enabled = voucherCode.isNotBlank()) {
                                                Text("Redeem")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 1: BELI PAKET INTERNET & TOP-UP SALDO
                        1 -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (data.internetPlans.isNotEmpty()) {
                                item { Text(strings.menuPlans, style = MaterialTheme.typography.titleMedium) }
                                items(data.internetPlans, key = { "inet_${it.id}" }) { p ->
                                    PortalPlanCard(plan = p, currency = data.currency, onOrder = { viewModel.openCheckoutForPlan(p) })
                                }
                            }
                            if (data.balancePlans.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(strings.topUpBalance, style = MaterialTheme.typography.titleMedium)
                                }
                                items(data.balancePlans, key = { "bal_${it.id}" }) { bp ->
                                    PortalPlanCard(plan = bp, currency = data.currency, onOrder = { viewModel.openCheckoutForPlan(bp) })
                                }
                            }
                        }

                        // TAB 2: RIWAYAT TRANSAKSI
                        2 -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(data.transactions, key = { it.id }) { tr ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tr.planName, fontWeight = FontWeight.Bold)
                                            Text("${tr.invoice} • ${tr.rechargedOn} ${tr.rechargedTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(tr.method, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = formatMoney(data.currency, tr.price),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // TAB 3: KONTROL MODEM TR-069 PELANGGAN
                        3 -> {
                            val modem = data.modemInfo
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (modem == null) {
                                    Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                StatusBadge(text = "Devices: ${maxOf(modem.connectedDevices, modem.connectedHostsCount)}", tone = BadgeTone.NEUTRAL)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilledTonalButton(onClick = viewModel::summonModem, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Refresh")
                                                }
                                                OutlinedButton(onClick = viewModel::rebootModem, modifier = Modifier.weight(1f)) {
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
                                            Text(strings.wifiSettings, style = MaterialTheme.typography.titleMedium)
                                            OutlinedTextField(
                                                value = wifiSsid,
                                                onValueChange = { viewModel.onWifiInputsChanged(it, wifiPass) },
                                                label = { Text("SSID WiFi") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = wifiPass,
                                                onValueChange = { viewModel.onWifiInputsChanged(wifiSsid, it) },
                                                label = { Text("Password WiFi (Min 8 chars)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = viewModel::saveWifi,
                                                enabled = wifiSsid.isNotBlank(),
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text(strings.save) }
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

            // Mode QRIS
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

            // Mode Virtual Account / Pay Code
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

            // Panduan Instruksi Pembayaran dari Gateway
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
private fun PortalPlanCard(
    plan: CustomerBuyPlanItem,
    currency: String,
    onOrder: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.namePlan, fontWeight = FontWeight.Bold)
                Text("${plan.validity} • ${formatMoney(currency, plan.price)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onOrder, shape = RoundedCornerShape(10.dp)) {
                Text("Pilih")
            }
        }
    }
}

@Composable
private fun FullScreenCustomerCheckout(
    plan: CustomerBuyPlanItem,
    dashboard: CustomerPortalDashboard,
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
    val selectedPg = dashboard.paymentGateways.find { it.id == selectedGatewayId }

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
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text(strings.confirm)
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
                items(dashboard.paymentGateways, key = { it.id }) { pg ->
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
