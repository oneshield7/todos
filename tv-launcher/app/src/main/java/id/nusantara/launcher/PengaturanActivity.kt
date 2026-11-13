package id.nusantara.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Layar pengaturan launcher: lokasi, beranda utama, animasi, bunyi, dan tampilan laci. */
class PengaturanActivity : Activity() {

    private lateinit var simpanan: Simpanan
    private lateinit var kolom: LinearLayout
    private var u = 1f

    private val terang = Color.parseColor("#F2F6F3")
    private val lembut = Color.parseColor("#B9C8C4")
    private val aksen = Color.parseColor("#C3E88D")
    private val latarBaris = Color.parseColor("#18272A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simpanan = Simpanan(this)
        Bunyi.siapkan(this)
        u = resources.displayMetrics.widthPixels / 1920f

        val akar = LinearLayout(this)
        akar.orientation = LinearLayout.VERTICAL
        akar.setBackgroundColor(Color.parseColor("#0F1A1C"))
        akar.setPadding(px(96f), px(56f), px(96f), px(40f))
        akar.addView(teks("Pengaturan launcher", 56f, terang, true))

        kolom = LinearLayout(this)
        kolom.orientation = LinearLayout.VERTICAL
        kolom.setPadding(px(6f), px(6f), px(6f), px(6f))
        kolom.clipToPadding = false
        val gulir = ScrollView(this)
        gulir.isVerticalScrollBarEnabled = false
        gulir.clipToPadding = false
        gulir.addView(kolom, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        akar.addView(gulir, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = px(24f) })
        setContentView(akar)
    }

    /** Disusun ulang tiap kali layar tampil, supaya nilai yang diubah di layar lain ikut terbarui. */
    override fun onResume() {
        super.onResume()
        isi(0)
    }

    private fun px(desain: Float) = (desain * u).toInt()

    private fun teks(isi: String, ukuran: Float, warna: Int, tebal: Boolean): TextView {
        val t = TextView(this)
        t.text = isi
        t.setTextColor(warna)
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, ukuran * u)
        t.typeface = if (tebal) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return t
    }

    private fun nyala(aktif: Boolean) = if (aktif) "Nyala" else "Mati"

    private fun isi(fokus: Int) {
        Bunyi.senyap = true
        kolom.removeAllViews()
        val lokasi = simpanan.lokasi ?: DaftarLokasi.bawaan
        val utama = Beranda.iniUtama(this)

        baris("Lokasi", "Menentukan latar dan cuaca", lokasi.judul) {
            startActivity(Intent(this, LokasiActivity::class.java))
        }
        baris(
            "Beranda utama",
            if (utama) "Tombol Home membuka launcher ini. Pilih untuk kembali ke launcher bawaan TV."
            else "Tombol Home masih membuka launcher bawaan TV. Pilih untuk memakai launcher ini.",
            if (utama) "Aktif" else "Belum aktif",
        ) {
            if (utama) {
                AlertDialog.Builder(this)
                    .setTitle("Kembalikan launcher bawaan?")
                    .setMessage("Tombol Home akan kembali membuka beranda bawaan TV. Nusantara Launcher tetap terpasang dan bisa diaktifkan lagi dari sini.")
                    .setPositiveButton("Kembalikan") { _, _ -> Beranda.kembalikanBawaan(this, simpanan) }
                    .setNegativeButton("Batal", null)
                    .show()
            } else {
                Beranda.jadikanUtama(this, simpanan)
            }
        }
        baris("Animasi cuaca", "Hujan, awan, kabut, dan kilat bergerak. Matikan bila TV terasa berat.", nyala(simpanan.animasi)) {
            simpanan.animasi = !simpanan.animasi
            isi(2)
        }
        baris("Bunyi navigasi", "Bunyi saat berpindah dan memilih ubin", nyala(simpanan.bunyi)) {
            simpanan.bunyi = !simpanan.bunyi
            Bunyi.aktif = simpanan.bunyi
            isi(3)
        }
        baris("Latar laci aplikasi", "Seberapa jelas beranda terlihat di balik laci Semua aplikasi", NAMA_BENING[simpanan.beningLaci]) {
            simpanan.beningLaci = (simpanan.beningLaci + 1) % NAMA_BENING.size
            isi(4)
        }
        baris("Pengaturan TV", "Jaringan, gambar, suara, dan pengaturan sistem lainnya", "") {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak bisa dibuka", Toast.LENGTH_SHORT).show()
            }
        }
        val versi = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        baris("Tentang", "Nusantara Launcher", "Versi $versi", null)

        (kolom.getChildAt(fokus) ?: kolom.getChildAt(0))?.requestFocus()
        kolom.post { Bunyi.senyap = false }
    }

    /** Satu baris pengaturan: judul dan keterangan di kiri, nilai saat ini di kanan. */
    private fun baris(judul: String, keterangan: String, nilai: String, aksi: (() -> Unit)?) {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER_VERTICAL
        b.setPadding(px(32f), px(22f), px(32f), px(22f))

        val kiri = LinearLayout(this)
        kiri.orientation = LinearLayout.VERTICAL
        kiri.addView(teks(judul, 30f, terang, true))
        kiri.addView(teks(keterangan, 22f, lembut, false))
        b.addView(kiri, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (nilai.isNotEmpty()) {
            b.addView(
                teks(nilai, 26f, if (aksi != null) aksen else lembut, true),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = px(32f) }
            )
        }

        val biasa = GradientDrawable()
        biasa.setColor(latarBaris)
        biasa.cornerRadius = 18f * u
        val terpilih = GradientDrawable()
        terpilih.setColor(latarBaris)
        terpilih.cornerRadius = 18f * u
        terpilih.setStroke(px(4f).coerceAtLeast(2), aksen)
        b.background = biasa
        b.isFocusable = true
        b.isSoundEffectsEnabled = false
        b.setOnFocusChangeListener { _, fokus ->
            b.background = if (fokus) terpilih else biasa
            if (fokus) Bunyi.pindah()
        }
        if (aksi != null) {
            b.setOnClickListener {
                Bunyi.pilih()
                aksi()
            }
        }
        kolom.addView(
            b,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(12f) }
        )
    }

    companion object {
        val NAMA_BENING = arrayOf("Bening", "Sedang", "Gelap")
    }
}
