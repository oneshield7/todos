# Nusantara Launcher (Android TV)

Launcher Android TV yang latarnya mengikuti lokasi dan cuaca. Ditulis dengan Kotlin native, tanpa framework.

## Yang sudah ada di versi 0.2

- Beranda: jam dengan detik, tanggal, suhu, kondisi cuaca, nama kota, dan satu baris aplikasi.
- Baris beranda hanya berisi aplikasi yang disematkan (paling banyak 8). Ubin "Semua aplikasi" membuka kisi tujuh kolom berisi seluruh aplikasi.
- Tekan lama OK pada ubin untuk menyematkan aplikasi di beranda atau melepasnya.
- Ubin aplikasi memakai banner asli aplikasi yang memenuhi ubin; nama hanya muncul pada ubin terpilih.
- Ubin bawaan: Semua aplikasi, Input (sumber HDMI/AV), Lokasi, dan Pengaturan.
- Tombol remote Menu/Pengaturan, Input, dan Semua aplikasi diteruskan ke fungsi yang sesuai. Tombol lain yang belum punya fungsi memunculkan pesan berisi nama tombolnya, sebagai bantuan selama masa uji.
- Bunyi saat berpindah dan memilih ubin.
- Latar bergambar untuk Jawa Barat, Bali, Sumatera Selatan, Kalimantan Selatan, Sulawesi Selatan, dan Papua Pegunungan. Provinsi lain memakai bentang alam umum (pegunungan, pesisir, atau perkotaan).
- Langit berganti menurut jam (pagi, siang, senja, malam) dan cuaca; awan bergumpal, hujan, kabut, dan kilat beranimasi.
- Pilih lokasi: 38 ibu kota provinsi, ditambah pencarian kota dan kabupaten lain.
- Cuaca dari Open-Meteo, diperbarui tiap 20 menit. TV harus tersambung internet.

## Yang belum ada

Deteksi lokasi otomatis, screensaver, mode redup, pembersihan aplikasi bawaan, wallpaper foto sendiri, mengatur urutan aplikasi di beranda, dan gambar ikon untuk 32 provinsi lainnya.

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
