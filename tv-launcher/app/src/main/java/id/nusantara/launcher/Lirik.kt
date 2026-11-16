package id.nusantara.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class BarisLirik(val waktu: Long, val teks: String)

/** Lirik satu lagu. [berwaktu] salah bila layanan hanya punya teks tanpa penanda waktu. */
class Lirik(val baris: List<BarisLirik>, val berwaktu: Boolean) {

    /** Baris yang sedang dinyanyikan pada posisi lagu ini; -1 sebelum baris pertama. */
    fun indeks(posisi: Long): Int {
        var hasil = -1
        for (i in baris.indices) {
            if (baris[i].waktu <= posisi) hasil = i else break
        }
        return hasil
    }
}

/** Mengambil lirik dari LRCLIB (gratis, tanpa kunci API). Judul dan artis lagu dikirim ke layanan itu. */
object PustakaLirik {
    private const val DASAR = "https://lrclib.net/api/"
    private const val AGEN = "NusantaraLauncher/0.6"

    private fun teks(o: JSONObject, kunci: String): String = if (o.isNull(kunci)) "" else o.optString(kunci)

    /** Mengembalikan null bila lagunya tidak punya lirik di layanan. Melempar galat bila jaringan gagal. */
    fun ambil(judul: String, artis: String, durasiMs: Long): Lirik? {
        val kueri = "track_name=" + URLEncoder.encode(judul, "UTF-8") + "&artist_name=" + URLEncoder.encode(artis, "UTF-8")
        var objek: JSONObject? = null
        if (durasiMs > 0L) {
            // pencarian tepat, memakai durasi lagu; layanan menjawab 404 bila tidak cocok
            try {
                objek = JSONObject(Layanan.ambil(DASAR + "get?" + kueri + "&duration=" + (durasiMs / 1000L), AGEN))
            } catch (e: Exception) {
                objek = null
            }
        }
        if (objek == null) {
            val daftar = JSONArray(Layanan.ambil(DASAR + "search?" + kueri, AGEN))
            for (i in 0 until daftar.length()) {
                val o = daftar.getJSONObject(i)
                if (teks(o, "syncedLyrics").isNotBlank()) {
                    objek = o
                    break
                }
                if (objek == null && teks(o, "plainLyrics").isNotBlank()) objek = o
            }
        }
        val o = objek ?: return null

        val berwaktu = urai(teks(o, "syncedLyrics"))
        if (berwaktu.isNotEmpty()) return Lirik(berwaktu, true)

        // Tanpa penanda waktu: baris dibagi rata sepanjang lagu sebagai perkiraan
        val polos = teks(o, "plainLyrics").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (polos.isEmpty()) return null
        val panjang = if (durasiMs > 0L) durasiMs else polos.size * 4000L
        return Lirik(polos.mapIndexed { i, isi -> BarisLirik(panjang * i / polos.size, isi) }, false)
    }

    /** Mengurai format LRC: tiap baris berbentuk "[menit:detik.pecahan] teks". */
    fun urai(lrc: String): List<BarisLirik> {
        val hasil = ArrayList<BarisLirik>()
        for (mentah in lrc.split("\n")) {
            val baris = mentah.trim()
            if (!baris.startsWith("[")) continue
            val tutup = baris.indexOf(']')
            if (tutup < 0) continue
            val bagian = baris.substring(1, tutup).split(":")
            if (bagian.size != 2) continue
            val menit = bagian[0].toIntOrNull() ?: continue
            val detik = bagian[1].toDoubleOrNull() ?: continue
            val isi = baris.substring(tutup + 1).trim()
            if (isi.isEmpty()) continue
            hasil.add(BarisLirik(menit * 60_000L + (detik * 1000.0).toLong(), isi))
        }
        hasil.sortBy { it.waktu }
        return hasil
    }
}

/**
 * Lima baris lirik di sisi kanan beranda: baris yang sedang dinyanyikan besar dan tebal,
 * dua baris sebelum dan sesudahnya makin pudar.
 */
class LirikView(context: Context, private val u: Float, private val pemantau: PemantauMusik) : LinearLayout(context) {

    private val barisTeks = Array(5) { TextView(context) }
    private val sumber = TextView(context)
    private var lirik: Lirik? = null
    private var indeksTampil = Int.MIN_VALUE
    private var berjalan = false

    private val detak = object : Runnable {
        override fun run() {
            segarkan()
            if (berjalan && lirik != null) postDelayed(this, 200L)
        }
    }

    init {
        orientation = VERTICAL
        val ukuran = floatArrayOf(32f, 34f, 46f, 34f, 32f)
        val pudar = intArrayOf(107, 158, 255, 158, 107)
        for (i in 0 until 5) {
            val t = barisTeks[i]
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, ukuran[i] * u)
            t.typeface = Typeface.DEFAULT_BOLD
            t.setTextColor(Color.argb(pudar[i], 244, 246, 244))
            t.maxLines = 2
            t.ellipsize = TextUtils.TruncateAt.END
            // bayangan tipis supaya tetap terbaca di atas latar yang terang
            t.setShadowLayer(6f * u, 0f, 2f * u, Color.argb(150, 0, 0, 0))
            addView(t, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = if (i == 0) 0 else (20f * u).toInt()
            })
        }
        sumber.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * u)
        sumber.setTextColor(Color.parseColor("#C3CCC8"))
        sumber.setShadowLayer(6f * u, 0f, 2f * u, Color.argb(150, 0, 0, 0))
        addView(sumber, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (30f * u).toInt()
        })
    }

    /** Memasang lirik lagu baru, atau null untuk mengosongkan. */
    fun pasang(baru: Lirik?) {
        lirik = baru
        indeksTampil = Int.MIN_VALUE
        removeCallbacks(detak)
        if (baru != null) {
            sumber.text = if (baru.berwaktu) "Lirik dari LRCLIB" else "Lirik dari LRCLIB, tanpa penanda waktu; posisinya perkiraan"
            segarkan()
            if (berjalan) postDelayed(detak, 200L)
        }
    }

    /** Dipanggil saat beranda tampil atau disembunyikan, supaya tidak bekerja di latar. */
    fun jalankan(aktif: Boolean) {
        berjalan = aktif
        removeCallbacks(detak)
        if (aktif && lirik != null) post(detak)
    }

    private fun segarkan() {
        val l = lirik ?: return
        val kini = l.indeks(pemantau.posisi())
        if (kini == indeksTampil) return
        indeksTampil = kini
        for (k in 0 until 5) {
            val n = kini - 2 + k
            barisTeks[k].text = if (n >= 0 && n < l.baris.size) l.baris[n].teks else ""
        }
        // baris baru muncul dengan pudar singkat
        barisTeks[2].alpha = 0.35f
        barisTeks[2].animate().alpha(1f).setDuration(260).start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(detak)
    }
}
