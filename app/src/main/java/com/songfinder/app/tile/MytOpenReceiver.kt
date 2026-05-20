package com.songfinder.app.tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.songfinder.app.ui.LastFmLauncher

/**
 * Bildirim üzerindeki "MYT'de Aç" butonundan tetiklenir.
 * Şarkıyı panoya kopyalar ve MYT Müzik'i açar.
 */
class MytOpenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val track  = intent.getStringExtra("track")  ?: return
        val artist = intent.getStringExtra("artist") ?: ""
        LastFmLauncher.playOnMyt(context, track, artist)
    }
}
