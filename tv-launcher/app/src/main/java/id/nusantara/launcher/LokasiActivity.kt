package id.nusantara.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.concurrent.Executors

/**
 * Menentukan lokasi TV. Layar pertama menawarkan dua cara: deteksi otomatis dari jaringan
 * internet, atau memilih sendiri dari daftar dan pencarian.
 */
class LokasiActivity : Activity() {

    private val pekerja = Executors.newSingleThreadExecutor()
    private val daftar = ArrayList<Lokasi>(DaftarLokasi.ibuKota)
    private lateinit var adaptor: ArrayAdapter<Lokasi>
    private lateinit var status: TextView

    private lateinit var halamanPilihan: View
    private lateinit var halamanDeteksi: LinearLayout
    private lateinit var halamanManual: View
    private lateinit var kartuOtomatis: View
    private lateinit var list: ListView

    private var u = 1f
    private val terang = Color.parseColor("#F2F6F3")
    private val lembut = Color.parseColor("#B9C8C4")
    private val aksen = Color.parseColor("#C3E88D")
    private val latarKartu = Color.parseColor("#1C302E")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        u = resources.displayMetrics.widthPixels / 1920f
        Bunyi.siapkan(this)

        val akar = FrameLayout(this)
        akar.setBackgroundColor(Color.parseColor("#0F1A1C"))
        halamanPilihan = bangunPilihan()
        halamanDeteksi = LinearLayout(this)
        halamanDeteksi.orientation = LinearLayout.VERTICAL
        halamanDeteksi.gravity = Gravity.CENTER
        halamanManual = bangunManual()
        val penuh = ViewGroup.LayoutParams.MATCH_PARENT
        akar.addView(halamanPilihan, FrameLayout.LayoutParams(penuh, penuh))
        akar.addView(halamanDeteksi, FrameLayout.LayoutParams(penuh, penuh))
        akar.addView(halamanManual, FrameLayout.LayoutParams(penuh, penuh))
        setContentView(akar)
        tampilkan(halamanPilihan)
        kartuOtomatis.requestFocus()
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

    private fun bungkus(atas: Float = 0f) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(atas) }

    /** Kotak yang bisa dipilih dengan remote: berbingkai aksen saat terfokus. */
    private fun bisaDipilih(v: View, sudut: Float, aksi: () -> Unit) {
        val biasa = GradientDrawable()
        biasa.setColor(latarKartu)
        biasa.cornerRadius = sudut * u
        val terpilih = GradientDrawable()
        terpilih.setColor(latarKartu)
        terpilih.cornerRadius = sudut * u
        terpilih.setStroke(px(5f).coerceAtLeast(2), aksen)
        v.background = biasa
        v.isFocusable = true
        v.isSoundEffectsEnabled = false
        v.setOnFocusChangeListener { _, fokus ->
            v.background = if (fokus) terpilih else biasa
            if (fokus) Bunyi.pindah()
        }
        v.setOnClickListener {
            Bunyi.pilih()
            aksi()
        }
    }

    private fun kartu(judul: String, isi: String, aksi: () -> Unit): View {
        val k = LinearLayout(this)
        k.orientation = LinearLayout.VERTICAL
        k.setPadding(px(44f), px(44f), px(44f), px(44f))
        k.addView(teks(judul, 40f, terang, true))
        k.addView(teks(isi, 26f, lembut, false), bungkus(12f))
        bisaDipilih(k, 28f, aksi)
        return k
    }

    private fun tombol(isi: String, aksi: () -> Unit): View {
        val t = teks(isi, 30f, terang, true)
        t.setPadding(px(40f), px(22f), px(40f), px(22f))
        bisaDipilih(t, 18f, aksi)
        return t
    }

    private fun tampilkan(halaman: View) {
        for (h in listOf(halamanPilihan, halamanDeteksi, halamanManual)) {
            h.visibility = if (h === halaman) View.VISIBLE else View.GONE
        }
    }

    // ---- Halaman 1: dua pilihan ----

    private fun bangunPilihan(): View {
        val kolom = LinearLayout(this)
        kolom.orientation = LinearLayout.VERTICAL
        kolom.gravity = Gravity.CENTER_HORIZONTAL
        kolom.setPadding(px(96f), px(170f), px(96f), px(60f))
        kolom.addView(teks("Di mana TV ini berada?", 76f, terang, true), bungkus())
        kolom.addView(teks("Latar dan cuaca akan mengikuti lokasi TV.", 28f, lembut, false), bungkus(14f))

        val baris = LinearLayout(this)
        baris.orientation = LinearLayout.HORIZONTAL
        kartuOtomatis = kartu("Deteksi otomatis", "Dari jaringan internet TV, tanpa GPS") { deteksi() }
        val kartuManual = kartu("Pilih sendiri", "Cari kota atau kabupaten") { bukaManual() }
        baris.addView(kartuOtomatis, LinearLayout.LayoutParams(px(620f), ViewGroup.LayoutParams.WRAP_CONTENT))
        baris.addView(kartuManual, LinearLayout.LayoutParams(px(620f), ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = px(40f) })
        kolom.addView(baris, bungkus(90f))
        return kolom
    }

    // ---- Halaman 2: deteksi otomatis dan hasilnya ----

    private fun isiDeteksi(kecil: String, besar: String, catatan: String, tombolTombol: List<View>) {
        halamanDeteksi.removeAllViews()
        halamanDeteksi.addView(teks(kecil, 28f, lembut, false), bungkus())
        if (besar.isNotEmpty()) halamanDeteksi.addView(teks(besar, 76f, terang, true), bungkus(10f))
        if (catatan.isNotEmpty()) {
            val t = teks(catatan, 26f, lembut, false)
            t.gravity = Gravity.CENTER
            halamanDeteksi.addView(t, bungkus(14f))
        }
        if (tombolTombol.isNotEmpty()) {
            val baris = LinearLayout(this)
            baris.orientation = LinearLayout.HORIZONTAL
            for ((i, t) in tombolTombol.withIndex()) {
                baris.addView(t, bungkus().apply { leftMargin = if (i == 0) 0 else px(28f) })
            }
            halamanDeteksi.addView(baris, bungkus(60f))
            tombolTombol[0].requestFocus()
        }
    }

    private fun deteksi() {
        tampilkan(halamanDeteksi)
        isiDeteksi("Mendeteksi lokasi", "", "Membaca perkiraan lokasi dari jaringan internet", emptyList())
        pekerja.execute {
            val hasil = try {
                Layanan.deteksi()
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (isFinishing || halamanDeteksi.visibility != View.VISIBLE) return@runOnUiThread
                if (hasil == null) {
                    isiDeteksi(
                        "Lokasi tidak bisa dideteksi",
                        "",
                        "Periksa sambungan internet TV, atau pilih lokasi sendiri.",
                        listOf(tombol("Coba lagi") { deteksi() }, tombol("Pilih sendiri") { bukaManual() }),
                    )
                } else {
                    isiDeteksi(
                        "Lokasi ditemukan",
                        hasil.judul,
                        "Perkiraan dari jaringan internet, bisa meleset ke kota tetangga.",
                        listOf(tombol("Pakai lokasi ini") { simpan(hasil) }, tombol("Pilih sendiri") { bukaManual() }),
                    )
                }
            }
        }
    }

    // ---- Halaman 3: pilih sendiri ----

    private fun bangunManual(): View {
        val kolom = LinearLayout(this)
        kolom.orientation = LinearLayout.VERTICAL
        kolom.setPadding(px(96f), px(56f), px(96f), px(40f))
        kolom.addView(teks("Pilih lokasi", 56f, terang, true))
        kolom.addView(teks("Pilih ibu kota provinsi di bawah, atau cari kota dan kabupaten lain.", 24f, lembut, false))

        val kolomCari = EditText(this)
        kolomCari.hint = "Cari kota atau kabupaten"
        kolomCari.setHintTextColor(lembut)
        kolomCari.setTextColor(terang)
        kolomCari.setTextSize(TypedValue.COMPLEX_UNIT_PX, 26f * u)
        kolomCari.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        kolomCari.maxLines = 1
        kolomCari.imeOptions = EditorInfo.IME_ACTION_SEARCH
        kolomCari.setOnEditorActionListener { _, aksi, _ ->
            if (aksi == EditorInfo.IME_ACTION_SEARCH || aksi == EditorInfo.IME_ACTION_DONE) {
                cari(kolomCari.text.toString())
                true
            } else {
                false
            }
        }
        kolom.addView(
            kolomCari,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(20f) }
        )

        status = teks("Ibu kota 38 provinsi", 22f, lembut, false)
        kolom.addView(status, bungkus(16f).apply { bottomMargin = px(8f) })

        adaptor = object : ArrayAdapter<Lokasi>(this, android.R.layout.simple_list_item_2, android.R.id.text1, daftar) {
            override fun getView(posisi: Int, lama: View?, induk: ViewGroup): View {
                val baris = super.getView(posisi, lama, induk)
                val lokasi = daftar[posisi]
                val atas = baris.findViewById<TextView>(android.R.id.text1)!!
                val bawah = baris.findViewById<TextView>(android.R.id.text2)!!
                atas.text = lokasi.nama
                atas.setTextColor(terang)
                atas.setTextSize(TypedValue.COMPLEX_UNIT_PX, 28f * u)
                bawah.text = lokasi.provinsi
                bawah.setTextColor(lembut)
                bawah.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * u)
                return baris
            }
        }
        list = ListView(this)
        list.adapter = adaptor
        list.setOnItemClickListener { _, _, posisi, _ -> simpan(daftar[posisi]) }
        kolom.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return kolom
    }

    private fun bukaManual() {
        tampilkan(halamanManual)
        list.requestFocus()
    }

    private fun cari(kata: String) {
        if (kata.isBlank()) {
            isiDaftar(DaftarLokasi.ibuKota, "Ibu kota 38 provinsi")
            return
        }
        status.text = "Mencari..."
        pekerja.execute {
            val hasil = try {
                Layanan.cari(kata)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                when {
                    hasil == null -> status.text = "Gagal terhubung. Periksa sambungan internet TV."
                    hasil.isEmpty() -> status.text = "Tidak ditemukan. Coba ejaan lain."
                    else -> isiDaftar(hasil, "Hasil pencarian")
                }
            }
        }
    }

    private fun isiDaftar(isi: List<Lokasi>, keterangan: String) {
        daftar.clear()
        daftar.addAll(isi)
        adaptor.notifyDataSetChanged()
        status.text = keterangan
    }

    private fun simpan(lokasi: Lokasi) {
        Simpanan(this).lokasi = lokasi
        finish()
    }

    /** Kembali dari halaman deteksi atau daftar ke dua pilihan awal. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && halamanPilihan.visibility != View.VISIBLE) {
            tampilkan(halamanPilihan)
            kartuOtomatis.requestFocus()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        pekerja.shutdownNow()
    }
}
