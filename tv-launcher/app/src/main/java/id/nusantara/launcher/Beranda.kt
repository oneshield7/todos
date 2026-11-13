package id.nusantara.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent

/** Hal-hal seputar status "beranda utama": siapa launcher bawaan TV dan apakah launcher ini yang aktif. */
object Beranda {

    private fun maksudBeranda() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** Layar beranda milik aplikasi lain, yaitu launcher bawaan TV. Layar cadangan sistem dikecualikan. */
    fun lain(konteks: Context): List<ActivityInfo> =
        konteks.packageManager.queryIntentActivities(maksudBeranda(), 0)
            .mapNotNull { it.activityInfo }
            .filter { info ->
                info.packageName != konteks.packageName &&
                    !info.packageName.contains("settings", ignoreCase = true) &&
                    !info.packageName.contains("setup", ignoreCase = true) &&
                    !info.name.contains("Fallback", ignoreCase = true)
            }

    /** Benar bila tombol Home saat ini membuka launcher ini. */
    fun iniUtama(konteks: Context): Boolean {
        val pilihan = konteks.packageManager.resolveActivity(maksudBeranda(), PackageManager.MATCH_DEFAULT_ONLY)
        return pilihan?.activityInfo?.packageName == konteks.packageName
    }
}

/**
 * Pengalih tombol Home, untuk TV yang mengunci launcher pabrikan dan tidak menyediakan
 * pilihan launcher. Hanya aktif bila pengguna menyalakannya sendiri di menu Aksesibilitas.
 * Saat layar beranda pabrikan tampil, layanan ini membuka kembali launcher ini.
 * Tidak membaca isi layar; yang dilihat hanya nama layar yang baru terbuka.
 */
class PengalihHome : AccessibilityService() {

    private var layarBawaan: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        layarBawaan = Beranda.lain(this).map { it.packageName + "/" + it.name }.toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val layar = (event.packageName?.toString() ?: return) + "/" + (event.className?.toString() ?: return)
        if (layar !in layarBawaan) return
        if (!Simpanan(this).alihkanHome) return
        try {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // TV menolak membuka layar dari latar; tidak ada yang bisa dilakukan di sini
        }
    }

    override fun onInterrupt() {}
}
