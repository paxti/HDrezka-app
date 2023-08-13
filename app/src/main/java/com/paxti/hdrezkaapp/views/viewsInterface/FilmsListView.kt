package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.constants.AdapterAction
import com.paxti.hdrezkaapp.interfaces.IProgressState
import com.paxti.hdrezkaapp.objects.Film

interface FilmsListView : IProgressState {
    fun setFilms(films: ArrayList<Film>)

    fun redrawFilms(from: Int, count: Int, action: AdapterAction, films: ArrayList<Film>)

    fun setCallView(cv: FilmListCallView)
}