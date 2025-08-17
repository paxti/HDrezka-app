package com.paxti.hdrezkaapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paxti.hdrezkaapp.objects.Bookmark

@Entity
data class BookmarkEntity (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "catalogId") val catId: String,
    @ColumnInfo(name = "amount") val amount: Int?,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "filmLInk") val filmLInk: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "info") val info: String,
    @ColumnInfo(name = "additionalInfo") val additionalInfo: String?,
    @ColumnInfo(name = "posterPath") val posterPath: String,
    @ColumnInfo(name = "isChecked") val isChecked: Boolean?
)

fun BookmarkEntity.toBookmark() = Bookmark(
    catId = id,
    name = name,
    amount = 1,
    link = filmLInk,
)
