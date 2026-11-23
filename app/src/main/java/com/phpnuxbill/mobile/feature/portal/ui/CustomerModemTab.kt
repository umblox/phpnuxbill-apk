package com.phpnuxbill.mobile.feature.portal.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.portal.model.CustomerConnectedHost
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemInfo
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemStatusResult
import com.phpnuxbill.mobile.feature.portal.model.CustomerWlanItem
import com.phpnuxbill.mobile.feature.portal.model.ModemSupportStatus

@Composable
fun CustomerModemTabContent(
    result: CustomerModemStatusResult,
    companyPhone: String,
    isSubmitting: Boolean,
    isEn: Boolean,
    onSaveWifi: (wlanIndex: Int, ssid: String, password: String, source: String, onDone: () -> Unit) -> Unit,
    onSummon: () -> Unit,
    onReboot: (onDone: () -> Unit) -> Unit,
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
                CustomerModemOnlineSection(
                    modem = modem,
                    isSubmitting = isSubmitting,
                    isEn = isEn,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                            text = "${modem.model} • ${modem.deviceType}",
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
                                    onClick = { launchCsWhatsApp(context, companyPhone) },
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
                                    onClick = { launchCsWhatsApp(context, companyPhone) },
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
private fun CustomerModemOnlineSection(
    modem: CustomerModemInfo,
    isSubmitting: Boolean,
    isEn: Boolean,
    onSaveWifi: (wlanIndex: Int, ssid: String, password: String, source: String, onDone: () -> Unit) -> Unit,
    onSummon: () -> Unit,
    onReboot: (onDone: () -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    var showHostsDialog by remember { mutableStateOf(false) }
    var showRebootConfirm by remember { mutableStateOf(false) }
    var editingWlan by remember { mutableStateOf<CustomerWlanItem?>(null) }
    var pendingWifiConfirm by remember { mutableStateOf<PendingWifiUpdate?>(null) }

    // 1. Dialog Daftar Perangkat Terhubung (Tersaring & Valid)
    if (showHostsDialog) {
        ConnectedHostsDialog(
            hosts = modem.connectedHosts,
            reportedCount = modem.connectedDevices,
            isEn = isEn,
            onDismiss = { showHostsDialog = false }
        )
    }

    // 2. Dialog Konfirmasi Reboot Modem
    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showRebootConfirm = false },
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
            title = {
                Text(
                    text = if (isEn) "Restart Modem?" else "Restart Modem?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "Your internet and WiFi connection will be temporarily disconnected for 1–2 minutes while the modem restarts."
                    } else {
                        "Koneksi internet dan WiFi akan terputus sementara selama 1–2 menit saat modem menyala ulang. Lanjutkan?"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { onReboot { showRebootConfirm = false } },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text(if (isEn) "Yes, Restart" else "Ya, Restart")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRebootConfirm = false },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 3. Tahap 1 Alur Ubah WiFi: Form Input SSID & Password Baru
    if (editingWlan != null) {
        EditWifiInputDialog(
            wlan = editingWlan!!,
            isEn = isEn,
            onDismiss = { editingWlan = null },
            onProceedToConfirm = { newSsid, newPass ->
                val target = editingWlan!!
                editingWlan = null
                pendingWifiConfirm = PendingWifiUpdate(
                    wlanIndex = target.index,
                    oldSsid = target.ssid,
                    newSsid = newSsid,
                    newPassword = newPass,
                    source = target.source
                )
            }
        )
    }

    // 4. Tahap 2 Alur Ubah WiFi: Dialog Konfirmasi Akhir Sebelum Kirim ke Server
    if (pendingWifiConfirm != null) {
        val pending = pendingWifiConfirm!!
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) pendingWifiConfirm = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            title = {
                Text(
                    text = if (isEn) "Confirm WiFi Changes" else "Konfirmasi Perubahan WiFi",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEn) {
                            "All devices currently connected to WiFi will disconnect and need to reconnect with the updated settings."
                        } else {
                            "Semua perangkat yang terhubung ke WiFi saat ini akan terputus sementara dan perlu menyambung ulang menggunakan pengaturan baru."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SSID: ${pending.newSsid}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (pending.newPassword.isNotBlank()) {
                                    if (isEn) "Password: Updated (hidden for security)" else "Password: Diperbarui (disembunyikan)"
                                } else {
                                    if (isEn) "Password: Kept unchanged" else "Password: Tidak diubah"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveWifi(
                            pending.wlanIndex,
                            pending.newSsid,
                            pending.newPassword,
                            pending.source
                        ) {
                            pendingWifiConfirm = null
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isEn) "Apply Changes" else "Ya, Terapkan")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingWifiConfirm = null },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // =========================================================================
    // KARTU 1: RINGKASAN STATUS & KESEHATAN PERANGKAT (AMAN TANPA IP ISP/SN)
    // =========================================================================
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = modem.model,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = modem.deviceType,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(text = strings.online, tone = BadgeTone.SUCCESS)
            }

            // Grid Metrik Aman: Status Internet + Uptime (+ Optik & Suhu jika ONT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val inetConnected = modem.internetStatus.equals("Connected", ignoreCase = true)
                ModemMetricTile(
                    icon = Icons.Outlined.Public,
                    label = if (isEn) "Internet" else "Koneksi Internet",
                    value = if (inetConnected) {
                        if (isEn) "Connected" else "Terhubung"
                    } else {
                        if (isEn) "Disconnected" else "Terputus"
                    },
                    isHealthy = inetConnected,
                    modifier = Modifier.weight(1f)
                )
                ModemMetricTile(
                    icon = Icons.Outlined.Schedule,
                    label = if (isEn) "Uptime" else "Waktu Menyala",
                    value = modem.uptime,
                    isHealthy = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (modem.isOptical || (modem.temperature.isNotBlank() && modem.temperature != "N/A")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (modem.isOptical) {
                        val rxHealthy = modem.rxPowerStatus.lowercase() !in listOf("critical", "los", "warning")
                        val rxLabel = when (modem.rxPowerStatus.lowercase()) {
                            "los" -> if (isEn) "No Signal (LOS)" else "Putus (LOS)"
                            "critical" -> "${modem.rxPower} (${if (isEn) "Weak" else "Lemah"})"
                            "warning" -> "${modem.rxPower} (${if (isEn) "Fair" else "Cukup"})"
                            else -> "${modem.rxPower} (${if (isEn) "Good" else "Baik"})"
                        }
                        ModemMetricTile(
                            icon = Icons.Outlined.SignalCellularAlt,
                            label = if (isEn) "Optical Signal" else "Sinyal Optik",
                            value = rxLabel,
                            isHealthy = rxHealthy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (modem.temperature.isNotBlank() && modem.temperature != "N/A") {
                        val tempHealthy = !modem.temperatureStatus.equals("critical", ignoreCase = true)
                        ModemMetricTile(
                            icon = Icons.Outlined.Thermostat,
                            label = if (isEn) "Temperature" else "Suhu Modem",
                            value = modem.temperature,
                            isHealthy = tempHealthy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Kartu Klik Interaktif: Perangkat Terhubung Aktif (Tersaring dari 00:00:00:xxx & Lease Mati)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHostsDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${modem.connectedDevices} ${if (isEn) "Active Devices" else "Perangkat Terhubung"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEn) "Tap to inspect connected devices" else "Ketuk untuk melihat daftar perangkat aktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "${if (isEn) "Last synced" else "Sinkron terakhir"}: ${modem.lastInform}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onSummon,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh")
                }
                OutlinedButton(
                    onClick = { showRebootConfirm = true },
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reboot")
                }
            }
        }
    }

    // =========================================================================
    // KARTU 2: JARINGAN WIFI (READ-ONLY AMAN + ALUR UBAH & KONFIRMASI)
    // =========================================================================
    val wlans = modem.wlanConfigs
    if (wlans.isNotEmpty()) {
        Text(
            text = if (isEn) "WiFi Networks" else "Jaringan WiFi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        wlans.forEach { wlan ->
            CustomerWifiSummaryCard(
                wlan = wlan,
                isEn = isEn,
                onEditClick = { editingWlan = wlan }
            )
        }
    }
}

@Composable
private fun ModemMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHealthy) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerWifiSummaryCard(
    wlan: CustomerWlanItem,
    isEn: Boolean,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showPassword by remember(wlan.index) { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wlan.ssid.ifBlank { "WiFi #${wlan.index}" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${wlan.band} • Ch ${wlan.channel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = wlan.security,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                StatusBadge(
                    text = if (wlan.enabled) "ON" else "OFF",
                    tone = if (wlan.enabled) BadgeTone.SUCCESS else BadgeTone.NEUTRAL
                )
            }

            // Baris Password Tersembunyi (Read-Only + Mata + Salin)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "WiFi Password" else "Kata Sandi WiFi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                wlan.password.isBlank() -> if (isEn) "(Protected / Not exposed)" else "(Terproteksi)"
                                showPassword -> wlan.password
                                else -> "••••••••••••"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (wlan.password.isNotBlank()) {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle Password",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(wlan.password))
                                Toast.makeText(
                                    context,
                                    if (isEn) "WiFi password copied" else "Kata sandi WiFi disalin",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onEditClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEn) "Change WiFi Name / Password" else "Ubah Nama / Sandi WiFi")
            }
        }
    }
}

@Composable
private fun EditWifiInputDialog(
    wlan: CustomerWlanItem,
    isEn: Boolean,
    onDismiss: () -> Unit,
    onProceedToConfirm: (ssid: String, password: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var ssidInput by remember(wlan.index) { mutableStateOf(wlan.ssid) }
    var passInput by remember(wlan.index) { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val ssidValid = ssidInput.trim().length in 1..32
    val passValid = passInput.isEmpty() || passInput.trim().length in 8..63
    val hasChanges = ssidInput.trim() != wlan.ssid || passInput.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                text = if (isEn) "Edit WiFi Settings" else "Ubah Pengaturan WiFi",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ssidInput,
                    onValueChange = { if (it.length <= 32) ssidInput = it },
                    label = { Text(if (isEn) "New WiFi Name (SSID)" else "Nama WiFi Baru (SSID)") },
                    singleLine = true,
                    isError = !ssidValid,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passInput,
                    onValueChange = { if (it.length <= 63) passInput = it },
                    label = { Text(if (isEn) "New Password (Optional, min 8)" else "Password Baru (Opsional, min 8)") },
                    placeholder = {
                        Text(if (isEn) "Leave empty to keep current" else "Kosongkan jika tidak diganti")
                    },
                    singleLine = true,
                    isError = !passValid,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                imageVector = if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    supportingText = {
                        if (!passValid) {
                            Text(
                                text = if (isEn) "Minimum 8 characters" else "Minimal 8 karakter",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onProceedToConfirm(ssidInput.trim(), passInput.trim()) },
                enabled = ssidValid && passValid && hasChanges,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEn) "Continue" else "Lanjutkan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun ConnectedHostsDialog(
    hosts: List<CustomerConnectedHost>,
    reportedCount: Int,
    isEn: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                text = if (isEn) "Connected Devices (${hosts.size.coerceAtLeast(reportedCount)})"
                else "Perangkat Terhubung (${hosts.size.coerceAtLeast(reportedCount)})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (hosts.isEmpty()) {
                Text(
                    text = if (reportedCount > 0) {
                        if (isEn) {
                            "$reportedCount device(s) detected via WiFi, but this router model does not report individual device details."
                        } else {
                            "$reportedCount perangkat terdeteksi di WiFi, namun tipe router ini tidak mengirimkan rincian daftar perangkat."
                        }
                    } else {
                        if (isEn) "No active devices currently connected." else "Tidak ada perangkat aktif yang terhubung saat ini."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(hosts, key = { "${it.macAddress}_${it.ipAddress}" }) { host ->
                        val isLan = host.interfaceType.contains("LAN", ignoreCase = true) &&
                            !host.interfaceType.contains("WiFi", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLan) Icons.Outlined.SettingsEthernet else Icons.Outlined.Smartphone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = host.hostname,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "IP: ${host.ipAddress} • ${host.interfaceType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "MAC: ${host.macAddress}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (host.signal.isNotBlank()) {
                                    StatusBadge(text = host.signal, tone = BadgeTone.NEUTRAL)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text(if (isEn) "Close" else "Tutup")
            }
        }
    )
}

private data class PendingWifiUpdate(
    val wlanIndex: Int,
    val oldSsid: String,
    val newSsid: String,
    val newPassword: String,
    val source: String
)

private fun launchCsWhatsApp(context: Context, rawPhone: String) {
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.isBlank()) return
    val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum")))
    }
}
