package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.interfaces.IConnection

interface SearchView : IConnection {
    fun redrawSearchFilms(films: ArrayList<String>)
}