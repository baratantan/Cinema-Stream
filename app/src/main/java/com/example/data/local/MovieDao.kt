package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE isWatchlist = 1 ORDER BY lastWatchedTimestamp DESC")
    fun getWatchlist(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE id = :id LIMIT 1")
    suspend fun getWatchHistoryById(id: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE id = :id LIMIT 1")
    fun observeWatchHistoryById(id: String): Flow<WatchHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(watchHistory: WatchHistoryEntity)

    @Query("UPDATE watch_history SET isWatchlist = :isWatchlist WHERE id = :id")
    suspend fun updateWatchlistStatus(id: String, isWatchlist: Boolean)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun delete(id: String)
}
