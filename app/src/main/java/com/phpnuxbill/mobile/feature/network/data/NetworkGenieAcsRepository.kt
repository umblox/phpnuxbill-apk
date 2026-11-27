package com.phpnuxbill.mobile.feature.network.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.network.model.GenieAcsConnectedHost
import com.phpnuxbill.mobile.feature.network.model.GenieAcsDeviceDetail
import com.phpnuxbill.mobile.feature.network.model.GenieAcsDeviceItem
import com.phpnuxbill.mobile.feature.network.model.GenieAcsHost
import com.phpnuxbill.mobile.feature.network.model.GenieAcsHostFormState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsStats
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWanEditState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWanItem
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWlanEditState
import com.phpnuxbill.mobile.feature.network.model.GenieAcsWlanItem
import com.phpnuxbill.mobile.feature.network.model.InterfaceTrafficItem
import com.phpnuxbill.mobile.feature.network.model.IpPoolFormState
import com.phpnuxbill.mobile.feature.network.model.IpPoolItem
import com.phpnuxbill.mobile.feature.network.model.OdpFormState
import com.phpnuxbill.mobile.feature.network.model.OdpItem
import com.phpnuxbill.mobile.feature.network.model.OnlineSessionUser
import com.phpnuxbill.mobile.feature.network.model.OnlineUsersSummary
import com.phpnuxbill.mobile.feature.network.model.PortPoolFormState
import com.phpnuxbill.mobile.feature.network.model.PortPoolItem
import com.phpnuxbill.mobile.feature.network.model.RouterFormState
import com.phpnuxbill.mobile.feature.network.model.RouterItem
import com.phpnuxbill.mobile.feature.network.model.RouterLiveStatus
import com.phpnuxbill.mobile.feature.network.model.RouterLogEntry

/**
 * Kontrak Repository Monitoring MikroTik, ODP, Port Pool & Manajemen GenieACS (SOLID - ISP)
 */
interface NetworkGenieAcsRepository {
    // MikroTik, Router & IP Pool
    suspend fun getRouters(): ApiResult<List<RouterItem>>
    suspend fun saveRouter(form: RouterFormState): ApiResult<String>
    suspend fun deleteRouter(routerId: Int): ApiResult<String>
    suspend fun getIpPools(): ApiResult<List<IpPoolItem>>
    suspend fun saveIpPool(form: IpPoolFormState): ApiResult<String>
    suspend fun deleteIpPool(poolId: Int): ApiResult<String>

    // Port Pool (tbl_port_pool)
    suspend fun getPortPools(search: String = ""): ApiResult<List<PortPoolItem>>
    suspend fun savePortPool(form: PortPoolFormState): ApiResult<String>
    suspend fun deletePortPool(portPoolId: Int): ApiResult<String>

    // ODP / Optical Distribution Point (tbl_odps)
    suspend fun getOdps(search: String = ""): ApiResult<List<OdpItem>>
    suspend fun saveOdp(form: OdpFormState): ApiResult<String>
    suspend fun deleteOdp(odpId: Int): ApiResult<String>

    suspend fun getMikrotikLive(routerId: Int = 0, interfaceName: String = ""): ApiResult<List<RouterLiveStatus>>
    suspend fun getOnlineUsers(routerId: Int = 0): ApiResult<OnlineUsersSummary>
    suspend fun kickOnlineUser(routerName: String, service: String, rosId: String, username: String): ApiResult<String>
    suspend fun rebootRouter(routerId: Int): ApiResult<String>
    suspend fun getRouterLogs(routerId: Int): ApiResult<List<RouterLogEntry>>

    // GenieACS TR-069
    suspend fun getGenieAcsHosts(): ApiResult<Pair<List<GenieAcsHost>, GenieAcsHost?>>
    suspend fun saveGenieAcsHost(form: GenieAcsHostFormState): ApiResult<String>
    suspend fun executeHostAction(subAction: String, hostId: String): ApiResult<String>
    suspend fun getGenieAcsDashboardAndDevices(
        hostId: String = "",
        search: String = "",
        statusFilter: String = "all"
    ): ApiResult<Triple<List<GenieAcsHost>, GenieAcsStats?, List<GenieAcsDeviceItem>>>

    suspend fun getGenieAcsDeviceDetail(hostId: String, deviceId: String): ApiResult<GenieAcsDeviceDetail>
    suspend fun executeDeviceAction(
        hostId: String,
        deviceId: String,
        subAction: String,
        tag: String = ""
    ): ApiResult<String>

    suspend fun saveGenieAcsWlan(hostId: String, form: GenieAcsWlanEditState): ApiResult<String>
    suspend fun saveGenieAcsWan(hostId: String, form: GenieAcsWanEditState): ApiResult<String>
}

class RemoteNetworkGenieAcsRepository(
    private val networkGateway: NetworkGateway
) : NetworkGenieAcsRepository {

    override suspend fun getRouters(): ApiResult<List<RouterItem>> {
        return when (val res = networkGateway.get(action = "admin_routers")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<RouterItem>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        RouterItem(
                            id = o.intVal("id"),
                            name = o.str("name"),
                            ipAddress = o.str("ip_address"),
                            username = o.str("username"),
                            description = o.str("description"),
                            coordinates = o.str("coordinates"),
                            coverage = o.str("coverage").ifBlank { "350" },
                            enabled = o.intVal("enabled") == 1
                        )
                    )
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun saveRouter(form: RouterFormState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            put("id", form.id)
            put("name", form.name.trim())
            put("ip_address", form.ipAddress.trim())
            put("username", form.username.trim())
            if (form.password.isNotBlank() || form.id == 0) {
                put("password", form.password)
            }
            put("description", form.description.trim())
            put("coordinates", form.coordinates.trim())
            put("coverage", form.coverage.trim().ifBlank { "350" })
            put("enabled", if (form.enabled) 1 else 0)
            if (form.testConnection) put("testIt", "1")
        }
        return when (val res = networkGateway.postJson(action = "admin_router_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteRouter(routerId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_router_delete",
                fields = mapOf("id" to routerId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getIpPools(): ApiResult<List<IpPoolItem>> {
        return when (val res = networkGateway.get(action = "admin_pools")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<IpPoolItem>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        IpPoolItem(
                            id = o.intVal("id"),
                            poolName = o.str("pool_name"),
                            localIp = o.str("local_ip"),
                            rangeIp = o.str("range_ip"),
                            routers = o.str("routers")
                        )
                    )
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun saveIpPool(form: IpPoolFormState): ApiResult<String> {
        val payload = mapOf<String, Any>(
            "id" to form.id,
            "pool_name" to form.poolName.trim(),
            "local_ip" to form.localIp.trim(),
            "range_ip" to form.rangeIp.trim(),
            "routers" to form.routers.trim()
        )
        return when (val res = networkGateway.postJson(action = "admin_pool_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteIpPool(poolId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_pool_delete",
                fields = mapOf("id" to poolId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    // =========================================================================
    // PORT POOL (tbl_port_pool)
    // =========================================================================

    override suspend fun getPortPools(search: String): ApiResult<List<PortPoolItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
        }
        return when (val res = networkGateway.get(action = "admin_port_pools", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<PortPoolItem>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        PortPoolItem(
                            id = o.intVal("id"),
                            portName = o.str("port_name"),
                            publicIp = o.str("public_ip"),
                            rangePort = o.str("range_port"),
                            routers = o.str("routers")
                        )
                    )
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun savePortPool(form: PortPoolFormState): ApiResult<String> {
        val payload = mapOf<String, Any>(
            "id" to form.id,
            "port_name" to form.portName.trim(),
            "public_ip" to form.publicIp.trim(),
            "range_port" to form.rangePort.trim(),
            "routers" to form.routers.trim()
        )
        return when (val res = networkGateway.postJson(action = "admin_port_pool_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deletePortPool(portPoolId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_port_pool_delete",
                fields = mapOf("id" to portPoolId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    // =========================================================================
    // ODP / OPTICAL DISTRIBUTION POINT (tbl_odps)
    // =========================================================================

    override suspend fun getOdps(search: String): ApiResult<List<OdpItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
        }
        return when (val res = networkGateway.get(action = "admin_odps", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<OdpItem>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        OdpItem(
                            id = o.intVal("id"),
                            name = o.str("name"),
                            portAmount = o.intVal("port_amount"),
                            attenuation = o.doubleVal("attenuation"),
                            address = o.str("address"),
                            coordinates = o.str("coordinates"),
                            coverage = o.intVal("coverage")
                        )
                    )
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun saveOdp(form: OdpFormState): ApiResult<String> {
        val attVal = form.attenuation.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val portVal = (form.portAmount.trim().toIntOrNull() ?: 0).coerceAtLeast(0)
        val covVal = (form.coverage.trim().filter { it.isDigit() }.toIntOrNull() ?: 0).coerceAtLeast(0)
        val payload = mapOf<String, Any>(
            "id" to form.id,
            "name" to form.name.trim(),
            "port_amount" to portVal,
            "attenuation" to attVal,
            "address" to form.address.trim(),
            "coordinates" to form.coordinates.trim(),
            "coverage" to covVal
        )
        return when (val res = networkGateway.postJson(action = "admin_odp_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteOdp(odpId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_odp_delete",
                fields = mapOf("id" to odpId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getMikrotikLive(
        routerId: Int,
        interfaceName: String
    ): ApiResult<List<RouterLiveStatus>> {
        val params = buildMap {
            if (routerId > 0) put("router_id", routerId.toString())
            if (interfaceName.isNotBlank()) put("interface", interfaceName)
        }
        return when (val res = networkGateway.get(action = "mikrotik_live", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<RouterLiveStatus>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val ifNames = mutableListOf<String>()
                    o.getAsJsonArray("interfaces_list")?.forEach { iEl ->
                        if (iEl.isJsonPrimitive) ifNames.add(iEl.asString)
                    }
                    val ifTraffic = mutableListOf<InterfaceTrafficItem>()
                    o.getAsJsonArray("interfaces_traffic")?.forEach { tEl ->
                        val to = tEl.asObjOrNull() ?: return@forEach
                        ifTraffic.add(
                            InterfaceTrafficItem(
                                name = to.str("name"),
                                type = to.str("type"),
                                running = to.boolVal("running"),
                                status = to.str("status"),
                                tx = to.str("tx"),
                                rx = to.str("rx"),
                                total = to.str("total")
                            )
                        )
                    }
                    list.add(
                        RouterLiveStatus(
                            id = o.intVal("id"),
                            name = o.str("name"),
                            ipAddress = o.str("ip_address"),
                            online = o.boolVal("online"),
                            pingMs = o.intVal("ping_ms"),
                            boardName = o.str("board_name").ifBlank { "-" },
                            version = o.str("version").ifBlank { "-" },
                            cpuLoad = o.intVal("cpu_load"),
                            ramPercent = o.doubleVal("ram_percent"),
                            freeMemory = o.str("free_memory").ifBlank { "0 B" },
                            totalMemory = o.str("total_memory").ifBlank { "0 B" },
                            hddPercent = o.doubleVal("hdd_percent"),
                            uptime = o.str("uptime").ifBlank { "-" },
                            hotspotOnline = o.intVal("hotspot_online"),
                            pppoeOnline = o.intVal("pppoe_online"),
                            monitoredInterface = o.str("monitored_interface"),
                            interfacesList = ifNames,
                            interfacesTraffic = ifTraffic,
                            rxBps = o.longVal("rx_bps"),
                            txBps = o.longVal("tx_bps"),
                            rxSpeedLabel = o.str("rx_speed_label").ifBlank { "0 bps" },
                            txSpeedLabel = o.str("tx_speed_label").ifBlank { "0 bps" }
                        )
                    )
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun getOnlineUsers(routerId: Int): ApiResult<OnlineUsersSummary> {
        val params = buildMap {
            if (routerId > 0) put("router_id", routerId.toString())
        }
        return when (val res = networkGateway.get(action = "online_users", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val sumObj = root?.getAsJsonObject("summary")
                val users = mutableListOf<OnlineSessionUser>()
                root?.getAsJsonArray("users")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    users.add(
                        OnlineSessionUser(
                            rosId = o.str("ros_id"),
                            username = o.str("username").ifBlank { "-" },
                            service = o.str("service").ifBlank { "Hotspot" },
                            server = o.str("server"),
                            router = o.str("router"),
                            routerId = o.intVal("router_id"),
                            ipAddress = o.str("ip_address").ifBlank { "-" },
                            macAddress = o.str("mac_address").ifBlank { "-" },
                            uptime = o.str("uptime").ifBlank { "0s" },
                            sessionTime = o.str("session_time").ifBlank { "-" },
                            upload = o.str("upload").ifBlank { "0 B" },
                            download = o.str("download").ifBlank { "0 B" },
                            total = o.str("total").ifBlank { "0 B" }
                        )
                    )
                }
                ApiResult.Success(
                    data = OnlineUsersSummary(
                        totalOnline = root.intVal("total_online").let { if (it > 0) it else users.size },
                        totalUpload = sumObj.str("total_upload").ifBlank { "0 B" },
                        totalDownload = sumObj.str("total_download").ifBlank { "0 B" },
                        totalTraffic = sumObj.str("total_traffic").ifBlank { "0 B" },
                        users = users
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun kickOnlineUser(
        routerName: String,
        service: String,
        rosId: String,
        username: String
    ): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "kick_user",
                fields = mapOf(
                    "router" to routerName,
                    "service" to service.lowercase(),
                    "ros_id" to rosId,
                    "username" to username
                )
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun rebootRouter(routerId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "router_reboot",
                fields = mapOf("id" to routerId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getRouterLogs(routerId: Int): ApiResult<List<RouterLogEntry>> {
        val params = buildMap {
            if (routerId > 0) put("router_id", routerId.toString())
            put("sub_type", "logs")
        }
        return when (val res = networkGateway.get(action = "mikrotik_extra", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val logs = mutableListOf<RouterLogEntry>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    logs.add(
                        RouterLogEntry(
                            time = o.str("time"),
                            topics = o.str("topics"),
                            message = o.str("message")
                        )
                    )
                }
                ApiResult.Success(data = logs, message = res.message)
            }
        }
    }

    // =========================================================================
    // GENIEACS TR-069
    // =========================================================================

    override suspend fun getGenieAcsHosts(): ApiResult<Pair<List<GenieAcsHost>, GenieAcsHost?>> {
        return when (val res = networkGateway.get(action = "admin_genieacs_hosts")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val hosts = parseHostList(root)
                val activeObj = root?.get("active_host")?.asObjOrNull()
                val activeHost = activeObj?.let { parseSingleHost(it) }
                ApiResult.Success(data = hosts to activeHost, message = res.message)
            }
        }
    }

    override suspend fun saveGenieAcsHost(form: GenieAcsHostFormState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            if (form.id.isNotBlank()) put("id", form.id)
            put("name", form.name.trim())
            put("host", form.host.trim())
            put("port", form.port.trim().toIntOrNull() ?: 7557)
            put("protocol", form.protocol)
            put("username", form.username.trim())
            if (form.password.isNotBlank()) put("password", form.password)
        }
        return when (val res = networkGateway.postJson(action = "admin_genieacs_host_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun executeHostAction(subAction: String, hostId: String): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_genieacs_host_action",
                fields = mapOf("sub_action" to subAction, "id" to hostId)
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getGenieAcsDashboardAndDevices(
        hostId: String,
        search: String,
        statusFilter: String
    ): ApiResult<Triple<List<GenieAcsHost>, GenieAcsStats?, List<GenieAcsDeviceItem>>> {
        val params = buildMap {
            if (hostId.isNotBlank()) put("host_id", hostId)
            if (search.isNotBlank()) put("search", search.trim())
            if (statusFilter.isNotBlank() && !statusFilter.equals("all", ignoreCase = true)) {
                put("status_filter", statusFilter)
            }
            put("limit", "100")
        }

        return when (val res = networkGateway.get(action = "admin_genieacs_devices", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val hosts = parseHostList(root)
                val devices = mutableListOf<GenieAcsDeviceItem>()
                root?.getAsJsonArray("devices")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    devices.add(parseDeviceItem(o))
                }

                // Prioritaskan objek stats global dari server; fallback hitung lokal hanya saat filter "all"
                val statsObj = root?.get("stats")?.asObjOrNull()
                val stats: GenieAcsStats? = when {
                    statsObj != null -> GenieAcsStats(
                        total = statsObj.intVal("total"),
                        online = statsObj.intVal("online"),
                        offline = statsObj.intVal("offline"),
                        badRxCount = statsObj.intVal("bad_rx_count"),
                        highTempCount = statsObj.intVal("high_temp_count")
                    )
                    statusFilter.isBlank() || statusFilter.equals("all", ignoreCase = true) -> GenieAcsStats(
                        total = root.intVal("total").let { if (it > 0) it else devices.size },
                        online = devices.count { it.online },
                        offline = devices.count { !it.online },
                        badRxCount = devices.count {
                            it.isOptical && it.rxPowerStatus in listOf("critical", "los", "warning")
                        },
                        highTempCount = devices.count {
                            it.isOptical && it.tempStatus in listOf("warning", "critical")
                        }
                    )
                    else -> null
                }

                ApiResult.Success(data = Triple(hosts, stats, devices), message = res.message)
            }
        }
    }

    override suspend fun getGenieAcsDeviceDetail(
        hostId: String,
        deviceId: String
    ): ApiResult<GenieAcsDeviceDetail> {
        val params = buildMap {
            if (hostId.isNotBlank()) put("host_id", hostId)
            put("id", deviceId)
        }
        return when (val res = networkGateway.get(action = "admin_genieacs_device_detail", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val devObj = root?.getAsJsonObject("device")
                    ?: return ApiResult.Error("Device detail payload missing.")
                val device = parseDeviceItem(devObj)

                val wans = mutableListOf<GenieAcsWanItem>()
                root.getAsJsonArray("wan_connections")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    wans.add(
                        GenieAcsWanItem(
                            index = o.intVal("index"),
                            type = o.str("type").ifBlank { "ppp" },
                            name = o.str("name"),
                            username = o.str("username"),
                            password = o.str("password"),
                            vlan = o.str("vlan"),
                            externalIp = o.str("external_ip").ifBlank { "-" },
                            connectionStatus = o.str("connection_status").ifBlank { "-" },
                            nat = o.boolVal("nat"),
                            enable = o.boolVal("enable"),
                            uptime = o.str("uptime").ifBlank { "-" }
                        )
                    )
                }

                val wlans = mutableListOf<GenieAcsWlanItem>()
                root.getAsJsonArray("wlan_configs")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    wlans.add(
                        GenieAcsWlanItem(
                            index = o.intVal("index"),
                            ssid = o.str("ssid"),
                            password = o.str("password"),
                            enable = o.boolVal("enable"),
                            channel = o.str("channel").ifBlank { "Auto" },
                            band = o.str("band").ifBlank { "2.4GHz" },
                            securityMode = o.str("security_mode").ifBlank { "WPA2PSK" },
                            connectedDevices = o.intVal("connected_devices"),
                            source = o.str("source").ifBlank { "tr098" }
                        )
                    )
                }

                val hosts = mutableListOf<GenieAcsConnectedHost>()
                root.getAsJsonArray("connected_hosts")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    hosts.add(
                        GenieAcsConnectedHost(
                            index = o.intVal("index"),
                            hostname = o.str("hostname").ifBlank { "Client" },
                            ipAddress = o.str("ip_address").ifBlank { "-" },
                            macAddress = o.str("mac_address").ifBlank { "-" },
                            interfaceType = o.str("interface_type").ifBlank { "WiFi/LAN" },
                            active = o.boolVal("active"),
                            signal = o.str("signal")
                        )
                    )
                }

                val availIdx = root.get("avail_wan_index")?.takeIf { !it.isJsonNull }?.asInt
                val tasksCnt = root.getAsJsonArray("pending_tasks")?.size() ?: 0

                ApiResult.Success(
                    data = GenieAcsDeviceDetail(
                        device = device,
                        wanConnections = wans,
                        availWanIndex = availIdx,
                        wlanConfigs = wlans,
                        connectedHosts = hosts,
                        pendingTasksCount = tasksCnt
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun executeDeviceAction(
        hostId: String,
        deviceId: String,
        subAction: String,
        tag: String
    ): ApiResult<String> {
        val fields = buildMap {
            if (hostId.isNotBlank()) put("host_id", hostId)
            if (deviceId.isNotBlank()) put("id", deviceId)
            put("sub_action", subAction)
            if (tag.isNotBlank()) put("tag", tag)
        }
        return when (val res = networkGateway.postForm(action = "admin_genieacs_device_action", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun saveGenieAcsWlan(hostId: String, form: GenieAcsWlanEditState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            if (hostId.isNotBlank()) put("host_id", hostId)
            put("id", form.deviceId)
            put("wlan_index", form.wlanIndex)
            put("ssid", form.ssid.trim())
            if (form.password.isNotBlank()) put("password", form.password)
            put("security_mode", form.securityMode)
            put("channel", form.channel)
            put("enable", if (form.enable) "1" else "0")
            put("source", form.source)
        }
        return when (val res = networkGateway.postJson(action = "admin_genieacs_wlan_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun saveGenieAcsWan(hostId: String, form: GenieAcsWanEditState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            if (hostId.isNotBlank()) put("host_id", hostId)
            put("id", form.deviceId)
            put("connection_index", form.connectionIndex)
            put("connection_type", form.connectionType)
            put("username", form.username.trim())
            if (form.password.isNotBlank()) put("password", form.password)
            if (form.vlan.isNotBlank()) put("vlan", form.vlan.trim())
            put("nat", if (form.nat) "1" else "0")
            put("enable", if (form.enable) "1" else "0")
        }
        return when (val res = networkGateway.postJson(action = "admin_genieacs_wan_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    private fun parseHostList(root: JsonObject?): List<GenieAcsHost> {
        val list = mutableListOf<GenieAcsHost>()
        root?.getAsJsonArray("hosts")?.forEach { el ->
            val o = el.asObjOrNull() ?: return@forEach
            list.add(parseSingleHost(o))
        }
        return list
    }

    private fun parseSingleHost(o: JsonObject): GenieAcsHost {
        return GenieAcsHost(
            id = o.str("id"),
            name = o.str("name"),
            host = o.str("host"),
            port = o.intVal("port").let { if (it > 0) it else 7557 },
            protocol = o.str("protocol").ifBlank { "http" },
            username = o.str("username"),
            active = o.boolVal("active"),
            fullUrl = o.str("full_url")
        )
    }

    private fun parseDeviceItem(o: JsonObject): GenieAcsDeviceItem {
        val tagList = mutableListOf<String>()
        o.getAsJsonArray("tags")?.forEach { tEl ->
            if (tEl.isJsonPrimitive) tagList.add(tEl.asString)
        }
        return GenieAcsDeviceItem(
            id = o.str("id").ifBlank { o.str("_id") },
            serialNumber = o.str("serial_number").ifBlank { "-" },
            manufacturer = o.str("manufacturer").ifBlank { "-" },
            model = o.str("model").ifBlank { "-" },
            softwareVersion = o.str("software_version").ifBlank { "-" },
            hardwareVersion = o.str("hardware_version").ifBlank { "-" },
            uptime = o.str("uptime").ifBlank { "-" },
            pppoeUsername = o.str("pppoe_username").ifBlank { "N/A" },
            wanIp = o.str("wan_ip").ifBlank { "N/A" },
            tr069Ip = o.str("tr069_ip").ifBlank { "N/A" },
            isOptical = o.boolVal("is_optical"),
            rxPower = o.str("rx_power").ifBlank { "N/A" },
            rxPowerStatus = o.str("rx_power_status").ifBlank { "none" },
            txPower = o.str("tx_power").ifBlank { "N/A" },
            temperature = o.str("temperature").ifBlank { "N/A" },
            tempStatus = o.str("temp_status").ifBlank { "none" },
            online = o.boolVal("online"),
            connectedDevices = o.intVal("connected_devices"),
            customerId = o.intVal("customer_id"),
            fullname = o.str("fullname"),
            phonenumber = o.str("phonenumber"),
            lastInform = o.str("last_inform").ifBlank { "-" },
            tags = tagList
        )
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

    private fun JsonObject?.longVal(key: String): Long {
        if (this == null || !has(key) || get(key).isJsonNull) return 0L
        return try {
            get(key).asLong
        } catch (_: Exception) {
            get(key).asString.toLongOrNull() ?: 0L
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
            val prim = get(key).asJsonPrimitive
            if (prim.isBoolean) prim.asBoolean
            else if (prim.isNumber) prim.asInt != 0
            else prim.asString.equals("true", ignoreCase = true) || prim.asString == "1"
        } catch (_: Exception) {
            false
        }
    }
}
