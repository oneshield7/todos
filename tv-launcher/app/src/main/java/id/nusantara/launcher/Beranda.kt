package id.nusantara.launcher

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

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

    private fun buka(layar: Activity, tujuan: Intent): Boolean {
        return try {
            layar.startActivity(tujuan)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun pesan(layar: Activity, isi: String) {
        Toast.makeText(layar, isi, Toast.LENGTH_LONG).show()
    }

    /** Android tidak mengizinkan aplikasi menjadikan dirinya beranda diam-diam; pengguna yang menyetujui. */
    fun jadikanUtama(layar: Activity, simpanan: Simpanan) {
        // Cara resmi di Android 10 ke atas: sistem menampilkan dialog persetujuan
        if (Build.VERSION.SDK_INT >= 29) {
            val peran = layar.getSystemService(RoleManager::class.java)
            if (peran != null && peran.isRoleAvailable(RoleManager.ROLE_HOME) && !peran.isRoleHeld(RoleManager.ROLE_HOME)) {
                try {
                    layar.startActivityForResult(peran.createRequestRoleIntent(RoleManager.ROLE_HOME), 7)
                    return
                } catch (e: Exception) {
                    // lanjut ke cara berikutnya
                }
            }
        }
        // Layar "aplikasi Beranda" di pengaturan TV
        if (buka(layar, Intent(Settings.ACTION_HOME_SETTINGS))) {
            pesan(layar, "Pilih Nusantara Launcher sebagai aplikasi Beranda")
            return
        }
        // TV mengunci launcher pabrikan: tawarkan pengalih tombol Home
        AlertDialog.Builder(layar)
            .setTitle("TV ini tidak menyediakan pilihan launcher")
            .setMessage(
                "Nyalakan \"Pengalih tombol Home\" milik Nusantara Launcher di menu Aksesibilitas. " +
                    "Setelah itu tombol Home membuka launcher ini. Pengalih bisa dimatikan kapan saja."
            )
            .setPositiveButton("Buka Aksesibilitas") { _, _ ->
                simpanan.alihkanHome = true
                if (!buka(layar, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) pesan(layar, "Menu Aksesibilitas tidak bisa dibuka")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /** Mengembalikan TV ke launcher bawaannya. */
    fun kembalikanBawaan(layar: Activity, simpanan: Simpanan) {
        simpanan.alihkanHome = false
        if (buka(layar, Intent(Settings.ACTION_HOME_SETTINGS))) {
            pesan(layar, "Pilih launcher bawaan TV sebagai aplikasi Beranda")
            return
        }
        @Suppress("DEPRECATION")
        layar.packageManager.clearPackagePreferredActivities(layar.packageName)
        val bawaan = lain(layar).firstOrNull()
        if (bawaan == null) {
            pesan(layar, "Launcher bawaan tidak ditemukan di TV ini")
            return
        }
        val tujuan = maksudBeranda().setClassName(bawaan.packageName, bawaan.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!buka(layar, tujuan)) pesan(layar, "Launcher bawaan tidak bisa dibuka")
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
