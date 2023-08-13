package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.interfaces.IConnection
import com.paxti.hdrezkaapp.interfaces.IMsg

interface BookmarksView : IMsg, IConnection {
    fun setBookmarksSpinner(bookmarksNames: ArrayList<String>)

    fun setNoBookmarks()
}