# Nusantara Launcher (Android TV)

Launcher Android TV yang latarnya mengikuti lokasi dan cuaca. Ditulis dengan Kotlin native, tanpa framework.

## Yang sudah ada di versi 0.1

- Beranda: jam, tanggal, suhu, kondisi cuaca, nama kota, dan satu baris aplikasi.
- Ubin aplikasi memakai banner asli aplikasi yang memenuhi ubin; nama hanya muncul pada ubin terpilih.
- Latar bergambar untuk Jawa Barat, Bali, Sumatera Selatan, Kalimantan Selatan, Sulawesi Selatan, dan Papua Pegunungan. Provinsi lain memakai bentang alam umum (pegunungan, pesisir, atau perkotaan).
- Langit berganti menurut jam (pagi, siang, senja, malam) dan cuaca; awan, hujan, kabut, dan kilat beranimasi.
- Pilih lokasi: 38 ibu kota provinsi, ditambah pencarian kota dan kabupaten lain.
- Cuaca dari Open-Meteo, diperbarui tiap 20 menit. TV harus tersambung internet.

## Yang belum ada

Deteksi lokasi otomatis, screensaver, mode redup, pembersihan aplikasi bawaan, wallpaper foto sendiri, pilihan aplikasi favorit, dan gambar ikon untuk 32 provinsi lainnya.

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
