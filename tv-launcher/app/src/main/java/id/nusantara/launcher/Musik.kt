package id.nusantara.launcher

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.audiofx.Visualizer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Android hanya mengizinkan aplikasi melihat musik yang diputar aplikasi lain bila pengguna
 * memberi "akses notifikasi". Layanan ini ada hanya untuk memegang izin itu; isi notifikasi
 * tidak dibaca dan tidak disimpan.
 */
class PendengarMedia : NotificationListenerService()

/** Meminta izin akses notifikasi, dengan penjelasan bila TV tidak menyediakan layarnya. */
object IzinMusik {

    fun ada(konteks: Context): Boolean {
        val pengelola = konteks.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return false
        return try {
            pengelola.getActiveSessions(ComponentName(konteks, PendengarMedia::class.java))
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun minta(layar: Activity) {
        val terbuka = try {
            layar.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            true
        } catch (e: Exception) {
            false
        }
        if (terbuka) return
        val ramKecil = (layar.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
        val perintah = "adb shell cmd notification allow_listener " +
            layar.packageName + "/" + PendengarMedia::class.java.name
        AlertDialog.Builder(layar)
            .setTitle("Layar izin tidak tersedia di TV ini")
            .setMessage(
                (if (ramKecil) "TV ini termasuk perangkat bermemori kecil, yang biasanya tidak mengizinkan akses notifikasi sama sekali.\n\n" else "") +
                    "Izin bisa diberikan dari komputer yang terhubung ke TV lewat adb:\n\n" + perintah
            )
            .setPositiveButton("Tutup", null)
            .show()
    }
}

/** Mengikuti musik yang sedang diputar aplikasi lain: judul, artis, sampul, posisi, dan tombolnya. */
class PemantauMusik(private val konteks: Context, private val saatBerubah: () -> Unit) {

    private val pengelola = konteks.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val pendengar = ComponentName(konteks, PendengarMedia::class.java)
    private val penangan = Handler(Looper.getMainLooper())
    private var pengendali: MediaController? = null
    private var keadaan: PlaybackState? = null
    private var terdaftar = false

    var judul = ""
        private set
    var artis = ""
        private set
    var aplikasi = ""
        private set
    var sampul: Bitmap? = null
        private set
    var durasi = 0L
        private set
    var memutar = false
        private set

    /** Benar bila ada lagu yang sedang diputar atau dijeda. */
    var ada = false
        private set

    private val saatSesiBerubah = MediaSessionManager.OnActiveSessionsChangedListener { daftar ->
        pilih(daftar ?: emptyList())
    }

    private val saatMedia = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = baca()
        override fun onMetadataChanged(metadata: MediaMetadata?) = baca()
        override fun onSessionDestroyed() = segarkan()
    }

    fun mulai() {
        if (!terdaftar) {
            try {
                pengelola?.addOnActiveSessionsChangedListener(saatSesiBerubah, pendengar, penangan)
                terdaftar = pengelola != null
            } catch (e: SecurityException) {
                // izin belum diberikan; dicoba lagi saat beranda tampil berikutnya
            }
        }
        segarkan()
    }

    fun berhenti() {
        if (terdaftar) {
            pengelola?.removeOnActiveSessionsChangedListener(saatSesiBerubah)
            terdaftar = false
        }
        lepas()
    }

    private fun segarkan() {
        val daftar = try {
            pengelola?.getActiveSessions(pendengar) ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
        pilih(daftar)
    }

    /** Sesi yang sedang berbunyi didahulukan; kalau tidak ada, sesi teratas. */
    private fun pilih(daftar: List<MediaController>) {
        val baru = daftar.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: daftar.firstOrNull()
        if (baru?.sessionToken != pengendali?.sessionToken) {
            lepas()
            pengendali = baru
            baru?.registerCallback(saatMedia, penangan)
        }
        baca()
    }

    private fun lepas() {
        pengendali?.unregisterCallback(saatMedia)
        pengendali = null
    }

    private fun baca() {
        val p = pengendali
        val meta = p?.metadata
        keadaan = p?.playbackState
        judul = meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        artis = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        sampul = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        durasi = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        aplikasi = try {
            if (p == null) "" else konteks.packageManager.getApplicationLabel(
                konteks.packageManager.getApplicationInfo(p.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            ""
        }
        val status = keadaan?.state
        memutar = status == PlaybackState.STATE_PLAYING || status == PlaybackState.STATE_BUFFERING
        ada = p != null && judul.isNotEmpty() && (memutar || status == PlaybackState.STATE_PAUSED)
        saatBerubah()
    }

    /** Posisi lagu sekarang dalam milidetik, dihitung maju sendiri selama lagu berjalan. */
    fun posisi(): Long {
        val k = keadaan ?: return 0L
        var pos = k.position
        if (k.state == PlaybackState.STATE_PLAYING) {
            pos += ((SystemClock.elapsedRealtime() - k.lastPositionUpdateTime) * k.playbackSpeed).toLong()
        }
        if (pos < 0L) pos = 0L
        return if (durasi > 0L && pos > durasi) durasi else pos
    }

    fun putarAtauJeda() {
        val kendali = pengendali?.transportControls ?: return
        if (memutar) kendali.pause() else kendali.play()
    }

    fun berikut() {
        pengendali?.transportControls?.skipToNext()
    }

    fun sebelum() {
        pengendali?.transportControls?.skipToPrevious()
    }

    /** Membuka aplikasi musiknya, di layar yang sedang memutar bila aplikasi menyediakannya. */
    fun bukaAplikasi() {
        val p = pengendali ?: return
        try {
            val layar = p.sessionActivity
            if (layar != null) {
                layar.send()
                return
            }
            val pm = konteks.packageManager
            val buka = pm.getLeanbackLaunchIntentForPackage(p.packageName) ?: pm.getLaunchIntentForPackage(p.packageName)
            if (buka != null) konteks.startActivity(buka.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // aplikasi menolak dibuka dari sini; tidak ada yang perlu dilakukan
        }
    }
}

/** Warna kartu dan latar yang diturunkan dari sampul album, seperti warna dinamis Material You. */
class SkemaMusik(val wadah: Int, val utama: Int, val diUtama: Int, val tirai: Int) {

    companion object {
        val netral = SkemaMusik(
            Color.argb(238, 28, 40, 42), Color.parseColor("#C3E88D"), Color.parseColor("#12211F"), Color.argb(150, 9, 18, 20)
        )

        fun dari(sampul: Bitmap?): SkemaMusik {
            if (sampul == null) return netral
            val kecil = Bitmap.createScaledBitmap(sampul, 16, 16, true)
            val hsv = FloatArray(3)
            var x = 0.0
            var y = 0.0
            var bobot = 0.0
            for (baris in 0 until 16) for (kolom in 0 until 16) {
                Color.colorToHSV(kecil.getPixel(kolom, baris), hsv)
                // warna yang jenuh dan terang paling menentukan rona
                val w = (hsv[1] * hsv[2]).toDouble()
                val sudut = hsv[0] * PI / 180.0
                x += cos(sudut) * w
                y += sin(sudut) * w
                bobot += w
            }
            // sampul hitam-putih atau nyaris tanpa warna: pakai warna netral
            if (bobot < 8.0) return netral
            var rona = (atan2(y, x) * 180.0 / PI).toFloat()
            if (rona < 0f) rona += 360f
            return SkemaMusik(
                wadah = Color.HSVToColor(238, floatArrayOf(rona, 0.42f, 0.22f)),
                utama = Color.HSVToColor(floatArrayOf(rona, 0.34f, 0.96f)),
                diUtama = Color.HSVToColor(floatArrayOf(rona, 0.55f, 0.16f)),
                tirai = Color.HSVToColor(150, floatArrayOf(rona, 0.5f, 0.10f)),
            )
        }

        /** Sampul diperkecil lalu diperbesar lagi: buram halus tanpa perlu fitur Android terbaru. */
        fun buram(sampul: Bitmap): Bitmap {
            val kecil = Bitmap.createScaledBitmap(sampul, 20, 20, true)
            return Bitmap.createScaledBitmap(kecil, 160, 160, true)
        }
    }
}

/**
 * Membaca kekuatan suara yang sedang keluar dari TV, per pita frekuensi (rendah ke tinggi).
 * Android mewajibkan izin "rekam audio" untuk ini walau mikrofon tidak dipakai;
 * datanya hanya dipakai untuk menggerakkan garis gelombang dan tidak disimpan.
 */
class PenganalisisSuara {

    /** Kekuatan tiap pita, 0 sampai 1, sudah dihaluskan. */
    val pita = FloatArray(24)

    private var visualizer: Visualizer? = null

    val berjalan: Boolean
        get() = visualizer != null

    fun mulai(): Boolean {
        if (visualizer != null) return true
        return try {
            val v = Visualizer(0)
            val rentang = Visualizer.getCaptureSizeRange()
            v.captureSize = 512.coerceIn(rentang[0], rentang[1])
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(sumber: Visualizer?, data: ByteArray?, laju: Int) {}

                override fun onFftDataCapture(sumber: Visualizer?, fft: ByteArray?, laju: Int) {
                    if (fft != null) olah(fft)
                }
            }, Visualizer.getMaxCaptureRate(), false, true)
            v.enabled = true
            visualizer = v
            true
        } catch (e: Throwable) {
            // TV tidak mengizinkan atau tidak mendukung pembacaan suara keluaran
            false
        }
    }

    fun berhenti() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Throwable) {
            // sudah dilepas
        }
        visualizer = null
        pita.fill(0f)
    }

    private fun olah(fft: ByteArray) {
        val bin = fft.size / 2
        if (bin < 8) return
        for (i in pita.indices) {
            // pita dibagi secara logaritmik: nada rendah mendapat pita sempit, nada tinggi lebar
            val awal = (bin - 1).toDouble().pow(i / pita.size.toDouble()).toInt().coerceIn(1, bin - 1)
            val akhir = (bin - 1).toDouble().pow((i + 1) / pita.size.toDouble()).toInt().coerceIn(awal + 1, bin)
            var puncak = 0f
            for (k in awal until akhir) {
                val nyata = fft[2 * k].toFloat()
                val khayal = fft[2 * k + 1].toFloat()
                val besar = sqrt(nyata * nyata + khayal * khayal)
                if (besar > puncak) puncak = besar
            }
            val nilai = sqrt((puncak / 90f).coerceIn(0f, 1f))
            // naik cepat, turun pelan, supaya gerakannya tidak berkedip
            pita[i] += (nilai - pita[i]) * (if (nilai > pita[i]) 0.7f else 0.22f)
        }
    }
}

/**
 * Garis progres bergelombang: bagian yang sudah diputar bergelombang, sisanya lurus.
 * Tinggi gelombang mengikuti suara bila [pita] diisi; tanpa itu gelombang tetap
 * naik-turun dengan pola buatan supaya tidak rata.
 */
class GelombangView(context: Context) : View(context) {

    var kemajuan = 0f
        set(nilai) {
            field = nilai.coerceIn(0f, 1f)
            invalidate()
        }
    var memutar = false
        set(nilai) {
            field = nilai
            invalidate()
        }
    var warna = Color.WHITE

    /** Kekuatan tiap pita frekuensi dari PenganalisisSuara; null berarti tidak ada data suara. */
    var pita: FloatArray? = null

    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val jalur = Path()

    /** Tinggi gelombang di titik x: nada rendah di kiri, nada tinggi di kanan. */
    private fun tinggi(x: Float, lebar: Float, h: Float, t: Float): Float {
        val p = pita
        if (p != null && p.size >= 2) {
            val posisi = (x / lebar).coerceIn(0f, 1f) * (p.size - 1)
            val i = posisi.toInt().coerceAtMost(p.size - 2)
            val sisa = posisi - i
            val kuat = p[i] * (1f - sisa) + p[i + 1] * sisa
            return h * (0.04f + 0.4f * kuat)
        }
        return h * (0.1f + 0.2f * (0.5f + 0.5f * sin(x / 170f + t * 1.7f)) * (0.55f + 0.45f * sin(x / 61f - t * 2.3f)))
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val tebal = h * 0.11f
        val tepi = tebal
        val lebar = w - tepi * 2f
        val tengah = h / 2f
        val batas = tepi + lebar * kemajuan
        val panjang = h * 1.1f
        val t = (SystemClock.uptimeMillis() % 600_000L) / 1000f
        val fase = if (memutar) t * 4.5f else 0f

        kuas.strokeWidth = tebal
        kuas.color = warna
        kuas.alpha = 70
        canvas.drawLine(batas, tengah, w - tepi, tengah, kuas)

        kuas.alpha = 255
        jalur.reset()
        var x = tepi
        var pertama = true
        while (true) {
            val amplitudo = if (memutar) tinggi(x - tepi, lebar, h, t) else 0f
            val y = tengah + amplitudo * sin(x / panjang * 2f * PI.toFloat() - fase)
            if (pertama) jalur.moveTo(x, y) else jalur.lineTo(x, y)
            pertama = false
            if (x >= batas) break
            x = if (x + 3f > batas) batas else x + 3f
        }
        canvas.drawPath(jalur, kuas)

        // penanda posisi
        kuas.strokeWidth = tebal * 1.3f
        canvas.drawLine(batas, h * 0.22f, batas, h * 0.78f, kuas)

        if (memutar) postInvalidateDelayed(33)
    }
}

/** Tombol putar, jeda, sebelumnya, dan berikutnya; bentuknya berubah antara bulat dan kotak membulat. */
class TombolMedia(context: Context, private val u: Float) : View(context) {

    enum class Jenis { SEBELUM, PUTAR, JEDA, BERIKUT }

    var jenis = Jenis.PUTAR
        set(nilai) {
            field = nilai
            invalidate()
        }
    var warnaLatar = 0
    var warnaIkon = Color.WHITE
    var warnaFokus = Color.WHITE

    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ikon = Path()

    init {
        isFocusable = true
        isSoundEffectsEnabled = false
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) Bunyi.pindah()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cincin = 4f * u
        val sela = cincin * 2f
        // saat lagu berjalan tombol utama menjadi kotak membulat, saat dijeda kembali bulat
        val sudut = if (jenis == Jenis.JEDA) (h - sela * 2f) * 0.3f else (h - sela * 2f) / 2f

        if (warnaLatar != 0) {
            kuas.style = Paint.Style.FILL
            kuas.color = warnaLatar
            canvas.drawRoundRect(sela, sela, w - sela, h - sela, sudut, sudut, kuas)
        }
        if (isFocused) {
            kuas.style = Paint.Style.STROKE
            kuas.strokeWidth = cincin
            kuas.color = warnaFokus
            canvas.drawRoundRect(cincin / 2f, cincin / 2f, w - cincin / 2f, h - cincin / 2f, sudut + sela, sudut + sela, kuas)
        }

        val sisi = (if (w < h) w else h) * 0.46f
        canvas.save()
        canvas.translate((w - sisi) / 2f, (h - sisi) / 2f)
        canvas.scale(sisi / 24f, sisi / 24f)
        kuas.style = Paint.Style.FILL
        kuas.color = warnaIkon
        ikon.reset()
        when (jenis) {
            Jenis.PUTAR -> {
                ikon.moveTo(7f, 4f)
                ikon.lineTo(20f, 12f)
                ikon.lineTo(7f, 20f)
                ikon.close()
            }
            Jenis.JEDA -> {
                ikon.addRoundRect(5f, 4f, 10f, 20f, 1.5f, 1.5f, Path.Direction.CW)
                ikon.addRoundRect(14f, 4f, 19f, 20f, 1.5f, 1.5f, Path.Direction.CW)
            }
            Jenis.BERIKUT -> {
                ikon.moveTo(5f, 5f)
                ikon.lineTo(15f, 12f)
                ikon.lineTo(5f, 19f)
                ikon.close()
                ikon.addRect(16.5f, 5f, 19.5f, 19f, Path.Direction.CW)
            }
            Jenis.SEBELUM -> {
                ikon.moveTo(19f, 5f)
                ikon.lineTo(9f, 12f)
                ikon.lineTo(19f, 19f)
                ikon.close()
                ikon.addRect(4.5f, 5f, 7.5f, 19f, Path.Direction.CW)
            }
        }
        canvas.drawPath(ikon, kuas)
        canvas.restore()
    }
}

/** Kartu "sedang diputar" di beranda: sampul, judul, artis, progres bergelombang, dan tombol. */
class KartuMusik(context: Context, private val u: Float, private val pemantau: PemantauMusik) : LinearLayout(context) {

    private val wadah = GradientDrawable()
    private val gambar = ImageView(context)
    private val keterangan = TextView(context)
    private val judul = TextView(context)
    private val artis = TextView(context)
    private val waktu = TextView(context)
    private val gelombang = GelombangView(context)
    private val sebelum = TombolMedia(context, u)
    private val putar = TombolMedia(context, u)
    private val berikut = TombolMedia(context, u)
    private var sampulTerakhir: Bitmap? = null

    var skema: SkemaMusik = SkemaMusik.netral
        private set

    private fun px(desain: Float) = (desain * u).toInt()

    private fun atur(t: TextView, ukuran: Float, tebal: Boolean) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, ukuran * u)
        t.typeface = if (tebal) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.includeFontPadding = false
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        wadah.cornerRadius = 56f * u
        background = wadah
        setPadding(px(28f), px(28f), px(36f), px(28f))

        // Sampul album; OK di sini membuka aplikasi musiknya
        gambar.scaleType = ImageView.ScaleType.CENTER_CROP
        gambar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 32f * u)
            }
        }
        gambar.clipToOutline = true
        gambar.isFocusable = true
        gambar.isSoundEffectsEnabled = false
        gambar.contentDescription = "Buka aplikasi musik"
        gambar.setOnFocusChangeListener { _, fokus ->
            gambar.alpha = if (fokus) 1f else 0.92f
            gambar.scaleX = if (fokus) 1.05f else 1f
            gambar.scaleY = if (fokus) 1.05f else 1f
            if (fokus) Bunyi.pindah()
        }
        gambar.setOnClickListener {
            Bunyi.pilih()
            pemantau.bukaAplikasi()
        }
        addView(gambar, LayoutParams(px(208f), px(208f)))

        val kanan = LinearLayout(context)
        kanan.orientation = VERTICAL
        atur(keterangan, 20f, true)
        atur(judul, 38f, true)
        atur(artis, 26f, false)
        atur(waktu, 20f, false)
        judul.setTextColor(Color.parseColor("#F4F6F4"))
        artis.setTextColor(Color.parseColor("#D5DBD8"))
        waktu.setTextColor(Color.parseColor("#D5DBD8"))
        val penuh = ViewGroup.LayoutParams.MATCH_PARENT
        val isi = ViewGroup.LayoutParams.WRAP_CONTENT
        kanan.addView(keterangan, LayoutParams(penuh, isi))
        kanan.addView(judul, LayoutParams(penuh, isi).apply { topMargin = px(8f) })
        kanan.addView(artis, LayoutParams(penuh, isi).apply { topMargin = px(4f) })
        kanan.addView(gelombang, LayoutParams(penuh, px(52f)).apply { topMargin = px(6f) })

        val bawah = LinearLayout(context)
        bawah.orientation = HORIZONTAL
        bawah.gravity = Gravity.CENTER_VERTICAL
        sebelum.jenis = TombolMedia.Jenis.SEBELUM
        berikut.jenis = TombolMedia.Jenis.BERIKUT
        sebelum.contentDescription = "Lagu sebelumnya"
        putar.contentDescription = "Putar atau jeda"
        berikut.contentDescription = "Lagu berikutnya"
        sebelum.setOnClickListener {
            Bunyi.pilih()
            pemantau.sebelum()
        }
        putar.setOnClickListener {
            Bunyi.pilih()
            pemantau.putarAtauJeda()
        }
        berikut.setOnClickListener {
            Bunyi.pilih()
            pemantau.berikut()
        }
        bawah.addView(sebelum, LayoutParams(px(64f), px(64f)))
        bawah.addView(putar, LayoutParams(px(84f), px(84f)).apply {
            leftMargin = px(6f)
            rightMargin = px(6f)
        })
        bawah.addView(berikut, LayoutParams(px(64f), px(64f)))
        bawah.addView(waktu, LayoutParams(0, isi, 1f))
        waktu.gravity = Gravity.END
        kanan.addView(bawah, LayoutParams(penuh, isi).apply { topMargin = px(4f) })

        addView(kanan, LayoutParams(0, isi, 1f).apply { leftMargin = px(28f) })
    }

    /** Data suara untuk garis gelombang; null bila tidak tersedia. */
    fun pitaSuara(pita: FloatArray?) {
        gelombang.pita = pita
    }

    /** Dipanggil saat lagu, sampul, atau status putar berubah. */
    fun perbarui() {
        if (pemantau.sampul !== sampulTerakhir || sampulTerakhir == null) {
            sampulTerakhir = pemantau.sampul
            skema = SkemaMusik.dari(pemantau.sampul)
            wadah.setColor(skema.wadah)
            if (pemantau.sampul != null) {
                gambar.setImageBitmap(pemantau.sampul)
            } else {
                gambar.setImageDrawable(null)
                gambar.setBackgroundColor(skema.diUtama)
            }
            keterangan.setTextColor(skema.utama)
            gelombang.warna = skema.utama
            putar.warnaLatar = skema.utama
            putar.warnaIkon = skema.diUtama
            for (t in listOf(sebelum, putar, berikut)) t.warnaFokus = Color.WHITE
            sebelum.warnaIkon = skema.utama
            berikut.warnaIkon = skema.utama
            sebelum.invalidate()
            berikut.invalidate()
        }
        keterangan.text = if (pemantau.aplikasi.isEmpty()) "Sedang diputar" else "Sedang diputar di " + pemantau.aplikasi
        judul.text = pemantau.judul
        artis.text = pemantau.artis
        putar.jenis = if (pemantau.memutar) TombolMedia.Jenis.JEDA else TombolMedia.Jenis.PUTAR
        gelombang.memutar = pemantau.memutar
        detak()
    }

    private fun menit(milidetik: Long): String {
        val detik = milidetik / 1000L
        return String.format(Locale.US, "%d.%02d", detik / 60L, detik % 60L)
    }

    /** Dipanggil tiap detik: memajukan garis progres dan angka waktu. */
    fun detak() {
        val posisi = pemantau.posisi()
        val durasi = pemantau.durasi
        gelombang.kemajuan = if (durasi > 0L) posisi.toFloat() / durasi.toFloat() else 0f
        waktu.text = if (durasi > 0L) menit(posisi) + " / " + menit(durasi) else ""
    }
}
