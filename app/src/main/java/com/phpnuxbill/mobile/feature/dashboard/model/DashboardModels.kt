package com.phpnuxbill.mobile.feature.dashboard.model

data class DashboardData(
    val companyName: String = "PHPNuxBill",
    val currencyCode: String = "Rp.",
    val periodLabel: String = "",
    val cards: DashboardCards = DashboardCards(),
    val monthlyRegistered: List<MonthlyCountPoint> = emptyList(),
    val monthlySales: List<MonthlySalesPoint> = emptyList(),
    val stocks: List<PlanStockItem> = emptyList(),
    val expiringUsers: List<ExpiringUserItem> = emptyList()
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
    SERVICES("services"),
    VOUCHERS("vouchers"),
    PLANS("plans"),
    BANDWIDTH("bandwidth"),
    MIKROTIK("mikrotik"),
    GENIEACS("genieacs"),
    REPORTS("reports"),
    PAYMENT_GATEWAY("payment_gateway"),
    COUPONS_ASSETS("coupons_assets"),
    MAP("map"),
    MESSAGES("messages"),
    SETTINGS("settings")
}
