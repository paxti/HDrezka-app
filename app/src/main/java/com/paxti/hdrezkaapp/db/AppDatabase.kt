package com.paxti.hdrezkaapp.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paxti.hdrezkaapp.db.BookmarkEntity
import com.paxti.hdrezkaapp.db.BookmarksDAO
import com.paxti.hdrezkaapp.db.WatchLaterDAO
import com.paxti.hdrezkaapp.db.WatchLaterEntity

@Database(entities = [WatchLaterEntity::class, BookmarkEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchLaterDAO(): WatchLaterDAO?
    abstract fun bookmarkDAO(): BookmarksDAO?

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE === null) {
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase::class.java, "local_db_1")
                    .build()
            }
            return INSTANCE!!
        }

        fun clearDatabase(context: Context) {
            val db = getDatabase(context)
            try {
                db.bookmarkDAO()?.clearAll()
                db.watchLaterDAO()?.clearAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}