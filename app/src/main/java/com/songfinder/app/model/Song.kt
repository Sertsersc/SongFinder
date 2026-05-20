package com.songfinder.app.model

import com.google.gson.annotations.SerializedName

// Last.fm API Search Response
data class LastFmSearchResponse(
    @SerializedName("results") val results: SearchResults?
)

data class SearchResults(
    @SerializedName("trackmatches") val trackMatches: TrackMatches?
)

data class TrackMatches(
    @SerializedName("track") val tracks: List<Track>?
)

data class Track(
    @SerializedName("name")     val name: String = "",
    @SerializedName("artist")   val artist: String = "",
    @SerializedName("url")      val url: String = "",
    @SerializedName("image")    val images: List<TrackImage>? = null,
    @SerializedName("listeners") val listeners: String = "0"
) {
    fun getCoverUrl(): String? {
        return images?.lastOrNull { it.size == "extralarge" || it.size == "large" }?.url
            ?.takeIf { it.isNotBlank() }
    }

    fun getListenerCount(): String {
        return try {
            val count = listeners.toLong()
            when {
                count >= 1_000_000 -> "${count / 1_000_000}M dinleyici"
                count >= 1_000     -> "${count / 1_000}K dinleyici"
                else               -> "$count dinleyici"
            }
        } catch (e: NumberFormatException) {
            ""
        }
    }
}

data class TrackImage(
    @SerializedName("#text") val url: String = "",
    @SerializedName("size")  val size: String = ""
)

// Last.fm Track Detail Response
data class LastFmTrackResponse(
    @SerializedName("track") val track: TrackDetail?
)

data class TrackDetail(
    @SerializedName("name")     val name: String = "",
    @SerializedName("artist")   val artist: ArtistInfo? = null,
    @SerializedName("album")    val album: AlbumInfo? = null,
    @SerializedName("duration") val duration: String = "0",
    @SerializedName("url")      val url: String = "",
    @SerializedName("toptags")  val topTags: TopTags? = null
) {
    fun getDurationFormatted(): String {
        val ms = duration.toLongOrNull() ?: return ""
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

data class ArtistInfo(@SerializedName("name") val name: String = "")
data class AlbumInfo(
    @SerializedName("title")  val title: String = "",
    @SerializedName("image")  val images: List<TrackImage>? = null
) {
    fun getCoverUrl(): String? =
        images?.lastOrNull { it.size == "extralarge" || it.size == "large" }?.url
            ?.takeIf { it.isNotBlank() }
}

data class TopTags(@SerializedName("tag") val tags: List<Tag>? = null)
data class Tag(@SerializedName("name") val name: String = "")

// UI State
sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val tracks: List<Track>) : SearchState()
    data class Error(val message: String) : SearchState()
    object Empty : SearchState()
}
