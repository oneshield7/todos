package id.nusantara.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.PathParser

/**
 * Satu ubin di baris aplikasi. Gambar memenuhi ubin; nama hanya muncul saat ubin dipilih.
 * [u] adalah skala dari ukuran desain 1920 px ke lebar layar sebenarnya.
 */
class Ubin(context: Context, private val u: Float, aksen: Int, nama: String) : LinearLayout(context) {

    val gambar = ImageView(context)
    private val bingkai = FrameLayout(context)
    private val label = TextView(context)
    private val cincin = GradientDrawable()
    private val redup = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0.7f) })

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        isFocusable = true
        clipChildren = false
        clipToPadding = false
        contentDescription = nama

        val lebar = (208f * u).toInt()
        val tinggi = (117f * u).toInt()
        val tepi = (6f * u).toInt()

        cincin.setColor(Color.TRANSPARENT)
        cincin.cornerRadius = 21f * u
        cincin.setStroke((4f * u).toInt().coerceAtLeast(2), aksen)

        gambar.scaleType = ImageView.ScaleType.CENTER_CROP
        gambar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 16f * u)
            }
        }
        gambar.clipToOutline = true
        bingkai.addView(
            gambar,
            FrameLayout.LayoutParams(lebar, tinggi).apply { setMargins(tepi, tepi, tepi, tepi) }
        )
        addView(bingkai, LayoutParams(lebar + tepi * 2, tinggi + tepi * 2))

        label.text = nama
        label.setTextColor(Color.parseColor("#F2F6F3"))
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * u)
        label.typeface = Typeface.DEFAULT_BOLD
        label.gravity = Gravity.CENTER
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        addView(
            label,
            LayoutParams(lebar + tepi * 2, LayoutParams.WRAP_CONTENT).apply { topMargin = (10f * u).toInt() }
        )
        terapkan(false)
    }

    /** Aplikasi tanpa banner: ikonnya diletakkan utuh di atas ubin berwarna. */
    fun pakaiIkon(ikon: Drawable?, latar: Int) {
        val ruang = (20f * u).toInt()
        gambar.scaleType = ImageView.ScaleType.FIT_CENTER
        gambar.setPadding(ruang, ruang, ruang, ruang)
        gambar.setBackgroundColor(latar)
        gambar.setImageDrawable(ikon)
    }

    private fun terapkan(dipilih: Boolean) {
        bingkai.background = if (dipilih) cincin else null
        label.visibility = if (dipilih) View.VISIBLE else View.INVISIBLE
        gambar.alpha = if (dipilih) 1f else 0.82f
        gambar.colorFilter = if (dipilih) null else redup
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        terapkan(gainFocus)
        val skala = if (gainFocus) 1.07f else 1f
        bingkai.animate().scaleX(skala).scaleY(skala).setDuration(140).start()
    }
}

/** Gambar ubin untuk fungsi bawaan launcher, memenuhi ubin seperti logo aplikasi. */
class GambarBawaan(
    private val jenis: Jenis,
    private val latar: Int,
    private val terang: Int,
    private val aksen: Int,
) : Drawable() {

    enum class Jenis { LOKASI, PENGATURAN }

    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(b.width() / 208f, b.height() / 117f)
        kuas.style = Paint.Style.FILL
        kuas.color = latar
        canvas.drawRect(0f, 0f, 208f, 117f, kuas)
        when (jenis) {
            Jenis.PENGATURAN -> {
                kuas.color = terang
                kuas.style = Paint.Style.STROKE
                kuas.strokeWidth = 12f
                kuas.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(28f, 40f, 180f, 40f, kuas)
                canvas.drawLine(28f, 78f, 180f, 78f, kuas)
                kuas.style = Paint.Style.FILL
                kuas.color = latar
                canvas.drawCircle(132f, 40f, 21f, kuas)
                canvas.drawCircle(76f, 78f, 21f, kuas)
                kuas.color = aksen
                canvas.drawCircle(132f, 40f, 15f, kuas)
                canvas.drawCircle(76f, 78f, 15f, kuas)
            }
            Jenis.LOKASI -> {
                kuas.color = terang
                kuas.style = Paint.Style.STROKE
                kuas.strokeWidth = 6f
                canvas.drawOval(54f, 84f, 154f, 108f, kuas)
                kuas.style = Paint.Style.FILL
                kuas.color = aksen
                canvas.drawCircle(104f, 44f, 30f, kuas)
                val ujung = Path()
                ujung.moveTo(78f, 59f)
                ujung.lineTo(130f, 59f)
                ujung.lineTo(104f, 98f)
                ujung.close()
                canvas.drawPath(ujung, kuas)
                kuas.color = latar
                canvas.drawCircle(104f, 44f, 12f, kuas)
            }
        }
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        kuas.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        kuas.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/** Ikon cuaca bergaris di samping suhu. */
class IkonCuaca(context: Context) : View(context) {

    private val kuas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var jalur: Path = buat(Kondisi.CERAH_BERAWAN, true)

    var warna: Int = Color.WHITE
        set(nilai) {
            field = nilai
            invalidate()
        }

    fun setel(kondisi: Kondisi, siang: Boolean) {
        jalur = buat(kondisi, siang)
        invalidate()
    }

    private fun buat(kondisi: Kondisi, siang: Boolean): Path {
        val awanRendah = "M7 18 h10 a4 4 0 0 0 0.6 -7.96 A6 6 0 0 0 6 9.5 A4.25 4.25 0 0 0 7 18 Z"
        val awanTinggi = "M7 15 h10 a4 4 0 0 0 0.6 -7.96 A6 6 0 0 0 6 6.5 A4.25 4.25 0 0 0 7 15 Z"
        val matahari = "M8 12 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 Z M12 2 v2 M12 20 v2 M2 12 h2 M20 12 h2 " +
            "M4.9 4.9 l1.4 1.4 M17.7 17.7 l1.4 1.4 M4.9 19.1 l1.4 -1.4 M17.7 6.3 l1.4 -1.4"
        val bulan = "M21 12.79 A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79 Z"
        val d = when (kondisi) {
            Kondisi.CERAH -> if (siang) matahari else bulan
            Kondisi.CERAH_BERAWAN, Kondisi.BERAWAN -> awanRendah
            Kondisi.KABUT -> "M7 13 h10 a4 4 0 0 0 0.6 -7.96 A6 6 0 0 0 6 4.5 A4.25 4.25 0 0 0 7 13 Z M5 17 h14 M8 21 h8"
            Kondisi.HUJAN_RINGAN, Kondisi.HUJAN_LEBAT -> "$awanTinggi M8 18 l-1 3 M12 18 l-1 3 M16 18 l-1 3"
            Kondisi.PETIR -> "$awanTinggi M13 16 l-3 4 h4 l-3 3"
        }
        return PathParser.createPathFromPathData(d)
    }

    override fun onDraw(canvas: Canvas) {
        val sisi = minOf(width, height).toFloat()
        if (sisi <= 0f) return
        canvas.save()
        canvas.scale(sisi / 24f, sisi / 24f)
        kuas.color = warna
        canvas.drawPath(jalur, kuas)
        canvas.restore()
    }
}
