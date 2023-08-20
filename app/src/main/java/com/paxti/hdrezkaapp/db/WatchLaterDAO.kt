package com.paxti.hdrezkaapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface WatchLaterDAO {
    @Query("SELECT * FROM watchLaterEntity ORDER BY date DESC")
    fun getAll(): List<WatchLaterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWatchLater(vararg users: WatchLaterEntity)

    @Query("DELETE FROM watchLaterEntity WHERE id = :id")
    fun removeFromWatchLater(vararg id: String)
}