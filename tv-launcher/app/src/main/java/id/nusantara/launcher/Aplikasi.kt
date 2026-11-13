package id.nusantara.launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** Satu aplikasi terpasang yang bisa dibuka dari launcher. */
class Aplikasi(val nama: String, val paket: String, val buka: Intent, val banner: Drawable?, val ikon: Drawable?)

object PustakaAplikasi {

    /** Semua aplikasi yang bisa dibuka, urut abjad. Panggil dari utas latar karena memuat gambar. */
    fun muat(konteks: Context): List<Aplikasi> {
        val pm = konteks.packageManager
        val hasil = LinkedHashMap<String, Aplikasi>()
        // Aplikasi TV lebih dulu (punya banner), lalu aplikasi biasa yang belum tercantum
        for (kategori in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val cari = Intent(Intent.ACTION_MAIN).addCategory(kategori)
            for (info in pm.queryIntentActivities(cari, 0)) {
                val aktivitas = info.activityInfo ?: continue
                if (aktivitas.packageName == konteks.packageName) continue
                if (hasil.containsKey(aktivitas.packageName)) continue
                val buka = Intent(Intent.ACTION_MAIN)
                    .addCategory(kategori)
                    .setClassName(aktivitas.packageName, aktivitas.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                val banner = aktivitas.loadBanner(pm) ?: aktivitas.applicationInfo.loadBanner(pm)
                hasil[aktivitas.packageName] = Aplikasi(
                    info.loadLabel(pm).toString(), aktivitas.packageName, buka, banner, info.loadIcon(pm)
                )
            }
        }
        return hasil.values.sortedBy { it.nama.lowercase() }
    }

    /**
     * Aplikasi untuk baris beranda: yang disematkan pengguna. Saat pertama kali dipakai,
     * enam aplikasi pertama disematkan otomatis sebagai titik awal.
     */
    fun untukBeranda(semua: List<Aplikasi>, simpanan: Simpanan): List<Aplikasi> {
        if (!simpanan.favoritDiatur && semua.isNotEmpty()) {
            simpanan.favorit = semua.take(6).map { it.paket }
        }
        val peta = semua.associateBy { it.paket }
        return simpanan.favorit.mapNotNull { peta[it] }
    }
}
