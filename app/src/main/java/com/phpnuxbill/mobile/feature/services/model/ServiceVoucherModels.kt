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
    val invoice: String
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
    val type: String, // Hotspot | PPPOE | VPN | Balance
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
    val format: String = "up", // up | low | rand | numbers
    val prefix: String = "",
    val batchName: String = "",
    val lengthCode: String = "6",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class ServiceVoucherLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

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
