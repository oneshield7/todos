package id.nusantara.launcher

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

enum class Kondisi(val label: String) {
    CERAH("Cerah"),
    CERAH_BERAWAN("Cerah berawan"),
    BERAWAN("Berawan"),
    KABUT("Berkabut"),
    HUJAN_RINGAN("Hujan ringan"),
    HUJAN_LEBAT("Hujan lebat"),
    PETIR("Hujan petir");

    val hujan: Boolean
        get() = this == HUJAN_RINGAN || this == HUJAN_LEBAT || this == PETIR
}

data class Cuaca(val suhu: Int, val kondisi: Kondisi, val siang: Boolean)

/** Mengambil cuaca dan mencari lokasi dari Open-Meteo (gratis, tanpa kunci API). */
object Layanan {

    private fun ambil(alamat: String): String {
        val sambungan = URL(alamat).openConnection() as HttpURLConnection
        sambungan.connectTimeout = 10_000
        sambungan.readTimeout = 10_000
        try {
            if (sambungan.responseCode != 200) throw IOException("HTTP " + sambungan.responseCode)
            return sambungan.inputStream.bufferedReader().use { it.readText() }
        } finally {
            sambungan.disconnect()
        }
    }

    fun cuaca(lokasi: Lokasi): Cuaca {
        val alamat = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=" + String.format(Locale.US, "%.4f", lokasi.lat) +
            "&longitude=" + String.format(Locale.US, "%.4f", lokasi.lon) +
            "&current=temperature_2m,weather_code,is_day&timezone=auto"
        val kini = JSONObject(ambil(alamat)).getJSONObject("current")
        return Cuaca(
            suhu = Math.round(kini.getDouble("temperature_2m")).toInt(),
            kondisi = kondisiDari(kini.getInt("weather_code")),
            siang = kini.optInt("is_day", 1) == 1,
        )
    }

    /** Kode cuaca WMO yang dipakai Open-Meteo. */
    fun kondisiDari(kode: Int): Kondisi = when (kode) {
        0 -> Kondisi.CERAH
        1, 2 -> Kondisi.CERAH_BERAWAN
        3 -> Kondisi.BERAWAN
        45, 48 -> Kondisi.KABUT
        51, 53, 55, 56, 57, 61, 80 -> Kondisi.HUJAN_RINGAN
        63, 65, 66, 67, 81, 82 -> Kondisi.HUJAN_LEBAT
        95, 96, 99 -> Kondisi.PETIR
        else -> Kondisi.BERAWAN
    }

    /**
     * Memperkirakan lokasi dari alamat internet TV, tanpa GPS. Hasilnya setingkat kota
     * dan bisa meleset, jadi pemanggil harus meminta persetujuan pengguna.
     */
    fun deteksi(): Lokasi {
        var galat: Exception? = null
        for (alamat in listOf("https://ipwho.is/", "https://get.geojs.io/v1/ip/geo.json")) {
            try {
                val o = JSONObject(ambil(alamat))
                val kota = o.optString("city")
                val lat = o.optString("latitude").toDoubleOrNull()
                val lon = o.optString("longitude").toDoubleOrNull()
                if (kota.isBlank() || lat == null || lon == null) continue
                return Lokasi(kota, PetaTema.namaProvinsi(o.optString("region")), lat, lon)
            } catch (e: Exception) {
                galat = e
            }
        }
        throw galat ?: IOException("Lokasi tidak ditemukan")
    }

    /** Mencari kota atau kabupaten di Indonesia berdasarkan nama. */
    fun cari(kata: String): List<Lokasi> {
        val alamat = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=" + URLEncoder.encode(kata.trim(), "UTF-8") +
            "&count=20&language=id&format=json&countryCode=ID"
        val hasil = JSONObject(ambil(alamat)).optJSONArray("results") ?: return emptyList()
        val daftar = ArrayList<Lokasi>()
        for (i in 0 until hasil.length()) {
            val o = hasil.getJSONObject(i)
            if (o.optString("country_code", "ID") != "ID") continue
            daftar.add(
                Lokasi(
                    nama = o.optString("name"),
                    provinsi = PetaTema.namaProvinsi(o.optString("admin1")),
                    lat = o.getDouble("latitude"),
                    lon = o.getDouble("longitude"),
                )
            )
        }
        return daftar
    }
}
