package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String, // tmdbId for movies, tmdbId_tv for series
    val tmdbId: Int,
    val title: String,
    val posterPath: String,
    val category: String,
    val isTvShow: Boolean,
    val isWatchlist: Boolean = false,
    val lastWatchedSeason: Int = 1,
    val lastWatchedEpisode: Int = 1,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
)
