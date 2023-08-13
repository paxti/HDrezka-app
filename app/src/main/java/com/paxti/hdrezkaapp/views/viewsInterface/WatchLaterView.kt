package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.constants.AdapterAction
import com.paxti.hdrezkaapp.interfaces.IConnection
import com.paxti.hdrezkaapp.interfaces.IMsg
import com.paxti.hdrezkaapp.interfaces.IProgressState
import com.paxti.hdrezkaapp.objects.WatchLater

interface WatchLaterView : IMsg, IProgressState, IConnection {
    fun setWatchLaterList(list: ArrayList<WatchLater>)

    fun redrawWatchLaterList(from: Int, count: Int, action: AdapterAction)
}