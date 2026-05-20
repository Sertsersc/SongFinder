package com.songfinder.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.songfinder.app.R
import com.songfinder.app.databinding.ItemTrackBinding
import com.songfinder.app.model.Track

class TrackAdapter(
    private val onTrackClick: (Track) -> Unit
) : ListAdapter<Track, TrackAdapter.TrackViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TrackViewHolder(
            ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class TrackViewHolder(
        private val b: ItemTrackBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(track: Track) {
            b.trackNameText.text  = track.name
            b.artistNameText.text = track.artist
            b.listenersText.text  = track.getListenerCount()

            Glide.with(b.root.context)
                .load(track.getCoverUrl())
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_music_placeholder)
                        .error(R.drawable.ic_music_placeholder)
                        .transform(RoundedCorners(16))
                )
                .into(b.coverImage)

            // Kart tamamına tıklama → MYT aç + panoya kopyala
            b.root.setOnClickListener { onTrackClick(track) }

            // MYT logo butonuna tıklama (aynı akış)
            b.openMytButton.setOnClickListener { onTrackClick(track) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Track>() {
            override fun areItemsTheSame(a: Track, b: Track) = a.url == b.url
            override fun areContentsTheSame(a: Track, b: Track) = a == b
        }
    }
}
