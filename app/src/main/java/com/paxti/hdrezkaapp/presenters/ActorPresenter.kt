package com.paxti.hdrezkaapp.presenters

import com.paxti.hdrezkaapp.models.ActorModel
import com.paxti.hdrezkaapp.objects.Actor
import com.paxti.hdrezkaapp.utils.ExceptionHelper
import com.paxti.hdrezkaapp.views.viewsInterface.ActorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException

class ActorPresenter(
    private val actorView: ActorView,
    private var actor: Actor
) {
    fun initActorData() {
        GlobalScope.launch {
            try {
                if (!actor.hasMainData) {
                    ActorModel.getActorMainInfo(actor)
                }

                ActorModel.getActorFilms(actor)

                actor.personCareerFilms?.let {
                    withContext(Dispatchers.Main) {
                        actorView.setBaseInfo(actor)
                        actorView.setCareersList(it)
                    }
                }
            } catch (e: Exception) {
                if (e is HttpStatusException) {
                    if (e.statusCode == 503) {
                        //actorView.showMsg(IConnection.ErrorType.PARSING_ERROR)
                    } else {
                        ExceptionHelper.catchException(e, actorView)
                    }
                } else {
                    ExceptionHelper.catchException(e, actorView)
                }
            }
        }
    }
}