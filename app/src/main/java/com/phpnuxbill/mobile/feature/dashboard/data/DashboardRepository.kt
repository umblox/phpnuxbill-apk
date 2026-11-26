package com.phpnuxbill.mobile.feature.dashboard.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.dashboard.model.ActiveServiceBreakdown
import com.phpnuxbill.mobile.feature.dashboard.model.AdminActivityLogItem
import com.phpnuxbill.mobile.feature.dashboard.model.AdminProfileInfo
import com.phpnuxbill.mobile.feature.dashboard.model.AdminServerFlags
import com.phpnuxbill.mobile.feature.dashboard.model.DashboardCards
import com.phpnuxbill.mobile.feature.dashboard.model.DashboardData
import com.phpnuxbill.mobile.feature.dashboard.model.ExpiringUserItem
import com.phpnuxbill.mobile.feature.dashboard.model.MonthlyCountPoint
import com.phpnuxbill.mobile.feature.dashboard.model.MonthlySalesPoint
import com.phpnuxbill.mobile.feature.dashboard.model.PlanStockItem

interface DashboardRepository {
    suspend fun fetchAdminDashboard(): ApiResult<DashboardData>
    suspend fun changeAdminPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): ApiResult<String>
}

class RemoteDashboardRepository(
    private val networkGateway: NetworkGateway,
    private val sessionStorage: SessionStorage
) : DashboardRepository {

    override suspend fun fetchAdminDashboard(): ApiResult<DashboardData> {
        return when (val res = networkGateway.get(action = "admin_dashboard")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.takeIf { it.isJsonObject }?.asJsonObject
                val session = sessionStorage.getCurrentSession()

                val compObj = root?.getAsJsonObject("company")
                val companyName = root.str("company_name")
                    .ifBlank { compObj.str("name") }
                    .ifBlank { session.effectiveCompanyName }
                val currencyCode = root.str("currency_code")
                    .ifBlank { compObj.str("currency") }
                    .ifBlank { session.currencyCode }
                val periodLabel = compObj.str("date_Period")

                val adminObj = root?.getAsJsonObject("admin")
                val resolvedUserType = adminObj.str("user_type").ifBlank { session.userType.ifBlank { "Admin" } }
                val resolvedFullname = adminObj.str("fullname").ifBlank { session.fullname.ifBlank { session.username } }
                val adminProfile = AdminProfileInfo(
                    id = adminObj.intVal("id"),
                    username = adminObj.str("username").ifBlank { session.username },
                    fullname = resolvedFullname,
                    userType = resolvedUserType,
                    photoUrl = adminObj.str("photo_url")
                )

                // Sinkronkan identitas perusahaan dan level role terbaru ke SessionStorage
                if (session.activeServerUrl.isNotBlank() &&
                    (companyName != session.companyName || currencyCode != session.currencyCode)
                ) {
                    sessionStorage.updateServerPreview(
                        serverUrl = session.activeServerUrl,
                        companyName = companyName,
                        currencyCode = currencyCode,
                        serverLogoUrl = session.serverLogoUrl
                    )
                }
                if (session.token.isNotBlank() &&
                    (resolvedUserType != session.userType || resolvedFullname != session.fullname)
                ) {
                    sessionStorage.saveAuthenticatedSession(
                        serverUrl = session.activeServerUrl,
                        token = session.token,
                        role = session.role,
                        username = session.username,
                        fullname = resolvedFullname,
                        userType = resolvedUserType,
                        companyName = companyName,
                        currencyCode = currencyCode,
                        serverLogoUrl = session.serverLogoUrl
                    )
                }

                val flagsObj = root?.getAsJsonObject("feature_flags")
                val featureFlags = AdminServerFlags(
                    enableBalance = if (flagsObj != null && flagsObj.has("enable_balance")) flagsObj.boolVal("enable_balance") else true,
                    disableVoucher = flagsObj?.boolVal("disable_voucher") ?: false,
                    enableCoupons = flagsObj?.boolVal("enable_coupons") ?: false,
                    radiusEnable = flagsObj?.boolVal("radius_enable") ?: false,
                    maintenanceMode = flagsObj?.boolVal("maintenance_mode") ?: false,
                    maintenanceForceLogout = flagsObj?.boolVal("maintenance_mode_logout") ?: false,
                    maintenanceDate = flagsObj.str("maintenance_date")
                )

                val enabledWidgets = root?.getAsJsonArray("enabled_widgets")
                    ?.mapNotNull { runCatching { it.asString.trim() }.getOrNull()?.takeIf { s -> s.isNotEmpty() } }
                    .orEmpty()

                val cardsObj = root?.getAsJsonObject("cards")
                val actUsers = cardsObj.intVal("active_users")
                val expUsers = cardsObj.intVal("expired_users")
                val unpaidCount = cardsObj.intVal("unpaid_qris_count")
                    .let { if (it == 0) root.intVal("unpaid_qris_count") else it }

                val cards = DashboardCards(
                    incomeToday = cardsObj.doubleVal("income_today"),
                    incomeMonth = cardsObj.doubleVal("income_month"),
                    activeUsers = actUsers,
                    expiredUsers = expUsers,
                    activeExpiredLabel = cardsObj.str("active_expired_label").ifBlank { "$actUsers/$expUsers" },
                    totalCustomers = cardsObj.intVal("total_customers"),
                    totalCustomerBalance = cardsObj.doubleVal("total_customer_balance"),
                    unpaidBillsCount = unpaidCount
                )

                val brObj = root?.getAsJsonObject("active_breakdown")
                val activeBreakdown = ActiveServiceBreakdown(
                    hotspot = brObj.intVal("hotspot"),
                    pppoe = brObj.intVal("pppoe"),
                    vpn = brObj.intVal("vpn")
                )

                val chartsObj = root?.getAsJsonObject("charts")
                val regArray = chartsObj?.getAsJsonArray("monthly_registered")
                    ?: root?.getAsJsonArray("monthly_registered")
                val salesArray = chartsObj?.getAsJsonArray("monthly_sales")
                    ?: root?.getAsJsonArray("monthly_sales")

                ApiResult.Success(
                    data = DashboardData(
                        companyName = companyName,
                        currencyCode = currencyCode,
                        periodLabel = periodLabel,
                        adminProfile = adminProfile,
                        featureFlags = featureFlags,
                        enabledWidgets = enabledWidgets,
                        cards = cards,
                        activeBreakdown = activeBreakdown,
                        monthlyRegistered = parseMonthlyRegistered(regArray),
                        monthlySales = parseMonthlySales(salesArray),
                        stocks = parseStocks(root?.getAsJsonArray("stocks")),
                        expiringUsers = parseExpiringUsers(root?.getAsJsonArray("expiring_users")),
                        recentLogs = parseRecentLogs(root?.getAsJsonArray("recent_logs"))
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun changeAdminPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_change_password",
                fields = mapOf(
                    "password" to currentPassword,
                    "npass" to newPassword,
                    "cnpass" to confirmPassword
                )
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    private fun parseMonthlyRegistered(arr: JsonArray?): List<MonthlyCountPoint> =
        arr?.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            MonthlyCountPoint(month = o.str("month"), count = o.intVal("count"))
        }.orEmpty()

    private fun parseMonthlySales(arr: JsonArray?): List<MonthlySalesPoint> =
        arr?.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            MonthlySalesPoint(month = o.str("month"), total = o.doubleVal("total"))
        }.orEmpty()

    private fun parseStocks(arr: JsonArray?): List<PlanStockItem> =
        arr?.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            PlanStockItem(
                planId = o.intVal("plan_id"),
                namePlan = o.str("name_plan").ifBlank { "-" },
                type = o.str("type").ifBlank { "Hotspot" },
                unused = o.intVal("unused"),
                used = o.intVal("used"),
                activeUsers = o.intVal("active_users")
            )
        }.orEmpty()

    private fun parseExpiringUsers(arr: JsonArray?): List<ExpiringUserItem> =
        arr?.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            ExpiringUserItem(
                id = o.intVal("id"),
                customerId = o.intVal("customer_id"),
                username = o.str("username").ifBlank { "-" },
                planId = o.intVal("plan_id"),
                planName = o.str("namebp").ifBlank { "-" },
                expiration = o.str("expiration").ifBlank { "-" },
                time = o.str("time").ifBlank { "23:59:59" },
                status = o.str("status").ifBlank { "on" },
                routers = o.str("routers").ifBlank { "-" },
                type = o.str("type").ifBlank { "Hotspot" }
            )
        }.orEmpty()

    private fun parseRecentLogs(arr: JsonArray?): List<AdminActivityLogItem> =
        arr?.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            AdminActivityLogItem(
                id = o.intVal("id"),
                date = o.str("date"),
                type = o.str("type"),
                description = o.str("description"),
                ip = o.str("ip")
            )
        }.orEmpty()

    private fun JsonElement?.asObjOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonObject?.str(key: String): String {
        if (this == null || !has(key) || get(key).isJsonNull) return ""
        return runCatching { get(key).asString.trim() }.getOrDefault("")
    }

    private fun JsonObject?.intVal(key: String): Int {
        if (this == null || !has(key) || get(key).isJsonNull) return 0
        return runCatching { get(key).asInt }.getOrElse { str(key).toIntOrNull() ?: 0 }
    }

    private fun JsonObject?.doubleVal(key: String): Double {
        if (this == null || !has(key) || get(key).isJsonNull) return 0.0
        return runCatching { get(key).asDouble }.getOrElse { str(key).toDoubleOrNull() ?: 0.0 }
    }

    private fun JsonObject?.boolVal(key: String): Boolean {
        if (this == null || !has(key) || get(key).isJsonNull) return false
        return runCatching {
            val el = get(key)
            if (el.isJsonPrimitive && el.asJsonPrimitive.isBoolean) el.asBoolean
            else {
                val s = el.asString.trim().lowercase()
                s == "1" || s == "true" || s == "yes"
            }
        }.getOrDefault(false)
    }
}
