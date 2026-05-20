package com.songfinder.app.tile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.songfinder.app.databinding.ItemTileResultBinding
import com.songfinder.app.model.Track

class TileResultAdapter(
    private val tracks: List<Track>,
    private val onClick: (Track) -> Unit
) : RecyclerView.Adapter<TileResultAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemTileResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(tracks[position])

    inner class VH(private val b: ItemTileResultBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(track: Track) {
            b.tileTrackName.text   = track.name
            b.tileArtistName.text  = track.artist
            b.tileListeners.text   = track.getListenerCount()
            b.root.setOnClickListener { onClick(track) }
        }
    }
}
