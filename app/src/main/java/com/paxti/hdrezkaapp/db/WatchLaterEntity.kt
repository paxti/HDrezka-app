package com.paxti.hdrezkaapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paxti.hdrezkaapp.objects.WatchLater

@Entity
data class WatchLaterEntity (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "filmLInk") val filmLInk: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "info") val info: String?,
    @ColumnInfo(name = "additionalInfo") val additionalInfo: String?,
    @ColumnInfo(name = "posterPath") val posterPath: String
)

fun WatchLaterEntity.toWatchLater() = WatchLater(
    id = id,
    date = date,
    filmLInk = filmLInk,
    name = name,
    info = info,
    additionalInfo = additionalInfo ?: "",
)