package id.nusantara.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
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

/** Layar beranda: jam, cuaca, dan satu baris aplikasi pilihan di atas latar daerah. */
class MainActivity : Activity() {

    private lateinit var simpanan: Simpanan
    private lateinit var latar: LatarView
    private lateinit var jam: TextView
    private lateinit var detik: TextView
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
    private val formatDetik = SimpleDateFormat("ss", bahasa)
    private val formatTanggal = SimpleDateFormat("EEEE, d MMMM", bahasa)

    private var u = 1f
    private var lokasi: Lokasi = DaftarLokasi.bawaan
    private var cuacaDiambil = 0L
    private var kunciBaris = ""
    private var semua: List<Aplikasi> = emptyList()

    /** Berdetak tepat di pergantian detik. */
    private val detak = object : Runnable {
        override fun run() {
            perbaruiJam()
            val kini = System.currentTimeMillis()
            if (kini - cuacaDiambil > 20 * 60_000L) muatCuaca()
            penangan.postDelayed(this, 1000L - kini % 1000L + 5L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simpanan = Simpanan(this)
        Bunyi.siapkan(this)
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
        t.maxLines = 1
        return t
    }

    /** Ukuran mengikuti isi. Tanpa ini LinearLayout tegak melebarkan anak selebar induk dan teks terpotong. */
    private fun pas(atas: Float = 0f, kanan: Boolean = false): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.topMargin = px(atas)
        if (kanan) p.gravity = Gravity.END
        return p
    }

    private fun bangun() {
        val akar = FrameLayout(this)
        latar = LatarView(this)
        akar.addView(latar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Kiri atas: jam dengan detik kecil, lalu tanggal
        val kiri = LinearLayout(this)
        kiri.orientation = LinearLayout.VERTICAL
        val barisJam = LinearLayout(this)
        barisJam.orientation = LinearLayout.HORIZONTAL
        jam = teks(104f, true)
        jam.fontFeatureSettings = "tnum"
        detik = teks(44f, true)
        detik.fontFeatureSettings = "tnum"
        barisJam.addView(jam, pas())
        barisJam.addView(detik, pas().apply { leftMargin = px(12f) })
        tanggal = teks(26f, false)
        kiri.addView(barisJam, pas())
        kiri.addView(tanggal, pas(8f))
        akar.addView(kiri, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            leftMargin = px(96f)
            topMargin = px(64f)
        })

        // Kanan atas: ikon cuaca, suhu, keterangan, kota; semuanya rata kanan
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
        barisSuhu.addView(suhu, pas())
        keterangan = teks(26f, true)
        keterangan.text = "Memuat cuaca"
        kota = teks(22f, false)
        kanan.addView(barisSuhu, pas(0f, true))
        kanan.addView(keterangan, pas(6f, true))
        kanan.addView(kota, pas(6f, true))
        akar.addView(kanan, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            rightMargin = px(96f)
            topMargin = px(64f)
        })

        // Bawah: satu baris ubin
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

    /**
     * Tombol remote yang biasanya ditangani launcher bawaan TV. Saat launcher ini di depan,
     * tombol itu dikirim ke sini, jadi harus diteruskan sendiri ke fungsi yang sesuai.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // Beranda adalah layar paling dasar
            KeyEvent.KEYCODE_BACK -> return true
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                bukaPengaturan()
                return true
            }
            KeyEvent.KEYCODE_TV_INPUT -> {
                pilihInput()
                return true
            }
            KeyEvent.KEYCODE_ALL_APPS -> {
                buka(Intent(this, SemuaActivity::class.java))
                return true
            }
        }
        if (event != null && event.repeatCount == 0 && keyCode !in TOMBOL_BIASA) {
            // Bantuan selama masa uji: menyebut tombol yang belum punya fungsi di launcher ini
            Toast.makeText(this, "Tombol belum didukung: " + KeyEvent.keyCodeToString(keyCode), Toast.LENGTH_SHORT).show()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun bukaPengaturan() {
        buka(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Daftar sumber input TV (HDMI, AV, dan sejenisnya) yang dilaporkan sistem. */
    private fun pilihInput() {
        val pengelola = getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        val masukan = try {
            pengelola?.tvInputList?.filter { it.isPassthroughInput } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        if (masukan.isEmpty()) {
            Toast.makeText(this, "Sumber input tidak ditemukan di TV ini", Toast.LENGTH_SHORT).show()
            return
        }
        val nama = Array(masukan.size) { masukan[it].loadLabel(this).toString() }
        AlertDialog.Builder(this)
            .setTitle("Sumber input")
            .setItems(nama) { _, pilihan ->
                val tujuan = TvContract.buildChannelUriForPassthroughInput(masukan[pilihan].id)
                buka(Intent(Intent.ACTION_VIEW, tujuan).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            .show()
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
        detik.text = formatDetik.format(kini)
        val teksJam = formatJam.format(kini)
        if (jam.text.toString() != teksJam) {
            jam.text = teksJam
            tanggal.text = formatTanggal.format(kini)
        }
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
        detik.setTextColor(lembut)
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
                PustakaAplikasi.muat(konteks)
            } catch (e: Exception) {
                emptyList<Aplikasi>()
            }
            runOnUiThread {
                semua = daftar
                isiBaris()
            }
        }
    }

    /** Baris beranda: aplikasi pilihan, lalu Semua aplikasi, Input, Lokasi, dan Pengaturan. */
    private fun isiBaris() {
        val tema = latar.tema
        val pilihan = PustakaAplikasi.untukBeranda(semua, simpanan)
        val kunci = tema.name + "|" + pilihan.joinToString(",") { it.paket }
        if (kunci == kunciBaris) return
        kunciBaris = kunci
        Bunyi.senyap = true
        baris.removeAllViews()
        val latarUbin = Color.parseColor("#1C302E")
        val jarak = px(8f)

        fun tambah(ubin: Ubin, aksi: () -> Unit) {
            ubin.setOnClickListener {
                Bunyi.pilih()
                aksi()
            }
            baris.addView(ubin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = jarak })
        }

        fun bawaan(nama: String, jenis: GambarBawaan.Jenis, latarWarna: String, terang: String, aksi: () -> Unit) {
            val ubin = Ubin(this, u, tema.aksen, nama)
            ubin.gambar.setImageDrawable(GambarBawaan(jenis, Color.parseColor(latarWarna), Color.parseColor(terang), tema.aksen))
            tambah(ubin, aksi)
        }

        for (aplikasi in pilihan) {
            val ubin = Ubin(this, u, tema.aksen, aplikasi.nama)
            if (aplikasi.banner != null) ubin.gambar.setImageDrawable(aplikasi.banner)
            else ubin.pakaiIkon(aplikasi.ikon, latarUbin)
            tambah(ubin) { buka(aplikasi.buka) }
            // Tekan lama OK: lepas dari beranda
            ubin.setOnLongClickListener {
                val pesan = when (simpanan.gantiFavorit(aplikasi.paket)) {
                    HasilFavorit.DITAMBAH -> aplikasi.nama + " disematkan di beranda"
                    HasilFavorit.DIHAPUS -> aplikasi.nama + " dilepas dari beranda"
                    HasilFavorit.PENUH -> "Beranda sudah penuh (" + Simpanan.MAKS_FAVORIT + " aplikasi)"
                }
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                isiBaris()
                true
            }
        }

        bawaan("Semua aplikasi", GambarBawaan.Jenis.SEMUA, "#35566B", "#BFD9E6") { buka(Intent(this, SemuaActivity::class.java)) }
        bawaan("Input", GambarBawaan.Jenis.INPUT, "#2F6A50", "#DCE7E4") { pilihInput() }
        bawaan("Lokasi", GambarBawaan.Jenis.LOKASI, "#35566B", "#BFD9E6") { buka(Intent(this, LokasiActivity::class.java)) }
        bawaan("Pengaturan", GambarBawaan.Jenis.PENGATURAN, "#3E575B", "#DCE7E4") { bukaPengaturan() }

        baris.getChildAt(0)?.requestFocus()
        baris.post { Bunyi.senyap = false }
    }

    private fun buka(tujuan: Intent) {
        try {
            startActivity(tujuan)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa dibuka", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        /** Tombol yang memang tidak perlu ditangani launcher: arah, OK, volume, media, daya. */
        private val TOMBOL_BIASA = setOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST,
        )
    }
}
