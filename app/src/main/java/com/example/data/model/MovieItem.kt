package com.example.data.model

import java.io.Serializable

data class MovieItem(
    val id: String, // String representation of TMDB ID or custom ID
    val tmdbId: Int, // The real numeric TMDB ID
    val title: String,
    val originalTitle: String,
    val overview: String,
    val posterPath: String,
    val backdropPath: String,
    val category: String, // Arabic Movies, Foreign Movies, Arabic Series, Foreign Series, Anime
    val genres: List<String>,
    val rating: Double,
    val releaseDate: String,
    val isTvShow: Boolean,
    val isArabic: Boolean,
    val defaultSeason: Int = 1,
    val totalSeasons: Int = 1,
    val episodesPerSeason: List<Int> = listOf(10) // list of episode count per season
) : Serializable

data class Genre(
    val id: Int,
    val name: String,
    val isTvGenre: Boolean = false
)

data class StreamingServer(
    val name: String,
    val baseUrl: String,
    val isMoviePattern: String, // e.g. "{baseUrl}/movie/{id}"
    val isTvPattern: String // e.g. "{baseUrl}/tv/{id}/{season}/{episode}"
) {
    fun getMovieUrl(tmdbId: Int): String {
        val sanitizedBase = if (baseUrl.contains("0.0.1.44") || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1") || baseUrl.contains("10.0.2.2") || baseUrl.contains("0.0.0.0")) {
            "https://vidsrc.to/embed"
        } else {
            baseUrl
        }
        return isMoviePattern
            .replace("{baseUrl}", sanitizedBase)
            .replace("{id}", tmdbId.toString())
    }

    fun getTvUrl(tmdbId: Int, season: Int, episode: Int): String {
        val sanitizedBase = if (baseUrl.contains("0.0.1.44") || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1") || baseUrl.contains("10.0.2.2") || baseUrl.contains("0.0.0.0")) {
            "https://vidsrc.to/embed"
        } else {
            baseUrl
        }
        return isTvPattern
            .replace("{baseUrl}", sanitizedBase)
            .replace("{id}", tmdbId.toString())
            .replace("{season}", season.toString())
            .replace("{episode}", episode.toString())
    }
}
