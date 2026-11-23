package com.phpnuxbill.mobile.feature.portal.ui

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
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
     * Menyimpan QRIS berlabel (Brand + Nama Paket + Nominal Bayar) ke Pictures/{NamaBrand}/QRIS_Terbaru.png
     * dengan sistem timpa agar hanya ada 1 gambar QRIS terbaru di folder tersebut.
     */
    suspend fun saveBrandedQrToGallery(
        context: Context,
        localBitmap: Bitmap?,
        imageUrl: String,
        brandName: String,
        planName: String,
        paymentAmountLabel: String,
        invoiceLabel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rawQr = localBitmap ?: downloadBitmapFromUrl(imageUrl)
                ?: throw IllegalStateException("Gambar QR belum siap disimpan.")

            val safeBrand = sanitizeFolderName(brandName)
            val posterBitmap = createBrandedQrPoster(
                qrBitmap = rawQr,
                brandName = brandName.ifBlank { "PHPNuxBill" },
                planName = planName,
                paymentAmountLabel = paymentAmountLabel,
                invoiceLabel = invoiceLabel
            )

            val fixedFileName = "QRIS_Terbaru.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePathNoSlash = "${Environment.DIRECTORY_PICTURES}/$safeBrand"
                val relativePathWithSlash = "$relativePathNoSlash/"
                val resolver = context.contentResolver

                // Hapus file QRIS_Terbaru.png lama di folder brand agar hanya tersisa 1 file terbaru
                runCatching {
                    val projection = arrayOf(MediaStore.Images.Media._ID)
                    val selection =
                        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND (${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} = ?)"
                    val selectionArgs = arrayOf(fixedFileName, relativePathNoSlash, relativePathWithSlash)
                    resolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        while (cursor.moveToNext()) {
                            val existingUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                cursor.getLong(idCol)
                            )
                            resolver.delete(existingUri, null, null)
                        }
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fixedFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePathWithSlash)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Gagal membuat file di Galeri.")
                resolver.openOutputStream(uri)?.use { out ->
                    posterBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: throw IllegalStateException("Gagal menulis file QR.")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "QR tersimpan di Pictures/$safeBrand/$fixedFileName"
            } else {
                val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
                val brandDir = File(baseDir, safeBrand).apply { if (!exists()) mkdirs() }
                val file = File(brandDir, fixedFileName)
                FileOutputStream(file, false).use { out ->
                    posterBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                "QR tersimpan di $safeBrand/$fixedFileName"
            }
        }
    }

    /**
     * Merangkai kartu PNG bersih berlatar putih berisi Brand, QR Code, Nama Paket, dan Nominal Pembayaran.
     */
    private fun createBrandedQrPoster(
        qrBitmap: Bitmap,
        brandName: String,
        planName: String,
        paymentAmountLabel: String,
        invoiceLabel: String
    ): Bitmap {
        val width = 900
        val height = 1180
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // Header bar lembut sesuai identitas warna aplikasi
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0C4A6E")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 170f, headerBg)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BAE6FD")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val cx = width / 2f
        canvas.drawText(ellipsizeText(brandName, brandPaint, width - 80f), cx, 86f, brandPaint)
        canvas.drawText("QRIS PEMBAYARAN", cx, 134f, tagPaint)

        // Bingkai QR dengan quiet-zone putih lebar agar pemindai bank/e-wallet selalu mudah membaca
        val qrBoxSize = 620
        val qrLeft = (width - qrBoxSize) / 2
        val qrTop = 215
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(
            RectF(qrLeft - 20f, qrTop - 20f, (qrLeft + qrBoxSize + 20).toFloat(), (qrTop + qrBoxSize + 20).toFloat()),
            28f,
            28f,
            framePaint
        )

        val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
        val dstRect = Rect(qrLeft, qrTop, qrLeft + qrBoxSize, qrTop + qrBoxSize)
        val qrPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(qrBitmap, srcRect, dstRect, qrPaint)

        // Blok informasi Paket & Nominal Pembayaran di bawah QR
        val infoBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val infoRect = RectF(64f, 880f, (width - 64).toFloat(), 1125f)
        canvas.drawRoundRect(infoRect, 24f, 24f, infoBoxBg)
        canvas.drawRoundRect(infoRect, 24f, 24f, framePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 25f
            textAlign = Paint.Align.CENTER
        }
        val planPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 23f
            textAlign = Paint.Align.CENTER
        }

        val maxInfoW = infoRect.width() - 48f
        canvas.drawText(ellipsizeText(planName, planPaint, maxInfoW), cx, 938f, planPaint)
        canvas.drawText("Nominal Pembayaran", cx, 982f, labelPaint)
        canvas.drawText(ellipsizeText(paymentAmountLabel, amountPaint, maxInfoW), cx, 1052f, amountPaint)
        if (invoiceLabel.isNotBlank()) {
            canvas.drawText(ellipsizeText(invoiceLabel, metaPaint, maxInfoW), cx, 1098f, metaPaint)
        }

        return result
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "..."
    }

    private fun sanitizeFolderName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim('.', ' ')
        return cleaned.ifBlank { "PHPNuxBill" }
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

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

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
