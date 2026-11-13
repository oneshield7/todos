package id.nusantara.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Latar beranda: langit menurut waktu dan cuaca, lanskap menurut daerah,
 * lalu awan, hujan, kabut, dan kilat yang bergerak di atasnya.
 */
class LatarView(context: Context) : View(context) {

    var tema: Tema = Tema.KOTA
        set(nilai) {
            if (field != nilai) {
                field = nilai
                lanskap?.recycle()
                lanskap = null
                invalidate()
            }
        }

    var kondisi: Kondisi = Kondisi.CERAH_BERAWAN
        set(nilai) {
            field = nilai
            usang = true
            invalidate()
        }

    var waktu: Waktu = Waktu.SIANG
        set(nilai) {
            field = nilai
            usang = true
            invalidate()
        }

    /** Animasi cuaca; kalau dimatikan latar digambar sebagai gambar diam. */
    var animasi = true

    /** Hanya menggambar ulang selama beranda terlihat. */
    var aktif = false
        set(nilai) {
            field = nilai
            if (nilai) invalidate()
        }

    /** Benar bila langit terang, sehingga teks di atasnya perlu berwarna gelap. */
    val latarTerang: Boolean
        get() = (waktu == Waktu.PAGI || waktu == Waktu.SIANG) &&
            (kondisi == Kondisi.CERAH || kondisi == Kondisi.CERAH_BERAWAN || kondisi == Kondisi.KABUT)

    private val mulai = SystemClock.uptimeMillis()
    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG)
    private val kuasLanskap = Paint(Paint.FILTER_BITMAP_FLAG)
    private val kuasHujan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(95, 235, 242, 245)
    }
    private val oval = RectF()

    private var usang = true
    private var langit: Shader? = null
    private var lanskap: Bitmap? = null
    private var tiraiAtas: Shader? = null
    private var tiraiBawah: Shader? = null

    private val acak = Random(11)
    private val bintang = FloatArray(90 * 3) { i ->
        when (i % 3) {
            0 -> acak.nextFloat() * 1920f
            1 -> acak.nextFloat() * 540f
            else -> 1f + acak.nextFloat() * 1.4f
        }
    }
    private val jumlahHujan = 260
    private val hujanX = FloatArray(jumlahHujan) { acak.nextFloat() * 2300f }
    private val hujanY = FloatArray(jumlahHujan) { acak.nextFloat() * 1230f }
    private val hujanPanjang = FloatArray(jumlahHujan) { 34f + acak.nextFloat() * 26f }
    private val hujanLaju = FloatArray(jumlahHujan) { 700f + acak.nextFloat() * 300f }
    private val titik = FloatArray(jumlahHujan * 4)

    // cx, cy, rx, ry, laju
    private val awan = arrayOf(
        floatArrayOf(300f, 90f, 320f, 70f, 9f),
        floatArrayOf(1100f, 60f, 420f, 86f, 6f),
        floatArrayOf(1750f, 120f, 300f, 64f, 11f),
        floatArrayOf(700f, 200f, 260f, 50f, 14f),
        floatArrayOf(1450f, 230f, 340f, 56f, 8f),
        floatArrayOf(40f, 250f, 280f, 52f, 12f),
        floatArrayOf(980f, 150f, 520f, 100f, 5f),
        floatArrayOf(1900f, 40f, 460f, 96f, 7f),
    )

    private val bulan: Path = Path().apply {
        val penuh = Path().apply { addCircle(1180f, 200f, 46f, Path.Direction.CW) }
        val potong = Path().apply { addCircle(1202f, 184f, 42f, Path.Direction.CW) }
        op(penuh, potong, Path.Op.DIFFERENCE)
    }

    private fun warna(vararg hex: String) = IntArray(hex.size) { Color.parseColor(hex[it]) }

    private fun warnaLangit(): IntArray = when (kondisi) {
        Kondisi.CERAH, Kondisi.CERAH_BERAWAN -> when (waktu) {
            Waktu.PAGI -> warna("#3FA7D6", "#9AD6EC", "#FFF0C9")
            Waktu.SIANG -> warna("#4F9FD8", "#A9D5EE", "#E4F1EE")
            Waktu.SENJA -> warna("#1B1846", "#B8455A", "#FFC06A")
            Waktu.MALAM -> warna("#070B24", "#141B47", "#3B2E63")
        }
        Kondisi.KABUT ->
            if (waktu == Waktu.MALAM || waktu == Waktu.SENJA) warna("#2E3840", "#4C575E", "#6F787C")
            else warna("#8FA3B0", "#C7C9C6", "#F2D9BD")
        Kondisi.PETIR ->
            if (waktu == Waktu.MALAM) warna("#0A0C12", "#1A1D26", "#2E3138")
            else warna("#1F2229", "#3A3F4A", "#6E727B")
        else -> when (waktu) {
            Waktu.MALAM -> warna("#0B1026", "#1A2146", "#2E2F4A")
            Waktu.SENJA -> warna("#16242B", "#2C4148", "#51686B")
            else -> warna("#56666F", "#8794A0", "#B9C2C6")
        }
    }

    /** Warna yang ditimpakan ke lanskap supaya ikut waktu dan cuaca. */
    private fun warnaTimpa(): Int = when {
        kondisi == Kondisi.PETIR ->
            if (waktu == Waktu.MALAM) Color.argb(190, 6, 8, 14) else Color.argb(140, 14, 16, 22)
        waktu == Waktu.MALAM -> Color.argb(165, 8, 12, 40)
        kondisi == Kondisi.KABUT ->
            if (waktu == Waktu.SENJA) Color.argb(120, 60, 70, 76) else Color.argb(80, 205, 205, 200)
        waktu == Waktu.SENJA ->
            if (kondisi.hujan || kondisi == Kondisi.BERAWAN) Color.argb(120, 14, 26, 30)
            else Color.argb(110, 50, 16, 46)
        kondisi.hujan || kondisi == Kondisi.BERAWAN -> Color.argb(80, 30, 40, 48)
        else -> 0
    }

    private fun siapkan() {
        if (!usang && langit != null) return
        langit = LinearGradient(
            0f, 0f, 0f, 700f, warnaLangit(), floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        val timpa = warnaTimpa()
        kuasLanskap.colorFilter = if (timpa != 0) PorterDuffColorFilter(timpa, PorterDuff.Mode.SRC_ATOP) else null
        usang = false
    }

    override fun onSizeChanged(w: Int, h: Int, lamaW: Int, lamaH: Int) {
        super.onSizeChanged(w, h, lamaW, lamaH)
        lanskap?.recycle()
        lanskap = null
        tiraiAtas = LinearGradient(
            0f, 0f, 0f, h * 0.28f, Color.argb(150, 9, 18, 20), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        tiraiBawah = LinearGradient(
            0f, h * 0.62f, 0f, h.toFloat(), Color.TRANSPARENT, Color.argb(235, 9, 14, 16), Shader.TileMode.CLAMP
        )
    }

    private fun buatLanskap(w: Int, h: Int, skala: Float, dx: Float, dy: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val kanvas = Canvas(bitmap)
        kanvas.translate(dx, dy)
        kanvas.scale(skala, skala)
        val k = Paint(Paint.ANTI_ALIAS_FLAG)
        k.strokeCap = Paint.Cap.ROUND
        k.strokeJoin = Paint.Join.ROUND
        for (b in Lanskap.bentuk(tema)) {
            k.color = b.warna
            if (b.garis > 0f) {
                k.style = Paint.Style.STROKE
                k.strokeWidth = b.garis
            } else {
                k.style = Paint.Style.FILL
            }
            kanvas.drawPath(b.jalur, k)
        }
        return bitmap
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = if (animasi) (SystemClock.uptimeMillis() - mulai) / 1000.0 else 0.0
        val skala = max(w / 1920f, h / 1080f)
        val dx = (w - 1920f * skala) / 2f
        val dy = (h - 1080f * skala) / 2f
        siapkan()

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(skala, skala)
        kuas.style = Paint.Style.FILL
        kuas.color = Color.BLACK
        kuas.shader = langit
        canvas.drawRect(0f, 0f, 1920f, 1080f, kuas)
        kuas.shader = null
        gambarBendaLangit(canvas, t)
        gambarAwan(canvas, t)
        if (kondisi == Kondisi.PETIR && animasi) gambarKilat(canvas, t)
        canvas.restore()

        val gambar = lanskap ?: buatLanskap(width, height, skala, dx, dy).also { lanskap = it }
        canvas.drawBitmap(gambar, 0f, 0f, kuasLanskap)

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(skala, skala)
        if (kondisi == Kondisi.KABUT) gambarKabut(canvas, t)
        if (kondisi.hujan) gambarHujan(canvas, t)
        canvas.restore()

        kuas.color = Color.BLACK
        if (!latarTerang) {
            kuas.shader = tiraiAtas
            canvas.drawRect(0f, 0f, w, h * 0.28f, kuas)
        }
        kuas.shader = tiraiBawah
        canvas.drawRect(0f, h * 0.62f, w, h, kuas)
        kuas.shader = null

        if (animasi && aktif) postInvalidateDelayed(33)
    }

    private fun gambarBendaLangit(canvas: Canvas, t: Double) {
        if (kondisi != Kondisi.CERAH && kondisi != Kondisi.CERAH_BERAWAN) return
        when (waktu) {
            Waktu.PAGI -> matahari(canvas, 840f, 210f, 64f, "#FFD45C", "#FFF3C4")
            Waktu.SIANG -> matahari(canvas, 960f, 150f, 56f, "#FFE38A", "#FFF6D6")
            Waktu.SENJA -> matahari(canvas, 860f, 640f, 80f, "#FFE9A8", "#FFD08A")
            Waktu.MALAM -> {
                kuas.color = Color.WHITE
                for (i in 0 until 90) {
                    val kedip = 0.6 + 0.4 * sin(t * 1.3 + i * 1.7)
                    kuas.alpha = (kedip * 230).toInt()
                    canvas.drawCircle(bintang[i * 3], bintang[i * 3 + 1], bintang[i * 3 + 2], kuas)
                }
                kuas.alpha = 255
                kuas.color = Color.parseColor("#F6E7B8")
                canvas.drawPath(bulan, kuas)
            }
        }
    }

    private fun matahari(canvas: Canvas, x: Float, y: Float, r: Float, inti: String, pendar: String) {
        kuas.color = Color.parseColor(pendar)
        kuas.alpha = 110
        canvas.drawCircle(x, y, r * 1.9f, kuas)
        kuas.alpha = 255
        kuas.color = Color.parseColor(inti)
        canvas.drawCircle(x, y, r, kuas)
    }

    private fun gambarAwan(canvas: Canvas, t: Double) {
        val jumlah = when (kondisi) {
            Kondisi.CERAH -> 0
            Kondisi.CERAH_BERAWAN -> 3
            Kondisi.KABUT -> 2
            Kondisi.BERAWAN, Kondisi.HUJAN_RINGAN -> 6
            Kondisi.HUJAN_LEBAT, Kondisi.PETIR -> 8
        }
        if (jumlah == 0) return
        kuas.color = when {
            kondisi == Kondisi.PETIR -> Color.argb(225, 21, 24, 30)
            kondisi == Kondisi.CERAH_BERAWAN || kondisi == Kondisi.KABUT -> when (waktu) {
                Waktu.MALAM -> Color.argb(200, 42, 49, 88)
                Waktu.SENJA -> Color.argb(150, 122, 53, 96)
                else -> Color.argb(235, 255, 255, 255)
            }
            else -> when (waktu) {
                Waktu.MALAM -> Color.argb(220, 28, 34, 64)
                Waktu.SENJA -> Color.argb(190, 15, 27, 33)
                else -> Color.argb(230, 108, 119, 128)
            }
        }
        for (i in 0 until jumlah) {
            val a = awan[i]
            val keliling = 1920.0 + a[2] * 2.0 + 200.0
            val x = ((a[0] + a[2] + a[4] * t) % keliling - a[2]).toFloat()
            oval.set(x - a[2], a[1] - a[3], x + a[2], a[1] + a[3])
            canvas.drawOval(oval, kuas)
        }
    }

    /** Kilat lembut tiap sekitar tujuh detik: naik cepat lalu padam pelan. */
    private fun gambarKilat(canvas: Canvas, t: Double) {
        val fase = t % 7.0
        val kuat = if (fase < 0.08) fase / 0.08 else exp(-(fase - 0.08) / 0.3)
        val alfa = (kuat * 90).toInt()
        if (alfa <= 2) return
        kuas.color = Color.argb(alfa, 255, 255, 240)
        canvas.drawRect(0f, 0f, 1920f, 1080f, kuas)
    }

    private fun gambarKabut(canvas: Canvas, t: Double) {
        kuas.color = Color.argb(46, 240, 238, 230)
        canvas.drawRect(0f, 0f, 1920f, 1080f, kuas)
        kuas.color = Color.argb(50, 244, 238, 230)
        for (i in 0 until 3) {
            val x = ((i * 700.0 + t * (12.0 + 6.0 * i)) % 3300.0 - 1400.0).toFloat()
            val y = 560f + i * 50f
            oval.set(x, y, x + 1400f, y + 90f)
            canvas.drawRoundRect(oval, 45f, 45f, kuas)
        }
    }

    private fun gambarHujan(canvas: Canvas, t: Double) {
        val jumlah = when (kondisi) {
            Kondisi.HUJAN_RINGAN -> 110
            Kondisi.HUJAN_LEBAT -> 210
            else -> jumlahHujan
        }
        val miring = if (kondisi == Kondisi.HUJAN_RINGAN) 0.25f else 0.42f
        val cepat = if (kondisi == Kondisi.HUJAN_RINGAN) 1.0 else 1.5
        for (i in 0 until jumlah) {
            val y = ((hujanY[i] + hujanLaju[i] * cepat * t) % 1230.0).toFloat() - 80f
            var x = (hujanX[i] - y * miring) % 2300f
            if (x < 0f) x += 2300f
            x -= 150f
            val j = i * 4
            titik[j] = x
            titik[j + 1] = y
            titik[j + 2] = x - hujanPanjang[i] * miring
            titik[j + 3] = y + hujanPanjang[i]
        }
        canvas.drawLines(titik, 0, jumlah * 4, kuasHujan)
    }
}
