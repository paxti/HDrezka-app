package com.paxti.hdrezkaapp.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paxti.hdrezkaapp.db.WatchLaterDAO
import com.paxti.hdrezkaapp.db.WatchLaterEntity

@Database(entities = [WatchLaterEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchLaterDAO(): WatchLaterDAO?

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE === null) {
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase::class.java, "local_history")
                    .build()
            }
            return INSTANCE!!
        }
    }
}