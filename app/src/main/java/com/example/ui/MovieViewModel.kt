package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.data.model.StreamingServer
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface MovieUiState {
    object Loading : MovieUiState
    data class Success(val items: List<MovieItem>) : MovieUiState
    data class Error(val message: String) : MovieUiState
}

class MovieViewModel(
    application: Application,
    private val repository: MovieRepository
) : AndroidViewModel(application) {

    // Main Sections: Arabic Movies, Foreign Movies, Arabic Series, Foreign Series, Anime
    val categories = listOf(
        "Arabic Movies",
        "Foreign Movies",
        "Arabic Series",
        "Foreign Series",
        "Anime"
    )

    // Sidebar/Filter genres
    val genres = listOf(
        "Action",
        "Romance",
        "History",
        "Comedy",
        "Sci-Fi",
        "Drama",
        "Mystery"
    )

    // Predefined scraping servers for automated stream switching
    val streamingServers = listOf(
        StreamingServer(
            name = "سيرفر ١ (VidSrc TO)",
            baseUrl = "https://vidsrc.to/embed",
            isMoviePattern = "{baseUrl}/movie/{id}",
            isTvPattern = "{baseUrl}/tv/{id}/{season}/{episode}"
        ),
        StreamingServer(
            name = "سيرفر ٢ (VidSrc ME)",
            baseUrl = "https://vidsrc.me/embed",
            isMoviePattern = "{baseUrl}/movie?tmdb={id}",
            isTvPattern = "{baseUrl}/tv?tmdb={id}&season={season}&episode={episode}"
        ),
        StreamingServer(
            name = "سيرفر ٣ (Embed SU)",
            baseUrl = "https://embed.su/embed",
            isMoviePattern = "{baseUrl}/movie/{id}",
            isTvPattern = "{baseUrl}/tv/{id}/{season}/{episode}"
        ),
        StreamingServer(
            name = "سيرفر ٤ (AutoEmbed)",
            baseUrl = "https://autoembed.co",
            isMoviePattern = "{baseUrl}/movie/tmdb/{id}",
            isTvPattern = "{baseUrl}/tv/tmdb/{id}-{season}-{episode}"
        )
    )

    // Mutable states
    private val _currentCategory = MutableStateFlow(categories[0])
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedServer = MutableStateFlow(streamingServers[0])
    val selectedServer: StateFlow<StreamingServer> = _selectedServer.asStateFlow()

    // Active screen / overlay
    private val _selectedMovie = MutableStateFlow<MovieItem?>(null)
    val selectedMovie: StateFlow<MovieItem?> = _selectedMovie.asStateFlow()

    // Episodes detail state for the selected TV series/anime
    private val _tvSeasonsCount = MutableStateFlow(1)
    val tvSeasonsCount: StateFlow<Int> = _tvSeasonsCount.asStateFlow()

    private val _tvEpisodesInSeasons = MutableStateFlow<List<Int>>(listOf(10))
    val tvEpisodesInSeasons: StateFlow<List<Int>> = _tvEpisodesInSeasons.asStateFlow()

    private val _currentSeason = MutableStateFlow(1)
    val currentSeason: StateFlow<Int> = _currentSeason.asStateFlow()

    private val _currentEpisode = MutableStateFlow(1)
    val currentEpisode: StateFlow<Int> = _currentEpisode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Reactive Watchlist and History flows from Room database
    val watchlist: StateFlow<List<WatchHistoryEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Movie list UI state
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<MovieUiState> = combine(
        _currentCategory,
        _selectedGenre,
        _searchQuery.debounce(300)
    ) { category, genre, query ->
        Triple(category, genre, query)
    }.flatMapLatest { (category, genre, query) ->
        flow {
            emit(MovieUiState.Loading)
            try {
                val q = if (query.isBlank()) null else query
                val items = repository.fetchCategoryItems(category, genre, q)
                emit(MovieUiState.Success(items))
            } catch (e: Exception) {
                emit(MovieUiState.Error(e.message ?: "An error occurred"))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MovieUiState.Loading)

    // Setters
    fun setCategory(category: String) {
        _currentCategory.value = category
        _selectedGenre.value = null // Reset genre filter when switching tabs
        _selectedMovie.value = null
    }

    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMovie(movie: MovieItem?) {
        _selectedMovie.value = movie
        _isPlaying.value = false
        if (movie != null) {
            _currentSeason.value = 1
            _currentEpisode.value = 1
            if (movie.isTvShow) {
                viewModelScope.launch {
                    val details = repository.getShowDetails(movie.tmdbId)
                    _tvSeasonsCount.value = details.first
                    _tvEpisodesInSeasons.value = details.second
                    
                    // Check if there's any watch progress to resume
                    val progressId = "${movie.tmdbId}_tv"
                    val history = repository.observeWatchHistoryById(progressId).firstOrNull()
                    if (history != null) {
                        _currentSeason.value = history.lastWatchedSeason
                        _currentEpisode.value = history.lastWatchedEpisode
                    }
                }
            } else {
                viewModelScope.launch {
                    val progressId = movie.tmdbId.toString()
                    val history = repository.observeWatchHistoryById(progressId).firstOrNull()
                    if (history != null) {
                        _currentSeason.value = history.lastWatchedSeason
                        _currentEpisode.value = history.lastWatchedEpisode
                    }
                }
            }
        }
    }

    fun setSeason(season: Int) {
        _currentSeason.value = season
        _currentEpisode.value = 1
    }

    fun setEpisode(episode: Int) {
        _currentEpisode.value = episode
    }

    fun selectServer(server: StreamingServer) {
        _selectedServer.value = server
    }

    fun startPlaying() {
        _isPlaying.value = true
        val movie = _selectedMovie.value ?: return
        viewModelScope.launch {
            repository.saveWatchProgress(
                tmdbId = movie.tmdbId,
                title = movie.title,
                posterPath = movie.posterPath,
                category = movie.category,
                isTvShow = movie.isTvShow,
                season = _currentSeason.value,
                episode = _currentEpisode.value
            )
        }
    }

    fun stopPlaying() {
        _isPlaying.value = false
    }

    fun toggleWatchlist(movie: MovieItem) {
        viewModelScope.launch {
            repository.toggleWatchlist(movie)
            // Refresh detail status by emitting or letting Flow handle it
            // Watch history update will be caught by compose UI observing Room Flow
        }
    }

    fun isBookmarked(movie: MovieItem): Flow<Boolean> {
        val id = if (movie.isTvShow) "${movie.tmdbId}_tv" else movie.tmdbId.toString()
        return repository.observeWatchHistoryById(id).map { it?.isWatchlist == true }
    }

    fun getOfflineCatalog(): List<MovieItem> {
        return repository.getOfflineCatalog()
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            repository.removeWatchHistory(id)
        }
    }

    // --- Settings and Preferences Logic ---
    private val prefs = application.getSharedPreferences("cinema_stream_prefs", android.content.Context.MODE_PRIVATE)

    private fun decryptKey(rawKey: String): String {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) return ""
        try {
            val decodedBytes = android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT)
            val decodedStr = String(decodedBytes, Charsets.UTF_8).trim()
            if (decodedStr.isNotEmpty() && decodedStr.all { it.code in 32..126 }) {
                return decodedStr
            }
        } catch (e: Exception) {
            // ignore
        }
        return trimmed
    }

    // --- Dynamic API Keys Overrides ---
    val overrideTmdbApiKey = MutableStateFlow(prefs.getString("override_tmdb_api_key", "") ?: "")
    val overrideAdsterraBannerKey = MutableStateFlow(prefs.getString("override_adsterra_banner_key", "") ?: "")
    val overrideAdsterraBannerDomain = MutableStateFlow(prefs.getString("override_adsterra_banner_domain", "") ?: "")
    val overrideAdsterraSmartLink = MutableStateFlow(prefs.getString("override_adsterra_smart_link", "") ?: "")
    val overrideAdsterraBannerWidth = MutableStateFlow(prefs.getString("override_adsterra_banner_width", "300") ?: "300")
    val overrideAdsterraBannerHeight = MutableStateFlow(prefs.getString("override_adsterra_banner_height", "250") ?: "250")

    fun getAdsterraBannerKey(): String {
        val override = overrideAdsterraBannerKey.value
        if (override.isNotBlank()) return override.trim()
        return decryptKey(com.example.BuildConfig.ADSTERRA_BANNER_KEY)
    }

    fun getAdsterraBannerDomain(): String {
        val override = overrideAdsterraBannerDomain.value
        if (override.isNotBlank()) return override.trim()
        return decryptKey(com.example.BuildConfig.ADSTERRA_BANNER_DOMAIN)
    }

    fun getAdsterraBannerWidth(): Int {
        val override = overrideAdsterraBannerWidth.value
        if (override.isNotBlank()) {
            return override.toIntOrNull() ?: 300
        }
        return try { com.example.BuildConfig.ADSTERRA_BANNER_WIDTH.toInt() } catch (e: Exception) { 300 }
    }

    fun getAdsterraBannerHeight(): Int {
        val override = overrideAdsterraBannerHeight.value
        if (override.isNotBlank()) {
            return override.toIntOrNull() ?: 250
        }
        return try { com.example.BuildConfig.ADSTERRA_BANNER_HEIGHT.toInt() } catch (e: Exception) { 250 }
    }

    fun getAdsterraSmartLink(): String {
        val override = overrideAdsterraSmartLink.value
        if (override.isNotBlank()) return override.trim()
        return decryptKey(com.example.BuildConfig.ADSTERRA_SMART_LINK)
    }

    fun saveKeysOverrides(
        tmdbKey: String,
        bannerKey: String,
        bannerDomain: String,
        smartLink: String,
        bannerWidth: String = "300",
        bannerHeight: String = "250"
    ) {
        prefs.edit().apply {
            putString("override_tmdb_api_key", tmdbKey.trim())
            putString("override_adsterra_banner_key", bannerKey.trim())
            putString("override_adsterra_banner_domain", bannerDomain.trim())
            putString("override_adsterra_smart_link", smartLink.trim())
            putString("override_adsterra_banner_width", bannerWidth.trim())
            putString("override_adsterra_banner_height", bannerHeight.trim())
            apply()
        }
        overrideTmdbApiKey.value = tmdbKey.trim()
        overrideAdsterraBannerKey.value = bannerKey.trim()
        overrideAdsterraBannerDomain.value = bannerDomain.trim()
        overrideAdsterraSmartLink.value = smartLink.trim()
        overrideAdsterraBannerWidth.value = bannerWidth.trim()
        overrideAdsterraBannerHeight.value = bannerHeight.trim()
    }

    fun clearKeysOverrides() {
        prefs.edit().apply {
            remove("override_tmdb_api_key")
            remove("override_adsterra_banner_key")
            remove("override_adsterra_banner_domain")
            remove("override_adsterra_smart_link")
            remove("override_adsterra_banner_width")
            remove("override_adsterra_banner_height")
            apply()
        }
        overrideTmdbApiKey.value = ""
        overrideAdsterraBannerKey.value = ""
        overrideAdsterraBannerDomain.value = ""
        overrideAdsterraSmartLink.value = ""
        overrideAdsterraBannerWidth.value = "300"
        overrideAdsterraBannerHeight.value = "250"
    }

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(prefs.getBoolean("is_notifications_enabled", true))
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("is_notifications_enabled", enabled).apply()
    }

    fun getCacheSize(): String {
        return try {
            val cacheDir = getApplication<Application>().cacheDir
            val sizeInBytes = getFolderSize(cacheDir)
            formatSize(sizeInBytes)
        } catch (e: Exception) {
            "0.00 B"
        }
    }

    private fun getFolderSize(file: java.io.File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        val files = file.listFiles() ?: return 0L
        for (f in files) {
            size += getFolderSize(f)
        }
        return size
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0.00 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = if (digitGroups < units.size) digitGroups else units.size - 1
        return String.format(java.util.Locale.US, "%.2f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun clearCache(): Boolean {
        return try {
            val cacheDir = getApplication<Application>().cacheDir
            val result = cacheDir.deleteRecursively()
            result
        } catch (e: Exception) {
            false
        }
    }

    // --- App Update Checker Logic ---
    private val _updateState = MutableStateFlow<UpdateInfo?>(null)
    val updateState: StateFlow<UpdateInfo?> = _updateState.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isCheckingForUpdates.value = true
            // Simulate network request latency
            kotlinx.coroutines.delay(if (manual) 1200 else 2500)
            
            if (manual) {
                // For demonstration of the fully functional update workflow
                _updateState.value = UpdateInfo(
                    latestVersion = "1.3.0",
                    updateUrl = "https://t.me/CinemaStreamOfficial",
                    releaseNotes = "• إضافة مشغل فيديو مدمج أسرع وأكثر استقراراً.\n• دعم الجودة التلقائية وسيرفرات تشغيل إضافية.\n• إصلاح مشكلة تحميل الترجمة لبعض الأفلام الأجنبية.\n• تحسين أداء التنقل بين الأقسام وسرعة استجابة الشاشات.",
                    isForceUpdate = false
                )
            } else {
                // On automatic background startup check: can stay up to date (null)
                // to not interrupt the user unless they manually check,
                // or we can mock/simulate an update check. Let's keep it null for silent background,
                // or show it if needed. Leaving it null by default is standard.
                _updateState.value = null
            }
            _isCheckingForUpdates.value = false
        }
    }

    fun dismissUpdate() {
        _updateState.value = null
    }

    // Factory Provider
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val repo = MovieRepository(db.movieDao(), application)
            return MovieViewModel(application, repo) as T
        }
    }
}

data class UpdateInfo(
    val latestVersion: String,
    val updateUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean
)
