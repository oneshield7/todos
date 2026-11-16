package id.nusantara.launcher

import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Screensaver: latar daerah dengan cuaca yang bergerak, tanpa aplikasi. Yang tampil hanya
 * jam, tanggal, dan cuaca. Blok jam berpindah sudut tiap dua menit supaya tidak membekas di panel.
 */
class LayarIstirahat : DreamService() {

    private val penangan = Handler(Looper.getMainLooper())
    private val pekerja = Executors.newSingleThreadExecutor()
    private val bahasa = Locale("id", "ID")
    private val formatJam = SimpleDateFormat("HH.mm", bahasa)
    private val formatTanggal = SimpleDateFormat("EEEE, d MMMM", bahasa)

    private lateinit var simpanan: Simpanan
    private lateinit var latar: LatarView
    private lateinit var blok: LinearLayout
    private lateinit var jam: TextView
    private lateinit var tanggal: TextView
    private lateinit var cuaca: TextView

    private var u = 1f
    private var lokasi: Lokasi = DaftarLokasi.bawaan
    private var sudut = 0
    private var pindahBerikut = 0L
    private var cuacaDiambil = 0L

    private val detak = object : Runnable {
        override fun run() {
            val kini = System.currentTimeMillis()
            perbaruiJam()
            if (kini >= pindahBerikut) pindah()
            if (kini - cuacaDiambil > 20 * 60_000L) muatCuaca()
            penangan.postDelayed(this, 1000L - kini % 1000L + 5L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false
        simpanan = Simpanan(this)
        u = resources.displayMetrics.widthPixels / 1920f
        lokasi = simpanan.lokasi ?: DaftarLokasi.bawaan

        val penuh = ViewGroup.LayoutParams.MATCH_PARENT
        val isi = ViewGroup.LayoutParams.WRAP_CONTENT
        val akar = FrameLayout(this)
        latar = LatarView(this)
        latar.tema = PetaTema.untuk(lokasi.provinsi)
        latar.waktu = Waktu.sekarang()
        latar.animasi = simpanan.animasi
        akar.addView(latar, FrameLayout.LayoutParams(penuh, penuh))

        blok = LinearLayout(this)
        blok.orientation = LinearLayout.VERTICAL
        jam = teks(128f, true)
        jam.fontFeatureSettings = "tnum"
        tanggal = teks(28f, false)
        cuaca = teks(28f, false)
        blok.addView(jam, LinearLayout.LayoutParams(isi, isi))
        blok.addView(tanggal, LinearLayout.LayoutParams(isi, isi).apply { topMargin = (8f * u).toInt() })
        blok.addView(cuaca, LinearLayout.LayoutParams(isi, isi).apply { topMargin = (4f * u).toInt() })
        akar.addView(blok, parameterSudut())

        // Cuaca terakhir yang diketahui beranda dipakai dulu, supaya tidak kosong saat mulai
        simpanan.cuacaTerakhir?.let { tampilkan(it) }
        if (simpanan.cuacaTerakhir == null) cuaca.text = lokasi.nama
        setContentView(akar)
    }

    private fun teks(ukuran: Float, tebal: Boolean): TextView {
        val t = TextView(this)
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, ukuran * u)
        t.typeface = if (tebal) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        t.includeFontPadding = false
        t.maxLines = 1
        t.setTextColor(Color.parseColor("#F2F6F3"))
        // bayangan tipis supaya terbaca juga di atas langit siang yang terang
        t.setShadowLayer(8f * u, 0f, 2f * u, Color.argb(160, 0, 0, 0))
        return t
    }

    /** Empat sudut bergiliran: kiri bawah, kanan atas, kanan bawah, kiri atas. */
    private fun parameterSudut(): FrameLayout.LayoutParams {
        val isi = ViewGroup.LayoutParams.WRAP_CONTENT
        val gravitasi = when (sudut % 4) {
            0 -> Gravity.BOTTOM or Gravity.START
            1 -> Gravity.TOP or Gravity.END
            2 -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.TOP or Gravity.START
        }
        val p = FrameLayout.LayoutParams(isi, isi, gravitasi)
        val samping = (96f * u).toInt()
        val tegak = (80f * u).toInt()
        p.setMargins(samping, tegak, samping, tegak)
        return p
    }

    private fun pindah() {
        val pertama = pindahBerikut == 0L
        pindahBerikut = System.currentTimeMillis() + 120_000L
        if (pertama) return
        blok.animate().alpha(0f).setDuration(700).withEndAction {
            sudut++
            blok.layoutParams = parameterSudut()
            blok.animate().alpha(1f).setDuration(700).start()
        }.start()
    }

    private fun perbaruiJam() {
        val kini = Date()
        val teksJam = formatJam.format(kini)
        if (jam.text.toString() != teksJam) {
            jam.text = teksJam
            tanggal.text = formatTanggal.format(kini)
        }
        val waktu = Waktu.sekarang()
        if (latar.waktu != waktu) latar.waktu = waktu
    }

    private fun tampilkan(hasil: Cuaca) {
        latar.kondisi = hasil.kondisi
        cuaca.text = hasil.suhu.toString() + "° " + hasil.kondisi.label + " · " + lokasi.nama
    }

    private fun muatCuaca() {
        cuacaDiambil = System.currentTimeMillis()
        val tujuan = lokasi
        pekerja.execute {
            val hasil = try {
                Layanan.cuaca(tujuan)
            } catch (e: Exception) {
                null
            }
            penangan.post {
                if (hasil != null) {
                    simpanan.simpanCuaca(hasil)
                    tampilkan(hasil)
                } else {
                    // coba lagi lima menit kemudian
                    cuacaDiambil = System.currentTimeMillis() - 15 * 60_000L
                }
            }
        }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        latar.aktif = true
        penangan.post(detak)
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        latar.aktif = false
        penangan.removeCallbacks(detak)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        penangan.removeCallbacksAndMessages(null)
        pekerja.shutdownNow()
    }
}
