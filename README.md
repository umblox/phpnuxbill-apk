# PHPNuxBill Mobile (Admin & Customer Portal — Android)

Aplikasi Android native berbasis **Kotlin** dan **Jetpack Compose (Material 3)** untuk mengelola server billing **PHPNuxBill** sekaligus menyediakan **Portal Pelanggan** dalam satu aplikasi. Dirancang untuk memudahkan operasional harian RT/RW Net dan ISP lokal langsung dari HP tanpa memodifikasi file inti (*core*) PHPNuxBill.

---

## Fitur Utama

### 1. Mode Admin & Staf (RBAC 5 Level)
Seluruh tampilan menu dan hak aksi mengikuti kebijakan resmi 5 level pengguna PHPNuxBill (**SuperAdmin**, **Admin**, **Report**, **Agent**, dan **Sales**):

- **Dasbor Role-Aware:** Ringkasan pendapatan/penjualan harian & bulanan, rincian pelanggan aktif (Hotspot, PPPoE, VPN), grafik performa 12 bulan, stok voucher per paket, daftar pelanggan segera jatuh tempo, log aktivitas terbaru, serta banner peringatan *Maintenance Mode* dengan tombol pintasan **Kelola / Matikan** (khusus SuperAdmin & Admin).
- **Manajemen Pelanggan & Layanan Aktif:** Tambah/edit/hapus pelanggan (Hotspot, PPPoE, VPN) lengkap dengan *Custom Fields* & *Quick Add*, isi ulang paket (*recharge*), top-up saldo deposit, perpanjang masa aktif (*extend*), edit/hapus langganan aktif, serta sinkronisasi ulang akun ke router.
- **Voucher & Kupon Promo:** Generate voucher massal sesuai format web (termasuk *prefix* & *batch*), aktivasi/*refill* voucher, bersihkan voucher terpakai, cetak via printer thermal Bluetooth, bagikan kode ke WhatsApp, dan kelola kupon diskon.
- **Paket Internet & Profil Bandwidth:** Kelola paket Hotspot, PPPoE, VPN, dan Saldo lengkap dengan preset *Burst Limit* resmi web serta sinkronisasi paket ke router.
- **Monitoring MikroTik Real-Time:** Pantau grafik trafik interface (`RX/TX`), cek beban CPU/RAM/Storage router, lihat daftar user yang sedang online, pantau *live speed* per pelanggan, putus sesi (*kick* user), lihat log RouterOS, hingga *reboot* router.
- **Manajemen GenieACS (TR-069 Pro):** Mendukung koneksi multi-host (HTTP lokal maupun HTTPS domain/tunnel). Pantau redaman optik (`RX Power`), suhu ONT, daftar perangkat/HP yang terhubung ke WiFi pelanggan, ubah SSID/Password WiFi, kelola koneksi WAN (TR-098 & TR-181), kelola *tags*, serta *summon*, *reboot*, dan *factory reset* modem dari jarak jauh.
- **Laporan Keuangan, Pesan Broadcast & Halaman Statis:** Rekap pemasukan berdasarkan rentang tanggal dan metode pembayaran, kirim pesan personal/broadcast massal (WhatsApp, SMS, Email, Inbox), serta editor halaman statis web (`pages/`).
- **Audit Payment Gateway:** Pantau status transaksi Payment Gateway (**ViaQRIS, Tripay, Duitku, Xendit, PayPal**, dll.) lengkap dengan fitur cek status otomatis dan *approve* manual.
- **Peta Jaringan & Inventaris Aset:** Visualisasi titik lokasi pelanggan, cakupan radius server/router, titik ODP/aset di peta Leaflet (kompatibel dengan *Focuslinks Asset Manager*), serta deteksi posisi GPS teknisi di lapangan.
- **Pengaturan Sistem, Maintenance Mode & Database:** Kontrol **Mode Pemeliharaan** (*Status*, *Force Logout*, dan *DatePicker* estimasi selesai), editor template notifikasi (`notifications.json`), tes gateway notifikasi, manajemen staf sesuai RBAC, alat sistem (*Clear Cache* & *Manual Cron*), serta status & backup JSON database (khusus SuperAdmin).

### 2. Mode Portal Pelanggan (Customer Portal)
- **Beranda Pelanggan:** Informasi status paket aktif/kedaluwarsa, saldo akun, pengumuman resmi ISP, notifikasi pesan masuk (*Inbox*) belum dibaca, tombol *Auto-Renewal*, sinkronisasi ulang koneksi ke router, dan perpanjangan masa tenggang darurat (*Extend*).
- **Pembelian Paket & Top-Up Saldo:** Pembayaran via saldo akun atau Payment Gateway otomatis (QRIS dinamis, Virtual Account, E-Wallet, Checkout URL) lengkap dengan dukungan kode kupon promo.
- **Layanan Mandiri & Sosial:** Redeem voucher mandiri, transfer saldo ke teman, serta belikan paket internet untuk teman.
- **Kontrol Modem Mandiri (GenieACS Anti-IDOR):** Pelanggan dapat memantau status modem dan perangkat terhubung, mengganti nama/sandi WiFi, serta melakukan *refresh* dan *reboot* modem miliknya sendiri secara aman.
- **Isolasi Maintenance Mode:** Saat server sedang dalam pemeliharaan, akses dan login pelanggan otomatis ditahan pada layar pemeliharaan khusus dengan animasi 3 roda gigi, hitung mundur *real-time*, tombol **Cek Status Lagi**, dan tombol **Hubungi CS via WhatsApp** (menghormati opsi *Force Logout* dari server).

### 3. Keunggulan Aplikasi
- **Multi-Server & White-Label Dinamis:** Simpan beberapa profil server billing dan akun (Admin maupun Pelanggan), dengan logo dan nama perusahaan tersinkron otomatis dari server atau bisa disesuaikan dari galeri HP.
- **Dwibahasa & Tema Adaptif:** Mendukung **Bahasa Indonesia (ID)** dan **English (EN)** serta mode tampilan **Ikuti Sistem / Terang / Gelap**.
- **Struk Thermal & WhatsApp:** Cetak struk transaksi langsung ke printer thermal Bluetooth (ESC/POS) dan kirim ulang struk WhatsApp mengikuti template resmi web (`notifications.json`).
- **Kompatibel Firewall Server:** Memakai gerbang resmi `system/api.php` dengan perpindahan otomatis (*fallback*) ke `index.php?_route=plugin/mobile_api` apabila server mengaktifkan `.htaccess_firewall`.

---

## Persyaratan Server (Wajib)

Aplikasi ini tidak memodifikasi file inti (*core*) PHPNuxBill. Agar aplikasi dapat terhubung ke server, Anda wajib memasang plugin **PHPNuxBill Mobile REST API Bridge v3.1.0+** di sisi server.

Repositori plugin:
👉 **[https://github.com/umblox/phpnuxbill-mobile-api-plugin](https://github.com/umblox/phpnuxbill-mobile-api-plugin)**

### Pemasangan Plugin di Server PHPNuxBill

Pasang melalui **Plugin Manager** di web Admin PHPNuxBill, atau secara manual dengan cara berikut:

1. Unduh atau *clone* repositori [phpnuxbill-mobile-api-plugin](https://github.com/umblox/phpnuxbill-mobile-api-plugin).
2. Salin file `mobile_api.php`, folder `ui/`, dan seluruh folder `mobile_api_modules/` ke dalam direktori `system/plugin/` pada instalasi PHPNuxBill Anda:
   ```text
   system/
   └── plugin/
       ├── mobile_api.php
       ├── ui/
       │   └── mobile_api_config.tpl
       └── mobile_api_modules/
           ├── dispatcher.php
           ├── helpers_common.php
           ├── helpers_payment_gateways.php
           ├── core_auth_dashboard.php
           ├── core_customers.php
           ├── core_services_vouchers.php
           ├── core_plans_bandwidth.php
           ├── core_network_mikrotik.php
           ├── core_reports_messages.php
           ├── core_settings_staff.php
           ├── plugin_coupons_assets_pg.php
           ├── plugin_genieacs.php
           └── customer_portal_api.php
