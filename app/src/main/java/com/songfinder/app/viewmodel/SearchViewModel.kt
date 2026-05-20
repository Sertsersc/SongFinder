package com.songfinder.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songfinder.app.BuildConfig
import com.songfinder.app.model.SearchState
import com.songfinder.app.model.Track
import com.songfinder.app.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private var searchJob: Job? = null

    // Debounced arama - kullanıcı yazmayı bırakınca 500ms sonra ara
    fun searchWithDebounce(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            search(query)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val response = RetrofitClient.apiService.searchTrack(
                    track  = query,
                    apiKey = BuildConfig.LASTFM_API_KEY
                )
                val tracks = response.results?.trackMatches?.tracks
                if (tracks.isNullOrEmpty()) {
                    _searchState.value = SearchState.Empty
                } else {
                    _searchState.value = SearchState.Success(tracks)
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(
                    e.message ?: "Bilinmeyen bir hata oluştu"
                )
            }
        }
    }

    fun clearResults() {
        searchJob?.cancel()
        _searchState.value = SearchState.Idle
    }
}
