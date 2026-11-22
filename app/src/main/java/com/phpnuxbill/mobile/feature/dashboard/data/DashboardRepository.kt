package com.phpnuxbill.mobile.feature.dashboard.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.dashboard.model.DashboardCards
import com.phpnuxbill.mobile.feature.dashboard.model.DashboardData
import com.phpnuxbill.mobile.feature.dashboard.model.ExpiringUserItem
import com.phpnuxbill.mobile.feature.dashboard.model.MonthlyCountPoint
import com.phpnuxbill.mobile.feature.dashboard.model.MonthlySalesPoint
import com.phpnuxbill.mobile.feature.dashboard.model.PlanStockItem

/**
 * Kontrak Repository Dasbor Admin (SOLID - Interface Segregation Principle)
 */
interface DashboardRepository {
    suspend fun fetchAdminDashboard(): ApiResult<DashboardData>
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

                // Sinkronkan CompanyName & Currency terbaru dari server ke SessionStorage
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
                    activeExpiredLabel = cardsObj.str("active_expired_label")
                        .ifBlank { "$actUsers/$expUsers" },
                    totalCustomers = cardsObj.intVal("total_customers"),
                    totalCustomerBalance = cardsObj.doubleVal("total_customer_balance"),
                    unpaidBillsCount = unpaidCount
                )

                val chartsObj = root?.getAsJsonObject("charts")
                val regArray = chartsObj?.getAsJsonArray("monthly_registered")
                    ?: root?.getAsJsonArray("monthly_registered")
                val salesArray = chartsObj?.getAsJsonArray("monthly_sales")
                    ?: root?.getAsJsonArray("monthly_sales")

                val monthlyReg = parseMonthlyRegistered(regArray)
                val monthlySales = parseMonthlySales(salesArray)
                val stocks = parseStocks(root?.getAsJsonArray("stocks"))
                val expiring = parseExpiringUsers(root?.getAsJsonArray("expiring_users"))

                ApiResult.Success(
                    data = DashboardData(
                        companyName = companyName,
                        currencyCode = currencyCode,
                        periodLabel = periodLabel,
                        cards = cards,
                        monthlyRegistered = monthlyReg,
                        monthlySales = monthlySales,
                        stocks = stocks,
                        expiringUsers = expiring
                    ),
                    message = res.message
                )
            }
        }
    }

    private fun parseMonthlyRegistered(arr: JsonArray?): List<MonthlyCountPoint> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            MonthlyCountPoint(
                month = o.str("month"),
                count = o.intVal("count")
            )
        }
    }

    private fun parseMonthlySales(arr: JsonArray?): List<MonthlySalesPoint> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            MonthlySalesPoint(
                month = o.str("month"),
                total = o.doubleVal("total")
            )
        }
    }

    private fun parseStocks(arr: JsonArray?): List<PlanStockItem> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            PlanStockItem(
                planId = o.intVal("plan_id"),
                namePlan = o.str("name_plan").ifBlank { "-" },
                type = o.str("type").ifBlank { "Hotspot" },
                unused = o.intVal("unused"),
                used = o.intVal("used"),
                activeUsers = o.intVal("active_users")
            )
        }
    }

    private fun parseExpiringUsers(arr: JsonArray?): List<ExpiringUserItem> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
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
        }
    }

    private fun JsonElement?.asObjOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonObject?.str(key: String): String {
        if (this == null || !has(key) || get(key).isJsonNull) return ""
        return try {
            get(key).asString.trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun JsonObject?.intVal(key: String): Int {
        if (this == null || !has(key) || get(key).isJsonNull) return 0
        return try {
            get(key).asInt
        } catch (_: Exception) {
            get(key).asString.toIntOrNull() ?: 0
        }
    }

    private fun JsonObject?.doubleVal(key: String): Double {
        if (this == null || !has(key) || get(key).isJsonNull) return 0.0
        return try {
            get(key).asDouble
        } catch (_: Exception) {
            get(key).asString.toDoubleOrNull() ?: 0.0
        }
    }
}
