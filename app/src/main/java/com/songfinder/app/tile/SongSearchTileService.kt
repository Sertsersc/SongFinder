package com.songfinder.app.tile

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.songfinder.app.R
import com.songfinder.app.recognition.AcrCloudRecognizer
import com.songfinder.app.ui.LastFmLauncher
import com.songfinder.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class SongSearchTileService : TileService() {

    private val scope      = CoroutineScope(Dispatchers.Main + Job())
    private val recognizer = AcrCloudRecognizer()

    private val notifManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val CH_LISTEN  = "ch_listen"
        private const val CH_RESULT  = "ch_result"
        private const val NOTIF_LISTEN = 1001
        private const val NOTIF_RESULT = 1002

        fun requestUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestListeningState(
                    context,
                    ComponentName(context, SongSearchTileService::class.java)
                )
            }
        }
    }

    // ── Tile dinlemeye başladı ─────────────────────────────────────────────
    override fun onStartListening() {
        super.onStartListening()
        setTileReady()
    }

    // ── Tile'a basıldı → direkt dinlemeye başla ───────────────────────────
    override fun onClick() {
        super.onClick()

        // Mikrofon izni var mı?
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // İzin yok → ana uygulamayı aç, oradan izin alınacak
            showToast("⚠️ Mikrofon izni gerekli! Uygulamayı açın.")
            openMainApp()
            return
        }

        // Zaten dinliyorsa durdur
        if (recognizer.isRecording) {
            recognizer.stopRecording()
            setTileReady()
            cancelListeningNotif()
            return
        }

        // Tile aktif göster
        setTileListening()

        // Paneli kapat, arka planda dinle
        // collapseStatusBar()

        // Dinleme bildirimi göster
        createChannels()
        showListeningNotif()

        // Kayıt + tanıma başlat
        scope.launch {
            val result = recognizer.recognizeSong { progress ->
                // Bildirim metnini güncelle
                updateListeningNotif(progress)
            }

            withContext(Dispatchers.Main) {
                cancelListeningNotif()
                setTileReady()

                if (result.success) {
                    // ✅ Şarkı bulundu
                    val songText = "${result.artist} - ${result.title}"

                    // Sonuç bildirimi göster + MYT butonu
                    showResultNotif(
                        title  = "🎵 ${result.title}",
                        text   = result.artist,
                        track  = result.title,
                        artist = result.artist
                    )

                    // Panoya kopyala + MYT aç
                    LastFmLauncher.playOnMyt(
                        applicationContext,
                        result.title,
                        result.artist
                    )

                    showToast("✅ $songText\nMYT açılıyor…")
                } else {
                    // ❌ Tanınamadı
                    showToast("❌ ${result.errorMsg}")
                    showErrorNotif(result.errorMsg)
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        if (recognizer.isRecording) recognizer.stopRecording()
    }

    // ── Tile durum yöneticileri ────────────────────────────────────────────

    private fun setTileReady() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Şarkı Tanı"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = "MYT Müzik"
            updateTile()
        }
    }

    private fun setTileListening() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Dinleniyor…"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = "Durdurmak için bas"
            updateTile()
        }
    }

    // ── Bildirim kanalları ─────────────────────────────────────────────────

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.createNotificationChannel(
                NotificationChannel(CH_LISTEN, "Dinleme", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Şarkı dinlenirken gösterilir"
                    setSound(null, null)
                }
            )
            notifManager.createNotificationChannel(
                NotificationChannel(CH_RESULT, "Sonuç", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Bulunan şarkı bildirimi"
                }
            )
        }
    }

    // ── Dinleme bildirimi (progress) ───────────────────────────────────────

    private fun showListeningNotif() {
        val notif = NotificationCompat.Builder(this, CH_LISTEN)
            .setSmallIcon(R.drawable.ic_myt_logo)
            .setContentTitle("🎙️ Şarkı dinleniyor…")
            .setContentText("Yakındaki müziği tanımaya çalışıyorum")
            .setProgress(0, 0, true)          // belirsiz progress bar
            .setOngoing(true)                  // kaydırılamaz
            .setSilent(true)
            .build()
        notifManager.notify(NOTIF_LISTEN, notif)
    }

    private fun updateListeningNotif(text: String) {
        val notif = NotificationCompat.Builder(this, CH_LISTEN)
            .setSmallIcon(R.drawable.ic_myt_logo)
            .setContentTitle("🎙️ Şarkı dinleniyor…")
            .setContentText(text)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .build()
        notifManager.notify(NOTIF_LISTEN, notif)
    }

    private fun cancelListeningNotif() {
        notifManager.cancel(NOTIF_LISTEN)
    }

    // ── Sonuç bildirimi (MYT butonu ile) ──────────────────────────────────

    private fun showResultNotif(title: String, text: String,
                                 track: String, artist: String) {
        // "MYT'de Aç" butonuna basınca LastFmLauncher çalışacak
        val mytIntent = Intent(applicationContext, MytOpenReceiver::class.java).apply {
            putExtra("track",  track)
            putExtra("artist", artist)
        }
        val mytPending = PendingIntent.getBroadcast(
            applicationContext, 0, mytIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CH_RESULT)
            .setSmallIcon(R.drawable.ic_myt_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_myt_logo, "MYT'de Aç 🎵", mytPending)
            .build()

        notifManager.notify(NOTIF_RESULT, notif)
    }

    private fun showErrorNotif(msg: String) {
        val notif = NotificationCompat.Builder(this, CH_RESULT)
            .setSmallIcon(R.drawable.ic_myt_logo)
            .setContentTitle("❌ Şarkı tanınamadı")
            .setContentText(msg)
            .setAutoCancel(true)
            .build()
        notifManager.notify(NOTIF_RESULT, notif)
    }

    // ── Yardımcılar ────────────────────────────────────────────────────────

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private fun openMainApp() {
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        intent?.let { startActivityAndCollapse(it) }
    }
}
