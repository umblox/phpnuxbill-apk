package com.phpnuxbill.mobile.core.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val label: String, val shortLabel: String) {
    INDONESIAN("id", "Bahasa Indonesia", "ID"),
    ENGLISH("en", "English", "EN");

    companion object {
        fun fromCode(code: String): AppLanguage =
            if (code.equals("en", ignoreCase = true) || code.equals("english", ignoreCase = true)) {
                ENGLISH
            } else {
                INDONESIAN
            }
    }
}

/**
 * Kontrak kamus UI dwibahasa (LSP - Liskov Substitution Principle)
 */
interface AppStrings {
    val langCode: String

    // Umum & Navigasi
    val appTagline: String
    val loading: String
    val retry: String
    val save: String
    val cancel: String
    val delete: String
    val edit: String
    val add: String
    val searchPlaceholder: String
    val close: String
    val confirm: String
    val back: String
    val refresh: String
    val copy: String
    val copied: String
    val share: String
    val print: String
    val all: String
    val active: String
    val inactive: String
    val online: String
    val offline: String
    val noData: String

    // Autentikasi & Multi-Server
    val loginTitle: String
    val loginSubtitle: String
    val serverUrlLabel: String
    val serverUrlHint: String
    val checkServer: String
    val serverConnected: String
    val savedServersTitle: String
    val switchServer: String
    val roleAdmin: String
    val roleCustomer: String
    val usernameLabel: String
    val passwordLabel: String
    val loginButton: String
    val logoutButton: String
    val logoutConfirmTitle: String
    val logoutConfirmMessage: String

    // Menu Utama Admin
    val menuDashboard: String
    val menuCustomers: String
    val menuServices: String
    val menuVouchers: String
    val menuPlans: String
    val menuBandwidth: String
    val menuMikrotik: String
    val menuGenieAcs: String
    val menuReports: String
    val menuPaymentGateway: String
    val menuCouponsAssets: String
    val menuMap: String
    val menuMessages: String
    val menuSettings: String

    // Dasbor & Metrik
    val incomeToday: String
    val incomeMonth: String
    val activeServices: String
    val expiredServices: String
    val totalCustomers: String
    val customerBalance: String
    val pendingPayments: String
    val expiringSoon: String
    val voucherStock: String

    // Pelanggan, Paket & Transaksi
    val addCustomer: String
    val editCustomer: String
    val fullName: String
    val phoneNumber: String
    val emailAddress: String
    val address: String
    val serviceType: String
    val selectPlan: String
    val paymentMethod: String
    val rechargeNow: String
    val topUpBalance: String
    val extendExpiry: String
    val generateVoucher: String
    val sendInvoiceWa: String
    val printReceipt: String
    val pickCoordinates: String

    // Jaringan & Modem
    val cpuLoad: String
    val memoryUsage: String
    val uptime: String
    val downloadSpeed: String
    val uploadSpeed: String
    val onlineUsers: String
    val disconnectUser: String
    val rebootRouter: String
    val summonModem: String
    val wifiSettings: String
    val wanSettings: String

    // Pengaturan Aplikasi & Branding
    val appPreferences: String
    val languageSetting: String
    val themeSetting: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val customLogoSetting: String
    val changeLogoGallery: String
    val resetLogoDefault: String
    val appVersionLabel: String
}

object IndonesianStrings : AppStrings {
    override val langCode = "id"

    override val appTagline = "Sistem Manajemen Billing & Jaringan ISP"
    override val loading = "Memuat data..."
    override val retry = "Coba Lagi"
    override val save = "Simpan"
    override val cancel = "Batal"
    override val delete = "Hapus"
    override val edit = "Ubah"
    override val add = "Tambah"
    override val searchPlaceholder = "Cari nama, username, atau nomor..."
    override val close = "Tutup"
    override val confirm = "Konfirmasi"
    override val back = "Kembali"
    override val refresh = "Segarkan"
    override val copy = "Salin"
    override val copied = "Tersalin"
    override val share = "Bagikan"
    override val print = "Cetak Struk"
    override val all = "Semua"
    override val active = "Aktif"
    override val inactive = "Nonaktif"
    override val online = "Online"
    override val offline = "Offline"
    override val noData = "Belum ada data tersedia"

    override val loginTitle = "Masuk ke Sistem"
    override val loginSubtitle = "Hubungkan ke server billing Anda untuk mulai mengelola layanan"
    override val serverUrlLabel = "URL Server Billing"
    override val serverUrlHint = "https://billing.domainanda.com"
    override val checkServer = "Cek Server"
    override val serverConnected = "Terhubung ke"
    override val savedServersTitle = "Server Tersimpan"
    override val switchServer = "Ganti Server"
    override val roleAdmin = "Admin / Staf"
    override val roleCustomer = "Pelanggan"
    override val usernameLabel = "Username"
    override val passwordLabel = "Kata Sandi"
    override val loginButton = "Masuk Sekarang"
    override val logoutButton = "Keluar"
    override val logoutConfirmTitle = "Keluar dari Akun?"
    override val logoutConfirmMessage = "Sesi login saat ini akan diakhiri, namun profil server tetap tersimpan."

    override val menuDashboard = "Dasbor"
    override val menuCustomers = "Pelanggan"
    override val menuServices = "Layanan & Isi Ulang"
    override val menuVouchers = "Voucher"
    override val menuPlans = "Paket Internet"
    override val menuBandwidth = "Profil Bandwidth"
    override val menuMikrotik = "Monitor MikroTik"
    override val menuGenieAcs = "Modem TR-069"
    override val menuReports = "Laporan Keuangan"
    override val menuPaymentGateway = "Payment Gateway"
    override val menuCouponsAssets = "Kupon & Aset"
    override val menuMap = "Peta Jaringan"
    override val menuMessages = "Kirim Pesan"
    override val menuSettings = "Pengaturan"

    override val incomeToday = "Pendapatan Hari Ini"
    override val incomeMonth = "Pendapatan Bulan Ini"
    override val activeServices = "Layanan Aktif"
    override val expiredServices = "Kedaluwarsa"
    override val totalCustomers = "Total Pelanggan"
    override val customerBalance = "Total Saldo Pelanggan"
    override val pendingPayments = "Tagihan Tertunda"
    override val expiringSoon = "Segera Jatuh Tempo"
    override val voucherStock = "Stok Voucher"

    override val addCustomer = "Tambah Pelanggan"
    override val editCustomer = "Edit Pelanggan"
    override val fullName = "Nama Lengkap"
    override val phoneNumber = "Nomor WhatsApp / HP"
    override val emailAddress = "Alamat Email"
    override val address = "Alamat Lengkap"
    override val serviceType = "Jenis Layanan"
    override val selectPlan = "Pilih Paket"
    override val paymentMethod = "Metode Pembayaran"
    override val rechargeNow = "Aktifkan / Isi Ulang"
    override val topUpBalance = "Top-Up Saldo"
    override val extendExpiry = "Perpanjang Masa Aktif"
    override val generateVoucher = "Buat Voucher"
    override val sendInvoiceWa = "Kirim Struk WA"
    override val printReceipt = "Cetak Thermal"
    override val pickCoordinates = "Pilih Titik Peta"

    override val cpuLoad = "Beban CPU"
    override val memoryUsage = "Penggunaan RAM"
    override val uptime = "Waktu Aktif"
    override val downloadSpeed = "Unduh (RX)"
    override val uploadSpeed = "Unggah (TX)"
    override val onlineUsers = "Pengguna Online"
    override val disconnectUser = "Putus Sesi"
    override val rebootRouter = "Reboot Router"
    override val summonModem = "Summon / Segarkan"
    override val wifiSettings = "Pengaturan WiFi"
    override val wanSettings = "Konfigurasi WAN"

    override val appPreferences = "Preferensi Aplikasi"
    override val languageSetting = "Bahasa Aplikasi"
    override val themeSetting = "Tema Tampilan"
    override val themeSystem = "Ikuti Sistem"
    override val themeLight = "Terang"
    override val themeDark = "Gelap"
    override val customLogoSetting = "Logo Perusahaan (In-App)"
    override val changeLogoGallery = "Ganti Logo dari Galeri"
    override val resetLogoDefault = "Gunakan Logo Bawaan"
    override val appVersionLabel = "Versi Aplikasi"
}

object EnglishStrings : AppStrings {
    override val langCode = "en"

    override val appTagline = "ISP Billing & Network Management System"
    override val loading = "Loading data..."
    override val retry = "Retry"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val edit = "Edit"
    override val add = "Add"
    override val searchPlaceholder = "Search name, username, or phone..."
    override val close = "Close"
    override val confirm = "Confirm"
    override val back = "Back"
    override val refresh = "Refresh"
    override val copy = "Copy"
    override val copied = "Copied"
    override val share = "Share"
    override val print = "Print Receipt"
    override val all = "All"
    override val active = "Active"
    override val inactive = "Inactive"
    override val online = "Online"
    override val offline = "Offline"
    override val noData = "No data available"

    override val loginTitle = "Sign In"
    override val loginSubtitle = "Connect to your billing server to manage services"
    override val serverUrlLabel = "Billing Server URL"
    override val serverUrlHint = "https://billing.yourdomain.com"
    override val checkServer = "Verify Server"
    override val serverConnected = "Connected to"
    override val savedServersTitle = "Saved Servers"
    override val switchServer = "Switch Server"
    override val roleAdmin = "Admin / Staff"
    override val roleCustomer = "Customer"
    override val usernameLabel = "Username"
    override val passwordLabel = "Password"
    override val loginButton = "Sign In"
    override val logoutButton = "Sign Out"
    override val logoutConfirmTitle = "Sign Out?"
    override val logoutConfirmMessage = "Your current session will end, while saved server profiles remain stored."

    override val menuDashboard = "Dashboard"
    override val menuCustomers = "Customers"
    override val menuServices = "Services & Recharge"
    override val menuVouchers = "Vouchers"
    override val menuPlans = "Internet Plans"
    override val menuBandwidth = "Bandwidth Profiles"
    override val menuMikrotik = "MikroTik Monitor"
    override val menuGenieAcs = "TR-069 Modems"
    override val menuReports = "Financial Reports"
    override val menuPaymentGateway = "Payment Gateways"
    override val menuCouponsAssets = "Coupons & Assets"
    override val menuMap = "Network Map"
    override val menuMessages = "Send Message"
    override val menuSettings = "Settings"

    override val incomeToday = "Today's Income"
    override val incomeMonth = "Monthly Income"
    override val activeServices = "Active Services"
    override val expiredServices = "Expired"
    override val totalCustomers = "Total Customers"
    override val customerBalance = "Total Customer Balance"
    override val pendingPayments = "Pending Bills"
    override val expiringSoon = "Expiring Soon"
    override val voucherStock = "Voucher Stock"

    override val addCustomer = "Add Customer"
    override val editCustomer = "Edit Customer"
    override val fullName = "Full Name"
    override val phoneNumber = "WhatsApp / Phone"
    override val emailAddress = "Email Address"
    override val address = "Full Address"
    override val serviceType = "Service Type"
    override val selectPlan = "Select Plan"
    override val paymentMethod = "Payment Method"
    override val rechargeNow = "Activate / Recharge"
    override val topUpBalance = "Top-Up Balance"
    override val extendExpiry = "Extend Validity"
    override val generateVoucher = "Generate Vouchers"
    override val sendInvoiceWa = "Send WA Receipt"
    override val printReceipt = "Thermal Print"
    override val pickCoordinates = "Pick Map Location"

    override val cpuLoad = "CPU Load"
    override val memoryUsage = "Memory Usage"
    override val uptime = "Uptime"
    override val downloadSpeed = "Download (RX)"
    override val uploadSpeed = "Upload (TX)"
    override val onlineUsers = "Online Users"
    override val disconnectUser = "Disconnect"
    override val rebootRouter = "Reboot Router"
    override val summonModem = "Summon / Refresh"
    override val wifiSettings = "WiFi Settings"
    override val wanSettings = "WAN Configuration"

    override val appPreferences = "App Preferences"
    override val languageSetting = "App Language"
    override val themeSetting = "Display Theme"
    override val themeSystem = "System Default"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val customLogoSetting = "Company Logo (In-App)"
    override val changeLogoGallery = "Pick Logo from Gallery"
    override val resetLogoDefault = "Use Default Logo"
    override val appVersionLabel = "App Version"
}

fun getStringsForLanguage(language: AppLanguage): AppStrings =
    when (language) {
        AppLanguage.ENGLISH -> EnglishStrings
        AppLanguage.INDONESIAN -> IndonesianStrings
    }

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { IndonesianStrings }
