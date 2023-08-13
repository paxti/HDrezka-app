package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.interfaces.IConnection
import com.paxti.hdrezkaapp.objects.Actor
import com.paxti.hdrezkaapp.objects.Film

interface ActorView : IConnection {
    fun setBaseInfo(actor: Actor)

    fun setCareersList(careers: ArrayList<Pair<String, ArrayList<Film>>>)
}