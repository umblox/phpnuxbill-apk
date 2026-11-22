package com.phpnuxbill.mobile.core.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

data class PairedPrinterDevice(
    val name: String,
    val address: String
)

sealed class PrintResult {
    data class Success(val message: String) : PrintResult()
    data class Error(val message: String) : PrintResult()
    data object PermissionRequired : PrintResult()
}

/**
 * Mesin Cetak Thermal Bluetooth ESC/POS (SOLID - Single Responsibility Principle)
 */
class BluetoothPrinterHelper(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(isEnglish: Boolean = false): Result<List<PairedPrinterDevice>> {
        if (!hasBluetoothPermission()) {
            return Result.failure(SecurityException("PERMISSION_REQUIRED"))
        }
        val adapter = getBluetoothAdapter()
            ?: return Result.failure(
                IllegalStateException(
                    if (isEnglish) "Bluetooth is not supported on this device."
                    else "Perangkat ini tidak mendukung Bluetooth."
                )
            )
        if (!adapter.isEnabled) {
            return Result.failure(
                IllegalStateException(
                    if (isEnglish) "Please enable Bluetooth first."
                    else "Aktifkan Bluetooth terlebih dahulu di pengaturan HP."
                )
            )
        }

        val devices = adapter.bondedDevices.orEmpty().map { dev ->
            PairedPrinterDevice(
                name = dev.name?.ifBlank { "Bluetooth Printer" } ?: "Bluetooth Printer",
                address = dev.address
            )
        }.sortedBy { it.name.lowercase() }

        return Result.success(devices)
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceiptText(
        macAddress: String,
        rawReceiptText: String,
        companyHeader: String = "",
        isEnglish: Boolean = false
    ): PrintResult = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext PrintResult.PermissionRequired
        }
        val adapter = getBluetoothAdapter()
            ?: return@withContext PrintResult.Error(
                if (isEnglish) "Bluetooth adapter unavailable." else "Adapter Bluetooth tidak tersedia."
            )
        if (!adapter.isEnabled) {
            return@withContext PrintResult.Error(
                if (isEnglish) "Please turn on Bluetooth." else "Silakan aktifkan Bluetooth terlebih dahulu."
            )
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(macAddress)
        } catch (_: IllegalArgumentException) {
            return@withContext PrintResult.Error(
                if (isEnglish) "Invalid printer MAC address." else "Alamat MAC printer tidak valid."
            )
        }

        try {
            adapter.cancelDiscovery()
        } catch (_: Exception) {
        }

        try {
            val socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.use { sock ->
                sock.connect()
                val out = sock.outputStream

                // ESC @ (Reset Printer)
                out.write(byteArrayOf(0x1B, 0x40))

                // Header Nama Perusahaan Dinamis (Bold + Center) jika belum ada di teks struk
                if (companyHeader.isNotBlank() &&
                    !rawReceiptText.contains(companyHeader, ignoreCase = true)
                ) {
                    out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                    out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
                    out.write((companyHeader.trim() + "\n").toByteArray(Charset.forName("UTF-8")))
                    out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF
                    out.write("--------------------------------\n".toByteArray())
                }

                // Isi Struk Utama (Align Left, bersihkan karakter markdown '*' WA)
                out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
                val cleanBody = rawReceiptText
                    .replace("*", "")
                    .replace("\r\n", "\n")
                    .trim()

                out.write((cleanBody + "\n\n\n").toByteArray(Charset.forName("UTF-8")))

                // Feed 3 baris & Partial Cut (jika didukung printer 80mm)
                out.write(byteArrayOf(0x1B, 0x64, 0x03))
                out.flush()
            }

            PrintResult.Success(
                if (isEnglish) "Receipt printed to ${device.name ?: macAddress}"
                else "Struk berhasil dicetak ke ${device.name ?: macAddress}"
            )
        } catch (e: IOException) {
            PrintResult.Error(
                if (isEnglish) "Failed to connect to printer: ${e.localizedMessage ?: "Check printer power"}"
                else "Gagal terhubung ke printer: ${e.localizedMessage ?: "Pastikan printer menyala"}"
            )
        } catch (e: Exception) {
            PrintResult.Error(
                if (isEnglish) "Printing error: ${e.localizedMessage}"
                else "Kesalahan cetak: ${e.localizedMessage}"
            )
        }
    }
}
