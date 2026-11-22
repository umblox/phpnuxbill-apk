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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    viewModel: CustomerPortalViewModel,
    session: SessionState,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voucherCode by viewModel.voucherCodeInput.collectAsStateWithLifecycle()
    val wifiSsid by viewModel.wifiSsidInput.collectAsStateWithLifecycle()
    val wifiPass by viewModel.wifiPassInput.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
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
                            Text(session.effectiveCompanyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${session.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadPortal) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logoutButton, tint = MaterialTheme.colorScheme.error)
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kartu Saldo & Auto-Renewal
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(data.fullname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatMoney(data.currency, data.balance),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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

                    // Tagihan Tertunda (Unpaid Bill)
                    val bill = data.unpaidBill
                    if (bill != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("${bill.planName} • ${formatMoney(data.currency, bill.price)}", fontWeight = FontWeight.Bold)
                                    Text("${bill.gateway.uppercase()} (${bill.paymentChannel})", style = MaterialTheme.typography.labelMedium)
                                    if (bill.payCode.isNotBlank()) {
                                        Text("VA / Code: ${bill.payCode}", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (bill.checkoutUrl.isNotBlank()) {
                                            Button(onClick = {
                                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bill.checkoutUrl))) }
                                            }) { Text("Bayar") }
                                        }
                                        OutlinedButton(onClick = { viewModel.checkBillStatus(bill.id) }) { Text("Cek Status") }
                                        OutlinedButton(onClick = { viewModel.cancelBill(bill.id) }) { Text(strings.cancel) }
                                    }
                                }
                            }
                        }
                    }

                    // Paket Aktif
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
                                    Text("Exp: ${pkg.expiration}", style = MaterialTheme.typography.labelMedium)
                                }
                                StatusBadge(text = if (isOn) "ON" else "OFF", tone = if (isOn) BadgeTone.SUCCESS else BadgeTone.DANGER)
                            }
                        }
                    }

                    // Aktivasi Voucher Mandiri
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

                    // Beli Paket via Saldo
                    items(data.availablePlans, key = { "buy_${it.id}" }) { p ->
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
                                    Text(p.namePlan, fontWeight = FontWeight.Bold)
                                    Text("${p.validity} • ${formatMoney(data.currency, p.price)}", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = { viewModel.buyPlanWithBalance(p.id) },
                                    enabled = data.balance >= p.price
                                ) {
                                    Text("Beli (Saldo)")
                                }
                            }
                        }
                    }

                    // Pengaturan WiFi Modem TR-069 Pelanggan
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.wifiSettings, style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = wifiSsid,
                                    onValueChange = { viewModel.onWifiInputsChanged(it, wifiPass) },
                                    label = { Text("SSID WiFi Baru") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = wifiPass,
                                    onValueChange = { viewModel.onWifiInputsChanged(wifiSsid, it) },
                                    label = { Text("Password WiFi Baru") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = viewModel::saveWifi,
                                    enabled = wifiSsid.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.save)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
