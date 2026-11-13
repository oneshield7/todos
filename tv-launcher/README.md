# Nusantara Launcher (Android TV)

Launcher Android TV yang latarnya mengikuti lokasi dan cuaca. Ditulis dengan Kotlin native, tanpa framework.

## Yang sudah ada di versi 0.3

- Beranda: jam dengan detik, tanggal, suhu, kondisi cuaca, nama kota, dan satu baris aplikasi yang disematkan (paling banyak 8).
- Laci aplikasi: tombol bawah (atau ubin "Semua aplikasi") menggeser laci naik dari bawah dengan latar tembus pandang; kisi tujuh kolom mengisi lebar layar. Tombol Kembali, atau tombol atas di baris teratas, menutupnya.
- Tekan lama OK pada ubin untuk menyematkan aplikasi di beranda atau melepasnya.
- Ubin aplikasi memakai banner asli aplikasi yang memenuhi ubin; nama hanya muncul pada ubin terpilih.
- Layar lokasi dengan dua pilihan: deteksi otomatis dari jaringan internet (tanpa GPS, dengan persetujuan atas hasilnya) atau pilih sendiri dari 38 ibu kota provinsi dan pencarian.
- Beranda utama: saat pertama dipakai, launcher menawarkan diri menjadi beranda utama lewat dialog resmi Android. Bila TV tidak menyediakan pilihan launcher, tersedia "Pengalih tombol Home" (layanan aksesibilitas yang dinyalakan pengguna sendiri).
- Ubin Pengaturan berisi: Pengaturan TV, Ubah lokasi, dan Jadikan beranda utama atau Kembalikan launcher bawaan.
- Tombol remote Menu, Pengaturan, Input, dan Semua aplikasi diteruskan ke fungsi yang sesuai. Tombol lain yang belum punya fungsi memunculkan pesan berisi nama tombolnya, sebagai bantuan selama masa uji.
- Bunyi saat berpindah dan memilih ubin.
- Latar bergambar untuk DK Jakarta (Monas), Jawa Barat, DI Yogyakarta (Prambanan), Bali, Sumatera Selatan, Kalimantan Selatan, Sulawesi Selatan, dan Papua Pegunungan. Provinsi lain memakai bentang alam umum (pegunungan, pesisir, atau perkotaan).
- Langit berganti menurut jam (pagi, siang, senja, malam) dan cuaca; awan bergumpal, hujan, kabut, dan kilat beranimasi.
- Cuaca dari Open-Meteo, diperbarui tiap 20 menit. TV harus tersambung internet.

## Layanan luar yang dihubungi

- `api.open-meteo.com` dan `geocoding-api.open-meteo.com`: cuaca dan pencarian lokasi.
- `ipwho.is`, dengan cadangan `get.geojs.io`: deteksi lokasi otomatis. Layanan ini melihat alamat internet TV. Hanya dihubungi bila pengguna memilih "Deteksi otomatis".

## Yang belum ada

Screensaver, mode redup, wallpaper foto sendiri, mengatur urutan aplikasi di beranda, dan gambar ikon untuk 30 provinsi lainnya.

## Merakit APK

GitHub Actions merakit APK tiap kali ada perubahan di folder ini (alur kerja `Rakit APK launcher TV`). Hasilnya ada di halaman Actions, bagian Artifacts, dengan nama `nusantara-launcher-debug`.

Merakit di komputer sendiri (perlu JDK 17 dan Android SDK):

```
cd tv-launcher
./gradlew assembleDebug
```

APK berada di `app/build/outputs/apk/debug/`.

## Memasang ke TV

- Flashdisk: salin APK ke flashdisk, buka dari pengelola berkas di TV, dan izinkan pemasangan dari sumber tidak dikenal.
- Lewat Wi-Fi: aktifkan Debugging USB di Opsi pengembang TV, lalu dari komputer di jaringan yang sama:

```
adb connect <alamat-IP-TV>:5555
adb install -r app-debug.apk
```

Setelah terpasang, tekan tombol Home dan pilih Nusantara Launcher. Sebagian TV (terutama Google TV) tidak menampilkan pilihan itu; di sana launcher dibuka dari daftar aplikasi, atau dijadikan bawaan lewat `adb`.
