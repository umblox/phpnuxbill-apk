package com.phpnuxbill.mobile.feature.dashboard.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

data class AdminProfileInfo(
    val id: Int = 0,
    val username: String = "",
    val fullname: String = "",
    val userType: String = "Admin",
    val photoUrl: String = ""
)

data class AdminServerFlags(
    val enableBalance: Boolean = true,
    val disableVoucher: Boolean = false,
    val enableCoupons: Boolean = false,
    val radiusEnable: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceForceLogout: Boolean = false,
    val maintenanceDate: String = ""
)

data class ActiveServiceBreakdown(
    val hotspot: Int = 0,
    val pppoe: Int = 0,
    val vpn: Int = 0
)

data class AdminActivityLogItem(
    val id: Int,
    val date: String,
    val type: String,
    val description: String,
    val ip: String = ""
)

data class DashboardData(
    val companyName: String = "PHPNuxBill",
    val currencyCode: String = "Rp.",
    val periodLabel: String = "",
    val adminProfile: AdminProfileInfo = AdminProfileInfo(),
    val featureFlags: AdminServerFlags = AdminServerFlags(),
    val enabledWidgets: List<String> = emptyList(),
    val cards: DashboardCards = DashboardCards(),
    val activeBreakdown: ActiveServiceBreakdown = ActiveServiceBreakdown(),
    val monthlyRegistered: List<MonthlyCountPoint> = emptyList(),
    val monthlySales: List<MonthlySalesPoint> = emptyList(),
    val stocks: List<PlanStockItem> = emptyList(),
    val expiringUsers: List<ExpiringUserItem> = emptyList(),
    val recentLogs: List<AdminActivityLogItem> = emptyList()
)

data class DashboardCards(
    val incomeToday: Double = 0.0,
    val incomeMonth: Double = 0.0,
    val activeUsers: Int = 0,
    val expiredUsers: Int = 0,
    val activeExpiredLabel: String = "0/0",
    val totalCustomers: Int = 0,
    val totalCustomerBalance: Double = 0.0,
    val unpaidBillsCount: Int = 0
)

data class MonthlyCountPoint(
    val month: String,
    val count: Int
)

data class MonthlySalesPoint(
    val month: String,
    val total: Double
)

data class PlanStockItem(
    val planId: Int,
    val namePlan: String,
    val type: String,
    val unused: Int,
    val used: Int,
    val activeUsers: Int
)

data class ExpiringUserItem(
    val id: Int,
    val customerId: Int,
    val username: String,
    val planId: Int,
    val planName: String,
    val expiration: String,
    val time: String,
    val status: String,
    val routers: String,
    val type: String
)

enum class AdminDestination(val route: String) {
    DASHBOARD("dashboard"),
    CUSTOMERS("customers"),
    SERVICES("services"), // Pelanggan Aktif (Active Customers)
    VOUCHERS("vouchers"),
    PLANS("plans"),
    BANDWIDTH("bandwidth"),
    MIKROTIK("mikrotik"),
    GENIEACS("genieacs"),
    REPORTS("reports"),
    PAYMENT_GATEWAY("payment_gateway"),
    COUPONS("coupons"),
    ASSETS("assets"),
    MAP("map"),
    MESSAGES("messages"),
    SETTINGS("settings")
}

/**
 * Label dwibahasa (ID / EN) khusus Dasbor Admin (OCP — tanpa mengubah AppStrings inti)
 */
class AdminDashboardLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    val incomeToday = if (isEn) "Today's Revenue" else "Pendapatan Hari Ini"
    val incomeMonth = if (isEn) "This Month" else "Bulan Ini"
    val mySalesToday = if (isEn) "Today's Sales" else "Penjualan Hari Ini"
    val mySalesMonth = if (isEn) "This Month's Sales" else "Penjualan Bulan Ini"
    val activeCustomers = if (isEn) "Active Customers" else "Pelanggan Aktif"
    val expiredLabel = if (isEn) "Expired" else "Kedaluwarsa"
    val totalCustomers = if (isEn) "Total Customers" else "Total Pelanggan"
    val customerBalance = if (isEn) "Customer Balance" else "Total Saldo Pelanggan"
    val pendingGateway = if (isEn) "Pending Bills" else "Tagihan Tertunda"
    val quickActions = if (isEn) "Quick Actions" else "Aksi Cepat"
    val activeBreakdownTitle = if (isEn) "Active Customers by Service" else "Rincian Pelanggan Aktif"
    val openActiveCustomers = if (isEn) "Manage Active" else "Kelola Aktif"
    val monthlyPerformance = if (isEn) "12-Month Performance" else "Performa 12 Bulan"
    val tabSales = if (isEn) "Sales" else "Penjualan"
    val tabRegistered = if (isEn) "New Users" else "Pelanggan Baru"
    val voucherStocksTitle = if (isEn) "Voucher Stock & Active Plans" else "Stok Voucher & Paket Aktif"
    val availableLabel = if (isEn) "Available" else "Tersedia"
    val usedLabel = if (isEn) "Used" else "Terpakai"
    val expiringTitle = if (isEn) "Expiring Soon" else "Segera Jatuh Tempo"
    val noExpiring = if (isEn) "No subscriptions expiring within 2 days." else "Tidak ada layanan yang jatuh tempo dalam 2 hari."
    val recentLogsTitle = if (isEn) "Recent System Activity" else "Aktivitas Sistem Terbaru"
    val maintenanceWarning = if (isEn) "Maintenance Mode is currently ON on the server." else "Mode Pemeliharaan (Maintenance) sedang AKTIF di server."
    val manageMaintenanceBtn = if (isEn) "Manage / Turn Off" else "Kelola / Matikan"
    val maintenanceEndPrefix = if (isEn) "Est. end" else "Estimasi selesai"
    val maintenanceForceLogoutOn = if (isEn) "Force Logout: ON" else "Paksa Logout: AKTIF"
    val maintenanceForceLogoutOff = if (isEn) "Force Logout: OFF" else "Paksa Logout: NONAKTIF"
    val roleSummaryReport = if (isEn) "Read-only monitoring & financial reports mode" else "Mode pantau & laporan keuangan (hanya baca)"
    val roleSummaryAgent = if (isEn) "Field operations, active customers & sales team" else "Operasional pelanggan aktif, voucher & tim sales"
    val roleSummarySales = if (isEn) "Customer registration, recharge & voucher sales" else "Registrasi pelanggan, isi ulang & penjualan voucher"
    val appearanceLabel = if (isEn) "Theme" else "Tema"
    val themeSystem = if (isEn) "Auto" else "Sistem"
    val themeLight = if (isEn) "Light" else "Terang"
    val themeDark = if (isEn) "Dark" else "Gelap"
}
