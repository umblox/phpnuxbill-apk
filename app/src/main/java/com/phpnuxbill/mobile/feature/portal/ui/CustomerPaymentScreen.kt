@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.portal.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBillItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomerPaymentScreen(
    bill: CustomerBillItem,
    isChecking: Boolean,
    onCheckStatus: (silent: Boolean) -> Unit,
    onCancelBill: () -> Unit,
    onBackToHome: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasOpenedExternalApp by remember(bill.id) { mutableStateOf(false) }
    var isSavingQr by remember { mutableStateOf(false) }

    val isPaid = bill.status == 2
    val isUnpaid = bill.status == 1

    // 1. Auto-Check instan saat user kembali (ON_RESUME) dari aplikasi Bank / E-Wallet / Browser
    DisposableEffect(lifecycleOwner, bill.id, isUnpaid) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isUnpaid) {
                onCheckStatus(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 2. Auto-Polling otomatis tiap 6 detik selama status masih UNPAID (krusial utk Tripay Proxy, ViaQRIS, PayPal)
    LaunchedEffect(bill.id, isUnpaid, bill.autoPoll) {
        if (!isUnpaid || !bill.autoPoll) return@LaunchedEffect
        while (true) {
            delay(6_000L)
            onCheckStatus(true)
        }
    }

    // Render QR lokal/offline terlebih dahulu jika qrisString tersedia, atau dekode Base64 jika data:image
    val localQrBitmap = remember(bill.qrisString, bill.qrisImage) {
        when {
            bill.qrisString.isNotBlank() -> PaymentUtils.generateQrBitmap(bill.qrisString, 720)
            bill.qrisImage.startsWith("data:image", ignoreCase = true) -> PaymentUtils.decodeBase64Image(bill.qrisImage)
            else -> null
        }
    }

    val installedTargetBank = remember(bill.bankCode) {
        PaymentUtils.findInstalledAppForCode(context, bill.bankCode)
    }
    val installedQrisApps = remember(bill.uiMode) {
        if (bill.uiMode == "qris") PaymentUtils.getInstalledQrisApps(context) else emptyList()
    }

    AppScreenScaffold(
        title = if (isEn) "Payment #${bill.id}" else "Pembayaran #${bill.id}",
        subtitle = "${bill.gateway.uppercase()} • ${bill.paymentChannel}",
        onBack = onBackToHome,
        actions = {
            if (isUnpaid) {
                IconButton(onClick = { onCheckStatus(false) }, enabled = !isChecking) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                if (isPaid) {
                    Button(
                        onClick = onBackToHome,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                    ) {
                        Text(if (isEn) "Back to Home" else "Kembali ke Beranda")
                    }
                } else if (isUnpaid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelBill,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(strings.cancel)
                        }
                        Button(
                            onClick = { onCheckStatus(false) },
                            enabled = !isChecking,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.6f)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isEn) "Check Status" else "Cek Pembayaran")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onBackToHome,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(if (isEn) "Close" else "Tutup")
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
            // =================================================================
            // 1. STATUS LUNAS / EXPIRED / RINGKASAN TAGIHAN
            // =================================================================
            if (isPaid) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Text(
                            text = if (isEn) "Payment Confirmed!" else "Pembayaran Diterima!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${bill.planName} • ${formatMoney(bill.currency, bill.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Invoice: ${bill.trxInvoice}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
                return@Column
            }

            // Kartu Ringkasan Tagihan (Nominal & Status Auto-Cek)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bill.planName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = bill.paymentChannel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = bill.statusLabel,
                            tone = if (isUnpaid) BadgeTone.WARNING else BadgeTone.DANGER
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEn) "Total Amount" else "Total Bayar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(bill.currency, bill.price),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(bill.price.toLong().toString()))
                                Toast.makeText(
                                    context,
                                    if (isEn) "Amount copied" else "Nominal disalin",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Copy" else "Salin")
                        }
                    }

                    Text(
                        text = "${if (isEn) "Valid until" else "Batas waktu"}: ${bill.expiredDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isUnpaid) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text(
                                    text = if (hasOpenedExternalApp) {
                                        if (isEn) "Returned to app — verifying payment automatically..." else "Kembali ke aplikasi — memeriksa status pembayaran otomatis..."
                                    } else {
                                        if (isEn) "Auto-checking payment every 6 seconds" else "Memantau pembayaran otomatis setiap 6 detik"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. KONTEN UTAMA SESUAI UI_MODE (QRIS | VA_CODE | EWALLET/CHECKOUT)
            // =================================================================
            when (bill.uiMode) {
                "qris" -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "QRIS (${bill.paymentChannel})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Kanvas putih solid agar kamera scanner / galeri selalu bisa membaca QR di Dark Mode
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(260.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        localQrBitmap != null -> {
                                            Image(
                                                bitmap = localQrBitmap.asImageBitmap(),
                                                contentDescription = "QRIS Code",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        bill.qrisImage.isNotBlank() -> {
                                            SubcomposeAsyncImage(
                                                model = bill.qrisImage,
                                                contentDescription = "QRIS Image",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator()
                                                    }
                                                },
                                                error = {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier.padding(12.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Outlined.ErrorOutline,
                                                            contentDescription = null,
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = if (isEn) "Failed to load QR image" else "Gagal memuat gambar QR",
                                                            color = Color(0xFF334155),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = if (isEn) "QR Code unavailable" else "Kode QR tidak tersedia",
                                                color = Color(0xFF334155),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Tombol Simpan QR ke Galeri + Salin Kode QRIS / Buka Link Fallback
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isSavingQr = true
                                            val res = PaymentUtils.saveQrToGallery(
                                                context = context,
                                                localBitmap = localQrBitmap,
                                                imageUrl = bill.qrisImage,
                                                fileNamePrefix = "QRIS_TRX_${bill.id}"
                                            )
                                            isSavingQr = false
                                            Toast.makeText(
                                                context,
                                                res.getOrElse { it.message ?: "Gagal menyimpan QR" },
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    enabled = !isSavingQr && (localQrBitmap != null || bill.qrisImage.isNotBlank()),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSavingQr) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEn) "Save QR" else "Simpan QR")
                                }

                                if (bill.checkoutUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            hasOpenedExternalApp = true
                                            PaymentUtils.openExternalPaymentUrl(context, bill.checkoutUrl)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEn) "Pay Page" else "Halaman Bayar")
                                    }
                                }
                            }

                            // Pintasan Buka Aplikasi E-Wallet / m-Banking yang Terpasang di HP User
                            if (installedQrisApps.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = if (isEn) "Open installed Bank / E-Wallet app:" else "Buka aplikasi Bank / E-Wallet terpasang:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(installedQrisApps, key = { it.packageName }) { app ->
                                        FilledTonalButton(
                                            onClick = {
                                                hasOpenedExternalApp = true
                                                PaymentUtils.launchInstalledApp(context, app.packageName)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Outlined.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(app.label, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "va_code" -> {
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
                                text = if (isEn) "Virtual Account / Payment Code" else "Nomor Virtual Account / Kode Bayar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bill.payCode,
                                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(bill.payCode))
                                            Toast.makeText(
                                                context,
                                                if (isEn) "VA number copied!" else "Nomor VA disalin!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = strings.copy)
                                    }
                                }
                            }

                            // Tombol Buka Aplikasi Bank Otomatis (Salin VA + Langsung Buka m-Banking Terpasang)
                            if (installedTargetBank != null) {
                                Button(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(bill.payCode))
                                        Toast.makeText(
                                            context,
                                            if (isEn) "VA copied! Opening ${installedTargetBank.label}..." else "Nomor VA disalin! Membuka ${installedTargetBank.label}...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        hasOpenedExternalApp = true
                                        PaymentUtils.launchInstalledApp(context, installedTargetBank.packageName)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Outlined.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isEn) "Copy VA & Open ${installedTargetBank.label}"
                                        else "Salin VA & Buka ${installedTargetBank.label}"
                                    )
                                }
                            } else if (bill.bankCode.isNotBlank()) {
                                val bankName = PaymentUtils.getBankDisplayName(bill.bankCode)
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(bill.payCode))
                                        Toast.makeText(
                                            context,
                                            if (isEn) "VA copied. Please open $bankName app manually." else "Nomor VA disalin. Silakan buka aplikasi $bankName secara manual.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Copy VA for $bankName" else "Salin Nomor VA ($bankName)")
                                }
                            }

                            if (bill.checkoutUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        hasOpenedExternalApp = true
                                        PaymentUtils.openExternalPaymentUrl(context, bill.checkoutUrl)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Open Payment Page" else "Buka Halaman Tagihan")
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Mode ewallet_link | checkout_url (DANA, OVO, ShopeePay, Xendit, PayPal, dll.)
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
                                text = if (isEn) {
                                    "Tap the button below to continue payment in ${bill.paymentChannel}. When finished, return to this app to automatically verify your payment."
                                } else {
                                    "Ketuk tombol di bawah untuk melanjutkan pembayaran ke ${bill.paymentChannel}. Setelah selesai bayar, kembali ke aplikasi ini agar status otomatis terverifikasi."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (bill.checkoutUrl.isNotBlank()) {
                                Button(
                                    onClick = {
                                        hasOpenedExternalApp = true
                                        val ok = PaymentUtils.openExternalPaymentUrl(context, bill.checkoutUrl)
                                        if (!ok) {
                                            Toast.makeText(
                                                context,
                                                if (isEn) "Unable to open payment link" else "Gagal membuka tautan pembayaran",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isEn) "Continue to ${bill.paymentChannel}"
                                        else "Bayar via ${bill.paymentChannel}"
                                    )
                                }
                            }

                            if (installedTargetBank != null && bill.checkoutUrl.isBlank()) {
                                FilledTonalButton(
                                    onClick = {
                                        hasOpenedExternalApp = true
                                        PaymentUtils.launchInstalledApp(context, installedTargetBank.packageName)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isEn) "Open ${installedTargetBank.label}" else "Buka ${installedTargetBank.label}")
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. PANDUAN LANGKAH PEMBAYARAN (INSTRUCTIONS)
            // =================================================================
            if (bill.instructions.isNotEmpty()) {
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
                        Text(
                            text = if (isEn) "Payment Instructions" else "Cara Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        bill.instructions.forEachIndexed { gIdx, group ->
                            if (gIdx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            group.steps.forEachIndexed { idx, step ->
                                Text(
                                    text = "${idx + 1}. $step",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
