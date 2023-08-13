package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.constants.HintType
import com.paxti.hdrezkaapp.interfaces.IConnection
import com.paxti.hdrezkaapp.objects.Film
import com.paxti.hdrezkaapp.objects.SeriesUpdateItem

interface SeriesUpdatesView: IConnection {
    fun setList(films: LinkedHashMap<String, ArrayList<Film>>)

    fun setHint(hintType: HintType)

    fun updateDialog(seriesUpdates: LinkedHashMap<String, ArrayList<SeriesUpdateItem>>)

    fun resetBadge()
}