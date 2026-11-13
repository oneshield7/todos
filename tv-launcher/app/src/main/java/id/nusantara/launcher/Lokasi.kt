package id.nusantara.launcher

import android.content.Context

/** Satu kota atau kabupaten: nama, provinsi, dan koordinatnya untuk mengambil cuaca. */
data class Lokasi(val nama: String, val provinsi: String, val lat: Double, val lon: Double) {
    val judul: String
        get() = if (provinsi.isBlank() || provinsi.equals(nama, ignoreCase = true)) nama else "$nama, $provinsi"
}

object DaftarLokasi {
    val bawaan = Lokasi("Jakarta", "DK Jakarta", -6.20, 106.82)

    /** Ibu kota 38 provinsi sebagai pilihan cepat; kota lain dicari lewat kolom pencarian. */
    val ibuKota: List<Lokasi> = listOf(
        Lokasi("Banda Aceh", "Aceh", 5.55, 95.32),
        Lokasi("Medan", "Sumatera Utara", 3.59, 98.67),
        Lokasi("Padang", "Sumatera Barat", -0.95, 100.35),
        Lokasi("Pekanbaru", "Riau", 0.51, 101.45),
        Lokasi("Tanjungpinang", "Kepulauan Riau", 0.92, 104.45),
        Lokasi("Jambi", "Jambi", -1.61, 103.61),
        Lokasi("Palembang", "Sumatera Selatan", -2.98, 104.75),
        Lokasi("Pangkalpinang", "Kepulauan Bangka Belitung", -2.13, 106.11),
        Lokasi("Bengkulu", "Bengkulu", -3.80, 102.26),
        Lokasi("Bandar Lampung", "Lampung", -5.43, 105.26),
        Lokasi("Serang", "Banten", -6.12, 106.15),
        bawaan,
        Lokasi("Bandung", "Jawa Barat", -6.91, 107.61),
        Lokasi("Semarang", "Jawa Tengah", -6.99, 110.42),
        Lokasi("Yogyakarta", "DI Yogyakarta", -7.80, 110.37),
        Lokasi("Surabaya", "Jawa Timur", -7.26, 112.75),
        Lokasi("Denpasar", "Bali", -8.65, 115.22),
        Lokasi("Mataram", "Nusa Tenggara Barat", -8.58, 116.12),
        Lokasi("Kupang", "Nusa Tenggara Timur", -10.18, 123.61),
        Lokasi("Pontianak", "Kalimantan Barat", -0.03, 109.34),
        Lokasi("Palangka Raya", "Kalimantan Tengah", -2.21, 113.92),
        Lokasi("Banjarmasin", "Kalimantan Selatan", -3.32, 114.59),
        Lokasi("Samarinda", "Kalimantan Timur", -0.50, 117.15),
        Lokasi("Tanjung Selor", "Kalimantan Utara", 2.84, 117.37),
        Lokasi("Manado", "Sulawesi Utara", 1.47, 124.84),
        Lokasi("Gorontalo", "Gorontalo", 0.54, 123.06),
        Lokasi("Palu", "Sulawesi Tengah", -0.90, 119.87),
        Lokasi("Mamuju", "Sulawesi Barat", -2.68, 118.89),
        Lokasi("Makassar", "Sulawesi Selatan", -5.15, 119.43),
        Lokasi("Kendari", "Sulawesi Tenggara", -3.99, 122.51),
        Lokasi("Ambon", "Maluku", -3.70, 128.18),
        Lokasi("Ternate", "Maluku Utara", 0.79, 127.38),
        Lokasi("Jayapura", "Papua", -2.53, 140.72),
        Lokasi("Manokwari", "Papua Barat", -0.86, 134.06),
        Lokasi("Sorong", "Papua Barat Daya", -0.88, 131.26),
        Lokasi("Merauke", "Papua Selatan", -8.50, 140.40),
        Lokasi("Nabire", "Papua Tengah", -3.37, 135.50),
        Lokasi("Wamena", "Papua Pegunungan", -4.10, 138.95),
    )
}

enum class HasilFavorit { DITAMBAH, DIHAPUS, PENUH }

/** Menyimpan lokasi dan aplikasi yang disematkan di beranda. */
class Simpanan(context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var lokasi: Lokasi?
        get() {
            val nama = prefs.getString("nama", null) ?: return null
            return Lokasi(
                nama,
                prefs.getString("provinsi", "") ?: "",
                prefs.getFloat("lat", 0f).toDouble(),
                prefs.getFloat("lon", 0f).toDouble(),
            )
        }
        set(nilai) {
            if (nilai == null) return
            prefs.edit()
                .putString("nama", nilai.nama)
                .putString("provinsi", nilai.provinsi)
                .putFloat("lat", nilai.lat.toFloat())
                .putFloat("lon", nilai.lon.toFloat())
                .apply()
        }

    /** Benar setelah pengguna pernah ditawari menjadikan launcher ini beranda utama. */
    var sudahTanyaBeranda: Boolean
        get() = prefs.getBoolean("tanya_beranda", false)
        set(nilai) {
            prefs.edit().putBoolean("tanya_beranda", nilai).apply()
        }

    /** Benar bila tombol Home dialihkan ke launcher ini lewat layanan aksesibilitas. */
    var alihkanHome: Boolean
        get() = prefs.getBoolean("alihkan_home", false)
        set(nilai) {
            prefs.edit().putBoolean("alihkan_home", nilai).apply()
        }

    /** Animasi cuaca di latar. */
    var animasi: Boolean
        get() = prefs.getBoolean("animasi", true)
        set(nilai) {
            prefs.edit().putBoolean("animasi", nilai).apply()
        }

    /** Bunyi saat berpindah dan memilih ubin. */
    var bunyi: Boolean
        get() = prefs.getBoolean("bunyi", true)
        set(nilai) {
            prefs.edit().putBoolean("bunyi", nilai).apply()
        }

    /** Latar laci aplikasi: 0 bening, 1 sedang, 2 gelap. */
    var beningLaci: Int
        get() = prefs.getInt("bening_laci", 1).coerceIn(0, 2)
        set(nilai) {
            prefs.edit().putInt("bening_laci", nilai).apply()
        }

    /** Salah sampai daftar sematan pertama kali disimpan. */
    val favoritDiatur: Boolean
        get() = prefs.contains("favorit")

    /** Paket aplikasi yang disematkan di beranda, berurutan. */
    var favorit: List<String>
        get() = (prefs.getString("favorit", "") ?: "").split("|").filter { it.isNotBlank() }
        set(nilai) {
            prefs.edit().putString("favorit", nilai.joinToString("|")).apply()
        }

    fun gantiFavorit(paket: String): HasilFavorit {
        val kini = favorit.toMutableList()
        return when {
            kini.remove(paket) -> {
                favorit = kini
                HasilFavorit.DIHAPUS
            }
            kini.size >= MAKS_FAVORIT -> HasilFavorit.PENUH
            else -> {
                kini.add(paket)
                favorit = kini
                HasilFavorit.DITAMBAH
            }
        }
    }

    companion object {
        const val MAKS_FAVORIT = 8
    }
}
