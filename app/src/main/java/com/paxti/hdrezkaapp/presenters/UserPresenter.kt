package com.paxti.hdrezkaapp.presenters

import android.content.Context
import com.paxti.hdrezkaapp.models.UserModel
import com.paxti.hdrezkaapp.objects.UserData
import com.paxti.hdrezkaapp.utils.ExceptionHelper.catchException
import com.paxti.hdrezkaapp.views.viewsInterface.UserView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPresenter(private val userView: UserView, private val context: Context) {
    fun getUserAvatar() {
        GlobalScope.launch {
            try {
                UserData.setAvatar(UserModel.getUserAvatarLink(), context)

                withContext(Dispatchers.Main) {
                    userView.setUserAvatar()
                }
            } catch (e: Exception) {
                catchException(e, userView)
            }
        }
    }

    fun login(name: String, password: String) {
        GlobalScope.launch {
            try {
                UserModel.login(name, password, context)

                withContext(Dispatchers.Main) {
                    UserData.setLoggedIn(context)
                    getUserAvatar()
                    userView.completeAuth()
                }
            } catch (e: Exception) {
                // catchException(e, userView)
                withContext(Dispatchers.Main) {
                    e.message?.let { userView.showError(it) }
                }
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        GlobalScope.launch {
            try {
                UserModel.register(email, username, password, context)

                withContext(Dispatchers.Main) {
                    UserData.setLoggedIn(context)
                    getUserAvatar()
                    userView.completeAuth()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.message?.let { userView.showError(it) }
                }
            }
        }
    }

    fun exit() {
        UserData.reset(context)
        userView.setUserAvatar()
    }
}