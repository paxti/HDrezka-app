package com.paxti.hdrezkaapp.objects

data class WatchLater(
    var id: String,
    val date: String,
    val filmLInk: String,
    val name: String,
    val info: String,
    val additionalInfo: String
) {
    var posterPath: String? = null
}