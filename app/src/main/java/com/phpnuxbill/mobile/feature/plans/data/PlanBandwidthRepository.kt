package com.phpnuxbill.mobile.feature.plans.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.plans.model.BandwidthFormState
import com.phpnuxbill.mobile.feature.plans.model.BandwidthItem
import com.phpnuxbill.mobile.feature.plans.model.BurstPresetItem
import com.phpnuxbill.mobile.feature.plans.model.PlanBandwidthMetadata
import com.phpnuxbill.mobile.feature.plans.model.PlanFormState
import com.phpnuxbill.mobile.feature.plans.model.PlanItem
import com.phpnuxbill.mobile.feature.plans.model.PlanPoolOption
import com.phpnuxbill.mobile.feature.plans.model.PlanRouterOption
import kotlin.math.roundToInt

/**
 * Kontrak Repository Paket & Bandwidth (SOLID - Interface Segregation Principle)
 */
interface PlanBandwidthRepository {
    suspend fun getPlansAndMetadata(
        typeFilter: String = "All",
        routerFilter: String = "All"
    ): ApiResult<Pair<List<PlanItem>, PlanBandwidthMetadata>>

    suspend fun savePlan(form: PlanFormState): ApiResult<String>

    suspend fun deletePlan(planId: Int): ApiResult<String>

    suspend fun syncPlans(type: String = "all"): ApiResult<String>

    suspend fun getBandwidths(search: String = ""): ApiResult<List<BandwidthItem>>

    suspend fun saveBandwidth(form: BandwidthFormState): ApiResult<String>

    suspend fun deleteBandwidth(bwId: Int): ApiResult<String>
}

class RemotePlanBandwidthRepository(
    private val networkGateway: NetworkGateway
) : PlanBandwidthRepository {

    override suspend fun getPlansAndMetadata(
        typeFilter: String,
        routerFilter: String
    ): ApiResult<Pair<List<PlanItem>, PlanBandwidthMetadata>> {
        val params = buildMap {
            if (typeFilter.isNotBlank() && !typeFilter.equals("All", ignoreCase = true)) {
                put("type", typeFilter)
            }
            if (routerFilter.isNotBlank() && !routerFilter.equals("All", ignoreCase = true)) {
                put("routers", routerFilter)
            }
        }

        return when (val res = networkGateway.get(action = "admin_plans", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()

                val supportedPlanTypes = root?.getAsJsonArray("supported_plan_types").toStringList()
                    .ifEmpty { listOf("Hotspot", "PPPOE", "Balance") }
                val supportsVpn = if (root != null && root.has("supports_vpn")) {
                    root.boolVal("supports_vpn")
                } else {
                    supportedPlanTypes.any { it.equals("VPN", ignoreCase = true) }
                }

                val plans = mutableListOf<PlanItem>()
                root?.getAsJsonArray("plans")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    plans.add(
                        PlanItem(
                            id = o.intVal("id"),
                            namePlan = o.str("name_plan"),
                            type = o.str("type").ifBlank { "Hotspot" },
                            price = o.doubleVal("price"),
                            priceOld = o.str("price_old"),
                            idBw = o.intVal("id_bw"),
                            bandwidthName = o.str("bandwidth_name").ifBlank { "-" },
                            rateLabel = o.str("rate_label").ifBlank { "-" },
                            validity = o.intVal("validity"),
                            validityUnit = o.str("validity_unit").ifBlank { "Days" },
                            routers = o.str("routers"),
                            isRadius = o.intVal("is_radius") == 1,
                            pool = o.str("pool"),
                            device = o.str("device"),
                            enabled = o.intVal("enabled") == 1,
                            prepaid = o.str("prepaid").ifBlank { "yes" },
                            planType = o.str("plan_type").ifBlank { "Personal" },
                            typebp = o.str("typebp").ifBlank { "Unlimited" },
                            limitType = o.str("limit_type").ifBlank { "Time_Limit" },
                            timeLimit = o.intVal("time_limit"),
                            timeUnit = o.str("time_unit").ifBlank { "Hrs" },
                            dataLimit = o.doubleVal("data_limit"),
                            dataUnit = o.str("data_unit").ifBlank { "MB" },
                            sharedUsers = o.intVal("shared_users").let { if (it > 0) it else 1 },
                            expiredDate = o.intVal("expired_date").let { if (it in 1..28) it else 20 },
                            planExpired = o.intVal("plan_expired"),
                            activeUsers = o.intVal("active_users"),
                            billingLabel = o.str("billing_label")
                        )
                    )
                }

                val bandwidths = mutableListOf<BandwidthItem>()
                root?.getAsJsonArray("bandwidths")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    bandwidths.add(parseBandwidthObject(o))
                }

                val presets = mutableListOf<BurstPresetItem>()
                root?.getAsJsonArray("burst_presets")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val burstArr = mutableListOf<String>()
                    o.getAsJsonArray("burst")?.forEach { bEl ->
                        if (bEl.isJsonPrimitive) burstArr.add(bEl.asString)
                    }
                    if (burstArr.size >= 5) {
                        presets.add(
                            BurstPresetItem(
                                label = o.str("label"),
                                group = o.str("group").ifBlank { "2x" },
                                rateDown = o.intVal("rate_down"),
                                rateDownUnit = o.str("rate_down_unit").ifBlank { "Mbps" },
                                rateUp = o.intVal("rate_up"),
                                rateUpUnit = o.str("rate_up_unit").ifBlank { "Mbps" },
                                burst = burstArr.take(5)
                            )
                        )
                    }
                }

                val defaultDevices = if (supportsVpn) {
                    listOf("MikrotikHotspot", "MikrotikPppoe", "MikrotikVpn", "Radius", "Dummy")
                } else {
                    listOf("MikrotikHotspot", "MikrotikPppoe", "Radius", "Dummy")
                }
                val rawDevices = root?.getAsJsonArray("devices").toStringList()
                val devices = rawDevices
                    .filter { dev -> supportsVpn || !dev.contains("vpn", ignoreCase = true) }
                    .ifEmpty { defaultDevices }

                val planTypes = root?.getAsJsonArray("plan_types").toStringList()
                    .ifEmpty { listOf("Personal", "Business") }
                val typebps = root?.getAsJsonArray("typebps").toStringList()
                    .ifEmpty { listOf("Unlimited", "Limited") }
                val limitTypes = root?.getAsJsonArray("limit_types").toStringList()
                    .ifEmpty { listOf("Time_Limit", "Data_Limit", "Both_Limit") }
                val validityUnits = root?.getAsJsonArray("validity_units").toStringList()
                    .ifEmpty { listOf("Mins", "Hrs", "Days", "Months", "Period") }

                val routers = mutableListOf<PlanRouterOption>()
                root?.getAsJsonArray("routers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    routers.add(PlanRouterOption(id = o.intVal("id"), name = o.str("name")))
                }

                val pools = mutableListOf<PlanPoolOption>()
                root?.getAsJsonArray("pools")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    pools.add(
                        PlanPoolOption(
                            id = o.intVal("id"),
                            poolName = o.str("pool_name"),
                            rangeIp = o.str("range_ip"),
                            routers = o.str("routers")
                        )
                    )
                }

                ApiResult.Success(
                    data = plans to PlanBandwidthMetadata(
                        supportedPlanTypes = supportedPlanTypes,
                        supportsVpn = supportsVpn,
                        bandwidths = bandwidths,
                        burstPresets = presets,
                        devices = devices,
                        planTypes = planTypes,
                        typebps = typebps,
                        limitTypes = limitTypes,
                        validityUnits = validityUnits,
                        routers = routers,
                        pools = pools
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun savePlan(form: PlanFormState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            put("id", form.id)
            put("type", form.type)
            put("name_plan", form.namePlan.trim())
            put("price", form.price.trim().toDoubleOrNull() ?: 0.0)
            if (form.priceOld.isNotBlank()) {
                put("price_old", form.priceOld.trim().toDoubleOrNull() ?: 0.0)
            }
            put("enabled", if (form.enabled) 1 else 0)
            put("prepaid", form.prepaid)

            if (!form.type.equals("Balance", ignoreCase = true)) {
                put("plan_type", form.planType)
                put("id_bw", form.idBw)
                put("validity", form.validity.trim().toIntOrNull() ?: 30)
                put("validity_unit", form.validityUnit)
                put("is_radius", if (form.isRadius) 1 else 0)
                put("routers", if (form.isRadius) "" else form.routers.trim())
                put("pool_name", form.pool.trim())
                put("device", form.device.trim())
                put("expired_date", form.expiredDate.trim().toIntOrNull() ?: 20)
                put("plan_expired", form.planExpired)

                if (form.type.equals("Hotspot", ignoreCase = true)) {
                    put("typebp", form.typebp)
                    put("limit_type", form.limitType)
                    put("time_limit", (form.timeLimit.trim().toIntOrNull() ?: 0).coerceAtLeast(0))
                    put("time_unit", form.timeUnit)
                    put("data_limit", (form.dataLimit.trim().toDoubleOrNull()?.roundToInt() ?: 0).coerceAtLeast(0))
                    put("data_unit", form.dataUnit)
                    put("shared_users", (form.sharedUsers.trim().toIntOrNull() ?: 1).coerceAtLeast(1))
                }
            }
        }

        return when (val res = networkGateway.postJson(action = "admin_plan_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deletePlan(planId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_plan_delete",
                fields = mapOf("id" to planId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun syncPlans(type: String): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_plan_sync",
                fields = mapOf("type" to type)
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getBandwidths(search: String): ApiResult<List<BandwidthItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
        }
        return when (val res = networkGateway.get(action = "admin_bandwidths", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<BandwidthItem>()
                val arr = res.data?.takeIf { it.isJsonArray }?.asJsonArray
                arr?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(parseBandwidthObject(o))
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun saveBandwidth(form: BandwidthFormState): ApiResult<String> {
        val fiveBurst = listOf(
            form.burstLimit.trim(),
            form.burstThreshold.trim(),
            form.burstTime.trim(),
            form.priority.trim(),
            form.limitAt.trim()
        )
        val hasCompleteBurst = fiveBurst.all { it.isNotEmpty() }

        val rateDownInt = (form.rateDown.trim().toDoubleOrNull()?.roundToInt() ?: 0).coerceAtLeast(0)
        val rateUpInt = (form.rateUp.trim().toDoubleOrNull()?.roundToInt() ?: 0).coerceAtLeast(0)

        val payload = buildMap<String, Any> {
            put("id", form.id)
            put("name_bw", form.nameBw.trim())
            put("rate_down", rateDownInt)
            put("rate_down_unit", form.rateDownUnit)
            put("rate_up", rateUpInt)
            put("rate_up_unit", form.rateUpUnit)
            if (hasCompleteBurst) {
                put("burst", fiveBurst)
            } else {
                put("burst", emptyList<String>())
            }
        }

        return when (val res = networkGateway.postJson(action = "admin_bandwidth_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteBandwidth(bwId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_bandwidth_delete",
                fields = mapOf("id" to bwId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    private fun parseBandwidthObject(o: JsonObject): BandwidthItem {
        val parts = mutableListOf<String>()
        o.getAsJsonArray("burst_parts")?.forEach { p ->
            if (p.isJsonPrimitive) parts.add(p.asString)
        }
        val rawBurst = o.str("burst")
        if (parts.isEmpty() && rawBurst.isNotBlank()) {
            parts.addAll(rawBurst.split(Regex("\\s+")).filter { it.isNotBlank() })
        }
        return BandwidthItem(
            id = o.intVal("id"),
            nameBw = o.str("name_bw"),
            rateDown = o.doubleVal("rate_down"),
            rateDownUnit = o.str("rate_down_unit").ifBlank { "Mbps" },
            rateUp = o.doubleVal("rate_up"),
            rateUpUnit = o.str("rate_up_unit").ifBlank { "Mbps" },
            burst = rawBurst,
            burstParts = parts
        )
    }

    private fun com.google.gson.JsonArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = mutableListOf<String>()
        forEach { el ->
            if (el.isJsonPrimitive) {
                val s = el.asString.trim()
                if (s.isNotBlank()) out.add(s)
            }
        }
        return out
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

    private fun JsonObject?.boolVal(key: String): Boolean {
        if (this == null || !has(key) || get(key).isJsonNull) return false
        return try {
            val el = get(key)
            if (el.isJsonPrimitive && el.asJsonPrimitive.isBoolean) {
                el.asBoolean
            } else {
                val s = el.asString.trim().lowercase()
                s == "1" || s == "true" || s == "yes"
            }
        } catch (_: Exception) {
            false
        }
    }
}
