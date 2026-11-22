package com.phpnuxbill.mobile.feature.operations.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

data class GatewayBreakdownItem(
    val gateway: String,
    val count: Int,
    val total: Double
)

data class ReportTransactionItem(
    val id: Int,
    val invoice: String,
    val username: String,
    val planName: String,
    val price: Double,
    val rechargedOn: String,
    val rechargedTime: String,
    val expiration: String,
    val method: String,
    val routers: String,
    val type: String
)

data class FinancialReportData(
    val startDate: String = "",
    val endDate: String = "",
    val totalTransactions: Int = 0,
    val totalIncome: Double = 0.0,
    val breakdown: List<GatewayBreakdownItem> = emptyList(),
    val transactions: List<ReportTransactionItem> = emptyList()
)

data class PgTransactionItem(
    val id: Int,
    val username: String,
    val planName: String,
    val price: Double,
    val currency: String,
    val gateway: String,
    val paymentMethod: String,
    val paymentChannel: String,
    val status: Int, // 1=Unpaid, 2=Paid, 3=Expired, 4=Canceled
    val statusLabel: String,
    val createdDate: String,
    val paidDate: String,
    val trxInvoice: String
)

data class PgAuditData(
    val totalPaidAmount: Double = 0.0,
    val totalUnpaidCount: Int = 0,
    val gateways: List<String> = emptyList(),
    val items: List<PgTransactionItem> = emptyList()
)

data class CouponItem(
    val id: Int,
    val code: String,
    val type: String, // fixed | percent
    val value: Double,
    val description: String,
    val maxUsage: Int,
    val usageCount: Int,
    val status: String, // active | inactive
    val minOrderAmount: Double,
    val maxDiscountAmount: Double,
    val startDate: String,
    val endDate: String
)

data class CouponFormState(
    val isOpen: Boolean = false,
    val id: Int = 0,
    val code: String = "",
    val type: String = "fixed",
    val value: String = "",
    val description: String = "",
    val maxUsage: String = "0",
    val minOrderAmount: String = "0",
    val maxDiscountAmount: String = "0",
    val status: String = "active",
    val startDate: String = "",
    val endDate: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class AssetItem(
    val id: Int,
    val name: String,
    val category: String,
    val brand: String,
    val serialNumber: String,
    val cost: Double,
    val location: String,
    val coordinates: String,
    val conditionStatus: String,
    val notes: String
)

data class AssetFormState(
    val isOpen: Boolean = false,
    val id: Int = 0,
    val name: String = "",
    val category: String = "ODP/ODC",
    val brand: String = "",
    val serialNumber: String = "",
    val cost: String = "0",
    val location: String = "",
    val coordinates: String = "",
    val conditionStatus: String = "Good",
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class MapPointItem(
    val title: String,
    val subtitle: String,
    val category: String, // customer_on | customer_off | router | asset
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int = 0
)

data class NetworkMapData(
    val customersCount: Int = 0,
    val routersCount: Int = 0,
    val assetsCount: Int = 0,
    val points: List<MapPointItem> = emptyList()
)

data class MessageSendFormState(
    val mode: String = "broadcast", // single | broadcast
    val targetUsername: String = "",
    val group: String = "all", // all | active | expired | hotspot | pppoe
    val via: String = "wa", // wa | sms | inbox | email | both
    val subject: String = "",
    val message: String = "",
    val isSending: Boolean = false
)

data class WebServerSettings(
    val companyName: String = "",
    val address: String = "",
    val phone: String = "",
    val currencyCode: String = "Rp.",
    val waUrl: String = "",
    val telegramBot: String = "",
    val telegramTargetId: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceForceLogout: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

class OperationsLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    val startDateLabel = if (isEn) "Start Date (YYYY-MM-DD)" else "Tanggal Mulai (YYYY-MM-DD)"
    val endDateLabel = if (isEn) "End Date (YYYY-MM-DD)" else "Tanggal Akhir (YYYY-MM-DD)"
    val filterBtn = if (isEn) "Apply Filter" else "Terapkan Filter"
    val revenueByMethod = if (isEn) "Revenue by Payment Method" else "Rincian per Metode Pembayaran"
    val totalTrxLabel = if (isEn) "Transactions" else "Transaksi"

    // PG Audit
    val approveManual = if (isEn) "Approve Paid" else "Lunasi Manual"
    val checkStatus = if (isEn) "Check PG" else "Cek Status PG"
    val cancelBill = if (isEn) "Cancel" else "Batalkan"
    val approveConfirmMsg = if (isEn) {
        "Manually mark this bill as PAID and activate the customer's plan?"
    } else {
        "Tandai tagihan ini LUNAS secara manual dan aktifkan paket pelanggan?"
    }

    // Coupons & Assets
    val tabCoupons = if (isEn) "Promo Coupons" else "Kupon Promo"
    val tabAssets = if (isEn) "Network Assets" else "Inventaris Aset"
    val couponCode = if (isEn) "Coupon Code" else "Kode Kupon"
    val discountValue = if (isEn) "Discount Value" else "Nilai Potongan"
    val maxUsageLabel = if (isEn) "Max Usage (0 = Unlimited)" else "Batas Pemakaian (0 = Tanpa Batas)"
    val minOrderLabel = if (isEn) "Minimum Order Amount" else "Minimal Transaksi"
    val assetName = if (isEn) "Asset Name (e.g. ODP-01)" else "Nama Aset (mis. ODP-01)"
    val assetCategory = if (isEn) "Category (ODP/ODC/OLT/Router)" else "Kategori (ODP/ODC/OLT/Router)"
    val assetCost = if (isEn) "Asset Value / Cost" else "Nilai Aset / Harga Beli"
    val assetLocation = if (isEn) "Installation Location" else "Lokasi Pemasangan"

    // Messages
    val sendSingle = if (isEn) "Single Customer" else "Satu Pelanggan"
    val sendBroadcast = if (isEn) "Broadcast Group" else "Broadcast Massal"
    val targetUsernameHint = if (isEn) "Customer Username" else "Username Pelanggan"
    val messageBodyLabel = if (isEn) "Message Content (Supports [[name]], [[username]], [[balance]])" else "Isi Pesan (Mendukung [[name]], [[username]], [[balance]])"
    val sendNow = if (isEn) "Send Message" else "Kirim Pesan Sekarang"

    // Settings
    val webBillingConfigTitle = if (isEn) "Server Billing Configuration (tbl_appconfig)" else "Pengaturan Server Billing (tbl_appconfig)"
    val companyNameLabel = if (isEn) "Company Name" else "Nama Perusahaan"
    val waGatewayUrlLabel = if (isEn) "WhatsApp Gateway URL" else "URL WhatsApp Gateway"
    val telegramBotLabel = if (isEn) "Telegram Bot Token" else "Token Bot Telegram"
    val telegramChatIdLabel = if (isEn) "Telegram Target Chat ID" else "Chat ID Tujuan Telegram"
    val maintenanceModeLabel = if (isEn) "Maintenance Mode" else "Mode Pemeliharaan (Maintenance)"
    val forceLogoutLabel = if (isEn) "Force Logout Customers During Maintenance" else "Paksa Logout Pelanggan Saat Maintenance"
    val clearCacheBtn = if (isEn) "Clear Server Cache" else "Bersihkan Cache Server"
    val runCronBtn = if (isEn) "Run Expiry Cronjob Now" else "Jalankan Cron Isolir Sekarang"
}
