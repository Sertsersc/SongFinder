package com.songfinder.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * MYT Müzik (com.lmr.lfm) entegrasyonu:
 *  1. Şarkı adını + sanatçıyı panoya kopyalar
 *  2. MYT Müzik uygulamasını açar
 *  3. MYT yüklü değilse Play Store'a yönlendirir
 */
object LastFmLauncher {

    private const val TAG     = "MYTLauncher"
    const val MYT_PKG         = "com.lmr.lfm"

    fun playOnMyt(context: Context, trackName: String, artistName: String) {
        val query = "$artistName - $trackName".trim(' ', '-')

        // ── 1. Panoya kopyala ──────────────────────────────────────────────
        copyToClipboard(context, query)

        // ── 2. MYT Müzik'i aç ─────────────────────────────────────────────
        if (!openMyt(context, query)) {
            // Yüklü değil → Play Store
            openPlayStore(context)
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("myt_search", text)
            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "Panoya kopyalandı: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Pano hatası: ${e.message}")
        }
    }

    private fun openMyt(context: Context, query: String): Boolean {
        val pm = context.packageManager

        // Uygulama yüklü mü kontrol et
        val installed = try {
            pm.getPackageInfo(MYT_PKG, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!installed) return false

        // Önce deep link dene (myt:// veya intent scheme)
        val deepLinkOpened = tryMytDeepLink(context, query)
        if (deepLinkOpened) return true

        // Fallback: launcher intent ile normal aç
        return tryLauncherIntent(context)
    }

    /** myt://search?q=... veya content:// ile arama ekranına git */
    private fun tryMytDeepLink(context: Context, query: String): Boolean {
        val encoded = Uri.encode(query)
        val uris = listOf(
            "myt://search?q=$encoded",
            "myt://music/search?query=$encoded",
            "content://com.lmr.lfm/search?q=$encoded"
        )
        for (uriStr in uris) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                    setPackage(MYT_PKG)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                context.startActivity(intent)
                Log.d(TAG, "MYT deep link açıldı: $uriStr")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Deep link başarısız ($uriStr): ${e.message}")
            }
        }
        return false
    }

    /** Normal launcher intent ile MYT'yi aç */
    private fun tryLauncherIntent(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(MYT_PKG) ?: return false
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            context.startActivity(intent)
            Log.d(TAG, "MYT launcher intent ile açıldı")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Launcher intent başarısız: ${e.message}")
            false
        }
    }

    private fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$MYT_PKG")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$MYT_PKG")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
