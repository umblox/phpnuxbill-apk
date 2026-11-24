@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.BuildConfig
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.LanguageToggleChip
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDashboardLabels

@Composable
fun AdminAccountScreen(
    session: SessionState,
    adminPhotoUrl: String,
    isChangingPassword: Boolean,
    bannerMessage: String?,
    onClearBanner: () -> Unit,
    onChangePassword: (current: String, newPass: String, confirm: String, onDone: () -> Unit) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { AdminDashboardLabels(strings) }
    val level = remember(session.userType) { AdminLevel.fromUserType(session.userType) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val passLengthValid = newPass.length in 6..15
    val passMatch = newPass.isNotBlank() && newPass == confirmPass
    val canSubmitPass = currPass.isNotBlank() && passLengthValid && passMatch && !isChangingPassword

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onClearBanner()
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text(strings.logoutConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.logoutConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) {
                    Text(strings.logoutButton, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    AppScreenScaffold(
        title = if (isEn) "My Account" else "Akun Saya",
        subtitle = "@${session.username} • ${level.displayLabel(isEn)}",
        onBack = onBack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Kartu Hero Profil Admin & Hak Akses
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AdminAvatarCircle(
                                photoUrl = adminPhotoUrl,
                                fullname = session.fullname.ifBlank { session.username },
                                size = 60.dp
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = session.fullname.ifBlank { session.username },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${session.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF5EEAD4).copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = level.displayLabel(isEn).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF5EEAD4),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.18f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = session.effectiveCompanyName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = session.activeServerUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Kartu Preferensi Aplikasi (Bahasa & Tema — tersedia untuk semua level admin)
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
                            text = strings.appPreferences,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.languageSetting, style = MaterialTheme.typography.bodyMedium)
                            LanguageToggleChip(
                                currentLanguageCode = session.languageCode,
                                onLanguageSelected = onLanguageSelected
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(labels.appearanceLabel, style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val themes = listOf(
                                    AppThemeMode.SYSTEM to labels.themeSystem,
                                    AppThemeMode.LIGHT to labels.themeLight,
                                    AppThemeMode.DARK to labels.themeDark
                                )
                                themes.forEachIndexed { idx, (mode, title) ->
                                    SegmentedButton(
                                        selected = session.themeMode == mode,
                                        onClick = { onThemeSelected(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                                        icon = {},
                                        label = { Text(title, maxLines = 1) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Kartu Ganti Kata Sandi Admin (endpoint admin_change_password)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isEn) "Change Password" else "Ganti Kata Sandi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showPasswords = !showPasswords }) {
                                Icon(
                                    imageVector = if (showPasswords) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle"
                                )
                            }
                        }

                        val vis = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()

                        OutlinedTextField(
                            value = currPass,
                            onValueChange = { currPass = it },
                            label = { Text(if (isEn) "Current Password" else "Kata Sandi Saat Ini") },
                            visualTransformation = vis,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { if (it.length <= 15) newPass = it },
                            label = { Text(if (isEn) "New Password (6–15 chars)" else "Kata Sandi Baru (6–15 karakter)") },
                            visualTransformation = vis,
                            isError = newPass.isNotEmpty() && !passLengthValid,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { if (it.length <= 15) confirmPass = it },
                            label = { Text(if (isEn) "Confirm New Password" else "Konfirmasi Kata Sandi Baru") },
                            visualTransformation = vis,
                            isError = confirmPass.isNotEmpty() && !passMatch,
                            supportingText = {
                                if (confirmPass.isNotEmpty() && !passMatch) {
                                    Text(
                                        text = if (isEn) "Passwords do not match" else "Konfirmasi kata sandi tidak cocok",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                onChangePassword(currPass, newPass, confirmPass) {
                                    currPass = ""
                                    newPass = ""
                                    confirmPass = ""
                                }
                            },
                            enabled = canSubmitPass,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isChangingPassword) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isEn) "Update Password" else "Simpan Kata Sandi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. Tombol Keluar (Logout)
                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.logoutButton, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
