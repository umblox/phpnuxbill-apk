package com.phpnuxbill.mobile.feature.portal.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class InstalledPaymentApp(
    val code: String,
    val label: String,
    val packageName: String
)

object PaymentUtils {

    private data class BankAppCandidate(
        val code: String,
        val label: String,
        val packages: List<String>,
        val supportsQris: Boolean = true
    )

    private val bankCatalog = listOf(
        BankAppCandidate("DANA", "DANA", listOf("id.dana")),
        BankAppCandidate("GOPAY", "GoPay", listOf("com.gojek.gopay", "com.gojek.app")),
        BankAppCandidate("OVO", "OVO", listOf("ovo.id")),
        BankAppCandidate("SHOPEEPAY", "ShopeePay", listOf("com.shopeepay.id", "com.shopee.id")),
        BankAppCandidate("LINKAJA", "LinkAja", listOf("com.telkom.mwallet")),
        BankAppCandidate("BCA", "BCA / myBCA", listOf("com.bca.mybca.omni.android", "com.bca")),
        BankAppCandidate("BRI", "BRImo", listOf("id.co.bri.brimo")),
        BankAppCandidate("MANDIRI", "Livin' Mandiri", listOf("id.bmri.livin")),
        BankAppCandidate("BNI", "BNI / wondr", listOf("id.bni.wondr", "src.com.bni")),
        BankAppCandidate("BSI", "BSI / BYOND", listOf("id.co.bankbsi.byond", "com.bsm.activity2")),
        BankAppCandidate("PERMATA", "PermataMobile X", listOf("net.myinfosys.PermataMobileX")),
        BankAppCandidate("CIMB", "OCTO Mobile", listOf("id.co.cimbniaga.mobile.android")),
        BankAppCandidate("DANAMON", "D-Bank PRO", listOf("id.co.danamon.dbankpro")),
        BankAppCandidate("MAYBANK", "M2U ID", listOf("id.com.uiux.mobile")),
        BankAppCandidate("MUAMALAT", "Muamalat DIN", listOf("com.muamalat.din")),
        BankAppCandidate("NEOBANK", "Neobank", listOf("com.bnc.finance")),
        BankAppCandidate("PAYPAL", "PayPal", listOf("com.paypal.android.p2pmobile"), supportsQris = false)
    )

    /**
     * Membuat QR Bitmap secara lokal/offline dari string EMV QRIS (000201...).
     */
    fun generateQrBitmap(content: String, sizePx: Int = 720): Bitmap? {
        if (content.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }

    /**
     * Mendekode gambar Data URI Base64 (data:image/png;base64,...) menjadi Bitmap.
     */
    fun decodeBase64Image(dataUri: String): Bitmap? {
        if (!dataUri.startsWith("data:image", ignoreCase = true)) return null
        return runCatching {
            val base64Part = dataUri.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /**
     * Menyimpan QR Code ke Galeri HP (MediaStore) baik dari Bitmap lokal maupun URL gambar (ViaQRIS/Tripay).
     */
    suspend fun saveQrToGallery(
        context: Context,
        localBitmap: Bitmap?,
        imageUrl: String,
        fileNamePrefix: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmapToSave = localBitmap ?: downloadBitmapFromUrl(imageUrl)
                ?: throw IllegalStateException("Gambar QR belum siap disimpan.")

            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QRIS")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Gagal membuat file di Galeri.")
                resolver.openOutputStream(uri)?.use { out ->
                    bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: throw IllegalStateException("Gagal menulis file QR.")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "QR tersimpan di Galeri (Pictures/QRIS)"
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: context.cacheDir
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.insertImage(context.contentResolver, file.absolutePath, fileName, "QRIS Payment")
                "QR berhasil disimpan ke perangkat"
            }
        }
    }

    private fun downloadBitmapFromUrl(urlStr: String): Bitmap? {
        if (urlStr.isBlank() || !urlStr.startsWith("http", ignoreCase = true)) return null
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
            }
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    fun getBankDisplayName(bankCode: String): String {
        val code = bankCode.trim().uppercase()
        return bankCatalog.find { it.code == code }?.label ?: code
    }

    /**
     * Mencari aplikasi bank/e-wallet terpasang untuk kode bank spesifik (mis. BCA, BRI, MANDIRI, DANA).
     */
    fun findInstalledAppForCode(context: Context, bankCode: String): InstalledPaymentApp? {
        val code = bankCode.trim().uppercase()
        if (code.isBlank() || code == "QRIS") return null
        val cand = bankCatalog.find { it.code == code } ?: return null
        val pm = context.packageManager
        for (pkg in cand.packages) {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                return InstalledPaymentApp(cand.code, cand.label, pkg)
            }
        }
        return null
    }

    /**
     * Mengambil daftar aplikasi m-Banking & E-Wallet yang benar-benar terpasang di HP user untuk pintasan QRIS.
     */
    fun getInstalledQrisApps(context: Context): List<InstalledPaymentApp> {
        val pm = context.packageManager
        val found = mutableListOf<InstalledPaymentApp>()
        for (cand in bankCatalog) {
            if (!cand.supportsQris) continue
            for (pkg in cand.packages) {
                if (pm.getLaunchIntentForPackage(pkg) != null) {
                    found.add(InstalledPaymentApp(cand.code, cand.label, pkg))
                    break
                }
            }
        }
        return found
    }

    /**
     * Membuka aplikasi bank/e-wallet tanpa menutup aplikasi kita di latar belakang.
     */
    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    /**
     * Membuka URL pembayaran (E-Wallet Deep Link / Xendit / PayPal).
     * Android akan otomatis mengarahkan ke aplikasi e-wallet/PayPal jika terpasang, atau browser bila tidak.
     */
    fun openExternalPaymentUrl(context: Context, url: String): Boolean {
        val clean = url.trim()
        if (clean.isBlank()) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean))
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
