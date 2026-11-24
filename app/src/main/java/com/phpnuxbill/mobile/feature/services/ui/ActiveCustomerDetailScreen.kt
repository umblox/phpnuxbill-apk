@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.services.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.auth.AdminRolePolicy
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.services.model.ActiveCustomerDetailData
import com.phpnuxbill.mobile.feature.services.model.EditPrepaidFormState
import com.phpnuxbill.mobile.feature.services.model.LiveSpeedSample
import com.phpnuxbill.mobile.feature.services.model.PrepaidServiceItem
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherLabels
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata
import kotlinx.coroutines.delay

@Composable
fun ActiveCustomerDetailScreen(
    selectedItem: PrepaidServiceItem,
    detailState: UiState<ActiveCustomerDetailData>,
    speedHistory: List<LiveSpeedSample>,
    isLivePollingEnabled: Boolean,
    isActionRunning: Boolean,
    editForm: EditPrepaidFormState,
    metadata: ServiceVoucherMetadata,
    adminLevel: AdminLevel,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onToggleLivePolling: () -> Unit,
    onPollLive: (silent: Boolean) -> Unit,
    onRecharge: (customerId: Int, planId: Int) -> Unit,
    onDeposit: (customerId: Int) -> Unit,
    onExtend: (rechargeId: Int, days: Int) -> Unit,
    onSyncRouter: () -> Unit,
    onKickSession: () -> Unit,
    onOpenEditPrepaid: (ActiveCustomerDetailData) -> Unit,
    onUpdateEditPrepaid: ((EditPrepaidFormState) -> EditPrepaidFormState) -> Unit,
    onSubmitEditPrepaid: () -> Unit,
    onCloseEditPrepaid: () -> Unit,
    onViewReceipt: () -> Unit,
    onDeleteService: (rechargeId: Int) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val context = LocalContext.current

    var showKickConfirm by remember { mutableStateOf(false) }
    var showExtendDialog by remember { mutableStateOf(false) }
    var extendDaysInput by remember { mutableStateOf("3") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canRecharge = AdminRolePolicy.canRechargeOrDeposit(adminLevel)
    val canExtend = AdminRolePolicy.canExtendService(adminLevel)
    val canEditOrDelete = AdminRolePolicy.canEditOrDeleteService(adminLevel)
    val canSync = AdminRolePolicy.canAddCustomer(adminLevel)

    BackHandler(enabled = true) {
        if (editForm.isOpen) onCloseEditPrepaid() else onBack()
    }

    // Polling Live Speed MikroTik otomatis setiap 2,5 detik selama layar aktif & tidak dijeda
    LaunchedEffect(selectedItem.id, isLivePollingEnabled) {
        if (!isLivePollingEnabled) return@LaunchedEffect
        while (true) {
            delay(2500L)
            onPollLive(true)
        }
    }

    val loadedDetail = (detailState as? UiState.Success)?.data

    // Dialog Konfirmasi Kick Sesi MikroTik
    if (showKickConfirm && loadedDetail != null && adminLevel.isFullAdmin) {
        AlertDialog(
            onDismissRequest = { showKickConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            title = { Text(labels.kickConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text("${labels.kickConfirmMsg}\n\n@${loadedDetail.username} (${loadedDetail.router})") },
            confirmButton = {
                Button(
                    onClick = {
                        showKickConfirm = false
                        onKickSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(labels.kickSession)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showKickConfirm = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog Extend Masa Aktif
    if (showExtendDialog && loadedDetail != null && canExtend) {
        AlertDialog(
            onDismissRequest = { showExtendDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text("${strings.extendExpiry} (@${loadedDetail.username})", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = extendDaysInput,
                    onValueChange = { extendDaysInput = it.filter { c -> c.isDigit() } },
                    label = { Text(labels.extendDaysPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = extendDaysInput.toIntOrNull()?.coerceAtLeast(1) ?: 3
                        val targetId = loadedDetail.packageId.takeIf { it > 0 } ?: selectedItem.id
                        showExtendDialog = false
                        onExtend(targetId, d)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExtendDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Layanan
    if (showDeleteConfirm && canEditOrDelete) {
        val targetId = loadedDetail?.packageId?.takeIf { it > 0 } ?: selectedItem.id
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text("${labels.deactivateConfirmTitle} (@${selectedItem.username})", fontWeight = FontWeight.Bold) },
            text = { Text(labels.deactivateConfirmMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteService(targetId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Dialog Edit Paket / Tanggal Jatuh Tempo (SuperAdmin / Admin)
    if (editForm.isOpen && canEditOrDelete) {
        EditPrepaidServiceDialog(
            form = editForm,
            metadata = metadata,
            currencyCode = currencyCode,
            labels = labels,
            onUpdate = onUpdateEditPrepaid,
            onSubmit = onSubmitEditPrepaid,
            onDismiss = onCloseEditPrepaid
        )
    }

    AppScreenScaffold(
        title = loadedDetail?.fullname?.ifBlank { selectedItem.fullname } ?: selectedItem.fullname,
        subtitle = "@${selectedItem.username} • ${selectedItem.type} • ${selectedItem.routers}",
        onBack = onBack,
        actions = {
            IconButton(onClick = onToggleLivePolling) {
                Icon(
                    imageVector = if (isLivePollingEnabled) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isLivePollingEnabled) labels.livePaused else labels.liveRunning,
                    tint = if (isLivePollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onPollLive(false) }) {
                Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
            }
        }
    ) { innerPadding ->
        when (val state = detailState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView(modifier = Modifier.padding(innerPadding))
            is UiState.Error -> ErrorStateView(
                message = state.message,
                onRetry = { onPollLive(false) },
                modifier = Modifier.padding(innerPadding)
            )
            is UiState.Success -> {
                val d = state.data
                val isPkgOn = d.packageStatus.equals("on", ignoreCase = true)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. HERO CARD LIVE SPEED MIKROTIK + GRAFIK REAL-TIME
                    item(key = "live_hero") {
                        MikrotikLiveSpeedHeroCard(
                            detail = d,
                            speedHistory = speedHistory,
                            isLivePolling = isLivePollingEnabled,
                            labels = labels,
                            isEn = isEn
                        )
                    }

                    // 2. BARIS AKSI OPERASIONAL (ROLE-AWARE)
                    item(key = "detail_actions") {
                        ActiveDetailQuickActionsBar(
                            detail = d,
                            isPkgOn = isPkgOn,
                            isActionRunning = isActionRunning,
                            canRecharge = canRecharge,
                            canExtend = canExtend,
                            canSync = canSync,
                            canEditOrDelete = canEditOrDelete,
                            isFullAdmin = adminLevel.isFullAdmin,
                            labels = labels,
                            onRecharge = { onRecharge(d.customerId, d.planId) },
                            onDeposit = { onDeposit(d.customerId) },
                            onExtend = {
                                extendDaysInput = "3"
                                showExtendDialog = true
                            },
                            onSync = onSyncRouter,
                            onKick = { showKickConfirm = true },
                            onEditPlan = { onOpenEditPrepaid(d) },
                            onReceipt = onViewReceipt,
                            onWhatsApp = {
                                val digits = d.phonenumber.replace(Regex("[^0-9]"), "")
                                if (digits.isNotBlank()) {
                                    val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum")))
                                    }
                                }
                            },
                            onDelete = { showDeleteConfirm = true }
                        )
                    }

                    // 3. DATA SESI MIKROTIK LENGKAP
                    item(key = "mikrotik_session") {
                        DetailInfoCard(
                            title = labels.mikrotikSessionTitle,
                            icon = Icons.Outlined.Router
                        ) {
                            DetailKeyValueRow(if (isEn) "Router" else "Router", d.router)
                            DetailKeyValueRow(if (isEn) "Connection Status" else "Status Koneksi", if (d.online) "ONLINE" else "OFFLINE", highlight = d.online)
                            DetailKeyValueRow(if (isEn) "Service / Server" else "Layanan / Server", "${d.service} • ${d.server}")
                            DetailKeyValueRow("IP Address", d.ipAddress, monospace = true)
                            DetailKeyValueRow(if (isEn) "MAC / Caller-ID" else "MAC / Caller-ID", d.macAddress, monospace = true)
                            DetailKeyValueRow("Uptime", d.uptime)
                            if (d.sessionTime != "-" && d.sessionTime.isNotBlank()) {
                                DetailKeyValueRow(if (isEn) "Session Left" else "Sisa Sesi", d.sessionTime)
                            }
                            if (d.idleTime != "-" && d.idleTime.isNotBlank()) {
                                DetailKeyValueRow("Idle Time", d.idleTime)
                            }
                            if (d.loginBy != "-" && d.loginBy.isNotBlank()) {
                                DetailKeyValueRow(if (isEn) "Auth / Encoding" else "Metode Login", d.loginBy)
                            }
                            DetailKeyValueRow(
                                if (isEn) "Active Devices" else "Perangkat Online",
                                "${d.activeDevices} ${if (isEn) "Device(s)" else "Perangkat"}"
                            )
                            DetailKeyValueRow(
                                if (isEn) "Session Usage" else "Pemakaian Sesi",
                                "↓ ${d.downloadBytes}  •  ↑ ${d.uploadBytes} (${d.totalBytes})"
                            )
                        }
                    }

                    // 4. DATA BILLING & LANGGANAN LENGKAP
                    item(key = "billing_info") {
                        val daysLeft = remember(d.expiration) { calcServiceDaysRemaining(d.expiration) }
                        val expBadge = remember(daysLeft, isPkgOn, isEn) {
                            formatRemainingDaysLabel(daysLeft, isPkgOn, isEn)
                        }
                        DetailInfoCard(
                            title = labels.billingInfoTitle,
                            icon = Icons.Outlined.AccountBalanceWallet
                        ) {
                            DetailKeyValueRow(if (isEn) "Package" else "Paket Aktif", d.planName, highlight = true)
                            DetailKeyValueRow(
                                if (isEn) "Billing Status" else "Status Paket",
                                "${if (isPkgOn) "ACTIVE (ON)" else "EXPIRED (OFF)"}${if (expBadge.isNotBlank()) " • $expBadge" else ""}",
                                highlight = isPkgOn
                            )
                            DetailKeyValueRow(if (isEn) "Bandwidth Rate" else "Kecepatan Paket", d.rateLabel)
                            DetailKeyValueRow(if (isEn) "Validity" else "Durasi Paket", d.validityLabel)
                            DetailKeyValueRow(if (isEn) "Plan Price" else "Harga Paket", formatMoney(currencyCode, d.price))
                            DetailKeyValueRow(if (isEn) "Customer Balance" else "Saldo Pelanggan", formatMoney(currencyCode, d.balance), highlight = true)
                            DetailKeyValueRow(
                                if (isEn) "Auto-Renewal" else "Perpanjang Otomatis",
                                if (d.autoRenewal) (if (isEn) "Enabled" else "Aktif") else (if (isEn) "Disabled" else "Nonaktif")
                            )
                            DetailKeyValueRow(if (isEn) "Recharged On" else "Tanggal Aktif", "${d.rechargedOn} ${d.rechargedTime}".trim())
                            DetailKeyValueRow(if (isEn) "Expires On" else "Jatuh Tempo", "${d.expiration} ${d.expirationTime}".trim())
                            DetailKeyValueRow(if (isEn) "Payment Method" else "Metode Bayar", d.method)
                            DetailKeyValueRow(if (isEn) "Account Type / Status" else "Tipe / Status Akun", "${d.accountType} • ${d.accountStatus}")
                            if (d.pppoeUsername.isNotBlank()) {
                                DetailKeyValueRow("PPPoE User", d.pppoeUsername, monospace = true)
                            }
                            if (d.pppoeIp.isNotBlank()) {
                                DetailKeyValueRow("PPPoE Static IP", d.pppoeIp, monospace = true)
                            }
                            if (d.phonenumber.isNotBlank()) {
                                DetailKeyValueRow(if (isEn) "Phone" else "No. HP", d.phonenumber)
                            }
                            if (d.address.isNotBlank() || d.city.isNotBlank()) {
                                DetailKeyValueRow(
                                    if (isEn) "Location" else "Alamat",
                                    listOf(d.address, d.city).filter { it.isNotBlank() }.joinToString(", ")
                                )
                            }
                        }
                    }

                    // 5. ATRIBUT TAMBAHAN (CUSTOM FIELDS)
                    if (d.customFields.isNotEmpty()) {
                        item(key = "custom_fields") {
                            DetailInfoCard(
                                title = labels.customFieldsTitle,
                                icon = Icons.Outlined.Devices
                            ) {
                                d.customFields.forEach { (k, v) ->
                                    DetailKeyValueRow(k, v.ifBlank { "-" })
                                }
                            }
                        }
                    }

                    // 6. RIWAYAT TRANSAKSI TERAKHIR
                    if (d.recentTransactions.isNotEmpty()) {
                        item(key = "recent_trx") {
                            DetailInfoCard(
                                title = labels.recentTrxTitle,
                                icon = Icons.Outlined.History
                            ) {
                                d.recentTransactions.forEachIndexed { idx, trx ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${trx.planName} (${trx.invoice})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${trx.rechargedOn} ${trx.rechargedTime} • ${trx.method}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatMoney(currencyCode, trx.price),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
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

@Composable
private fun MikrotikLiveSpeedHeroCard(
    detail: ActiveCustomerDetailData,
    speedHistory: List<LiveSpeedSample>,
    isLivePolling: Boolean,
    labels: ServiceVoucherLabels,
    isEn: Boolean
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(BrandNavy900, BrandBlue600, BrandTeal500)))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Live Monitor + Badge Online/Offline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = Color(0xFF5EEAD4),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = labels.liveTrafficTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (detail.online) Color(0xFF22C55E).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.28f)
                    ) {
                        Text(
                            text = if (detail.online) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (detail.online) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (isLivePolling) labels.liveRunning else labels.livePaused,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Metrik Kecepatan Download & Upload Real-time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF5EEAD4),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Download",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = detail.downloadSpeedLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5EEAD4),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Total: ${detail.downloadBytes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Upload",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = detail.uploadSpeedLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFDE68A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Total: ${detail.uploadBytes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            // Grafik Sparkline Real-time Dual Garis (Download & Upload)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.22f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    LiveTrafficSparklineCanvas(
                        samples = speedHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (isEn) "IP" else "IP"}: ${detail.ipAddress} • Uptime: ${detail.uptime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = detail.totalBytes,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTrafficSparklineCanvas(
    samples: List<LiveSpeedSample>,
    modifier: Modifier = Modifier
) {
    val dlColor = Color(0xFF5EEAD4)
    val ulColor = Color(0xFFFDE68A)
    val gridColor = Color.White.copy(alpha = 0.14f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Garis kisi horizontal halus
        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
        }

        if (samples.isEmpty()) return@Canvas

        val maxKbps = samples.maxOf { maxOf(it.downloadKbps, it.uploadKbps) }.coerceAtLeast(10f)
        val count = samples.size
        val stepX = if (count > 1) w / (count - 1).toFloat() else w

        fun buildLinePath(selector: (LiveSpeedSample) -> Float): Path {
            val p = Path()
            samples.forEachIndexed { idx, s ->
                val x = if (count == 1) w / 2f else idx * stepX
                val ratio = (selector(s) / maxKbps).coerceIn(0f, 1f)
                val y = h - (ratio * (h - 10f)) - 5f
                if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p
        }

        if (count == 1) {
            val s = samples.first()
            val yDl = h - ((s.downloadKbps / maxKbps).coerceIn(0f, 1f) * (h - 10f)) - 5f
            val yUl = h - ((s.uploadKbps / maxKbps).coerceIn(0f, 1f) * (h - 10f)) - 5f
            drawCircle(dlColor, radius = 5f, center = Offset(w / 2f, yDl))
            drawCircle(ulColor, radius = 4f, center = Offset(w / 2f, yUl))
            return@Canvas
        }

        val strokeDl = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val strokeUl = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawPath(path = buildLinePath { it.uploadKbps }, color = ulColor, style = strokeUl)
        drawPath(path = buildLinePath { it.downloadKbps }, color = dlColor, style = strokeDl)
    }
}

@Composable
private fun ActiveDetailQuickActionsBar(
    detail: ActiveCustomerDetailData,
    isPkgOn: Boolean,
    isActionRunning: Boolean,
    canRecharge: Boolean,
    canExtend: Boolean,
    canSync: Boolean,
    canEditOrDelete: Boolean,
    isFullAdmin: Boolean,
    labels: ServiceVoucherLabels,
    onRecharge: () -> Unit,
    onDeposit: () -> Unit,
    onExtend: () -> Unit,
    onSync: () -> Unit,
    onKick: () -> Unit,
    onEditPlan: () -> Unit,
    onReceipt: () -> Unit,
    onWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (canRecharge && detail.customerId > 0) {
            item {
                Button(
                    onClick = onRecharge,
                    enabled = !isActionRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recharge")
                }
            }
            item {
                FilledTonalButton(
                    onClick = onDeposit,
                    enabled = !isActionRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deposit")
                }
            }
        }
        if (!isPkgOn && canExtend) {
            item {
                FilledTonalButton(
                    onClick = onExtend,
                    enabled = !isActionRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extend")
                }
            }
        }
        if (canSync && detail.customerId > 0) {
            item {
                OutlinedButton(
                    onClick = onSync,
                    enabled = !isActionRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(labels.syncRouter)
                }
            }
        }
        if (isFullAdmin && detail.online) {
            item {
                OutlinedButton(
                    onClick = onKick,
                    enabled = !isActionRunning,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kick")
                }
            }
        }
        if (canEditOrDelete && detail.packageId > 0) {
            item {
                OutlinedButton(
                    onClick = onEditPlan,
                    enabled = !isActionRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(labels.editPlanExpiry)
                }
            }
        }
        if (detail.customerId > 0 || detail.packageId > 0) {
         item {
             OutlinedButton(
                 onClick = onReceipt,
                 shape = RoundedCornerShape(12.dp),
                 contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
             ) {
                 Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                 Spacer(modifier = Modifier.width(6.dp))
                 Text("Invoice")
             }
         }
     }
        if (detail.phonenumber.isNotBlank()) {
            item {
                OutlinedButton(
                    onClick = onWhatsApp,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            }
        }
        if (canEditOrDelete && detail.packageId > 0) {
            item {
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isActionRunning,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.delete)
                }
            }
        }
    }
}

@Composable
private fun DetailInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun DetailKeyValueRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.95f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value.ifBlank { "-" },
            style = if (monospace) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.25f)
        )
    }
}

@Composable
private fun EditPrepaidServiceDialog(
    form: EditPrepaidFormState,
    metadata: ServiceVoucherMetadata,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onUpdate: ((EditPrepaidFormState) -> EditPrepaidFormState) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var planExpanded by remember { mutableStateOf(false) }
    val selectedPlan = metadata.internetPlans.find { it.id == form.planId }

    AlertDialog(
        onDismissRequest = { if (!form.isSubmitting) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        title = { Text("${labels.editPlanExpiry} (@${form.username})", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = form.error.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = planExpanded,
                    onExpandedChange = { planExpanded = !planExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPlan?.let { "${it.namePlan} (${formatMoney(currencyCode, it.price)})" }
                            ?: strings.selectPlan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectPlan) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = planExpanded,
                        onDismissRequest = { planExpanded = false }
                    ) {
                        metadata.internetPlans.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.namePlan} • ${p.type} (${formatMoney(currencyCode, p.price)})") },
                                onClick = {
                                    onUpdate { it.copy(planId = p.id) }
                                    planExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = form.expiration,
                    onValueChange = { v -> onUpdate { it.copy(expiration = v) } },
                    label = { Text(labels.editExpiryLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = form.time,
                    onValueChange = { v -> onUpdate { it.copy(time = v) } },
                    label = { Text(labels.editTimeLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !form.isSubmitting && form.planId > 0 && form.expiration.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(strings.save)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !form.isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.cancel)
            }
        }
    )
}
