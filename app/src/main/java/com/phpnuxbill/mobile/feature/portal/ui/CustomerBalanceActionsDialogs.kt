package com.phpnuxbill.mobile.feature.portal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.FriendPlanLookupResult

/**
 * Dialog Transfer Saldo (2 Tahap):
 * Tahap 1: Input Username Tujuan & Nominal -> Cek & Verifikasi Akun Penerima
 * Tahap 2: Konfirmasi Detail (Nama Lengkap, @username, Nominal, Sisa Saldo) -> Setuju & Kirim / Kembali
 */
@Composable
fun TransferBalanceDialog(
    currency: String,
    currentBalance: Double,
    minimumTransfer: Double,
    lookupState: UiState<FriendPlanLookupResult>,
    isSubmitting: Boolean,
    onLookupFriend: (username: String) -> Unit,
    onResetLookup: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (friend: FriendPlanLookupResult, amount: Double) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var targetUser by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val belowMin = minimumTransfer > 0.0 && parsedAmount > 0.0 && parsedAmount < minimumTransfer
    val overBalance = parsedAmount > currentBalance
    val isInputValid = targetUser.trim().isNotBlank() && parsedAmount > 0.0 && !belowMin && !overBalance

    val verifiedFriend = (lookupState as? UiState.Success)?.data
    val isCheckingFriend = lookupState is UiState.Loading
    val lookupError = (lookupState as? UiState.Error)?.message

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onResetLookup()
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (verifiedFriend != null) Icons.Outlined.VerifiedUser else Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(
                text = if (verifiedFriend != null) {
                    if (isEn) "Confirm Transfer" else "Konfirmasi Transfer Saldo"
                } else {
                    if (isEn) "Transfer Balance" else "Transfer Saldo"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (verifiedFriend == null) {
                    // TAHAP 1: Input & Verifikasi Penerima
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isEn) "Available Balance" else "Saldo Tersedia",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(currency, currentBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (minimumTransfer > 0.0) {
                                Text(
                                    text = "Min. transfer: ${formatMoney(currency, minimumTransfer)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetUser,
                        onValueChange = {
                            targetUser = it
                            if (lookupError != null) onResetLookup()
                        },
                        label = { Text(if (isEn) "Recipient Username" else "Username Tujuan") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isEn) "Amount ($currency)" else "Nominal ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = belowMin || overBalance,
                        supportingText = {
                            when {
                                overBalance -> Text(
                                    if (isEn) "Amount exceeds your available balance."
                                    else "Nominal melebihi saldo yang tersedia."
                                )
                                belowMin -> Text(
                                    if (isEn) "Minimum transfer is ${formatMoney(currency, minimumTransfer)}"
                                    else "Minimal transfer adalah ${formatMoney(currency, minimumTransfer)}"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!lookupError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = lookupError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                } else {
                    // TAHAP 2: Konfirmasi Detail Sebelum Memotong Saldo
                    Text(
                        text = if (isEn) {
                            "Please verify the transfer details below before approving:"
                        } else {
                            "Pastikan rincian transfer berikut sudah benar sebelum disetujui:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConfirmSummaryRow(
                                label = if (isEn) "Recipient" else "Penerima",
                                value = verifiedFriend.fullname,
                                highlight = true
                            )
                            ConfirmSummaryRow(
                                label = "Username",
                                value = "@${verifiedFriend.username}"
                            )
                            ConfirmSummaryRow(
                                label = if (isEn) "Service Type" else "Layanan",
                                value = verifiedFriend.serviceType
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ConfirmSummaryRow(
                                label = if (isEn) "Transfer Amount" else "Nominal Kirim",
                                value = formatMoney(currency, parsedAmount),
                                highlight = true
                            )
                            ConfirmSummaryRow(
                                label = if (isEn) "Current Balance" else "Saldo Saat Ini",
                                value = formatMoney(currency, currentBalance)
                            )
                            ConfirmSummaryRow(
                                label = if (isEn) "Balance After" else "Estimasi Sisa Saldo",
                                value = formatMoney(currency, (currentBalance - parsedAmount).coerceAtLeast(0.0)),
                                highlight = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (verifiedFriend == null) {
                Button(
                    onClick = { onLookupFriend(targetUser.trim()) },
                    enabled = !isCheckingFriend && isInputValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingFriend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (isEn) "Continue" else "Lanjut")
                }
            } else {
                Button(
                    onClick = { onConfirm(verifiedFriend, parsedAmount) },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (isEn) "Approve & Send" else "Setuju & Kirim", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (verifiedFriend == null) {
                OutlinedButton(
                    onClick = {
                        onResetLookup()
                        onDismiss()
                    },
                    enabled = !isCheckingFriend,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.cancel)
                }
            } else {
                OutlinedButton(
                    onClick = onResetLookup,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEn) "Back" else "Kembali")
                }
            }
        }
    )
}

/**
 * Dialog Belikan Paket Teman (3 Tahap):
 * Tahap 1: Cek & Verifikasi Akun Teman (customer_lookup_friend)
 * Tahap 2: Pilih Paket yang Sesuai Tipe Layanan Teman (atau terkunci paket aktif teman)
 * Tahap 3: Konfirmasi Detail (Nama Teman, Paket, Harga, Saldo Sebelum -> Sisa Saldo) -> Setuju & Aktifkan
 */
@Composable
fun SendPlanToFriendDialog(
    currency: String,
    currentBalance: Double,
    lookupState: UiState<FriendPlanLookupResult>,
    isSubmitting: Boolean,
    onLookupFriend: (username: String) -> Unit,
    onResetLookup: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (friend: FriendPlanLookupResult, plan: CustomerBuyPlanItem) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var friendUsername by remember { mutableStateOf("") }
    var selectedPlanId by remember { mutableIntStateOf(0) }
    var isConfirmStep by remember { mutableStateOf(false) }

    val friendData = (lookupState as? UiState.Success)?.data
    val isCheckingFriend = lookupState is UiState.Loading
    val lookupError = (lookupState as? UiState.Error)?.message

    // Auto-pilih paket aktif/terakhir teman saat data teman baru diterima
    LaunchedEffect(friendData) {
        isConfirmStep = false
        if (friendData != null) {
            selectedPlanId = when {
                friendData.hasActivePlan && friendData.activePlanId > 0 -> friendData.activePlanId
                friendData.lastPlan != null -> friendData.lastPlan.id
                else -> friendData.eligiblePlans.firstOrNull()?.id ?: 0
            }
        } else {
            selectedPlanId = 0
        }
    }

    val candidatePlans = remember(friendData) {
        if (friendData == null) emptyList()
        else if (friendData.hasActivePlan && friendData.activePlanId > 0) {
            val activeObj = friendData.eligiblePlans.find { it.id == friendData.activePlanId }
                ?: friendData.lastPlan?.takeIf { it.id == friendData.activePlanId }
            listOfNotNull(activeObj)
        } else {
            friendData.eligiblePlans
        }
    }

    val selectedPlan = candidatePlans.find { it.id == selectedPlanId }
        ?: friendData?.lastPlan?.takeIf { it.id == selectedPlanId }
    val canAffordSelected = selectedPlan != null && currentBalance >= selectedPlan.price

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onResetLookup()
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConfirmStep) Icons.Outlined.VerifiedUser else Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        title = {
            Text(
                text = when {
                    isConfirmStep -> if (isEn) "Confirm Friend's Plan" else "Konfirmasi Beli Paket Teman"
                    friendData != null -> if (isEn) "Select Friend's Plan" else "Pilih Paket untuk Teman"
                    else -> if (isEn) "Buy Plan for Friend" else "Belikan Paket Teman"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    // TAHAP 1: Cek Akun Teman
                    friendData == null -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isEn) "Paid via Account Balance" else "Dibayar Memakai Saldo Anda",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatMoney(currency, currentBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = friendUsername,
                            onValueChange = {
                                friendUsername = it
                                if (lookupError != null) onResetLookup()
                            },
                            label = { Text(if (isEn) "Friend's Username" else "Username Teman") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!lookupError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = lookupError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // TAHAP 2: Pilih Paket Sesuai Tipe Akun Teman
                    !isConfirmStep -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = friendData.fullname,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusBadge(text = friendData.serviceType.uppercase(), tone = BadgeTone.NEUTRAL)
                                }
                                Text(
                                    text = "@${friendData.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (friendData.hasActivePlan && friendData.activePlanName.isNotBlank()) {
                                    Text(
                                        text = if (isEn) {
                                            "Active plan: ${friendData.activePlanName} (exp ${friendData.activeExpiration}). Renewal is locked to the same plan."
                                        } else {
                                            "Paket aktif: ${friendData.activePlanName} (s/d ${friendData.activeExpiration}). Perpanjangan terkunci ke paket yang sama."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (candidatePlans.isEmpty()) {
                            Text(
                                text = if (isEn) "No eligible plans found for this account."
                                else "Tidak ada paket aktif yang cocok untuk akun teman ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            candidatePlans.forEach { p ->
                                val canAfford = currentBalance >= p.price
                                val picked = selectedPlanId == p.id
                                val isLast = friendData.lastPlan?.id == p.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (picked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.background,
                                    border = BorderStroke(
                                        width = if (picked) 1.5.dp else 1.dp,
                                        color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(if (canAfford) 1f else 0.55f)
                                        .clickable(enabled = canAfford) { selectedPlanId = p.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = p.namePlan,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                                                )
                                                if (isLast) {
                                                    StatusBadge(
                                                        text = if (isEn) "Last" else "Terakhir",
                                                        tone = BadgeTone.SUCCESS
                                                    )
                                                }
                                            }
                                            val metaText = listOf(p.validity, p.bandwidth).filter { it.isNotBlank() }.joinToString(" • ")
                                            if (metaText.isNotBlank()) {
                                                Text(
                                                    text = metaText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!canAfford) {
                                                Text(
                                                    text = if (isEn) "Insufficient balance" else "Saldo tidak cukup",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = formatMoney(currency, p.price),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            if (picked && canAfford) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAHAP 3: Konfirmasi Detail Pembelian Paket Teman
                    selectedPlan != null -> {
                        Text(
                            text = if (isEn) {
                                "Please confirm the package purchase for your friend below:"
                            } else {
                                "Pastikan rincian pembelian paket untuk teman berikut sudah sesuai:"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ConfirmSummaryRow(
                                    label = if (isEn) "Recipient" else "Penerima",
                                    value = friendData.fullname,
                                    highlight = true
                                )
                                ConfirmSummaryRow(
                                    label = "Username",
                                    value = "@${friendData.username}"
                                )
                                ConfirmSummaryRow(
                                    label = if (isEn) "Service" else "Layanan",
                                    value = friendData.serviceType
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                ConfirmSummaryRow(
                                    label = if (isEn) "Package" else "Paket Dipilih",
                                    value = selectedPlan.namePlan,
                                    highlight = true
                                )
                                if (selectedPlan.validity.isNotBlank()) {
                                    ConfirmSummaryRow(
                                        label = if (isEn) "Validity" else "Masa Aktif",
                                        value = selectedPlan.validity
                                    )
                                }
                                if (selectedPlan.bandwidth.isNotBlank()) {
                                    ConfirmSummaryRow(
                                        label = "Bandwidth",
                                        value = selectedPlan.bandwidth
                                    )
                                }
                                ConfirmSummaryRow(
                                    label = if (isEn) "Price" else "Harga Paket",
                                    value = formatMoney(currency, selectedPlan.price),
                                    highlight = true
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                ConfirmSummaryRow(
                                    label = if (isEn) "Your Balance" else "Saldo Anda",
                                    value = formatMoney(currency, currentBalance)
                                )
                                ConfirmSummaryRow(
                                    label = if (isEn) "Balance After" else "Estimasi Sisa Saldo",
                                    value = formatMoney(currency, (currentBalance - selectedPlan.price).coerceAtLeast(0.0)),
                                    highlight = true
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                friendData == null -> {
                    Button(
                        onClick = { onLookupFriend(friendUsername.trim()) },
                        enabled = !isCheckingFriend && friendUsername.trim().isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCheckingFriend) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (isEn) "Check Account" else "Cek Akun")
                    }
                }
                !isConfirmStep -> {
                    Button(
                        onClick = { isConfirmStep = true },
                        enabled = canAffordSelected,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEn) "Continue" else "Lanjut Konfirmasi")
                    }
                }
                selectedPlan != null -> {
                    Button(
                        onClick = { onConfirm(friendData, selectedPlan) },
                        enabled = !isSubmitting && canAffordSelected,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (isEn) "Approve & Activate" else "Setuju & Aktifkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            when {
                friendData == null -> {
                    OutlinedButton(
                        onClick = {
                            onResetLookup()
                            onDismiss()
                        },
                        enabled = !isCheckingFriend,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                }
                !isConfirmStep -> {
                    OutlinedButton(
                        onClick = onResetLookup,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEn) "Change Friend" else "Ganti Teman")
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = { isConfirmStep = false },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEn) "Back" else "Kembali")
                    }
                }
            }
        }
    )
}

@Composable
private fun ConfirmSummaryRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.25f)
        )
    }
}
