package com.songfinder.app.network

import com.songfinder.app.model.LastFmSearchResponse
import com.songfinder.app.model.LastFmTrackResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApiService {

    @GET("2.0/")
    suspend fun searchTrack(
        @Query("method")  method: String  = "track.search",
        @Query("track")   track: String,
        @Query("api_key") apiKey: String,
        @Query("format")  format: String  = "json",
        @Query("limit")   limit: Int      = 20
    ): LastFmSearchResponse

    @GET("2.0/")
    suspend fun getTrackInfo(
        @Query("method")  method: String = "track.getInfo",
        @Query("track")   track: String,
        @Query("artist")  artist: String,
        @Query("api_key") apiKey: String,
        @Query("format")  format: String = "json"
    ): LastFmTrackResponse
}
