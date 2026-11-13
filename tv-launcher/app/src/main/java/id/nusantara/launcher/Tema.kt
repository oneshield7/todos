package id.nusantara.launcher

import android.graphics.Color
import java.util.Calendar

/** Latar yang tersedia: delapan ikon daerah dan tiga bentang alam umum sebagai cadangan. */
enum class Tema(val aksen: Int) {
    GEDUNG_SATE(Color.parseColor("#C3E88D")),
    MONAS(Color.parseColor("#FF9A4D")),
    PRAMBANAN(Color.parseColor("#E3B45C")),
    MERU(Color.parseColor("#FFB627")),
    AMPERA(Color.parseColor("#F2C46B")),
    PASAR_TERAPUNG(Color.parseColor("#F29A4A")),
    PINISI(Color.parseColor("#FFC857")),
    HONAI(Color.parseColor("#F2B441")),
    PEGUNUNGAN(Color.parseColor("#C3E88D")),
    PESISIR(Color.parseColor("#8FD3E8")),
    KOTA(Color.parseColor("#F2C46B")),
}

enum class Waktu {
    PAGI, SIANG, SENJA, MALAM;

    companion object {
        /** Dihitung dari jam TV: pagi 05.00, siang 09.00, senja 16.00, malam 18.30. */
        fun sekarang(): Waktu {
            val kalender = Calendar.getInstance()
            val menit = kalender.get(Calendar.HOUR_OF_DAY) * 60 + kalender.get(Calendar.MINUTE)
            return when {
                menit < 300 -> MALAM
                menit < 540 -> PAGI
                menit < 960 -> SIANG
                menit < 1110 -> SENJA
                else -> MALAM
            }
        }
    }
}

/** Mengenali provinsi dari namanya (ejaan Indonesia atau Inggris) dan menentukan latarnya. */
object PetaTema {
    private class Provinsi(val nama: String, val tema: Tema, vararg val lain: String)

    private val daftar = listOf(
        Provinsi("Aceh", Tema.PESISIR),
        Provinsi("Sumatera Utara", Tema.PEGUNUNGAN, "North Sumatra"),
        Provinsi("Sumatera Barat", Tema.PEGUNUNGAN, "West Sumatra"),
        Provinsi("Riau", Tema.KOTA),
        Provinsi("Kepulauan Riau", Tema.PESISIR, "Riau Islands"),
        Provinsi("Jambi", Tema.PEGUNUNGAN),
        Provinsi("Sumatera Selatan", Tema.AMPERA, "South Sumatra"),
        Provinsi("Kepulauan Bangka Belitung", Tema.PESISIR, "Bangka Belitung Islands", "Bangka Belitung"),
        Provinsi("Bengkulu", Tema.PESISIR),
        Provinsi("Lampung", Tema.PESISIR),
        Provinsi("Banten", Tema.KOTA),
        Provinsi("DK Jakarta", Tema.MONAS, "Jakarta", "DKI Jakarta", "Daerah Khusus Jakarta", "Daerah Khusus Ibukota Jakarta", "Jakarta Raya"),
        Provinsi("Jawa Barat", Tema.GEDUNG_SATE, "West Java"),
        Provinsi("Jawa Tengah", Tema.PEGUNUNGAN, "Central Java"),
        Provinsi("DI Yogyakarta", Tema.PRAMBANAN, "Yogyakarta", "Daerah Istimewa Yogyakarta", "Special Region of Yogyakarta"),
        Provinsi("Jawa Timur", Tema.PEGUNUNGAN, "East Java"),
        Provinsi("Bali", Tema.MERU),
        Provinsi("Nusa Tenggara Barat", Tema.PEGUNUNGAN, "West Nusa Tenggara"),
        Provinsi("Nusa Tenggara Timur", Tema.PESISIR, "East Nusa Tenggara"),
        Provinsi("Kalimantan Barat", Tema.KOTA, "West Kalimantan"),
        Provinsi("Kalimantan Tengah", Tema.KOTA, "Central Kalimantan"),
        Provinsi("Kalimantan Selatan", Tema.PASAR_TERAPUNG, "South Kalimantan"),
        Provinsi("Kalimantan Timur", Tema.PESISIR, "East Kalimantan"),
        Provinsi("Kalimantan Utara", Tema.PESISIR, "North Kalimantan"),
        Provinsi("Sulawesi Utara", Tema.PESISIR, "North Sulawesi"),
        Provinsi("Gorontalo", Tema.PEGUNUNGAN),
        Provinsi("Sulawesi Tengah", Tema.PEGUNUNGAN, "Central Sulawesi"),
        Provinsi("Sulawesi Barat", Tema.PESISIR, "West Sulawesi"),
        Provinsi("Sulawesi Selatan", Tema.PINISI, "South Sulawesi"),
        Provinsi("Sulawesi Tenggara", Tema.PESISIR, "Southeast Sulawesi", "South East Sulawesi"),
        Provinsi("Maluku", Tema.PESISIR),
        Provinsi("Maluku Utara", Tema.PESISIR, "North Maluku"),
        Provinsi("Papua", Tema.PESISIR),
        Provinsi("Papua Barat", Tema.PEGUNUNGAN, "West Papua"),
        Provinsi("Papua Barat Daya", Tema.PESISIR, "Southwest Papua", "South West Papua"),
        Provinsi("Papua Selatan", Tema.PESISIR, "South Papua"),
        Provinsi("Papua Tengah", Tema.PEGUNUNGAN, "Central Papua"),
        Provinsi("Papua Pegunungan", Tema.HONAI, "Highland Papua", "Papua Highlands"),
    )

    private fun kunci(teks: String) = teks.lowercase().filter { it.isLetter() }

    private val peta: Map<String, Provinsi> = HashMap<String, Provinsi>().apply {
        for (p in daftar) {
            put(kunci(p.nama), p)
            for (alias in p.lain) put(kunci(alias), p)
        }
    }

    fun untuk(provinsi: String): Tema = peta[kunci(provinsi)]?.tema ?: Tema.PESISIR

    /** Nama provinsi dalam ejaan Indonesia; nama yang tidak dikenal dikembalikan apa adanya. */
    fun namaProvinsi(provinsi: String): String = peta[kunci(provinsi)]?.nama ?: provinsi
}
