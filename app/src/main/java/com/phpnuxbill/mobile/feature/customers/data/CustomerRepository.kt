package com.phpnuxbill.mobile.feature.customers.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.customers.model.CustomerActivePackage
import com.phpnuxbill.mobile.feature.customers.model.CustomerCustomField
import com.phpnuxbill.mobile.feature.customers.model.CustomerFormMetadata
import com.phpnuxbill.mobile.feature.customers.model.CustomerFormState
import com.phpnuxbill.mobile.feature.customers.model.CustomerItem
import com.phpnuxbill.mobile.feature.customers.model.PlanOption
import com.phpnuxbill.mobile.feature.customers.model.RouterCoordHint
import com.phpnuxbill.mobile.feature.services.model.InvoiceReceiptData

/**
 * Kontrak Repository Pelanggan (SOLID - Interface Segregation & Dependency Inversion)
 */
interface CustomerRepository {
    suspend fun getCustomers(
        search: String = "",
        serviceType: String = "All",
        limit: Int = 150
    ): ApiResult<Pair<Int, List<CustomerItem>>>

    suspend fun getFormMetadata(): ApiResult<CustomerFormMetadata>

    suspend fun saveCustomer(form: CustomerFormState): ApiResult<InvoiceReceiptData?>

    suspend fun deleteCustomer(customerId: Int): ApiResult<String>

    suspend fun syncCustomer(customerId: Int): ApiResult<String>

    suspend fun syncAllCustomers(): ApiResult<String>
}

class RemoteCustomerRepository(
    private val networkGateway: NetworkGateway
) : CustomerRepository {

    override suspend fun getCustomers(
        search: String,
        serviceType: String,
        limit: Int
    ): ApiResult<Pair<Int, List<CustomerItem>>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
            if (serviceType.isNotBlank() && !serviceType.equals("All", ignoreCase = true)) {
                put("service_type", serviceType)
            }
            put("limit", limit.toString())
        }

        return when (val res = networkGateway.get(action = "admin_customers", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.takeIf { it.isJsonObject }?.asJsonObject
                val total = root.intVal("total")
                val arr = root?.getAsJsonArray("customers")
                val list = mutableListOf<CustomerItem>()

                arr?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val pkgObj = o.get("active_package")?.asObjOrNull()
                    val activePkg = pkgObj?.let { p ->
                        CustomerActivePackage(
                            id = p.intVal("id"),
                            planId = p.intVal("plan_id"),
                            namebp = p.str("namebp").ifBlank { "-" },
                            status = p.str("status").ifBlank { "off" },
                            expiration = p.str("expiration").ifBlank { "-" },
                            time = p.str("time").ifBlank { "23:59:59" },
                            routers = p.str("routers").ifBlank { "-" },
                            type = p.str("type").ifBlank { "Hotspot" }
                        )
                    }

                    val cfList = mutableListOf<CustomerCustomField>()
                    o.getAsJsonArray("custom_fields")?.forEach { cfEl ->
                        val cfObj = cfEl.asObjOrNull() ?: return@forEach
                        val n = cfObj.str("name")
                        val v = cfObj.str("value")
                        if (n.isNotBlank()) {
                            cfList.add(CustomerCustomField(name = n, value = v))
                        }
                    }

                    list.add(
                        CustomerItem(
                            id = o.intVal("id"),
                            username = o.str("username"),
                            fullname = o.str("fullname").ifBlank { o.str("username") },
                            phonenumber = o.str("phonenumber"),
                            email = o.str("email"),
                            address = o.str("address"),
                            city = o.str("city"),
                            district = o.str("district"),
                            state = o.str("state"),
                            zip = o.str("zip"),
                            balance = o.doubleVal("balance"),
                            serviceType = o.str("service_type").ifBlank { "Hotspot" },
                            accountType = o.str("account_type").ifBlank { "Personal" },
                            status = o.str("status").ifBlank { "Active" },
                            autoRenewal = o.intVal("auto_renewal"),
                            pppoeUsername = o.str("pppoe_username"),
                            pppoePassword = o.str("pppoe_password"),
                            pppoeIp = o.str("pppoe_ip"),
                            coordinates = o.str("coordinates"),
                            customFields = cfList,
                            activePackage = activePkg
                        )
                    )
                }

                ApiResult.Success(
                    data = (if (total > 0) total else list.size) to list,
                    message = res.message
                )
            }
        }
    }

    override suspend fun getFormMetadata(): ApiResult<CustomerFormMetadata> {
        return when (val res = networkGateway.get(action = "admin_plans")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.takeIf { it.isJsonObject }?.asJsonObject
                val plans = mutableListOf<PlanOption>()
                root?.getAsJsonArray("plans")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val type = o.str("type")
                    if (type.equals("Balance", ignoreCase = true)) return@forEach
                    val valNum = o.str("validity")
                    val valUnit = o.str("validity_unit")
                    plans.add(
                        PlanOption(
                            id = o.intVal("id"),
                            namePlan = o.str("name_plan"),
                            type = type.ifBlank { "Hotspot" },
                            price = o.doubleVal("price"),
                            routers = o.str("routers").ifBlank { "Radius" },
                            validityLabel = "$valNum $valUnit".trim()
                        )
                    )
                }

                val usings = mutableListOf<String>()
                root?.getAsJsonArray("usings")?.forEach { el ->
                    if (el.isJsonPrimitive) {
                        val v = el.asString.trim()
                        if (v.isNotBlank()) usings.add(v)
                    }
                }
                if (usings.isEmpty()) usings.add("Cash")
                if (usings.none { it.equals("Recharge Zero", ignoreCase = true) }) {
                    usings.add("Recharge Zero")
                }

                val routerHints = mutableListOf<RouterCoordHint>()
                root?.getAsJsonArray("routers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val rawCoord = o.str("coordinates")
                    val parts = rawCoord.split(",").map { it.trim() }
                    if (parts.size >= 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            routerHints.add(
                                RouterCoordHint(
                                    id = o.intVal("id"),
                                    name = o.str("name"),
                                    lat = lat,
                                    lng = lng,
                                    coverageMeters = o.intVal("coverage").let { if (it > 0) it else 350 }
                                )
                            )
                        }
                    }
                }

                ApiResult.Success(
                    data = CustomerFormMetadata(
                        plans = plans,
                        paymentUsings = usings,
                        routerHints = routerHints
                    )
                )
            }
        }
    }

    override suspend fun saveCustomer(form: CustomerFormState): ApiResult<InvoiceReceiptData?> {
        val validFields = form.customFields.filter { it.name.isNotBlank() && it.value.isNotBlank() }
        val payload = buildMap<String, Any> {
            put("id", form.id)
            put("username", form.username.trim())
            put("fullname", form.fullname.trim())
            if (form.password.isNotBlank()) put("password", form.password)
            put("phonenumber", form.phonenumber.trim())
            put("email", form.email.trim())
            put("address", form.address.trim())
            put("city", form.city.trim())
            put("district", form.district.trim())
            put("state", form.state.trim())
            put("zip", form.zip.trim())
            put("service_type", form.serviceType)
            put("account_type", form.accountType)
            put("status", form.status)
            put("auto_renewal", if (form.autoRenewal) 1 else 0)
            put("pppoe_username", form.pppoeUsername.trim())
            put("pppoe_password", form.pppoePassword.trim())
            put("pppoe_ip", form.pppoeIp.trim())
            put("coordinates", form.coordinates.trim())
            if (form.selectedPlanId > 0) {
                put("ppln", form.selectedPlanId)
                put("using", form.selectedUsing.ifBlank { "Cash" })
            }
            if (form.sendWelcomeMessage) {
                put("send_welcome_message", 1)
                put("wa", if (form.welcomeViaWa) 1 else 0)
                put("sms", if (form.welcomeViaSms) 1 else 0)
                put("mail", if (form.welcomeViaMail) 1 else 0)
                put("inbox", if (form.welcomeViaInbox) 1 else 0)
            }
            put("custom_field_name", validFields.map { it.name.trim() })
            put("custom_field_value", validFields.map { it.value.trim() })
        }

        return when (val res = networkGateway.postJson(action = "admin_customer_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val receiptObj = res.data?.asObjOrNull()?.get("receipt")?.asObjOrNull()
                ApiResult.Success(data = parseReceipt(receiptObj), message = res.message)
            }
        }
    }

    private fun parseReceipt(o: JsonObject?): InvoiceReceiptData? {
        if (o == null) return null
        return InvoiceReceiptData(
            trxId = o.intVal("trx_id"),
            invoice = o.str("invoice").ifBlank { "-" },
            customerId = o.intVal("customer_id"),
            username = o.str("username").ifBlank { "-" },
            fullname = o.str("fullname").ifBlank { o.str("username") },
            phonenumber = o.str("phonenumber"),
            planName = o.str("plan_name").ifBlank { "-" },
            type = o.str("type").ifBlank { "Hotspot" },
            price = o.doubleVal("price"),
            priceFormatted = o.str("price_formatted"),
            method = o.str("method").ifBlank { "Cash" },
            routers = o.str("routers").ifBlank { "-" },
            rechargedAt = o.str("recharged_at"),
            expiration = o.str("expiration"),
            expirationPretty = o.str("expiration_pretty").ifBlank { o.str("expiration") },
            balanceAfter = o.doubleVal("balance_after"),
            balanceFormatted = o.str("balance_formatted"),
            note = o.str("note"),
            waWebText = o.str("wa_web_text")
        )
    }

    override suspend fun deleteCustomer(customerId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_customer_delete",
                fields = mapOf("id" to customerId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun syncCustomer(customerId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_customer_sync",
                fields = mapOf("id" to customerId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun syncAllCustomers(): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_customer_sync",
                fields = mapOf("id" to "0", "sync_all" to "1")
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
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
