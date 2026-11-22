package com.phpnuxbill.mobile.feature.portal.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.portal.model.CustomerBillItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPackageItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface CustomerPortalRepository {
    suspend fun getPortalHome(): ApiResult<CustomerPortalDashboard>
    suspend fun toggleAutoRenewal(): ApiResult<String>
    suspend fun redeemVoucher(code: String): ApiResult<String>
    suspend fun buyWithBalance(planId: Int): ApiResult<String>
    suspend fun checkBillStatus(billId: Int): ApiResult<String>
    suspend fun cancelBill(billId: Int): ApiResult<String>
    suspend fun updateCustomerWifi(ssid: String, password: String): ApiResult<String>
}

class RemoteCustomerPortalRepository(
    private val networkGateway: NetworkGateway
) : CustomerPortalRepository {

    override suspend fun getPortalHome(): ApiResult<CustomerPortalDashboard> = coroutineScope {
        val dashDef = async { networkGateway.get(action = "dashboard") }
        val plansDef = async { networkGateway.get(action = "plans") }

        val dashRes = dashDef.await()
        val plansRes = plansDef.await()
        if (dashRes is ApiResult.Error) return@coroutineScope dashRes

        val root = (dashRes as ApiResult.Success).data?.asObjOrNull()
        val comp = root?.getAsJsonObject("company")
        val cust = root?.getAsJsonObject("customer")

        val pkgs = mutableListOf<CustomerPackageItem>()
        root?.getAsJsonArray("active_packages")?.forEach { el ->
            val o = el.asObjOrNull() ?: return@forEach
            pkgs.add(
                CustomerPackageItem(
                    id = o.intVal("id"),
                    namebp = o.str("namebp"),
                    status = o.str("status"),
                    expiration = o.str("expiration"),
                    routers = o.str("routers"),
                    type = o.str("type")
                )
            )
        }

        val buyPlans = mutableListOf<CustomerBuyPlanItem>()
        if (plansRes is ApiResult.Success) {
            plansRes.data?.asObjOrNull()?.getAsJsonArray("internet_plans")?.forEach { el ->
                val o = el.asObjOrNull() ?: return@forEach
                buyPlans.add(
                    CustomerBuyPlanItem(
                        id = o.intVal("id"),
                        namePlan = o.str("name_plan"),
                        price = o.doubleVal("price"),
                        type = o.str("type"),
                        validity = o.str("validity")
                    )
                )
            }
        }

        val billObj = root?.get("unpaid_bill")?.asObjOrNull()
        val unpaidBill = billObj?.let { b ->
            CustomerBillItem(
                id = b.intVal("id"),
                planName = b.str("plan_name"),
                price = b.doubleVal("price"),
                gateway = b.str("gateway"),
                paymentChannel = b.str("payment_channel"),
                payCode = b.str("pay_code"),
                checkoutUrl = b.str("checkout_url"),
                statusLabel = b.str("status_label")
            )
        }

        ApiResult.Success(
            data = CustomerPortalDashboard(
                companyName = comp.str("name").ifBlank { "PHPNuxBill" },
                currency = comp.str("currency").ifBlank { "Rp." },
                enableBalance = comp?.get("enable_balance")?.asBoolean ?: true,
                customerId = cust.intVal("id"),
                username = cust.str("username"),
                fullname = cust.str("fullname"),
                balance = cust.doubleVal("balance"),
                serviceType = cust.str("service_type"),
                autoRenewal = cust.intVal("auto_renewal") == 1,
                activePackages = pkgs,
                availablePlans = buyPlans,
                unpaidBill = unpaidBill
            )
        )
    }

    override suspend fun toggleAutoRenewal(): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "toggle_renewal")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun redeemVoucher(code: String): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "redeem_voucher", fields = mapOf("code" to code.trim()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun buyWithBalance(planId: Int): ApiResult<String> =
        when (
            val res = networkGateway.postForm(
                action = "buy",
                fields = mapOf("plan_id" to planId.toString(), "gateway" to "balance")
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun checkBillStatus(billId: Int): ApiResult<String> =
        when (val res = networkGateway.get(action = "check_payment", params = mapOf("id" to billId.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun cancelBill(billId: Int): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "cancel_payment", fields = mapOf("id" to billId.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun updateCustomerWifi(ssid: String, password: String): ApiResult<String> =
        when (
            val res = networkGateway.postForm(
                action = "customer_wifi_save",
                fields = mapOf("ssid" to ssid.trim(), "password" to password.trim())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    private fun JsonElement?.asObjOrNull(): JsonObject? = if (this != null && isJsonObject) asJsonObject else null
    private fun JsonObject?.str(k: String): String =
        if (this == null || !has(k) || get(k).isJsonNull) "" else runCatching { get(k).asString.trim() }.getOrDefault("")
    private fun JsonObject?.intVal(k: String): Int =
        if (this == null || !has(k) || get(k).isJsonNull) 0 else runCatching { get(k).asInt }.getOrDefault(0)
    private fun JsonObject?.doubleVal(k: String): Double =
        if (this == null || !has(k) || get(k).isJsonNull) 0.0 else runCatching { get(k).asDouble }.getOrDefault(0.0)
}
