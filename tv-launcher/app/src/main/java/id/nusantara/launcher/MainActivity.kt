package id.nusantara.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Satu aplikasi terpasang yang bisa dibuka dari beranda. */
class Aplikasi(val nama: String, val paket: String, val buka: Intent, val banner: Drawable?, val ikon: Drawable?)

/** Layar beranda: jam, cuaca, dan satu baris aplikasi di atas latar daerah. */
class MainActivity : Activity() {

    private lateinit var simpanan: Simpanan
    private lateinit var latar: LatarView
    private lateinit var jam: TextView
    private lateinit var tanggal: TextView
    private lateinit var suhu: TextView
    private lateinit var keterangan: TextView
    private lateinit var kota: TextView
    private lateinit var ikon: IkonCuaca
    private lateinit var baris: LinearLayout

    private val penangan = Handler(Looper.getMainLooper())
    private val pekerja = Executors.newSingleThreadExecutor()
    private val bahasa = Locale("id", "ID")
    private val formatJam = SimpleDateFormat("HH.mm", bahasa)
    private val formatTanggal = SimpleDateFormat("EEEE, d MMMM", bahasa)

    private var u = 1f
    private var lokasi: Lokasi = DaftarLokasi.bawaan
    private var cuacaDiambil = 0L
    private var kunciBaris = ""

    private val detak = object : Runnable {
        override fun run() {
            perbaruiJam()
            if (System.currentTimeMillis() - cuacaDiambil > 20 * 60_000L) muatCuaca()
            penangan.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simpanan = Simpanan(this)
        u = resources.displayMetrics.widthPixels / 1920f
        bangun()
        if (simpanan.lokasi == null) startActivity(Intent(this, LokasiActivity::class.java))
    }

    private fun px(desain: Float) = (desain * u).toInt()

    private fun teks(ukuran: Float, tebal: Boolean): TextView {
        val t = TextView(this)
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, ukuran * u)
        t.typeface = if (tebal) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        t.includeFontPadding = false
        return t
    }

    private fun bangun() {
        val akar = FrameLayout(this)
        latar = LatarView(this)
        akar.addView(latar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Kiri atas: jam dan tanggal
        val kiri = LinearLayout(this)
        kiri.orientation = LinearLayout.VERTICAL
        jam = teks(104f, true)
        tanggal = teks(26f, false)
        kiri.addView(jam)
        kiri.addView(tanggal, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(8f) })
        akar.addView(kiri, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            leftMargin = px(96f)
            topMargin = px(64f)
        })

        // Kanan atas: ikon cuaca, suhu, keterangan, kota
        val kanan = LinearLayout(this)
        kanan.orientation = LinearLayout.VERTICAL
        kanan.gravity = Gravity.END
        val barisSuhu = LinearLayout(this)
        barisSuhu.orientation = LinearLayout.HORIZONTAL
        barisSuhu.gravity = Gravity.CENTER_VERTICAL
        ikon = IkonCuaca(this)
        suhu = teks(64f, true)
        suhu.text = "--°"
        barisSuhu.addView(ikon, LinearLayout.LayoutParams(px(56f), px(56f)).apply { rightMargin = px(14f) })
        barisSuhu.addView(suhu)
        keterangan = teks(26f, true)
        keterangan.text = "Memuat cuaca"
        kota = teks(22f, false)
        kanan.addView(barisSuhu)
        kanan.addView(keterangan, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(6f) })
        kanan.addView(kota, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(6f) })
        akar.addView(kanan, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            rightMargin = px(96f)
            topMargin = px(64f)
        })

        // Bawah: satu baris ubin aplikasi yang bisa digulir ke samping
        baris = LinearLayout(this)
        baris.orientation = LinearLayout.HORIZONTAL
        baris.clipChildren = false
        baris.clipToPadding = false
        baris.setPadding(px(90f), px(14f), px(90f), 0)
        val gulir = HorizontalScrollView(this)
        gulir.isHorizontalScrollBarEnabled = false
        gulir.clipChildren = false
        gulir.clipToPadding = false
        gulir.addView(baris, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        akar.addView(gulir, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            bottomMargin = px(44f)
        })

        setContentView(akar)
    }

    override fun onStart() {
        super.onStart()
        latar.aktif = true
        terapkanLokasi()
        penangan.post(detak)
        muatAplikasi()
    }

    override fun onStop() {
        super.onStop()
        latar.aktif = false
        penangan.removeCallbacks(detak)
    }

    override fun onDestroy() {
        super.onDestroy()
        pekerja.shutdownNow()
    }

    /** Tombol Home saat sudah di beranda: kembali ke ubin pertama. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (baris.childCount > 0) baris.getChildAt(0).requestFocus()
    }

    /** Beranda adalah layar paling dasar, jadi tombol Kembali tidak melakukan apa-apa. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    private fun terapkanLokasi() {
        val baru = simpanan.lokasi ?: DaftarLokasi.bawaan
        val berubah = baru != lokasi || kota.text.isEmpty()
        lokasi = baru
        kota.text = lokasi.judul
        latar.tema = PetaTema.untuk(lokasi.provinsi)
        if (berubah) cuacaDiambil = 0L
        warnaiTeks()
    }

    private fun perbaruiJam() {
        val kini = Date()
        jam.text = formatJam.format(kini)
        tanggal.text = formatTanggal.format(kini)
        val waktu = Waktu.sekarang()
        if (latar.waktu != waktu) {
            latar.waktu = waktu
            warnaiTeks()
        }
    }

    private fun warnaiTeks() {
        val gelap = latar.latarTerang
        val utama = Color.parseColor(if (gelap) "#0B2233" else "#F2F6F3")
        val lembut = Color.parseColor(if (gelap) "#12324A" else "#D3DEDA")
        jam.setTextColor(utama)
        suhu.setTextColor(utama)
        keterangan.setTextColor(utama)
        tanggal.setTextColor(lembut)
        kota.setTextColor(lembut)
        ikon.warna = if (gelap) utama else latar.tema.aksen
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
            runOnUiThread {
                if (tujuan != lokasi) return@runOnUiThread
                if (hasil == null) {
                    keterangan.text = "Cuaca tidak tersedia"
                    // coba lagi lima menit kemudian, bukan dua puluh
                    cuacaDiambil = System.currentTimeMillis() - 15 * 60_000L
                } else {
                    suhu.text = hasil.suhu.toString() + "°"
                    keterangan.text = hasil.kondisi.label
                    latar.kondisi = hasil.kondisi
                    ikon.setel(hasil.kondisi, hasil.siang)
                    warnaiTeks()
                }
            }
        }
    }

    private fun muatAplikasi() {
        val konteks = applicationContext
        pekerja.execute {
            val daftar = try {
                daftarAplikasi(konteks)
            } catch (e: Exception) {
                emptyList<Aplikasi>()
            }
            runOnUiThread { isiBaris(daftar) }
        }
    }

    private fun daftarAplikasi(konteks: Context): List<Aplikasi> {
        val pm = konteks.packageManager
        val hasil = LinkedHashMap<String, Aplikasi>()
        // Aplikasi TV lebih dulu (punya banner), lalu aplikasi biasa yang belum tercantum
        for (kategori in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val cari = Intent(Intent.ACTION_MAIN).addCategory(kategori)
            for (info in pm.queryIntentActivities(cari, 0)) {
                val aktivitas = info.activityInfo ?: continue
                if (aktivitas.packageName == konteks.packageName) continue
                if (hasil.containsKey(aktivitas.packageName)) continue
                val buka = Intent(Intent.ACTION_MAIN)
                    .addCategory(kategori)
                    .setClassName(aktivitas.packageName, aktivitas.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                val banner = aktivitas.loadBanner(pm) ?: aktivitas.applicationInfo.loadBanner(pm)
                hasil[aktivitas.packageName] = Aplikasi(
                    info.loadLabel(pm).toString(), aktivitas.packageName, buka, banner, info.loadIcon(pm)
                )
            }
        }
        return hasil.values.sortedBy { it.nama.lowercase() }
    }

    private fun isiBaris(daftar: List<Aplikasi>) {
        val tema = latar.tema
        val kunci = tema.name + "|" + daftar.joinToString(",") { it.paket }
        if (kunci == kunciBaris) return
        kunciBaris = kunci
        baris.removeAllViews()
        val latarUbin = Color.parseColor("#1C302E")
        val jarak = px(8f)

        fun tambah(ubin: Ubin, aksi: () -> Unit) {
            ubin.setOnClickListener { aksi() }
            baris.addView(ubin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = jarak })
        }

        for (aplikasi in daftar) {
            val ubin = Ubin(this, u, tema.aksen, aplikasi.nama)
            if (aplikasi.banner != null) ubin.gambar.setImageDrawable(aplikasi.banner)
            else ubin.pakaiIkon(aplikasi.ikon, latarUbin)
            tambah(ubin) { buka(aplikasi.buka) }
        }

        val ubinLokasi = Ubin(this, u, tema.aksen, "Lokasi")
        ubinLokasi.gambar.setImageDrawable(
            GambarBawaan(GambarBawaan.Jenis.LOKASI, Color.parseColor("#35566B"), Color.parseColor("#BFD9E6"), tema.aksen)
        )
        tambah(ubinLokasi) { buka(Intent(this, LokasiActivity::class.java)) }

        val ubinPengaturan = Ubin(this, u, tema.aksen, "Pengaturan")
        ubinPengaturan.gambar.setImageDrawable(
            GambarBawaan(GambarBawaan.Jenis.PENGATURAN, Color.parseColor("#3E575B"), Color.parseColor("#DCE7E4"), tema.aksen)
        )
        tambah(ubinPengaturan) { buka(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

        baris.getChildAt(0)?.requestFocus()
    }

    private fun buka(tujuan: Intent) {
        try {
            startActivity(tujuan)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa dibuka", Toast.LENGTH_SHORT).show()
        }
    }
}
