@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.portal.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerInboxItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun CustomerAvatarCircle(
    photoUrl: String,
    fullname: String,
    size: Dp = 36.dp,
    onClick: (() -> Unit)? = null
) {
    val initials = remember(fullname) {
        fullname.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "U" }
    }

    val mod = Modifier
        .size(size)
        .clip(CircleShape)
        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Box(modifier = mod, contentAlignment = Alignment.Center) {
        if (photoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = fullname,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                error = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CustomerProfileScreen(
    dashboard: CustomerPortalDashboard,
    session: SessionState,
    isSubmitting: Boolean,
    onSaveProfile: (fullname: String, address: String, phone: String, email: String, photoBase64: String) -> Unit,
    onChangePassword: (oldPass: String, newPass: String, confirmPass: String, onDone: () -> Unit) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullname by remember(dashboard.fullname) { mutableStateOf(dashboard.fullname) }
    var address by remember(dashboard.customerAddress) { mutableStateOf(dashboard.customerAddress) }
    var phone by remember(dashboard.customerPhone) { mutableStateOf(dashboard.customerPhone) }
    var email by remember(dashboard.customerEmail) { mutableStateOf(dashboard.customerEmail) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoBase64 by remember { mutableStateOf("") }

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingPhotoUri = uri
            scope.launch {
                pendingPhotoBase64 = encodeImageUriToBase64(context, uri).orEmpty()
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(strings.logoutConfirmTitle) },
            text = { Text(strings.logoutConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(strings.logoutButton, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    AppScreenScaffold(
        title = if (isEn) "My Account" else "Akun Saya",
        subtitle = "@${dashboard.username} • ${dashboard.serviceType}",
        onBack = onBack
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Foto Profil Asli Web + Tombol Ganti Foto dari Galeri
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (pendingPhotoUri != null) {
                            SubcomposeAsyncImage(
                                model = pendingPhotoUri,
                                contentDescription = fullname,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        } else {
                            CustomerAvatarCircle(
                                photoUrl = dashboard.photoUrl,
                                fullname = dashboard.fullname,
                                size = 92.dp,
                                onClick = { photoPicker.launch("image/*") }
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { photoPicker.launch("image/*") }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = "Change Photo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = dashboard.fullname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${dashboard.username} • ${formatMoney(dashboard.currency, dashboard.balance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pendingPhotoBase64.isNotBlank()) {
                        Text(
                            text = if (isEn) "New photo ready — tap Save Profile below" else "Foto baru siap — ketuk Simpan Profil di bawah",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. Edit Data Profil
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isEn) "Profile Details" else "Data Profil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = fullname,
                        onValueChange = { fullname = it },
                        label = { Text(if (isEn) "Full Name" else "Nama Lengkap") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (!dashboard.allowPhoneOtp) phone = it },
                        readOnly = dashboard.allowPhoneOtp,
                        label = {
                            Text(
                                if (dashboard.allowPhoneOtp) {
                                    if (isEn) "Phone Number (OTP Protected)" else "Nomor HP (Terkunci OTP)"
                                } else {
                                    if (isEn) "Phone Number" else "Nomor HP"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { if (!dashboard.allowEmailOtp) email = it },
                        readOnly = dashboard.allowEmailOtp,
                        label = {
                            Text(
                                if (dashboard.allowEmailOtp) {
                                    if (isEn) "Email (OTP Protected)" else "Email (Terkunci OTP)"
                                } else {
                                    "Email"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isEn) "Address" else "Alamat") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveProfile(fullname, address, phone, email, pendingPhotoBase64)
                            pendingPhotoBase64 = ""
                        },
                        enabled = !isSubmitting && fullname.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isEn) "Save Profile" else "Simpan Profil")
                        }
                    }
                }
            }

            // 3. Ganti Kata Sandi (+ Sinkron ke Router)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isEn) "Change Password" else "Ganti Kata Sandi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text(if (isEn) "Current Password" else "Kata Sandi Saat Ini") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text(if (isEn) "New Password" else "Kata Sandi Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text(if (isEn) "Confirm New Password" else "Ulangi Kata Sandi Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        onClick = {
                            onChangePassword(oldPass, newPass, confirmPass) {
                                oldPass = ""
                                newPass = ""
                                confirmPass = ""
                            }
                        },
                        enabled = !isSubmitting && oldPass.isNotBlank() && newPass.length >= 4 && newPass == confirmPass,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEn) "Update Password" else "Perbarui Kata Sandi")
                    }
                }
            }

            // 4. Preferensi Aplikasi (Bahasa & Tema)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (isEn) "Language" else "Bahasa Aplikasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("id" to "Indonesia", "en" to "English").forEachIndexed { idx, (code, label) ->
                            SegmentedButton(
                                selected = session.languageCode == code,
                                onClick = { onLanguageSelected(code) },
                                shape = SegmentedButtonDefaults.itemShape(idx, 2),
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (isEn) "Appearance" else "Tema Tampilan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val themes = listOf(
                            AppThemeMode.SYSTEM to (if (isEn) "System" else "Sistem"),
                            AppThemeMode.LIGHT to (if (isEn) "Light" else "Terang"),
                            AppThemeMode.DARK to (if (isEn) "Dark" else "Gelap")
                        )
                        themes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = session.themeMode == mode,
                                onClick = { onThemeSelected(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            // 5. Tombol Keluar (Logout)
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.logoutButton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomerInboxScreen(
    inboxState: UiState<List<CustomerInboxItem>>,
    onRefresh: () -> Unit,
    onMarkRead: (CustomerInboxItem) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var expandedId by remember { mutableIntStateOf(-1) }

    AppScreenScaffold(
        title = if (isEn) "Inbox" else "Kotak Masuk",
        onBack = onBack,
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
            }
        }
    ) { pad ->
        when (inboxState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView(modifier = Modifier.padding(pad))
            is UiState.Error -> ErrorStateView(message = inboxState.message, onRetry = onRefresh, modifier = Modifier.padding(pad))
            is UiState.Success -> {
                val list = inboxState.data
                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "No messages in your inbox yet." else "Belum ada pesan masuk.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.id }) { mail ->
                            val isExpanded = expandedId == mail.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (!mail.isRead) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = if (!mail.isRead) 1.5.dp else 1.dp,
                                    color = if (!mail.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedId = if (isExpanded) -1 else mail.id
                                        onMarkRead(mail)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (mail.isRead) Icons.Outlined.Drafts else Icons.Outlined.MarkEmailUnread,
                                            contentDescription = null,
                                            tint = if (mail.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mail.subject,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (mail.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                                                maxLines = if (isExpanded) 3 else 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${mail.from} • ${mail.dateCreated}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDelete(mail.id) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = strings.delete,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = mail.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                        overflow = TextOverflow.Ellipsis
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
fun TransferBalanceDialog(
    currency: String,
    currentBalance: Double,
    minimumTransfer: Double,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (username: String, amount: Double) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var targetUser by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = targetUser.isNotBlank() &&
        parsedAmount > 0.0 &&
        parsedAmount <= currentBalance &&
        (minimumTransfer <= 0.0 || parsedAmount >= minimumTransfer)

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(
                text = if (isEn) "Transfer Balance" else "Transfer Saldo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                text = "${if (isEn) "Min. transfer" else "Min. transfer"}: ${formatMoney(currency, minimumTransfer)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetUser,
                    onValueChange = { targetUser = it },
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
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(targetUser, parsedAmount) },
                enabled = !isSubmitting && isValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEn) "Send" else "Kirim")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun SendPlanToFriendDialog(
    currency: String,
    currentBalance: Double,
    availablePlans: List<CustomerBuyPlanItem>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (username: String, planId: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var friendUsername by remember { mutableStateOf("") }
    var selectedPlanId by remember { mutableIntStateOf(0) } // 0 = otomatis paket teman

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        title = {
            Text(
                text = if (isEn) "Buy Plan for Friend" else "Belikan Paket Teman",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isEn) "Paid via Account Balance" else "Dibayar Memakai Saldo",
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
                    onValueChange = { friendUsername = it },
                    label = { Text(if (isEn) "Friend's Username" else "Username Teman") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isEn) "Choose package (leave Auto to renew friend's current plan):"
                    else "Pilih paket (biarkan Otomatis untuk memperpanjang paket teman):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val autoSelected = selectedPlanId == 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (autoSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        width = if (autoSelected) 1.5.dp else 1.dp,
                        color = if (autoSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPlanId = 0 }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) "Auto (Friend's last package)" else "Otomatis (Paket terakhir teman)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (autoSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (autoSelected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                availablePlans.forEach { p ->
                    val canAfford = currentBalance >= p.price
                    val picked = selectedPlanId == p.id && canAfford
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (picked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            width = if (picked) 1.5.dp else 1.dp,
                            color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canAfford) { selectedPlanId = p.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.namePlan,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (canAfford) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (p.validity.isNotBlank()) {
                                    Text(
                                        text = p.validity,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatMoney(currency, p.price),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(friendUsername, selectedPlanId) },
                enabled = !isSubmitting && friendUsername.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEn) "Send Plan" else "Kirim Paket")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.cancel)
            }
        }
    )
}

private suspend fun encodeImageUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return@runCatching null
        val maxDim = 600
        val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height, 1f)
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else decoded
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()
}
