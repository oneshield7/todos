package id.nusantara.launcher

import android.graphics.Color
import java.util.Calendar

/** Latar yang tersedia: enam ikon daerah dan tiga bentang alam umum sebagai cadangan. */
enum class Tema(val aksen: Int) {
    GEDUNG_SATE(Color.parseColor("#C3E88D")),
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

/** Menentukan latar dari nama provinsi, dalam ejaan Indonesia maupun Inggris. */
object PetaTema {
    private fun kunci(teks: String) = teks.lowercase().filter { it.isLetter() }

    private val khusus = mapOf(
        "jawabarat" to Tema.GEDUNG_SATE, "westjava" to Tema.GEDUNG_SATE,
        "bali" to Tema.MERU,
        "sumateraselatan" to Tema.AMPERA, "southsumatra" to Tema.AMPERA,
        "kalimantanselatan" to Tema.PASAR_TERAPUNG, "southkalimantan" to Tema.PASAR_TERAPUNG,
        "sulawesiselatan" to Tema.PINISI, "southsulawesi" to Tema.PINISI,
        "papuapegunungan" to Tema.HONAI, "highlandpapua" to Tema.HONAI,
    )

    private val perkotaan = setOf(
        "dkjakarta", "jakarta", "dkijakarta", "daerahkhususjakarta", "daerahkhususibukotajakarta",
        "banten", "riau", "kalimantanbarat", "westkalimantan", "kalimantantengah", "centralkalimantan",
    )

    private val pegunungan = setOf(
        "sumaterautara", "northsumatra", "sumaterabarat", "westsumatra", "jambi",
        "jawatengah", "centraljava", "diyogyakarta", "yogyakarta", "daerahistimewayogyakarta",
        "specialregionofyogyakarta", "jawatimur", "eastjava", "nusatenggarabarat", "westnusatenggara",
        "sulawesitengah", "centralsulawesi", "gorontalo", "papuatengah", "centralpapua",
        "papuabarat", "westpapua",
    )

    fun untuk(provinsi: String): Tema {
        val k = kunci(provinsi)
        return khusus[k] ?: when (k) {
            in perkotaan -> Tema.KOTA
            in pegunungan -> Tema.PEGUNUNGAN
            else -> Tema.PESISIR
        }
    }
}
