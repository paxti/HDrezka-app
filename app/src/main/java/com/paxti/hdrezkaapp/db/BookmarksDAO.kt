package com.paxti.hdrezkaapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarksDAO {
    @Query("SELECT * FROM bookmarkEntity ORDER BY date DESC")
    fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarkEntity WHERE id = :id")
    fun remove(vararg id: String)

    @Query("SELECT EXISTS(SELECT * FROM bookmarkEntity WHERE id = :id)")
    fun isInList(vararg id: String): Boolean

    @Query("DELETE FROM bookmarkEntity")
    fun clearAll()
}