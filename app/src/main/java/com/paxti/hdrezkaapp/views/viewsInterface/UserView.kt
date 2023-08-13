package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.interfaces.IConnection

interface UserView : IConnection {
    fun showError(text: String)

    fun setUserAvatar()

    fun completeAuth()
}