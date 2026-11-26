package com.phpnuxbill.mobile.feature.portal.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.MaintenanceInfo
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.LanguageToggleChip
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.core.ui.theme.StatusWarningBgLight
import com.phpnuxbill.mobile.core.ui.theme.StatusWarningFgLight
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private data class MaintenanceCountdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isExpired: Boolean
)

@Composable
fun CustomerMaintenanceScreen(
    maintenance: MaintenanceInfo,
    session: SessionState,
    isRechecking: Boolean,
    onRecheck: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBackToLoginOrLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }
    var wasRechecking by remember { mutableStateOf(false) }

    val companyName = maintenance.companyName.ifBlank { session.effectiveCompanyName }
    val logoUrl = maintenance.logoUrl.ifBlank { session.serverLogoUrl }
    val targetMillis = remember(maintenance.maintenanceDate) {
        parseMaintenanceTargetMillis(maintenance.maintenanceDate)
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        if (targetMillis != null) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    // Umpan balik singkat apabila tombol Cek Status Lagi selesai tetapi server masih dalam pemeliharaan
    LaunchedEffect(isRechecking) {
        if (wasRechecking && !isRechecking) {
            delay(120L)
            val msg = if (isEn) {
                "Server is still under maintenance. Please try again later."
            } else {
                "Server masih dalam pemeliharaan. Silakan coba kembali nanti."
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
        wasRechecking = isRechecking
    }

    val countdown = remember(targetMillis, nowMillis) {
        targetMillis?.let { computeCountdown(it, nowMillis) }
    }

    // Jika pelanggan sudah login (forceLogout = false), tombol Back tidak boleh menerobos ke Portal;
    // jika belum login (dari halaman Login), tombol Back kembali ke Login agar dapat masuk sebagai Admin / ganti server.
    BackHandler(enabled = true) {
        if (!session.isLoggedIn) {
            onBackToLoginOrLogout()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressedAt <= 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressedAt = now
                val exitMsg = if (isEn) "Press back again to exit" else "Tekan kembali sekali lagi untuk keluar"
                Toast.makeText(context, exitMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Gradient selaras identitas aplikasi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BrandNavy900, BrandBlue600)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Baris Atas: Toggle Bahasa ID | EN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageToggleChip(
                    currentLanguageCode = session.languageCode,
                    onLanguageSelected = onLanguageSelected
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BrandLogo(
                customLocalLogoPath = session.customLocalLogoPath,
                serverLogoUrl = logoUrl,
                size = 72.dp,
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = companyName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Kartu Utama Maintenance (selaras ui/ui/admin/maintenance.tpl)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animasi 3 Roda Gigi Berputar simetris & kontras di Light/Dark Mode
                    AnimatedMaintenanceGears()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEn) {
                            "Site is Temporarily Unavailable"
                        } else {
                            "Layanan Sedang Dalam Pemeliharaan"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = maintenance.message.ifBlank {
                            if (isEn) {
                                "Scheduled maintenance is currently in progress. Please check back soon."
                            } else {
                                "Pemeliharaan sistem terjadwal sedang berlangsung. Silakan coba kembali beberapa saat lagi."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isEn) {
                            "We apologize for any inconvenience.\n— The $companyName Team."
                        } else {
                            "Mohon maaf atas ketidaknyamanannya.\n— Tim $companyName."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    // 4 Kotak Hitung Mundur Real-Time (jika maintenance_date terisi)
                    if (countdown != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountdownUnitTile(
                                value = countdown.days,
                                label = if (isEn) "Days" else "Hari",
                                containerColor = Color(0xFF1ABC9C),
                                modifier = Modifier.weight(1f)
                            )
                            CountdownUnitTile(
                                value = countdown.hours,
                                label = if (isEn) "Hours" else "Jam",
                                containerColor = Color(0xFF3498DB),
                                modifier = Modifier.weight(1f)
                            )
                            CountdownUnitTile(
                                value = countdown.minutes,
                                label = if (isEn) "Minutes" else "Menit",
                                containerColor = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            CountdownUnitTile(
                                value = countdown.seconds,
                                label = if (isEn) "Seconds" else "Detik",
                                containerColor = Color(0xFFE74C3C),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (countdown.isExpired) {
                                        if (isEn) {
                                            "Estimated time reached (${maintenance.maintenanceDate}). Tap Check Status."
                                        } else {
                                            "Estimasi waktu tercapai (${maintenance.maintenanceDate}). Ketuk Cek Status."
                                        }
                                    } else {
                                        if (isEn) {
                                            "Estimated end: ${maintenance.maintenanceDate}"
                                        } else {
                                            "Estimasi selesai: ${maintenance.maintenanceDate}"
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Info Status Sesi (Force Logout vs Sesi Tersimpan)
                    if (maintenance.forceLogout || session.isLoggedIn) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val isForced = maintenance.forceLogout
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isForced) StatusWarningBgLight else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isForced) StatusWarningFgLight.copy(alpha = 0.25f) else BrandTeal500.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = if (isForced) StatusWarningFgLight else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isForced) {
                                        if (isEn) {
                                            "Your session was signed out for security during maintenance. Please sign in again once service resumes."
                                        } else {
                                            "Sesi Anda telah diakhiri demi keamanan selama pemeliharaan. Silakan login kembali setelah layanan aktif."
                                        }
                                    } else {
                                        if (isEn) {
                                            "Your session remains saved. Tap Check Status once maintenance finishes to resume without signing in again."
                                        } else {
                                            "Sesi login Anda tetap tersimpan. Ketuk Cek Status setelah pemeliharaan selesai untuk langsung masuk kembali."
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isForced) StatusWarningFgLight else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tombol 1: Cek Status Lagi (Refresh)
                    Button(
                        onClick = onRecheck,
                        enabled = !isRechecking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isRechecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isEn) "Check Status Again" else "Cek Status Lagi",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Tombol 2: Hubungi CS WhatsApp (bila nomor perusahaan tersedia)
                    if (maintenance.companyPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                val waDigits = formatPhoneForWhatsApp(maintenance.companyPhone)
                                if (waDigits.isNotBlank()) {
                                    val greeting = if (isEn) {
                                        "Hello $companyName Support, I would like to ask about the ongoing maintenance."
                                    } else {
                                        "Halo CS $companyName, saya ingin menanyakan informasi pemeliharaan layanan."
                                    }
                                    val waUri = Uri.Builder()
                                        .scheme("https")
                                        .authority("wa.me")
                                        .appendPath(waDigits)
                                        .appendQueryParameter("text", greeting)
                                        .build()
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            if (isEn) "Unable to open WhatsApp." else "Tidak dapat membuka WhatsApp.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Contact Support (WhatsApp)" else "Hubungi CS via WhatsApp",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tombol 3: Keluar / Ganti Akun atau Kembali ke Login
                    OutlinedButton(
                        onClick = onBackToLoginOrLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isLoggedIn) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.AutoMirrored.Outlined.ArrowBack
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (session.isLoggedIn) {
                                if (isEn) "Sign Out / Switch Account" else "Keluar / Ganti Akun"
                            } else {
                                if (isEn) "Back to Sign In" else "Kembali ke Halaman Login"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedMaintenanceGears() {
    val infiniteTransition = rememberInfiniteTransition(label = "maintenance_gears")
    val spinCw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_cw"
    )
    val spinCcw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_ccw"
    )

    Box(
        modifier = Modifier
            .width(172.dp)
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Roda gigi kiri atas (adaptif Light/Dark Mode, berlawanan arah jarum jam)
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .offset(x = (-48).dp, y = (-14).dp)
                .size(44.dp)
                .rotate(spinCcw)
        )
        // Roda gigi tengah utama (oranye #F6921E sesuai maintenance.tpl resmi)
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            tint = Color(0xFFF6921E),
            modifier = Modifier
                .size(62.dp)
                .rotate(spinCw)
        )
        // Roda gigi kanan bawah (adaptif Light/Dark Mode, berlawanan arah jarum jam)
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .offset(x = 48.dp, y = 14.dp)
                .size(44.dp)
                .rotate(spinCcw)
        )
    }
}

@Composable
private fun CountdownUnitTile(
    value: Long,
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun parseMaintenanceTargetMillis(rawDate: String): Long? {
    val clean = rawDate.trim()
    if (clean.isBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )
    for (pattern in patterns) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }.parse(clean)?.time
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

private fun computeCountdown(targetMillis: Long, nowMillis: Long): MaintenanceCountdown {
    val diff = targetMillis - nowMillis
    if (diff <= 0L) {
        return MaintenanceCountdown(
            days = 0L,
            hours = 0L,
            minutes = 0L,
            seconds = 0L,
            isExpired = true
        )
    }
    val totalSeconds = diff / 1000L
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return MaintenanceCountdown(
        days = days,
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        isExpired = false
    )
}

private fun formatPhoneForWhatsApp(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.isBlank()) return ""
    return when {
        digits.startsWith("0") -> "62${digits.drop(1)}"
        else -> digits
    }
}