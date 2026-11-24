package com.phpnuxbill.mobile.feature.operations.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

// =========================================================================
// 1. LAPORAN KEUANGAN, PESAN & HALAMAN STATIS (core_reports_messages.php)
// =========================================================================

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

data class MessageSendFormState(
    val mode: String = "broadcast", // single | broadcast
    val targetUsername: String = "",
    val group: String = "all", // all | active | expired | hotspot | pppoe
    val via: String = "wa", // wa | sms | inbox | email | both | all
    val subject: String = "",
    val message: String = "",
    val isSending: Boolean = false
)

data class StaticPageItem(
    val name: String,
    val title: String,
    val content: String
)

// =========================================================================
// 2. PAYMENT GATEWAY, KUPON, ASET & PETA (plugin_coupons_assets_pg.php)
// =========================================================================

data class PgGatewayStat(
    val gateway: String,
    val paidCount: Int,
    val paidTotal: Double,
    val unpaidCount: Int
)

data class PgTransactionItem(
    val id: Int,
    val username: String,
    val planName: String,
    val price: Double,
    val currency: String,
    val gateway: String,
    val gatewayTrxId: String,
    val paymentMethod: String,
    val paymentChannel: String,
    val status: Int, // 1=Unpaid, 2=Paid, 3=Expired, 4=Canceled
    val statusLabel: String,
    val createdDate: String,
    val expiredDate: String,
    val paidDate: String,
    val trxInvoice: String
)

data class PgAuditData(
    val totalPaidAmount: Double = 0.0,
    val totalUnpaidCount: Int = 0,
    val byGateway: List<PgGatewayStat> = emptyList(),
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
    val purchaseDate: String,
    val notes: String,
    val assetTag: String = "",
    val modelName: String = "",
    val status: String = "Active",
    val warrantyExpiry: String = "",
    val assignedToName: String = ""
)

data class AssetFormState(
    val isOpen: Boolean = false,
    val id: Int = 0,
    val name: String = "",
    val category: String = "Network Equipment",
    val brand: String = "",
    val serialNumber: String = "",
    val cost: String = "0",
    val location: String = "",
    val coordinates: String = "",
    val conditionStatus: String = "Good",
    val purchaseDate: String = "",
    val notes: String = "",
    val assetTag: String = "",
    val modelName: String = "",
    val status: String = "Active",
    val warrantyExpiry: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class MapPointItem(
    val id: Int = 0,
    val title: String,
    val subtitle: String,
    val category: String, // router | customer_on | customer_off | odp | asset
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int = 0,
    val isCovered: Boolean = false,
    val coveredBy: String = "",
    val nearestRouter: String = "",
    val nearestDistanceM: Int = -1,
    val coveredCount: Int = 0,
    val routerName: String = "",
    val phone: String = "",
    val serviceType: String = ""
)

data class NetworkMapData(
    val customersCount: Int = 0,
    val routersCount: Int = 0,
    val odpsCount: Int = 0,
    val assetsCount: Int = 0,
    val coveredCustomersCount: Int = 0,
    val uncoveredCustomersCount: Int = 0,
    val points: List<MapPointItem> = emptyList()
)

// =========================================================================
// 3. PENGATURAN, NOTIFIKASI, STAF, DB & LOGS (core_settings_staff.php)
// =========================================================================

data class WebServerSettings(
    val companyName: String = "",
    val address: String = "",
    val phone: String = "",
    val currencyCode: String = "Rp.",
    val resetDay: String = "1",
    val enableTax: Boolean = false,
    val taxRate: String = "11",
    val waUrl: String = "",
    val smsUrl: String = "",
    val smtpHost: String = "",
    val mailFrom: String = "",
    val telegramBot: String = "",
    val telegramTargetId: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceForceLogout: Boolean = false,
    val maintenanceDate: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

data class NotificationTemplateItem(
    val key: String,
    val value: String,
    val defaultValue: String
)

data class StaffUserItem(
    val id: Int,
    val username: String,
    val fullname: String,
    val userType: String, // SuperAdmin | Admin | Report | Agent | Sales
    val phone: String,
    val email: String,
    val city: String,
    val subdistrict: String,
    val ward: String,
    val status: String,
    val root: Int,
    val rootName: String,
    val lastLogin: String
)

data class StaffFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val username: String = "",
    val fullname: String = "",
    val password: String = "",
    val userType: String = "Agent",
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val status: String = "Active",
    val rootAgentId: Int = 0,
    val sendNotif: String = "", // "" | wa | sms
    val isSaving: Boolean = false,
    val error: String? = null
)

data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false
)

data class DbTableStatusItem(
    val name: String,
    val rows: Int,
    val engine: String,
    val sizeLabel: String
)

data class DbStatusData(
    val totalTables: Int = 0,
    val totalRows: Int = 0,
    val totalSize: String = "0 B",
    val tables: List<DbTableStatusItem> = emptyList()
)

data class SystemLogItem(
    val id: Int,
    val date: String,
    val type: String,
    val description: String,
    val ip: String
)

class OperationsLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    // Reports
    val startDateLabel = if (isEn) "Start Date (YYYY-MM-DD)" else "Tanggal Mulai (YYYY-MM-DD)"
    val endDateLabel = if (isEn) "End Date (YYYY-MM-DD)" else "Tanggal Akhir (YYYY-MM-DD)"
    val filterBtn = if (isEn) "Apply Filter" else "Terapkan Filter"
    val revenueByMethod = if (isEn) "Revenue by Payment Method (Tap to Filter)" else "Rincian per Metode (Ketuk untuk Filter)"
    val totalTrxLabel = if (isEn) "Transactions" else "Transaksi"
    val clearFilter = if (isEn) "Reset Filter" else "Reset Filter"

    // PG Audit
    val totalPaidLabel = if (isEn) "Total Paid (PG)" else "Total Lunas (PG)"
    val totalUnpaidLabel = if (isEn) "Pending Bills" else "Tagihan Menunggu"
    val approveManual = if (isEn) "Approve Paid" else "Lunasi Manual"
    val checkStatus = if (isEn) "Check PG" else "Cek Status PG"
    val cancelBill = if (isEn) "Cancel" else "Batalkan"
    val approveConfirmMsg = if (isEn) {
        "Manually mark this bill as PAID and activate the customer's plan immediately?"
    } else {
        "Tandai tagihan ini LUNAS secara manual dan aktifkan paket pelanggan sekarang?"
    }
    val cancelConfirmMsg = if (isEn) {
        "Cancel this pending payment gateway bill?"
    } else {
        "Batalkan tagihan Payment Gateway yang sedang menunggu ini?"
    }

    // Coupons & Assets
    val tabCoupons = if (isEn) "Promo Coupons" else "Kupon Promo"
    val tabAssets = if (isEn) "Network Assets" else "Inventaris Aset"
    val couponCode = if (isEn) "Coupon Code" else "Kode Kupon"
    val discountValue = if (isEn) "Discount Value" else "Nilai Potongan"
    val descriptionLabel = if (isEn) "Description" else "Deskripsi Promo"
    val maxUsageLabel = if (isEn) "Max Usage (0 = Unlimited)" else "Batas Pemakaian (0 = Tanpa Batas)"
    val minOrderLabel = if (isEn) "Min Order Amount" else "Minimal Transaksi"
    val maxDiscountLabel = if (isEn) "Max Discount Cap" else "Maksimal Potongan"
    val validFromLabel = if (isEn) "Start Date (YYYY-MM-DD)" else "Berlaku Dari (YYYY-MM-DD)"
    val validUntilLabel = if (isEn) "End Date (YYYY-MM-DD)" else "Berlaku Sampai (YYYY-MM-DD)"
    val assetName = if (isEn) "Asset Name (e.g. ODP-01)" else "Nama Aset (mis. ODP-01)"
    val assetCategory = if (isEn) "Category (ODP/ODC/OLT/Router)" else "Kategori (ODP/ODC/OLT/Router)"
    val assetBrand = if (isEn) "Brand / Vendor" else "Merek / Vendor"
    val assetSerial = if (isEn) "Serial Number (SN)" else "Nomor Seri (SN)"
    val assetCost = if (isEn) "Asset Value / Cost" else "Nilai Aset / Harga Beli"
    val assetLocation = if (isEn) "Installation Location" else "Lokasi Pemasangan"
    val assetCondition = if (isEn) "Condition" else "Kondisi"
    val assetNotes = if (isEn) "Technical Notes" else "Catatan Teknis"

    // Messages & Static Pages
    val tabBroadcastMsg = if (isEn) "Send Message" else "Kirim Pesan"
    val tabStaticPages = if (isEn) "Static Pages" else "Halaman Statis"
    val sendSingle = if (isEn) "Single Customer" else "Satu Pelanggan"
    val sendBroadcast = if (isEn) "Broadcast Group" else "Broadcast Massal"
    val targetUsernameHint = if (isEn) "Customer Username" else "Username Pelanggan"
    val messageBodyLabel = if (isEn) "Message Content" else "Isi Pesan"
    val quickTagsLabel = if (isEn) "Tap to insert variable:" else "Ketuk untuk sisipkan tag:"
    val sendNow = if (isEn) "Send Message" else "Kirim Pesan Sekarang"
    val editPageTitle = if (isEn) "Edit HTML Page" else "Edit Halaman HTML"

    // Settings 4 Tabs
    val tabGeneralSettings = if (isEn) "General" else "Umum & Web"
    val tabNotifTemplates = if (isEn) "Templates" else "Template WA"
    val tabStaffSecurity = if (isEn) "Staff & Key" else "Staf & Sandi"
    val tabDbLogs = if (isEn) "DB & Logs" else "DB & Log"
    val webBillingConfigTitle = if (isEn) "Server Billing Configuration (tbl_appconfig)" else "Pengaturan Server Billing (tbl_appconfig)"
    val companyNameLabel = if (isEn) "Company Name" else "Nama Perusahaan"
    val resetDayLabel = if (isEn) "Monthly Reset Day (1-28)" else "Tanggal Reset Bulanan (1-28)"
    val waGatewayUrlLabel = if (isEn) "WhatsApp Gateway URL" else "URL WhatsApp Gateway"
    val smsGatewayUrlLabel = if (isEn) "SMS Gateway URL" else "URL SMS Gateway"
    val smtpHostLabel = if (isEn) "SMTP Host" else "SMTP Host"
    val telegramBotLabel = if (isEn) "Telegram Bot Token" else "Token Bot Telegram"
    val telegramChatIdLabel = if (isEn) "Telegram Target Chat ID" else "Chat ID Tujuan Telegram"
    val maintenanceModeLabel = if (isEn) "Maintenance Mode" else "Mode Pemeliharaan (Maintenance)"
    val forceLogoutLabel = if (isEn) "Force Logout Customers" else "Paksa Logout Pelanggan"
    val maintenanceDateLabel = if (isEn) "Maintenance End Estimate" else "Estimasi Selesai Maintenance"
    val testGatewayTitle = if (isEn) "Test Notification Gateways" else "Tes Gateway Notifikasi"
    val testTargetHint = if (isEn) "Target Phone / Email (Empty for Telegram)" else "No. HP / Email Tujuan (Kosongkan utk Telegram)"
    val sendTestBtn = if (isEn) "Send Test" else "Kirim Tes"
    val clearCacheBtn = if (isEn) "Clear Cache" else "Bersihkan Cache"
    val runCronBtn = if (isEn) "Run Cronjob" else "Jalankan Cron"
    val addStaffBtn = if (isEn) "Add Staff" else "Tambah Staf"
    val changePasswordTitle = if (isEn) "Change My Admin Password" else "Ganti Kata Sandi Saya"
    val currentPassLabel = if (isEn) "Current Password" else "Kata Sandi Saat Ini"
    val newPassLabel = if (isEn) "New Password (6-14 chars)" else "Kata Sandi Baru (6-14 karakter)"
    val confirmPassLabel = if (isEn) "Confirm New Password" else "Ulangi Kata Sandi Baru"
    val backupDbNowBtn = if (isEn) "Create JSON Backup on Server" else "Buat Backup DB JSON di Server"
    val systemLogsTitle = if (isEn) "System Activity Logs" else "Log Aktivitas Sistem"
}
