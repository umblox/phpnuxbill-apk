package com.phpnuxbill.mobile.feature.services.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

data class PrepaidServiceItem(
    val id: Int,
    val customerId: Int,
    val username: String,
    val fullname: String,
    val phonenumber: String,
    val planId: Int,
    val namebp: String,
    val rechargedOn: String,
    val rechargedTime: String,
    val expiration: String,
    val time: String,
    val status: String,
    val method: String,
    val routers: String,
    val type: String,
    val price: Double,
    val invoice: String,
    val pppoeUsername: String = "",
    val pppoeIp: String = "",
    val accountStatus: String = "Active"
)

data class LiveSpeedSample(
    val downloadKbps: Float,
    val uploadKbps: Float
)

data class ActiveCustomerTrxItem(
    val id: Int,
    val invoice: String,
    val planName: String,
    val price: Double,
    val rechargedOn: String,
    val rechargedTime: String,
    val expiration: String,
    val method: String,
    val routers: String,
    val type: String
)

data class ActiveCustomerDetailData(
    val online: Boolean = false,
    val rosId: String = "",
    val activeDevices: Int = 0,
    val username: String = "",
    val service: String = "Hotspot",
    val router: String = "-",
    val routerId: Int = 0,
    val server: String = "-",
    val ipAddress: String = "-",
    val macAddress: String = "-",
    val uptime: String = "Offline",
    val sessionTime: String = "-",
    val idleTime: String = "-",
    val loginBy: String = "-",
    val uploadBytes: String = "0 B",
    val downloadBytes: String = "0 B",
    val totalBytes: String = "0 B",
    val rxBps: Long = 0L,
    val txBps: Long = 0L,
    val uploadKbps: Double = 0.0,
    val downloadKbps: Double = 0.0,
    val uploadSpeedLabel: String = "0 bps",
    val downloadSpeedLabel: String = "0 bps",
    val customerId: Int = 0,
    val fullname: String = "",
    val phonenumber: String = "",
    val address: String = "",
    val city: String = "",
    val balance: Double = 0.0,
    val accountType: String = "Personal",
    val accountStatus: String = "Active",
    val autoRenewal: Boolean = false,
    val pppoeUsername: String = "",
    val pppoeIp: String = "",
    val lastLogin: String = "-",
    val packageId: Int = 0,
    val planId: Int = 0,
    val planName: String = "-",
    val packageStatus: String = "on",
    val rechargedOn: String = "-",
    val rechargedTime: String = "",
    val expiration: String = "-",
    val expirationTime: String = "",
    val method: String = "-",
    val price: Double = 0.0,
    val rateLabel: String = "-",
    val validityLabel: String = "-",
    val customFields: List<Pair<String, String>> = emptyList(),
    val recentTransactions: List<ActiveCustomerTrxItem> = emptyList()
)

data class EditPrepaidFormState(
    val isOpen: Boolean = false,
    val rechargeId: Int = 0,
    val username: String = "",
    val planId: Int = 0,
    val expiration: String = "",
    val time: String = "23:59:59",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

data class VoucherItem(
    val id: Int,
    val type: String,
    val routers: String,
    val idPlan: Int,
    val code: String,
    val user: String,
    val status: String, // "0" = Unused, "1" = Used
    val generatedByName: String,
    val batchName: String,
    val namePlan: String,
    val price: Double,
    val validity: String,
    val createdAt: String,
    val usedDate: String
) {
    val isUsed: Boolean
        get() = status == "1"
}

data class InvoiceReceiptData(
    val trxId: Int,
    val invoice: String,
    val customerId: Int,
    val username: String,
    val fullname: String,
    val phonenumber: String,
    val planName: String,
    val type: String,
    val price: Double,
    val priceFormatted: String,
    val method: String,
    val routers: String,
    val rechargedAt: String,
    val expiration: String,
    val expirationPretty: String,
    val balanceAfter: Double,
    val balanceFormatted: String,
    val note: String,
    val waWebText: String
)

data class CustomerOption(
    val id: Int,
    val username: String,
    val fullname: String,
    val phonenumber: String,
    val balance: Double,
    val serviceType: String
)

data class ServicePlanOption(
    val id: Int,
    val namePlan: String,
    val type: String,
    val price: Double,
    val routers: String,
    val validityLabel: String
)

data class RouterOption(
    val id: Int,
    val name: String
)

data class ServiceVoucherMetadata(
    val customers: List<CustomerOption> = emptyList(),
    val internetPlans: List<ServicePlanOption> = emptyList(),
    val balancePlans: List<ServicePlanOption> = emptyList(),
    val paymentUsings: List<String> = listOf("Cash"),
    val routers: List<RouterOption> = emptyList(),
    val defaultVoucherPrefix: String = ""
)

enum class ActiveServiceSheetMode {
    NONE,
    RECHARGE_PLAN,
    DEPOSIT_BALANCE,
    REFILL_VOUCHER
}

data class RechargeFormState(
    val customerId: Int = 0,
    val customerQuery: String = "",
    val planId: Int = 0,
    val usingMethod: String = "Cash",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

data class DepositFormState(
    val customerId: Int = 0,
    val customerQuery: String = "",
    val planId: Int = 0,
    val customAmount: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

data class RefillFormState(
    val customerId: Int = 0,
    val customerQuery: String = "",
    val voucherCode: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

data class GenerateVoucherFormState(
    val isOpen: Boolean = false,
    val planId: Int = 0,
    val server: String = "",
    val count: String = "10",
    val format: String = "up",
    val prefix: String = "",
    val batchName: String = "",
    val lengthCode: String = "6",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class ServiceVoucherLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    val activeCustomersTitle = if (isEn) "Active Customers" else "Pelanggan Aktif"
    val tabActiveServices = if (isEn) "Active Services" else "Layanan Aktif"
    val selectCustomer = if (isEn) "Select Customer" else "Pilih Pelanggan"
    val filterCustomerHint = if (isEn) "Filter customer name/username..." else "Cari nama/username pelanggan..."
    val customNominalLabel = if (isEn) "Or Custom Amount" else "Atau Nominal Kustom"
    val noteLabel = if (isEn) "Transaction Note (Optional)" else "Catatan Transaksi (Opsional)"
    val extendDaysPrompt = if (isEn) "Extend Days" else "Tambah Hari"
    val deactivateConfirmTitle = if (isEn) "Deactivate & Remove Service?" else "Hapus Layanan Pelanggan?"
    val deactivateConfirmMsg = if (isEn) {
        "Active package will be removed from the router immediately."
    } else {
        "Paket aktif pelanggan akan segera dihapus dari router."
    }

    // Detail MikroTik & Billing Pelanggan Aktif
    val detailScreenTitle = if (isEn) "Live Service Detail" else "Detail Pelanggan Aktif"
    val tapForLiveDetail = if (isEn) "Live Speed & Detail" else "Detail & Live Speed"
    val liveTrafficTitle = if (isEn) "MikroTik Live Speed" else "Live Speed MikroTik"
    val liveRunning = if (isEn) "LIVE" else "LIVE"
    val livePaused = if (isEn) "PAUSED" else "JEDA"
    val mikrotikSessionTitle = if (isEn) "MikroTik Session Data" else "Data Sesi MikroTik"
    val billingInfoTitle = if (isEn) "Billing & Subscription" else "Data Billing & Langganan"
    val recentTrxTitle = if (isEn) "Recent Transactions" else "Riwayat Transaksi Terakhir"
    val customFieldsTitle = if (isEn) "Additional Attributes" else "Atribut Tambahan"
    val kickSession = if (isEn) "Disconnect (Kick)" else "Putuskan Sesi (Kick)"
    val kickConfirmTitle = if (isEn) "Disconnect Active Session?" else "Putuskan Sesi MikroTik?"
    val kickConfirmMsg = if (isEn) {
        "This user will be disconnected from MikroTik immediately and will need to reconnect."
    } else {
        "Sesi aktif pelanggan di MikroTik akan diputus sekarang dan perlu menyambung ulang."
    }
    val syncRouter = if (isEn) "Sync Router" else "Sync Router"
    val editPlanExpiry = if (isEn) "Edit Plan / Expiry" else "Ubah Paket / Exp"
    val editExpiryLabel = if (isEn) "Expiration Date (YYYY-MM-DD)" else "Tanggal Jatuh Tempo (YYYY-MM-DD)"
    val editTimeLabel = if (isEn) "Expiration Time (HH:mm:ss)" else "Jam Jatuh Tempo (HH:mm:ss)"

    // Voucher
    val unusedStatus = if (isEn) "Available" else "Tersedia"
    val usedStatus = if (isEn) "Used" else "Terpakai"
    val refillVoucherTitle = if (isEn) "Redeem / Refill Voucher" else "Aktifkan Voucher ke Pelanggan"
    val voucherCodeLabel = if (isEn) "Voucher Code" else "Kode Voucher"
    val voucherCountLabel = if (isEn) "Quantity (1 - 500)" else "Jumlah Voucher (1 - 500)"
    val voucherLengthLabel = if (isEn) "Code Length (4 - 16)" else "Panjang Kode (4 - 16)"
    val voucherPrefixLabel = if (isEn) "Voucher Prefix" else "Awalan (Prefix) Voucher"
    val batchNameLabel = if (isEn) "Batch Name (Optional)" else "Nama Batch (Opsional)"
    val codeFormatLabel = if (isEn) "Code Format" else "Format Karakter Kode"
    val cleanUsedVouchers = if (isEn) "Clean Used Vouchers" else "Bersihkan Voucher Terpakai"
    val cleanUsedConfirmMsg = if (isEn) {
        "Delete all used vouchers from database?"
    } else {
        "Hapus seluruh voucher yang sudah terpakai dari database?"
    }

    // Receipt & Thermal Printer
    val receiptTitle = if (isEn) "Official Transaction Receipt" else "Struk Transaksi Resmi"
    val selectPrinterTitle = if (isEn) "Select Bluetooth Printer" else "Pilih Printer Bluetooth"
    val noPairedPrinter = if (isEn) {
        "No paired Bluetooth printer found. Pair your thermal printer in Android Bluetooth Settings first."
    } else {
        "Belum ada printer Bluetooth terpasang. Silakan hubungkan printer thermal di Pengaturan Bluetooth HP terlebih dahulu."
    }
    val printingMessage = if (isEn) "Sending to thermal printer..." else "Mengirim ke printer thermal..."
    val resendGatewayWa = if (isEn) "Send via Server WA Gateway" else "Kirim via WA Gateway Server"
    val openWhatsAppApp = if (isEn) "Open WhatsApp App" else "Buka Aplikasi WhatsApp"
    val batchCodesCreated = if (isEn) "Generated Voucher Codes" else "Kode Voucher Berhasil Dibuat"
}
