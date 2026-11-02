package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.MovieDao
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.data.remote.TmdbApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MovieRepository(
    private val movieDao: MovieDao,
    private val context: android.content.Context,
    private val apiService: TmdbApiService = TmdbApiService.create()
) {
    private val prefs = context.getSharedPreferences("cinema_stream_prefs", android.content.Context.MODE_PRIVATE)

    private fun getApiKey(): String {
        val override = prefs.getString("override_tmdb_api_key", "") ?: ""
        if (override.isNotBlank()) return override.trim()
        return decryptKey(BuildConfig.TMDB_API_KEY)
    }

    private fun isApiKeyAvailable(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && 
                !key.contains("YOUR_TMDB_API_KEY") && 
                !key.contains("TMDB")
    }

    val watchHistory: Flow<List<WatchHistoryEntity>> = movieDao.getAllWatchHistory()
    val watchlist: Flow<List<WatchHistoryEntity>> = movieDao.getWatchlist()

    fun observeWatchHistoryById(id: String): Flow<WatchHistoryEntity?> {
        return movieDao.observeWatchHistoryById(id)
    }

    suspend fun saveWatchProgress(
        tmdbId: Int,
        title: String,
        posterPath: String,
        category: String,
        isTvShow: Boolean,
        season: Int = 1,
        episode: Int = 1,
        isWatchlist: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val id = if (isTvShow) "${tmdbId}_tv" else tmdbId.toString()
        val existing = movieDao.getWatchHistoryById(id)
        val updated = WatchHistoryEntity(
            id = id,
            tmdbId = tmdbId,
            title = title,
            posterPath = posterPath,
            category = category,
            isTvShow = isTvShow,
            isWatchlist = existing?.isWatchlist ?: isWatchlist,
            lastWatchedSeason = season,
            lastWatchedEpisode = episode,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        movieDao.insertOrUpdate(updated)
    }

    suspend fun toggleWatchlist(movie: MovieItem) = withContext(Dispatchers.IO) {
        val id = if (movie.isTvShow) "${movie.tmdbId}_tv" else movie.tmdbId.toString()
        val existing = movieDao.getWatchHistoryById(id)
        if (existing != null) {
            val updated = existing.copy(
                isWatchlist = !existing.isWatchlist,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
            movieDao.insertOrUpdate(updated)
        } else {
            val newItem = WatchHistoryEntity(
                id = id,
                tmdbId = movie.tmdbId,
                title = movie.title,
                posterPath = movie.posterPath,
                category = movie.category,
                isTvShow = movie.isTvShow,
                isWatchlist = true,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
            movieDao.insertOrUpdate(newItem)
        }
    }

    suspend fun removeWatchHistory(id: String) = withContext(Dispatchers.IO) {
        movieDao.delete(id)
    }

    // Modern sidebar genre filters mapped to TMDB IDs
    fun getGenreId(genre: String, isTvShow: Boolean): Int? {
        return when (genre.lowercase()) {
            "action" -> if (isTvShow) 10759 else 28
            "romance" -> if (isTvShow) 18 else 10749 // TV romance is usually categorized under Soap/Drama
            "history" -> if (isTvShow) 10768 else 36 // TV History/War & Politics
            "comedy" -> 35
            "sci-fi" -> if (isTvShow) 10765 else 878 // Sci-Fi & Fantasy
            "drama" -> 18
            "mystery" -> 9648
            else -> null
        }
    }

    // Unified fetcher: Dynamic TMDB or fallback offline curated catalog
    suspend fun fetchCategoryItems(
        category: String,
        selectedGenre: String? = null,
        searchQuery: String? = null
    ): List<MovieItem> = withContext(Dispatchers.IO) {
        // 1. If searching, first check TMDB API if key is available
        if (searchQuery != null && searchQuery.isNotBlank()) {
            if (isApiKeyAvailable()) {
                try {
                    val response = apiService.searchMulti(getApiKey(), searchQuery)
                    val items = response.results.mapNotNull { result ->
                        mapTmdbResultToMovieItem(result, category)
                    }
                    if (items.isNotEmpty()) return@withContext items
                } catch (e: Exception) {
                    Log.e("MovieRepository", "Search failed: ${e.message}", e)
                }
            }
            // Offline search fallback
            return@withContext getOfflineCatalog().filter {
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.originalTitle.contains(searchQuery, ignoreCase = true) ||
                it.overview.contains(searchQuery, ignoreCase = true)
            }
        }

        // 2. Main list loading (Categorized + Optional Genre filter)
        if (isApiKeyAvailable()) {
            try {
                val isTv = category.contains("Series") || category.contains("Anime")
                val genreId = selectedGenre?.let { getGenreId(it, isTv) }
                val genreQueryString = genreId?.toString()

                if (isTv) {
                    val originalLanguage = when (category) {
                        "Arabic Series" -> "ar"
                        "Anime" -> "ja"
                        else -> "en" // Foreign Series
                    }
                    // Animation genre is 16
                    val finalGenreQuery = if (category == "Anime") {
                        if (genreQueryString != null) "16,$genreQueryString" else "16"
                    } else {
                        genreQueryString
                    }

                    val response = apiService.discoverTvShows(
                        apiKey = getApiKey(),
                        originalLanguage = originalLanguage,
                        withGenres = finalGenreQuery
                    )
                    return@withContext response.results.mapNotNull {
                        mapTmdbResultToMovieItem(it, category)
                    }
                } else {
                    val originalLanguage = if (category == "Arabic Movies") "ar" else "en"
                    val response = apiService.discoverMovies(
                        apiKey = getApiKey(),
                        originalLanguage = originalLanguage,
                        withGenres = genreQueryString
                    )
                    return@withContext response.results.mapNotNull {
                        mapTmdbResultToMovieItem(it, category)
                    }
                }
            } catch (e: Exception) {
                Log.e("MovieRepository", "TMDB API request failed, falling back to offline catalog: ${e.message}", e)
            }
        }

        // 3. Fallback to offline curated catalog
        var filtered = getOfflineCatalog().filter { it.category == category }
        if (selectedGenre != null) {
            filtered = filtered.filter { item ->
                item.genres.any { it.equals(selectedGenre, ignoreCase = true) }
            }
        }
        return@withContext filtered
    }

    suspend fun getShowDetails(tmdbId: Int): Pair<Int, List<Int>> = withContext(Dispatchers.IO) {
        if (isApiKeyAvailable()) {
            try {
                val details = apiService.getTvDetails(tmdbId, getApiKey())
                val totalSeasons = details.numberOfSeasons ?: 1
                val episodes = details.seasons?.map { it.episodeCount } ?: listOf(10)
                return@withContext Pair(totalSeasons, episodes)
            } catch (e: Exception) {
                Log.e("MovieRepository", "Failed to fetch TV details", e)
            }
        }
        // Fallback for series/anime from local config
        val offlineItem = getOfflineCatalog().find { it.tmdbId == tmdbId }
        val seasons = offlineItem?.totalSeasons ?: 1
        val eps = offlineItem?.episodesPerSeason ?: listOf(10)
        Pair(seasons, eps)
    }

    private fun mapTmdbResultToMovieItem(result: com.example.data.remote.TmdbMovieResult, category: String): MovieItem? {
        val tmdbId = result.id
        val isTv = category.contains("Series") || category.contains("Anime")
        
        val title = if (isTv) result.name ?: result.originalName else result.title ?: result.originalTitle
        val originalTitle = if (isTv) result.originalName ?: "" else result.originalTitle ?: ""
        
        if (title.isNullOrBlank()) return null

        val releaseDate = if (isTv) result.firstAirDate ?: "" else result.releaseDate ?: ""
        val genres = result.genreIds?.mapNotNull { id ->
            mapTmdbGenreIdToName(id)
        }?.distinct() ?: listOf("Action")

        return MovieItem(
            id = if (isTv) "${tmdbId}_tv" else tmdbId.toString(),
            tmdbId = tmdbId,
            title = title,
            originalTitle = originalTitle,
            overview = result.overview ?: "No overview available.",
            posterPath = result.posterPath ?: "",
            backdropPath = result.backdropPath ?: "",
            category = category,
            genres = genres,
            rating = result.voteAverage ?: 7.0,
            releaseDate = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "N/A",
            isTvShow = isTv,
            isArabic = category.contains("Arabic"),
            totalSeasons = if (category == "Anime") 1 else 3,
            episodesPerSeason = listOf(12, 12, 12)
        )
    }

    private fun mapTmdbGenreIdToName(id: Int): String? {
        return when (id) {
            28, 10759 -> "Action"
            10749 -> "Romance"
            36, 10768 -> "History"
            35 -> "Comedy"
            878, 10765 -> "Sci-Fi"
            18 -> "Drama"
            9648 -> "Mystery"
            else -> null // Ignore generic genres to keep our sidebar filter clean and focused
        }
    }

    // Rich offline curated streaming catalog
    fun getOfflineCatalog(): List<MovieItem> {
        return listOf(
            // Arabic Movies
            MovieItem(
                id = "818511",
                tmdbId = 818511,
                title = "كيرة والجن",
                originalTitle = "Kira & El Gin",
                overview = "يرصد الفيلم حالة الغليان التي كانت يموج بها الشارع المصري بالتزامن مع اندلاع ثورة 1919، وهو الحدث الكبير الذي يجمع مصائر أحمد عبدالحي كيرة وعبدالقادر الجن ليشتركا في النضال ضد الاحتلال الإنجليزي.",
                posterPath = "/5g6U97SgG5iPbeasYVf8A9R9O8P.jpg",
                backdropPath = "/7f893e17s0.jpg",
                category = "Arabic Movies",
                genres = listOf("Action", "Drama", "History"),
                rating = 8.1,
                releaseDate = "2022",
                isTvShow = false,
                isArabic = true
            ),
            MovieItem(
                id = "1121175",
                tmdbId = 1121175,
                title = "ولاد رزق ٣: القاضية",
                originalTitle = "Welad Rizk 3",
                overview = "بعد مرور سنوات وانفصال الأخوة، يعود شبح من الماضي ليلقي بظلاله على ولاد رزق، مما يجبرهم على العودة إلى حياة الجريمة والسرقة مرة أخرى في عملية مصيرية هي الأكبر والأخطر في تاريخهم.",
                posterPath = "/p5y3WqU9D2P5u7MofqX9D0TjXqK.jpg",
                backdropPath = "/wr3s89s7.jpg",
                category = "Arabic Movies",
                genres = listOf("Action", "Comedy"),
                rating = 8.4,
                releaseDate = "2024",
                isTvShow = false,
                isArabic = true
            ),
            MovieItem(
                id = "581523",
                tmdbId = 581523,
                title = "الفيل الأزرق ٢",
                originalTitle = "The Blue Elephant 2",
                overview = "تبدأ أحداث الجزء الثاني بعد 5 سنوات من نهاية الجزء الأول، حيث يتزوج الدكتور (يحيى) من (لبنى)، ويتم استدعاؤه لقسم الحالات الخطرة (8 غرب حريم)، ويلتقي هناك بمن يتلاعب بحياته وحياة أسرته.",
                posterPath = "/p3D0Cst5tA1pU0r5bA2O9F3vT6O.jpg",
                backdropPath = "/blueelephant.jpg",
                category = "Arabic Movies",
                genres = listOf("Drama", "Mystery"),
                rating = 7.9,
                releaseDate = "2019",
                isTvShow = false,
                isArabic = true
            ),
            MovieItem(
                id = "541014",
                tmdbId = 541014,
                title = "البدلة",
                originalTitle = "El Badlah",
                overview = "يدور الفيلم حول شابين يقعان في مواقف مضحكة وغريبة بسبب انتحالهما صفة ضابطي شرطة لحضور حفلة تنكرية، وتتوالى الأحداث في قالب كوميدي أكشن مثير.",
                posterPath = "/s4pWstzYmD9qKjK5P0A3b8FvT3I.jpg",
                backdropPath = "/elbadlah.jpg",
                category = "Arabic Movies",
                genres = listOf("Comedy", "Action"),
                rating = 7.5,
                releaseDate = "2018",
                isTvShow = false,
                isArabic = true
            ),

            // Foreign Movies
            MovieItem(
                id = "157336",
                tmdbId = 157336,
                title = "Interstellar",
                originalTitle = "Interstellar",
                overview = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
                posterPath = "/gEU2Qv6vKM7Usl76g90asgZ6vby.jpg",
                backdropPath = "/rAiRF0WgDzz68966YF6S6XNu6gD.jpg",
                category = "Foreign Movies",
                genres = listOf("Sci-Fi", "Drama"),
                rating = 8.6,
                releaseDate = "2014",
                isTvShow = false,
                isArabic = false
            ),
            MovieItem(
                id = "27205",
                tmdbId = 27205,
                title = "Inception",
                originalTitle = "Inception",
                overview = "Cobb, a skilled thief who steals valuable secrets from deep within the subconscious during the dream state, is given a chance at redemption: plant an idea into a CEO's mind.",
                posterPath = "/o0gCOmS7v68tcCHY9g4686p6b9c.jpg",
                backdropPath = "/ztv46byg3R.jpg",
                category = "Foreign Movies",
                genres = listOf("Sci-Fi", "Action", "Mystery"),
                rating = 8.4,
                releaseDate = "2010",
                isTvShow = false,
                isArabic = false
            ),
            MovieItem(
                id = "155",
                tmdbId = 155,
                title = "The Dark Knight",
                originalTitle = "The Dark Knight",
                overview = "Batman raises the stakes in his war on crime. With the help of Lt. Jim Gordon and District Attorney Harvey Dent, Batman sets out to dismantle the remaining criminal organizations that plague the streets.",
                posterPath = "/qJ2tWGB26Z7vC659OHg76t9uAas.jpg",
                backdropPath = "/dqK069v37AVyz06Z6uYV6O7z.jpg",
                category = "Foreign Movies",
                genres = listOf("Action", "Drama", "Mystery"),
                rating = 8.5,
                releaseDate = "2008",
                isTvShow = false,
                isArabic = false
            ),

            // Arabic Series
            MovieItem(
                id = "247076_tv",
                tmdbId = 247076,
                title = "الحشاشين",
                originalTitle = "The Assassins",
                overview = "يتناول المسلسل قصة طائفة الحشاشين بقيادة حسن الصباح، والذين اشتهروا بالقيام باغتيالات دموية لشخصيات هامة في القرن الحادي عشر.",
                posterPath = "/gEU2Qv6vKM7Usl76g90asgZ6vby.jpg", // placeholder
                backdropPath = "",
                category = "Arabic Series",
                genres = listOf("History", "Drama"),
                rating = 8.5,
                releaseDate = "2024",
                isTvShow = true,
                isArabic = true,
                totalSeasons = 1,
                episodesPerSeason = listOf(30)
            ),
            MovieItem(
                id = "221808_tv",
                tmdbId = 221808,
                title = "جعفر العمدة",
                originalTitle = "Gaafar El Omda",
                overview = "يدور المسلسل حول جعفر العمدة الذي يعيش في حي السيدة زينب، وهو متزوج من أربع سيدات، ويمتلك شركة للمقاولات، ويبحث عن ابنه المفقود منذ 19 عاماً.",
                posterPath = "/9j8t3Gv9A9qS6v9Y9vTqK8xO9bX.jpg",
                backdropPath = "",
                category = "Arabic Series",
                genres = listOf("Drama"),
                rating = 8.0,
                releaseDate = "2023",
                isTvShow = true,
                isArabic = true,
                totalSeasons = 1,
                episodesPerSeason = listOf(30)
            ),

            // Foreign Series
            MovieItem(
                id = "1396_tv",
                tmdbId = 1396,
                title = "Breaking Bad",
                originalTitle = "Breaking Bad",
                overview = "When Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer, he turns to a life of crime, producing and selling methamphetamine with a former student, Jesse Pinkman.",
                posterPath = "/ztkUQ656176g4b9v6pBvU7gYj9O.jpg",
                backdropPath = "/9bX.jpg",
                category = "Foreign Series",
                genres = listOf("Drama"),
                rating = 9.5,
                releaseDate = "2008",
                isTvShow = true,
                isArabic = false,
                totalSeasons = 5,
                episodesPerSeason = listOf(7, 13, 13, 13, 16)
            ),
            MovieItem(
                id = "66732_tv",
                tmdbId = 66732,
                title = "Stranger Things",
                originalTitle = "Stranger Things",
                overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                posterPath = "/49W5XmU9D2P5u7MofqX9D0TjXqK.jpg",
                backdropPath = "",
                category = "Foreign Series",
                genres = listOf("Sci-Fi", "Mystery", "Drama"),
                rating = 8.6,
                releaseDate = "2016",
                isTvShow = true,
                isArabic = false,
                totalSeasons = 4,
                episodesPerSeason = listOf(8, 9, 8, 9)
            ),

            // Anime
            MovieItem(
                id = "1429_tv",
                tmdbId = 1429,
                title = "Attack on Titan",
                originalTitle = "Shingeki no Kyojin",
                overview = "Several hundred years ago, humans were nearly exterminated by Titans. Titans are typically stories tall, seem to have no intelligence, devour human beings and, worst of all, seem to do it for the pleasure rather than as a food source.",
                posterPath = "/h90A9qS6v9Y9vTqK8xO9bX9bX.jpg",
                backdropPath = "",
                category = "Anime",
                genres = listOf("Action", "Drama", "Sci-Fi"),
                rating = 9.1,
                releaseDate = "2013",
                isTvShow = true,
                isArabic = false,
                totalSeasons = 4,
                episodesPerSeason = listOf(25, 12, 22, 28)
            ),
            MovieItem(
                id = "85937_tv",
                tmdbId = 85937,
                title = "Demon Slayer: Kimetsu no Yaiba",
                originalTitle = "Kimetsu no Yaiba",
                overview = "It is the Taisho Period in Japan. Tanjiro, a kindhearted boy who sells charcoal for a living, finds his family slaughtered by a demon. To make matters worse, his younger sister Nezuko, the sole survivor, has been transformed into a demon herself.",
                posterPath = "/x6v9Y9vTqK8xO9bX9bX9bX9b.jpg",
                backdropPath = "",
                category = "Anime",
                genres = listOf("Action", "Drama"),
                rating = 8.8,
                releaseDate = "2019",
                isTvShow = true,
                isArabic = false,
                totalSeasons = 4,
                episodesPerSeason = listOf(26, 18, 11, 8)
            )
        )
    }

    /**
     * Decrypts or deobfuscates the API key if it has been encoded in Base64
     * to protect it from static analysis / cleartext extraction in decompiled APKs.
     */
    private fun decryptKey(rawKey: String): String {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty() || trimmed.contains("YOUR_TMDB_API_KEY")) return ""

        try {
            // Attempt to decode as Base64 to see if the user obfuscated the key
            val decodedBytes = android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT)
            val decodedStr = String(decodedBytes, Charsets.UTF_8).trim()

            // Validate that the decoded string matches a standard TMDB hex format or v4 token format
            val isHex32 = decodedStr.matches(Regex("^[0-9a-fA-F]{32}$"))
            val isLikelyV4 = decodedStr.length > 50 && decodedStr.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }

            if (isHex32 || isLikelyV4) {
                Log.d("MovieRepository", "Successfully detected and decrypted Base64 obfuscated TMDB API Key!")
                return decodedStr
            }
        } catch (e: Exception) {
            // If it fails to decode or isn't a valid key string, fallback to using rawKey
        }
        return trimmed
    }
}
