package id.nusantara.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.concurrent.Executors

/** Memilih kota atau kabupaten: 38 ibu kota provinsi sebagai pilihan cepat, sisanya lewat pencarian. */
class LokasiActivity : Activity() {

    private val pekerja = Executors.newSingleThreadExecutor()
    private val daftar = ArrayList<Lokasi>(DaftarLokasi.ibuKota)
    private lateinit var adaptor: ArrayAdapter<Lokasi>
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val u = resources.displayMetrics.widthPixels / 1920f
        fun px(desain: Float) = (desain * u).toInt()
        val terang = Color.parseColor("#F2F6F3")
        val lembut = Color.parseColor("#B9C8C4")

        val akar = LinearLayout(this)
        akar.orientation = LinearLayout.VERTICAL
        akar.setBackgroundColor(Color.parseColor("#0F1A1C"))
        akar.setPadding(px(96f), px(56f), px(96f), px(40f))

        val judul = TextView(this)
        judul.text = "Lokasi"
        judul.setTextColor(terang)
        judul.typeface = Typeface.DEFAULT_BOLD
        judul.setTextSize(TypedValue.COMPLEX_UNIT_PX, 56f * u)
        akar.addView(judul)

        val keterangan = TextView(this)
        keterangan.text = "Pilih ibu kota provinsi di bawah, atau cari kota dan kabupaten lain."
        keterangan.setTextColor(lembut)
        keterangan.setTextSize(TypedValue.COMPLEX_UNIT_PX, 24f * u)
        akar.addView(keterangan)

        val kolom = EditText(this)
        kolom.hint = "Cari kota atau kabupaten"
        kolom.setHintTextColor(lembut)
        kolom.setTextColor(terang)
        kolom.setTextSize(TypedValue.COMPLEX_UNIT_PX, 26f * u)
        kolom.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        kolom.maxLines = 1
        kolom.imeOptions = EditorInfo.IME_ACTION_SEARCH
        kolom.setOnEditorActionListener { _, aksi, _ ->
            if (aksi == EditorInfo.IME_ACTION_SEARCH || aksi == EditorInfo.IME_ACTION_DONE) {
                cari(kolom.text.toString())
                true
            } else {
                false
            }
        }
        akar.addView(
            kolom,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(20f) }
        )

        status = TextView(this)
        status.setTextColor(lembut)
        status.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * u)
        status.text = "Ibu kota 38 provinsi"
        akar.addView(
            status,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(16f)
                bottomMargin = px(8f)
            }
        )

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
        val list = ListView(this)
        list.adapter = adaptor
        list.setOnItemClickListener { _, _, posisi, _ ->
            Simpanan(this).lokasi = daftar[posisi]
            finish()
        }
        akar.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(akar)
        list.requestFocus()
    }

    private fun cari(kata: String) {
        if (kata.isBlank()) {
            tampilkan(DaftarLokasi.ibuKota, "Ibu kota 38 provinsi")
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
                    else -> tampilkan(hasil, "Hasil pencarian")
                }
            }
        }
    }

    private fun tampilkan(isi: List<Lokasi>, keterangan: String) {
        daftar.clear()
        daftar.addAll(isi)
        adaptor.notifyDataSetChanged()
        status.text = keterangan
    }

    override fun onDestroy() {
        super.onDestroy()
        pekerja.shutdownNow()
    }
}
