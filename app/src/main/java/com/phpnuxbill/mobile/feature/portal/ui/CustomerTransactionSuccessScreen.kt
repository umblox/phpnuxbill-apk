package com.phpnuxbill.mobile.feature.portal.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.ui.components.AppFloatingSnackbarHost
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.portal.model.CustomerTransactionReceipt
import kotlinx.coroutines.launch

@Composable
fun CustomerTransactionSuccessScreen(
    receipt: CustomerTransactionReceipt,
    onDone: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AppScreenScaffold(
        title = if (isEn) "Transaction Success" else "Transaksi Berhasil",
        subtitle = receipt.invoiceRef.takeIf { it.isNotBlank() },
        onBack = onDone,
        snackbarHost = { AppFloatingSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (receipt.isFriendTransaction) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    openWhatsAppWithText(
                                        context = context,
                                        phone = receipt.recipientPhone,
                                        text = receipt.shareText
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEn) "WA Friend" else "WA ke Teman",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { shareReceiptText(context, receipt.title, receipt.shareText) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.share, maxLines = 1)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { shareReceiptText(context, receipt.title, receipt.shareText) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEn) "Share Receipt" else "Bagikan Bukti",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(receipt.shareText))
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isEn) "Receipt copied successfully" else "Bukti transaksi berhasil disalin"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.copy, maxLines = 1)
                            }
                        }
                    }

                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Done" else "Selesai",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Kartu Hero Status Sukses
            Surface(
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(BrandNavy900, BrandBlue600, BrandTeal500)))
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF5EEAD4),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text(
                        text = receipt.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (receipt.amountFormatted.isNotBlank()) {
                        Text(
                            text = receipt.amountFormatted,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                    if (receipt.subtitle.isNotBlank()) {
                        Text(
                            text = receipt.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${receipt.companyName} • ${receipt.timestamp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Rincian Transaksi Terstruktur
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
                        text = if (isEn) "Transaction Details" else "Rincian Transaksi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (receipt.invoiceRef.isNotBlank()) {
                        SuccessDetailRow(
                            label = if (isEn) "Reference / Invoice" else "No. Referensi",
                            value = receipt.invoiceRef,
                            monospace = true
                        )
                    }
                    SuccessDetailRow(
                        label = if (isEn) "Account" else "Akun Pengirim",
                        value = "${receipt.senderName} (@${receipt.senderUsername})"
                    )
                    if (receipt.recipientUsername.isNotBlank()) {
                        val recLabel = if (receipt.recipientName.isNotBlank()) {
                            "${receipt.recipientName} (@${receipt.recipientUsername})"
                        } else {
                            "@${receipt.recipientUsername}"
                        }
                        SuccessDetailRow(
                            label = if (isEn) "Recipient Friend" else "Penerima (Teman)",
                            value = recLabel,
                            highlight = true
                        )
                    }
                    SuccessDetailRow(
                        label = if (isEn) "Item / Service" else "Layanan / Transaksi",
                        value = receipt.itemLabel,
                        highlight = !receipt.isFriendTransaction
                    )
                    if (receipt.paymentMethod.isNotBlank()) {
                        SuccessDetailRow(
                            label = if (isEn) "Payment Method" else "Metode Bayar",
                            value = receipt.paymentMethod
                        )
                    }
                    if (receipt.amountFormatted.isNotBlank()) {
                        SuccessDetailRow(
                            label = if (isEn) "Total Amount" else "Total Nominal",
                            value = receipt.amountFormatted,
                            highlight = true
                        )
                    }
                    if (receipt.balanceBeforeFormatted.isNotBlank() && receipt.balanceAfterFormatted.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SuccessDetailRow(
                            label = if (isEn) "Previous Balance" else "Saldo Sebelumnya",
                            value = receipt.balanceBeforeFormatted
                        )
                        SuccessDetailRow(
                            label = if (isEn) "Remaining Balance" else "Sisa Saldo",
                            value = receipt.balanceAfterFormatted,
                            highlight = true
                        )
                    }
                    if (receipt.extraNote.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = receipt.extraNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (receipt.isFriendTransaction) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(receipt.shareText))
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isEn) "Receipt text copied" else "Teks bukti berhasil disalin"
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Copy Proof Text" else "Salin Teks Bukti")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessDetailRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    monospace: Boolean = false
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (highlight || monospace) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.25f)
        )
    }
}

private fun shareReceiptText(context: android.content.Context, title: String, text: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}

private fun openWhatsAppWithText(context: android.content.Context, phone: String, text: String) {
    val digits = phone.replace(Regex("[^0-9]"), "")
    val waNum = if (digits.startsWith("0")) "62${digits.drop(1)}" else digits
    val encoded = Uri.encode(text)
    val url = if (waNum.isNotBlank()) {
        "https://wa.me/$waNum?text=$encoded"
    } else {
        "https://wa.me/?text=$encoded"
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
