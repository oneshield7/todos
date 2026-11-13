package id.nusantara.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Layar beranda: jam, cuaca, dan satu baris aplikasi pilihan di atas latar daerah.
 * Laci aplikasi bergeser naik dari bawah saat tombol bawah ditekan.
 */
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
    private lateinit var gulirBaris: HorizontalScrollView
    private lateinit var petunjuk: TextView
    private lateinit var redup: View
    private lateinit var laci: FrameLayout
    private lateinit var kisi: LinearLayout

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
    private var kunciLaci = ""
    private var semua: List<Aplikasi> = emptyList()
    private var laciTerbuka = false
    private var tinggiLaci = 0

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
        val penuh = ViewGroup.LayoutParams.MATCH_PARENT
        val isi = ViewGroup.LayoutParams.WRAP_CONTENT
        val akar = FrameLayout(this)
        latar = LatarView(this)
        akar.addView(latar, FrameLayout.LayoutParams(penuh, penuh))

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
        akar.addView(kiri, FrameLayout.LayoutParams(isi, isi, Gravity.TOP or Gravity.START).apply {
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
        akar.addView(kanan, FrameLayout.LayoutParams(isi, isi, Gravity.TOP or Gravity.END).apply {
            rightMargin = px(96f)
            topMargin = px(64f)
        })

        // Bawah: satu baris ubin, dengan petunjuk kecil di bawahnya
        baris = LinearLayout(this)
        baris.orientation = LinearLayout.HORIZONTAL
        baris.clipChildren = false
        baris.clipToPadding = false
        baris.setPadding(px(90f), px(14f), px(90f), 0)
        gulirBaris = HorizontalScrollView(this)
        gulirBaris.isHorizontalScrollBarEnabled = false
        gulirBaris.clipChildren = false
        gulirBaris.clipToPadding = false
        gulirBaris.addView(baris, ViewGroup.LayoutParams(isi, isi))
        akar.addView(gulirBaris, FrameLayout.LayoutParams(penuh, isi, Gravity.BOTTOM).apply { bottomMargin = px(52f) })

        petunjuk = teks(20f, true)
        petunjuk.text = "Tekan bawah untuk semua aplikasi"
        petunjuk.setTextColor(Color.parseColor("#D3DEDA"))
        petunjuk.alpha = 0.85f
        akar.addView(petunjuk, FrameLayout.LayoutParams(isi, isi, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = px(18f) })

        // Lapisan peredup dan laci aplikasi, keduanya di atas beranda
        redup = View(this)
        redup.setBackgroundColor(Color.argb(105, 9, 18, 20))
        redup.alpha = 0f
        akar.addView(redup, FrameLayout.LayoutParams(penuh, penuh))

        tinggiLaci = (resources.displayMetrics.heightPixels * 0.8f).toInt()
        laci = FrameLayout(this)
        val latarLaci = GradientDrawable()
        // Tembus pandang: latar beranda masih samar terlihat di balik laci
        latarLaci.setColor(Color.argb(212, 15, 26, 28))
        val sudut = 36f * u
        latarLaci.cornerRadii = floatArrayOf(sudut, sudut, sudut, sudut, 0f, 0f, 0f, 0f)
        laci.background = latarLaci

        val kolom = LinearLayout(this)
        kolom.orientation = LinearLayout.VERTICAL
        val pegangan = View(this)
        val latarPegangan = GradientDrawable()
        latarPegangan.setColor(Color.argb(115, 242, 246, 243))
        latarPegangan.cornerRadius = 3f * u
        pegangan.background = latarPegangan
        kolom.addView(pegangan, LinearLayout.LayoutParams(px(72f), px(6f).coerceAtLeast(2)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = px(16f)
        })
        val judul = teks(40f, true)
        judul.text = "Semua aplikasi"
        judul.setTextColor(Color.parseColor("#F2F6F3"))
        kolom.addView(judul, pas(18f).apply { leftMargin = px(96f) })
        val catatan = teks(20f, false)
        catatan.text = "Tekan lama OK untuk menyematkan ke beranda atau melepasnya. Titik di sudut ubin menandai yang disematkan."
        catatan.setTextColor(Color.parseColor("#B9C8C4"))
        kolom.addView(catatan, pas(6f).apply { leftMargin = px(96f) })

        kisi = LinearLayout(this)
        kisi.orientation = LinearLayout.VERTICAL
        kisi.clipChildren = false
        kisi.clipToPadding = false
        kisi.setPadding(px(90f), px(14f), px(90f), px(40f))
        val gulirLaci = ScrollView(this)
        gulirLaci.isVerticalScrollBarEnabled = false
        gulirLaci.clipChildren = false
        gulirLaci.clipToPadding = false
        gulirLaci.addView(kisi, ViewGroup.LayoutParams(penuh, isi))
        kolom.addView(gulirLaci, LinearLayout.LayoutParams(penuh, 0, 1f).apply { topMargin = px(10f) })
        laci.addView(kolom, FrameLayout.LayoutParams(penuh, penuh))

        laci.translationY = tinggiLaci.toFloat()
        laci.visibility = View.INVISIBLE
        akar.addView(laci, FrameLayout.LayoutParams(penuh, tinggiLaci, Gravity.BOTTOM))

        setContentView(akar)
    }

    override fun onStart() {
        super.onStart()
        latar.aktif = true
        terapkanLokasi()
        penangan.post(detak)
        muatAplikasi()
        tawarkanBerandaUtama()
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

    /** Tombol Home saat sudah di beranda: tutup laci dan kembali ke ubin pertama. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (laciTerbuka) tutupLaci() else fokusBaris()
    }

    /**
     * Tombol remote yang biasanya ditangani launcher bawaan TV. Saat launcher ini di depan,
     * tombol itu dikirim ke sini, jadi harus diteruskan sendiri ke fungsi yang sesuai.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (laciTerbuka) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    tutupLaci()
                    return true
                }
                // Tombol atas di baris teratas: tidak ada apa-apa lagi di atasnya, jadi laci ditutup
                KeyEvent.KEYCODE_DPAD_UP -> {
                    val fokus = currentFocus
                    if (fokus != null && kisi.childCount > 0 && fokus.parent === kisi.getChildAt(0)) {
                        tutupLaci()
                        return true
                    }
                }
            }
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            // Beranda adalah layar paling dasar
            KeyEvent.KEYCODE_BACK -> return true
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_ALL_APPS -> {
                bukaLaci()
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                menuLauncher()
                return true
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                bukaPengaturanTv()
                return true
            }
            KeyEvent.KEYCODE_TV_INPUT -> {
                pilihInput()
                return true
            }
        }
        if (event != null && event.repeatCount == 0 && keyCode !in TOMBOL_BIASA) {
            // Bantuan selama masa uji: menyebut tombol yang belum punya fungsi di launcher ini
            Toast.makeText(this, "Tombol belum didukung: " + KeyEvent.keyCodeToString(keyCode), Toast.LENGTH_SHORT).show()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun pesan(isi: String) {
        Toast.makeText(this, isi, Toast.LENGTH_LONG).show()
    }

    /** Membuka layar lain; mengembalikan salah bila TV tidak punya layar itu. */
    private fun buka(tujuan: Intent): Boolean {
        return try {
            startActivity(tujuan)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun bukaAtauPesan(tujuan: Intent) {
        if (!buka(tujuan)) pesan("Tidak bisa dibuka")
    }

    private fun bukaPengaturanTv() {
        bukaAtauPesan(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---- Pengaturan launcher ----

    private fun menuLauncher() {
        val utama = Beranda.iniUtama(this)
        val pilihan = arrayOf(
            "Pengaturan TV",
            "Ubah lokasi",
            if (utama) "Kembalikan launcher bawaan" else "Jadikan beranda utama",
        )
        AlertDialog.Builder(this)
            .setTitle("Pengaturan")
            .setItems(pilihan) { _, nomor ->
                when (nomor) {
                    0 -> bukaPengaturanTv()
                    1 -> bukaAtauPesan(Intent(this, LokasiActivity::class.java))
                    else -> if (utama) kembalikanBawaan() else jadikanBerandaUtama()
                }
            }
            .show()
    }

    /** Ditawarkan sekali, setelah lokasi dipilih, bila launcher ini belum menjadi beranda utama. */
    private fun tawarkanBerandaUtama() {
        if (simpanan.lokasi == null || simpanan.sudahTanyaBeranda || Beranda.iniUtama(this)) return
        simpanan.sudahTanyaBeranda = true
        AlertDialog.Builder(this)
            .setTitle("Jadikan beranda utama?")
            .setMessage(
                "Tombol Home akan membuka Nusantara Launcher, sehingga beranda bawaan TV beserta iklannya tidak tampil lagi. " +
                    "Bisa dikembalikan kapan saja lewat ubin Pengaturan."
            )
            .setPositiveButton("Jadikan") { _, _ -> jadikanBerandaUtama() }
            .setNegativeButton("Nanti", null)
            .show()
    }

    /** Android tidak mengizinkan aplikasi menjadikan dirinya beranda diam-diam; pengguna yang menyetujui. */
    private fun jadikanBerandaUtama() {
        // Cara resmi di Android 10 ke atas: sistem menampilkan dialog persetujuan
        if (Build.VERSION.SDK_INT >= 29) {
            val peran = getSystemService(RoleManager::class.java)
            if (peran != null && peran.isRoleAvailable(RoleManager.ROLE_HOME) && !peran.isRoleHeld(RoleManager.ROLE_HOME)) {
                try {
                    startActivityForResult(peran.createRequestRoleIntent(RoleManager.ROLE_HOME), MINTA_BERANDA)
                    return
                } catch (e: Exception) {
                    // lanjut ke cara berikutnya
                }
            }
        }
        // Layar "aplikasi Beranda" di pengaturan TV
        if (buka(Intent(Settings.ACTION_HOME_SETTINGS))) {
            pesan("Pilih Nusantara Launcher sebagai aplikasi Beranda")
            return
        }
        // TV mengunci launcher pabrikan: tawarkan pengalih tombol Home
        AlertDialog.Builder(this)
            .setTitle("TV ini tidak menyediakan pilihan launcher")
            .setMessage(
                "Nyalakan \"Pengalih tombol Home\" milik Nusantara Launcher di menu Aksesibilitas. " +
                    "Setelah itu tombol Home membuka launcher ini. Pengalih bisa dimatikan kapan saja."
            )
            .setPositiveButton("Buka Aksesibilitas") { _, _ ->
                simpanan.alihkanHome = true
                bukaAtauPesan(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /** Mengembalikan TV ke launcher bawaannya. */
    private fun kembalikanBawaan() {
        simpanan.alihkanHome = false
        if (buka(Intent(Settings.ACTION_HOME_SETTINGS))) {
            pesan("Pilih launcher bawaan TV sebagai aplikasi Beranda")
            return
        }
        @Suppress("DEPRECATION")
        packageManager.clearPackagePreferredActivities(packageName)
        val bawaan = Beranda.lain(this).firstOrNull()
        if (bawaan == null) {
            pesan("Launcher bawaan tidak ditemukan di TV ini")
            return
        }
        bukaAtauPesan(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setClassName(bawaan.packageName, bawaan.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
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
            pesan("Sumber input tidak ditemukan di TV ini")
            return
        }
        val nama = Array(masukan.size) { masukan[it].loadLabel(this).toString() }
        AlertDialog.Builder(this)
            .setTitle("Sumber input")
            .setItems(nama) { _, pilihan ->
                val tujuan = TvContract.buildChannelUriForPassthroughInput(masukan[pilihan].id)
                bukaAtauPesan(Intent(Intent.ACTION_VIEW, tujuan).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            .show()
    }

    // ---- Lokasi, jam, cuaca ----

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

    // ---- Aplikasi: baris beranda dan laci ----

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
                if (laciTerbuka) isiLaci()
            }
        }
    }

    private fun ubinAplikasi(aplikasi: Aplikasi, lebar: Float): Ubin {
        val ubin = Ubin(this, u, latar.tema.aksen, aplikasi.nama, lebar)
        if (aplikasi.banner != null) ubin.gambar.setImageDrawable(aplikasi.banner)
        else ubin.pakaiIkon(aplikasi.ikon, Color.parseColor("#1C302E"))
        ubin.setOnClickListener {
            Bunyi.pilih()
            bukaAtauPesan(aplikasi.buka)
        }
        return ubin
    }

    private fun pesanSematan(aplikasi: Aplikasi, hasil: HasilFavorit): String = when (hasil) {
        HasilFavorit.DITAMBAH -> aplikasi.nama + " disematkan di beranda"
        HasilFavorit.DIHAPUS -> aplikasi.nama + " dilepas dari beranda"
        HasilFavorit.PENUH -> "Beranda sudah penuh (" + Simpanan.MAKS_FAVORIT + " aplikasi). Lepas satu dulu."
    }

    private fun fokusBaris() {
        Bunyi.senyap = true
        baris.getChildAt(0)?.requestFocus()
        baris.post { Bunyi.senyap = false }
    }

    /** Baris beranda: aplikasi yang disematkan, lalu Semua aplikasi, Input, dan Pengaturan. */
    private fun isiBaris() {
        val tema = latar.tema
        val pilihan = PustakaAplikasi.untukBeranda(semua, simpanan)
        val kunci = tema.name + "|" + pilihan.joinToString(",") { it.paket }
        if (kunci == kunciBaris) return
        kunciBaris = kunci
        baris.removeAllViews()
        val jarak = px(8f)
        val ukuran = ViewGroup.LayoutParams.WRAP_CONTENT

        for (aplikasi in pilihan) {
            val ubin = ubinAplikasi(aplikasi, 208f)
            // Tekan lama OK: lepas dari beranda
            ubin.setOnLongClickListener {
                pesan(pesanSematan(aplikasi, simpanan.gantiFavorit(aplikasi.paket)))
                isiBaris()
                true
            }
            baris.addView(ubin, LinearLayout.LayoutParams(ukuran, ukuran).apply { rightMargin = jarak })
        }

        fun bawaan(nama: String, jenis: GambarBawaan.Jenis, latarWarna: String, terang: String, aksi: () -> Unit) {
            val ubin = Ubin(this, u, tema.aksen, nama)
            ubin.gambar.setImageDrawable(GambarBawaan(jenis, Color.parseColor(latarWarna), Color.parseColor(terang), tema.aksen))
            ubin.setOnClickListener {
                Bunyi.pilih()
                aksi()
            }
            baris.addView(ubin, LinearLayout.LayoutParams(ukuran, ukuran).apply { rightMargin = jarak })
        }
        bawaan("Semua aplikasi", GambarBawaan.Jenis.SEMUA, "#35566B", "#BFD9E6") { bukaLaci() }
        bawaan("Input", GambarBawaan.Jenis.INPUT, "#2F6A50", "#DCE7E4") { pilihInput() }
        bawaan("Pengaturan", GambarBawaan.Jenis.PENGATURAN, "#3E575B", "#DCE7E4") { menuLauncher() }

        if (!laciTerbuka) fokusBaris()
    }

    /** Kisi tujuh kolom yang mengisi lebar layar sampai tepi kanan. */
    private fun isiLaci() {
        val tema = latar.tema
        val kunci = tema.name + "|" + semua.joinToString(",") { it.paket }
        if (kunci == kunciLaci) {
            tandaiSematan()
            return
        }
        kunciLaci = kunci
        kisi.removeAllViews()
        val ukuran = ViewGroup.LayoutParams.WRAP_CONTENT
        var barisKisi: LinearLayout? = null
        for ((nomor, aplikasi) in semua.withIndex()) {
            if (nomor % KOLOM == 0) {
                barisKisi = LinearLayout(this)
                barisKisi.orientation = LinearLayout.HORIZONTAL
                barisKisi.clipChildren = false
                barisKisi.clipToPadding = false
                kisi.addView(barisKisi, LinearLayout.LayoutParams(ukuran, ukuran).apply { bottomMargin = px(4f) })
            }
            val ubin = ubinAplikasi(aplikasi, LEBAR_UBIN_LACI)
            ubin.tag = aplikasi.paket
            ubin.setOnLongClickListener {
                pesan(pesanSematan(aplikasi, simpanan.gantiFavorit(aplikasi.paket)))
                tandaiSematan()
                true
            }
            barisKisi?.addView(ubin, LinearLayout.LayoutParams(ukuran, ukuran).apply {
                rightMargin = if (nomor % KOLOM == KOLOM - 1) 0 else px(8f)
            })
        }
        tandaiSematan()
    }

    private fun tandaiSematan() {
        val favorit = simpanan.favorit.toHashSet()
        for (b in 0 until kisi.childCount) {
            val barisKisi = kisi.getChildAt(b) as? ViewGroup ?: continue
            for (k in 0 until barisKisi.childCount) {
                val ubin = barisKisi.getChildAt(k) as? Ubin ?: continue
                val paket = ubin.tag as? String ?: continue
                ubin.tandai(paket in favorit)
            }
        }
    }

    /** Laci bergeser naik dari bawah; beranda meredup dan baris aplikasinya memudar. */
    private fun bukaLaci() {
        if (laciTerbuka) return
        laciTerbuka = true
        isiLaci()
        laci.visibility = View.VISIBLE
        // selama laci terbuka, fokus tidak boleh pindah ke baris beranda di belakangnya
        gulirBaris.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        laci.animate().translationY(0f).setDuration(280).setInterpolator(DecelerateInterpolator()).withEndAction(null).start()
        redup.animate().alpha(1f).setDuration(280).start()
        gulirBaris.animate().alpha(0f).setDuration(200).start()
        petunjuk.animate().alpha(0f).setDuration(200).start()
        Bunyi.senyap = true
        (kisi.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.requestFocus()
        kisi.post { Bunyi.senyap = false }
    }

    /** Laci bergeser turun dan beranda tampil lagi, dengan sematan terbaru. */
    private fun tutupLaci() {
        if (!laciTerbuka) return
        laciTerbuka = false
        gulirBaris.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        laci.animate().translationY(tinggiLaci.toFloat()).setDuration(240).setInterpolator(AccelerateInterpolator())
            .withEndAction { if (!laciTerbuka) laci.visibility = View.INVISIBLE }.start()
        redup.animate().alpha(0f).setDuration(240).start()
        gulirBaris.animate().alpha(1f).setDuration(240).start()
        petunjuk.animate().alpha(0.85f).setDuration(240).start()
        isiBaris()
        fokusBaris()
    }

    companion object {
        private const val MINTA_BERANDA = 7
        private const val KOLOM = 7

        /** Lebar ubin laci pada layar 1920: tujuh kolom tepat mengisi dari tepi kiri sampai tepi kanan. */
        private const val LEBAR_UBIN_LACI = 229.7f

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
