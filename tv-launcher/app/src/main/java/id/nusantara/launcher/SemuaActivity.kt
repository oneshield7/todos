package id.nusantara.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Semua aplikasi dalam kisi tujuh kolom, supaya tidak perlu menggulir satu baris panjang.
 * OK membuka aplikasi; tekan lama OK menyematkan atau melepasnya dari beranda.
 */
class SemuaActivity : Activity() {

    private val pekerja = Executors.newSingleThreadExecutor()
    private lateinit var simpanan: Simpanan
    private lateinit var kisi: LinearLayout
    private var u = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simpanan = Simpanan(this)
        Bunyi.siapkan(this)
        u = resources.displayMetrics.widthPixels / 1920f
        val terang = Color.parseColor("#F2F6F3")
        val lembut = Color.parseColor("#B9C8C4")

        val akar = LinearLayout(this)
        akar.orientation = LinearLayout.VERTICAL
        akar.setBackgroundColor(Color.parseColor("#0F1A1C"))

        val judul = TextView(this)
        judul.text = "Semua aplikasi"
        judul.setTextColor(terang)
        judul.typeface = Typeface.DEFAULT_BOLD
        judul.setTextSize(TypedValue.COMPLEX_UNIT_PX, 48f * u)
        judul.setPadding(px(96f), px(48f), px(96f), 0)
        akar.addView(judul)

        val petunjuk = TextView(this)
        petunjuk.text = "Tekan lama OK untuk menyematkan aplikasi di beranda atau melepasnya. Titik di sudut ubin menandai yang disematkan."
        petunjuk.setTextColor(lembut)
        petunjuk.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * u)
        petunjuk.setPadding(px(96f), px(6f), px(96f), px(8f))
        akar.addView(petunjuk)

        kisi = LinearLayout(this)
        kisi.orientation = LinearLayout.VERTICAL
        kisi.clipChildren = false
        kisi.clipToPadding = false
        kisi.setPadding(px(90f), px(14f), px(90f), px(40f))
        val gulir = ScrollView(this)
        gulir.isVerticalScrollBarEnabled = false
        gulir.clipChildren = false
        gulir.clipToPadding = false
        gulir.addView(kisi, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        akar.addView(gulir, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(akar)
        muat()
    }

    private fun px(desain: Float) = (desain * u).toInt()

    private fun muat() {
        val konteks = applicationContext
        pekerja.execute {
            val daftar = try {
                PustakaAplikasi.muat(konteks)
            } catch (e: Exception) {
                emptyList<Aplikasi>()
            }
            runOnUiThread { isi(daftar) }
        }
    }

    private fun isi(daftar: List<Aplikasi>) {
        Bunyi.senyap = true
        kisi.removeAllViews()
        val aksen = PetaTema.untuk((simpanan.lokasi ?: DaftarLokasi.bawaan).provinsi).aksen
        val latarUbin = Color.parseColor("#1C302E")
        val favorit = simpanan.favorit.toHashSet()
        var baris: LinearLayout? = null

        for ((nomor, aplikasi) in daftar.withIndex()) {
            if (nomor % KOLOM == 0) {
                baris = LinearLayout(this)
                baris.orientation = LinearLayout.HORIZONTAL
                baris.clipChildren = false
                baris.clipToPadding = false
                kisi.addView(baris, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(6f) })
            }
            val ubin = Ubin(this, u, aksen, aplikasi.nama)
            if (aplikasi.banner != null) ubin.gambar.setImageDrawable(aplikasi.banner)
            else ubin.pakaiIkon(aplikasi.ikon, latarUbin)
            ubin.tandai(aplikasi.paket in favorit)
            ubin.setOnClickListener {
                Bunyi.pilih()
                try {
                    startActivity(aplikasi.buka)
                } catch (e: Exception) {
                    Toast.makeText(this, "Tidak bisa dibuka", Toast.LENGTH_SHORT).show()
                }
            }
            ubin.setOnLongClickListener {
                val pesan = when (simpanan.gantiFavorit(aplikasi.paket)) {
                    HasilFavorit.DITAMBAH -> {
                        ubin.tandai(true)
                        aplikasi.nama + " disematkan di beranda"
                    }
                    HasilFavorit.DIHAPUS -> {
                        ubin.tandai(false)
                        aplikasi.nama + " dilepas dari beranda"
                    }
                    HasilFavorit.PENUH -> "Beranda sudah penuh (" + Simpanan.MAKS_FAVORIT + " aplikasi). Lepas satu dulu."
                }
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                true
            }
            baris?.addView(ubin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = px(8f) })
        }

        (kisi.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.requestFocus()
        kisi.post { Bunyi.senyap = false }
    }

    override fun onDestroy() {
        super.onDestroy()
        pekerja.shutdownNow()
    }

    companion object {
        private const val KOLOM = 7
    }
}
