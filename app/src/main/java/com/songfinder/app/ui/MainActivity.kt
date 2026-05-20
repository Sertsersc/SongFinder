package com.songfinder.app.ui
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.songfinder.app.R
import androidx.activity.viewModels

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.songfinder.app.databinding.ActivityMainBinding
import com.songfinder.app.model.SearchState
import com.songfinder.app.model.Track
import com.songfinder.app.recognition.AcrCloudRecognizer
import com.songfinder.app.tile.SongSearchTileService
import com.songfinder.app.viewmodel.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: TrackAdapter
    private val recognizer = AcrCloudRecognizer()

    // Mikrofon izin isteyici
    private val micPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else Toast.makeText(this, "Mikrofon izni gerekli!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupSearch()
        setupListenButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = TrackAdapter { track -> onTrackSelected(track) }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            val q = text?.toString() ?: ""
            binding.clearButton.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
            viewModel.searchWithDebounce(q)
        }
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(binding.searchEditText.text?.toString() ?: "")
                hideKeyboard(); true
            } else false
        }
        binding.clearButton.setOnClickListener { binding.searchEditText.setText("") }
        binding.searchButton.setOnClickListener {
            val q = binding.searchEditText.text?.toString() ?: ""
            if (q.isNotBlank()) { viewModel.search(q); hideKeyboard() }
        }
    }

    // ── Ses tanıma butonu ─────────────────────────────────────────────────
    private fun setupListenButton() {
        binding.listenFab.setOnClickListener {
            if (recognizer.isRecording) {
                recognizer.stopRecording()
                setListenIdle()
            } else {
                checkMicAndListen()
            }
        }
    }

    private fun checkMicAndListen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED -> startListening()
            else -> micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        setListenActive()
        CoroutineScope(Dispatchers.Main).launch {
            val result = recognizer.recognizeSong { progress ->
                withContext(Dispatchers.Main) {
                    binding.listenStatusText.text = progress
                }
            }
            setListenIdle()
            if (result.success) {
                // Şarkı bulundu → arama kutusuna yaz + Last.fm'de ara
                val query = "${result.artist} ${result.title}"
                binding.searchEditText.setText(query)
                viewModel.search(query)
                Toast.makeText(
                    this@MainActivity,
                    "🎵 ${result.title} — ${result.artist}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this@MainActivity, "❌ ${result.errorMsg}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setListenActive() {
        binding.listenFab.setImageResource(android.R.drawable.ic_media_pause)
        binding.listenStatusText.text    = "🎙️ Dinleniyor…"
        binding.listenStatusText.visibility = View.VISIBLE
    }

    private fun setListenIdle() {
        binding.listenFab.setImageResource(R.drawable.ic_mic)
        binding.listenStatusText.visibility = View.GONE
        binding.listenStatusText.text    = ""
    }

    private fun onTrackSelected(track: Track) {
        LastFmLauncher.playOnMyt(this, track.name, track.artist)
        Toast.makeText(
            this, "📋 Kopyalandı & MYT açılıyor…\n${track.artist} - ${track.name}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun observeViewModel() {
        viewModel.searchState.observe(this) { state ->
            when (state) {
                is SearchState.Idle    -> showIdle()
                is SearchState.Loading -> showLoading()
                is SearchState.Success -> showResults(state.tracks)
                is SearchState.Empty   -> showEmpty()
                is SearchState.Error   -> showError(state.message)
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility     = View.GONE
        binding.recyclerView.visibility    = View.GONE
        binding.emptyView.visibility       = View.GONE
        binding.errorView.visibility       = View.GONE
        binding.idleView.visibility        = View.VISIBLE
        binding.resultCountText.visibility = View.GONE
    }
    private fun showLoading() {
        binding.progressBar.visibility     = View.VISIBLE
        binding.recyclerView.visibility    = View.GONE
        binding.emptyView.visibility       = View.GONE
        binding.errorView.visibility       = View.GONE
        binding.idleView.visibility        = View.GONE
        binding.resultCountText.visibility = View.GONE
    }
    private fun showResults(tracks: List<Track>) {
        binding.progressBar.visibility     = View.GONE
        binding.recyclerView.visibility    = View.VISIBLE
        binding.emptyView.visibility       = View.GONE
        binding.errorView.visibility       = View.GONE
        binding.idleView.visibility        = View.GONE
        adapter.submitList(tracks)
        binding.resultCountText.text       = "${tracks.size} sonuç — MYT logosuna dokun"
        binding.resultCountText.visibility = View.VISIBLE
    }
    private fun showEmpty() {
        binding.progressBar.visibility     = View.GONE
        binding.recyclerView.visibility    = View.GONE
        binding.emptyView.visibility       = View.VISIBLE
        binding.errorView.visibility       = View.GONE
        binding.idleView.visibility        = View.GONE
        binding.resultCountText.visibility = View.GONE
    }
    private fun showError(message: String) {
        binding.progressBar.visibility     = View.GONE
        binding.recyclerView.visibility    = View.GONE
        binding.emptyView.visibility       = View.GONE
        binding.errorView.visibility       = View.VISIBLE
        binding.idleView.visibility        = View.GONE
        binding.errorText.text             = "Hata: $message"
        binding.resultCountText.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}
