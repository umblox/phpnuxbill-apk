package com.phpnuxbill.mobile.feature.network.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

// =========================================================================
// MODEL MIKROTIK, ROUTER, IP POOL, PORT POOL & ODP
// =========================================================================

data class RouterItem(
    val id: Int,
    val name: String,
    val ipAddress: String,
    val username: String,
    val description: String,
    val coordinates: String,
    val coverage: String,
    val enabled: Boolean
)

data class IpPoolItem(
    val id: Int,
    val poolName: String,
    val localIp: String,
    val rangeIp: String,
    val routers: String
)

data class PortPoolItem(
    val id: Int,
    val portName: String,
    val publicIp: String,
    val rangePort: String,
    val routers: String
)

data class OdpItem(
    val id: Int,
    val name: String,
    val portAmount: Int,
    val attenuation: Double,
    val address: String,
    val coordinates: String,
    val coverage: Int
)

data class InterfaceTrafficItem(
    val name: String,
    val type: String,
    val running: Boolean,
    val status: String,
    val tx: String,
    val rx: String,
    val total: String
)

data class RouterLiveStatus(
    val id: Int,
    val name: String,
    val ipAddress: String,
    val online: Boolean,
    val pingMs: Int,
    val boardName: String,
    val version: String,
    val cpuLoad: Int,
    val ramPercent: Double,
    val freeMemory: String,
    val totalMemory: String,
    val hddPercent: Double,
    val uptime: String,
    val hotspotOnline: Int,
    val pppoeOnline: Int,
    val monitoredInterface: String,
    val interfacesList: List<String> = emptyList(),
    val interfacesTraffic: List<InterfaceTrafficItem> = emptyList(),
    val rxBps: Long = 0L,
    val txBps: Long = 0L,
    val rxSpeedLabel: String = "0 bps",
    val txSpeedLabel: String = "0 bps"
)

data class TrafficSpeedSample(
    val timestampMs: Long,
    val rxBps: Long,
    val txBps: Long
)

data class OnlineSessionUser(
    val rosId: String,
    val username: String,
    val service: String, // Hotspot | PPPoE
    val server: String,
    val router: String,
    val routerId: Int,
    val ipAddress: String,
    val macAddress: String,
    val uptime: String,
    val sessionTime: String,
    val upload: String,
    val download: String,
    val total: String
)

data class OnlineUsersSummary(
    val totalOnline: Int = 0,
    val totalUpload: String = "0 B",
    val totalDownload: String = "0 B",
    val totalTraffic: String = "0 B",
    val users: List<OnlineSessionUser> = emptyList()
)

data class RouterLogEntry(
    val time: String,
    val topics: String,
    val message: String
)

data class RouterFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val name: String = "",
    val ipAddress: String = "",
    val username: String = "",
    val password: String = "",
    val description: String = "",
    val coordinates: String = "",
    val coverage: String = "350",
    val enabled: Boolean = true,
    val testConnection: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

data class IpPoolFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val poolName: String = "",
    val localIp: String = "",
    val rangeIp: String = "",
    val routers: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class PortPoolFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val portName: String = "",
    val publicIp: String = "",
    val rangePort: String = "",
    val routers: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class OdpFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val name: String = "",
    val portAmount: String = "8",
    val attenuation: String = "0.00",
    val address: String = "",
    val coordinates: String = "",
    val coverage: String = "150",
    val isSaving: Boolean = false,
    val error: String? = null
)

// =========================================================================
// MODEL GENIEACS TR-069
// =========================================================================

data class GenieAcsHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val username: String,
    val active: Boolean,
    val fullUrl: String
)

data class GenieAcsStats(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val badRxCount: Int = 0,
    val highTempCount: Int = 0
)

data class GenieAcsDeviceItem(
    val id: String,
    val serialNumber: String,
    val manufacturer: String,
    val model: String,
    val softwareVersion: String,
    val hardwareVersion: String,
    val uptime: String,
    val pppoeUsername: String,
    val wanIp: String,
    val tr069Ip: String,
    val isOptical: Boolean,
    val rxPower: String,
    val rxPowerStatus: String,
    val txPower: String,
    val temperature: String,
    val tempStatus: String,
    val online: Boolean,
    val connectedDevices: Int,
    val customerId: Int,
    val fullname: String,
    val phonenumber: String,
    val lastInform: String,
    val tags: List<String> = emptyList()
)

data class GenieAcsWlanItem(
    val index: Int,
    val ssid: String,
    val password: String,
    val enable: Boolean,
    val channel: String,
    val band: String,
    val securityMode: String,
    val connectedDevices: Int,
    val source: String
)

data class GenieAcsWanItem(
    val index: Int,
    val type: String, // ppp | ip
    val name: String,
    val username: String,
    val password: String,
    val vlan: String,
    val externalIp: String,
    val connectionStatus: String,
    val nat: Boolean,
    val enable: Boolean,
    val uptime: String
)

data class GenieAcsConnectedHost(
    val index: Int,
    val hostname: String,
    val ipAddress: String,
    val macAddress: String,
    val interfaceType: String,
    val active: Boolean,
    val signal: String
)

data class GenieAcsDeviceDetail(
    val device: GenieAcsDeviceItem,
    val wanConnections: List<GenieAcsWanItem> = emptyList(),
    val availWanIndex: Int? = null,
    val wlanConfigs: List<GenieAcsWlanItem> = emptyList(),
    val connectedHosts: List<GenieAcsConnectedHost> = emptyList(),
    val pendingTasksCount: Int = 0
)

data class GenieAcsHostFormState(
    val isOpen: Boolean = false,
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: String = "7557",
    val protocol: String = "http",
    val username: String = "",
    val password: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class GenieAcsWlanEditState(
    val isOpen: Boolean = false,
    val deviceId: String = "",
    val wlanIndex: Int = 1,
    val ssid: String = "",
    val password: String = "",
    val securityMode: String = "WPA2PSK",
    val channel: String = "Auto",
    val enable: Boolean = true,
    val source: String = "tr098",
    val isSaving: Boolean = false,
    val error: String? = null
)

data class GenieAcsWanEditState(
    val isOpen: Boolean = false,
    val deviceId: String = "",
    val connectionIndex: Int = 1,
    val connectionType: String = "ppp",
    val username: String = "",
    val password: String = "",
    val vlan: String = "",
    val nat: Boolean = true,
    val enable: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

class NetworkGenieAcsLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    // MikroTik Tabs & Labels
    val tabLiveMonitor = if (isEn) "Live Monitor" else "Monitor Live"
    val tabOnlineUsers = if (isEn) "Online Users" else "User Online"
    val tabRoutersPools = if (isEn) "Routers & ODP" else "Router & ODP"
    val tabRouterLogs = if (isEn) "Logs" else "Log Router"
    val selectRouter = if (isEn) "Select Router" else "Pilih Router"
    val monitoredInterface = if (isEn) "Monitored Interface" else "Interface Dipantau"
    val liveTrafficTitle = if (isEn) "Real-Time Interface Traffic" else "Trafik Interface Real-Time"
    val interfaceStatsTitle = if (isEn) "All Router Interfaces" else "Daftar Interface Router"
    val addRouter = if (isEn) "Add Router" else "Tambah Router"
    val editRouter = if (isEn) "Edit Router" else "Edit Router"
    val routerNameLabel = if (isEn) "Router Name" else "Nama Router"
    val routerIpPortLabel = if (isEn) "IP Address / Host:Port" else "Alamat IP / Host:Port"
    val testConnectionToggle = if (isEn) "Test RouterOS API Connection on Save" else "Tes Koneksi API RouterOS Saat Simpan"
    val addIpPool = if (isEn) "Add IP Pool" else "Tambah IP Pool"
    val editIpPool = if (isEn) "Edit IP Pool" else "Edit IP Pool"
    val poolNameLabel = if (isEn) "Pool Name" else "Nama IP Pool"
    val rangeIpLabel = if (isEn) "IP Range (e.g. 10.10.10.2-10.10.10.254)" else "Rentang IP (mis. 10.10.10.2-10.10.10.254)"
    val localIpLabel = if (isEn) "Local / Gateway IP (Optional)" else "IP Lokal / Gateway (Opsional)"
    val rebootConfirmMsg = if (isEn) "Reboot this MikroTik router now?" else "Mulai ulang (Reboot) router MikroTik ini sekarang?"
    val kickConfirmMsg = if (isEn) "Disconnect this active user session?" else "Putuskan sesi pengguna aktif ini dari router?"

    // Port Pool (tbl_port_pool) Labels
    val portPoolsTitle = if (isEn) "Port Pools" else "Port Pool"
    val addPortPool = if (isEn) "Add Port Pool" else "Tambah Port Pool"
    val editPortPool = if (isEn) "Edit Port Pool" else "Edit Port Pool"
    val portNameLabel = if (isEn) "Port Pool Name (3-30 chars)" else "Nama Port Pool (3-30 karakter)"
    val publicIpLabel = if (isEn) "Public IP Address" else "Alamat IP Publik"
    val rangePortLabel = if (isEn) "Port Range (e.g. 10000-20000)" else "Rentang Port (mis. 10000-20000)"
    val portPoolRuleHint = if (isEn) {
        "Each router can only have 1 port range (used for NAT / remote port mapping)."
    } else {
        "Setiap router hanya boleh memiliki 1 rentang port (untuk pemetaan port NAT / remote)."
    }
    val deletePortPoolConfirm = if (isEn) "Delete this Port Pool permanently?" else "Hapus Port Pool ini secara permanen?"

    // ODP / Optical Distribution Point (tbl_odps) Labels
    val odpsTitle = if (isEn) "ODP List" else "Daftar ODP"
    val addOdp = if (isEn) "Add ODP" else "Tambah ODP"
    val editOdp = if (isEn) "Edit ODP" else "Edit ODP"
    val odpNameLabel = if (isEn) "ODP Name (5-15 chars)" else "Nama ODP (5-15 karakter)"
    val odpPortAmountLabel = if (isEn) "Port Capacity (e.g. 8 / 16)" else "Jumlah Port (mis. 8 / 16)"
    val odpAttenuationLabel = if (isEn) "Attenuation (dB)" else "Redaman / Attenuation (dB)"
    val odpAddressLabel = if (isEn) "Location Address" else "Alamat Lokasi ODP"
    val odpCoordinatesLabel = if (isEn) "Coordinates (Lat, Lng)" else "Koordinat (Lat, Lng)"
    val odpCoverageLabel = if (isEn) "Coverage Radius (meters)" else "Radius Cakupan (meter)"
    val pickCoordinatesMap = if (isEn) "Pick on Map" else "Pilih di Peta"
    val deleteOdpConfirm = if (isEn) "Delete this ODP point permanently?" else "Hapus titik ODP ini secara permanen?"

    // GenieACS Labels
    val manageHosts = if (isEn) "GenieACS Servers" else "Server GenieACS"
    val addHost = if (isEn) "Add GenieACS Server" else "Tambah Server GenieACS"
    val hostNameLabel = if (isEn) "Server Label (e.g. ACS Utama)" else "Label Server (mis. ACS Utama)"
    val hostAddressLabel = if (isEn) "Host / IP / Domain" else "Host / IP / Domain"
    val testHost = if (isEn) "Test" else "Tes"
    val cleanJunkBots = if (isEn) "Clean Junk" else "Bersihkan Sampah"
    val badRxFilter = if (isEn) "Weak Optical" else "Redaman Tinggi"
    val deviceDetailTitle = if (isEn) "TR-069 Device Control" else "Kontrol Perangkat TR-069"
    val connectedClientsTitle = if (isEn) "Connected Devices / Hosts" else "Perangkat Terhubung (HP / LAN)"
    val wanConnectionsTitle = if (isEn) "WAN Connections" else "Koneksi WAN"
    val wlanConfigsTitle = if (isEn) "WiFi / WLAN Configs" else "Konfigurasi WiFi / WLAN"
    val addWanBtn = if (isEn) "Add WAN" else "Tambah WAN"
    val editWifiTitle = if (isEn) "Edit WiFi Settings" else "Ubah Pengaturan WiFi"
    val editWanTitle = if (isEn) "Configure WAN Connection" else "Konfigurasi Koneksi WAN"
    val ssidLabel = if (isEn) "WiFi Name (SSID)" else "Nama WiFi (SSID)"
    val wifiPassLabel = if (isEn) "WiFi Password (Min 8 chars)" else "Password WiFi (Min 8 karakter)"
    val vlanIdLabel = if (isEn) "VLAN ID (Optional)" else "VLAN ID (Opsional)"
    val rebootModemConfirm = if (isEn) "Send reboot command to this modem?" else "Kirim perintah restart ke modem pelanggan ini?"
}
