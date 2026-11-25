package com.phpnuxbill.mobile.feature.portal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.FriendPlanLookupResult

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

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                    // TAHAP 1: Input Username Tujuan & Nominal
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
                            if (lookupState is UiState.Error) onResetLookup()
                        },
                        label = { Text(if (isEn) "Recipient Username" else "Username Tujuan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isEn) "Amount ($currency)" else "Nominal ($currency)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isInputValid && !isCheckingFriend) {
                                    onLookupFriend(targetUser.trim())
                                }
                            }
                        ),
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

                    if (lookupState is UiState.Error) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = lookupState.message,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                } else {
                    // TAHAP 2: Konfirmasi Detail Penerima & Saldo
                    Text(
                        text = if (isEn) "Please verify the transfer details below before approving:"
                        else "Pastikan rincian transfer berikut sudah benar sebelum disetujui:",
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
                    onClick = onDismiss,
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

    var usernameInput by remember { mutableStateOf("") }
    var selectedPlanId by remember { mutableIntStateOf(0) }
    var isConfirmStep by remember { mutableStateOf(false) }

    val verifiedFriend = (lookupState as? UiState.Success)?.data

    // Otomatis pilih paket terakhir/aktif teman jika saldo mencukupi begitu akun ditemukan
    LaunchedEffect(verifiedFriend) {
        isConfirmStep = false
        if (verifiedFriend == null) {
            selectedPlanId = 0
            return@LaunchedEffect
        }
        val lp = verifiedFriend.lastPlan
        selectedPlanId = when {
            lp != null && currentBalance >= lp.price -> lp.id
            !verifiedFriend.hasActivePlan -> verifiedFriend.eligiblePlans.firstOrNull { currentBalance >= it.price }?.id ?: 0
            else -> 0
        }
    }

    val allCandidatePlans = remember(verifiedFriend) {
        if (verifiedFriend == null) emptyList()
        else buildList {
            verifiedFriend.lastPlan?.let { add(it) }
            verifiedFriend.eligiblePlans.forEach { p ->
                if (none { it.id == p.id }) add(p)
            }
        }
    }
    val pickedPlan = allCandidatePlans.find { it.id == selectedPlanId }
    val canProceed = verifiedFriend != null &&
        pickedPlan != null &&
        currentBalance >= pickedPlan.price

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                text = if (isConfirmStep) {
                    if (isEn) "Confirm Friend's Plan" else "Konfirmasi Beli Paket Teman"
                } else {
                    if (isEn) "Buy Plan for Friend" else "Belikan Paket Teman"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Saldo Pengirim (tampil pada Tahap 1 & Tahap 2)
                if (!isConfirmStep) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEn) "Your Balance" else "Saldo Anda",
                                style = MaterialTheme.typography.labelMedium,
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
                }

                when {
                    // =========================================================
                    // TAHAP 1: CARI & VERIFIKASI USERNAME TEMAN
                    // =========================================================
                    verifiedFriend == null -> {
                        Text(
                            text = if (isEn) "Step 1: Enter your friend's username to check their account and eligible plans."
                            else "Langkah 1: Masukkan username teman untuk mengecek akun dan paket yang sesuai.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = {
                                    usernameInput = it
                                    if (lookupState is UiState.Error) onResetLookup()
                                },
                                label = { Text(if (isEn) "Friend's Username" else "Username Teman") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (usernameInput.isNotBlank() && lookupState !is UiState.Loading) {
                                            onLookupFriend(usernameInput)
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onLookupFriend(usernameInput) },
                                enabled = usernameInput.isNotBlank() && lookupState !is UiState.Loading,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                if (lookupState is UiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isEn) "Check" else "Cek")
                                }
                            }
                        }

                        if (lookupState is UiState.Error) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = lookupState.message,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================
                    // TAHAP 2: AKUN TERVERIFIKASI -> PILIH PAKET SESUAI AKUN
                    // =========================================================
                    !isConfirmStep -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = verifiedFriend.fullname,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@${verifiedFriend.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        StatusBadge(text = verifiedFriend.serviceType.uppercase(), tone = BadgeTone.NEUTRAL)
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        selectedPlanId = 0
                                        onResetLookup()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isEn) "Change" else "Ganti")
                                }
                            }
                        }

                        // 2A. Paket Terakhir / Aktif Milik Teman
                        if (verifiedFriend.lastPlan != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (verifiedFriend.hasActivePlan) {
                                        if (isEn) "Friend's Active Plan (Renew)" else "Paket Aktif Teman (Perpanjang)"
                                    } else {
                                        if (isEn) "Friend's Last Plan" else "Paket Terakhir Teman"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            FriendPlanOptionRow(
                                plan = verifiedFriend.lastPlan,
                                currency = currency,
                                currentBalance = currentBalance,
                                isSelected = selectedPlanId == verifiedFriend.lastPlan.id,
                                isEn = isEn,
                                onSelect = { selectedPlanId = verifiedFriend.lastPlan.id }
                            )

                            if (verifiedFriend.hasActivePlan) {
                                Text(
                                    text = if (isEn) {
                                        "This account currently has an active package (${verifiedFriend.activePlanName}). You can only renew the same package."
                                    } else {
                                        "Akun teman sedang memiliki paket aktif (${verifiedFriend.activePlanName}). Pembelian hanya dapat memperpanjang paket yang sama."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 2B. Daftar Paket Sesuai Jenis Pelanggan Teman (Jika tidak terkunci oleh paket aktif)
                        if (!verifiedFriend.hasActivePlan) {
                            val otherPlans = remember(verifiedFriend) {
                                verifiedFriend.eligiblePlans.filter { it.id != verifiedFriend.lastPlan?.id }
                            }
                            if (otherPlans.isNotEmpty()) {
                                if (verifiedFriend.lastPlan != null) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                Text(
                                    text = if (isEn) "Available ${verifiedFriend.serviceType} Plans"
                                    else "Pilihan Paket ${verifiedFriend.serviceType}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                otherPlans.forEach { p ->
                                    FriendPlanOptionRow(
                                        plan = p,
                                        currency = currency,
                                        currentBalance = currentBalance,
                                        isSelected = selectedPlanId == p.id,
                                        isEn = isEn,
                                        onSelect = { selectedPlanId = p.id }
                                    )
                                }
                            } else if (verifiedFriend.lastPlan == null) {
                                Text(
                                    text = if (isEn) "No eligible plans available for this account type."
                                    else "Belum ada paket yang tersedia untuk tipe akun ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // =========================================================
                    // TAHAP 3: KONFIRMASI DETAIL SEBELUM AKTIVASI PAKET TEMAN
                    // =========================================================
                    pickedPlan != null -> {
                        Text(
                            text = if (isEn) "Please verify the package purchase details for your friend:"
                            else "Pastikan rincian pembelian paket untuk teman berikut sudah sesuai:",
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
                                    label = if (isEn) "Selected Plan" else "Paket Dipilih",
                                    value = pickedPlan.namePlan,
                                    highlight = true
                                )
                                if (pickedPlan.validity.isNotBlank()) {
                                    ConfirmSummaryRow(
                                        label = if (isEn) "Validity" else "Masa Aktif",
                                        value = pickedPlan.validity
                                    )
                                }
                                if (pickedPlan.bandwidth.isNotBlank()) {
                                    ConfirmSummaryRow(
                                        label = "Bandwidth",
                                        value = pickedPlan.bandwidth
                                    )
                                }
                                ConfirmSummaryRow(
                                    label = if (isEn) "Price" else "Harga Paket",
                                    value = formatMoney(currency, pickedPlan.price),
                                    highlight = true
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                ConfirmSummaryRow(
                                    label = if (isEn) "Your Balance" else "Saldo Saat Ini",
                                    value = formatMoney(currency, currentBalance)
                                )
                                ConfirmSummaryRow(
                                    label = if (isEn) "Balance After" else "Estimasi Sisa Saldo",
                                    value = formatMoney(currency, (currentBalance - pickedPlan.price).coerceAtLeast(0.0)),
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
                verifiedFriend != null && !isConfirmStep -> {
                    Button(
                        onClick = { isConfirmStep = true },
                        enabled = canProceed && !isSubmitting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEn) "Continue" else "Lanjut Konfirmasi")
                    }
                }
                verifiedFriend != null && isConfirmStep && pickedPlan != null -> {
                    Button(
                        onClick = { onConfirm(verifiedFriend, pickedPlan) },
                        enabled = canProceed && !isSubmitting,
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
            if (isConfirmStep) {
                OutlinedButton(
                    onClick = { isConfirmStep = false },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEn) "Back" else "Kembali")
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.cancel)
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

@Composable
private fun FriendPlanOptionRow(
    plan: CustomerBuyPlanItem,
    currency: String,
    currentBalance: Double,
    isSelected: Boolean,
    isEn: Boolean,
    onSelect: () -> Unit
) {
    val canAfford = currentBalance >= plan.price
    val deficit = (plan.price - currentBalance).coerceAtLeast(0.0)
    val activeSelected = isSelected && canAfford

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (activeSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = if (activeSelected) 1.5.dp else 1.dp,
            color = if (activeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canAfford) 1f else 0.55f)
            .clickable(enabled = canAfford, onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = plan.namePlan,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (activeSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = listOf(plan.type, plan.bandwidth, plan.validity)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!canAfford) {
                    Text(
                        text = if (isEn) "Insufficient balance (short ${formatMoney(currency, deficit)})"
                        else "Saldo kurang ${formatMoney(currency, deficit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(currency, plan.price),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (activeSelected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
