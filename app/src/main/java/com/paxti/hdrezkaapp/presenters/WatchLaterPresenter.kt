package com.paxti.hdrezkaapp.presenters

import android.util.Log
import com.paxti.hdrezkaapp.constants.AdapterAction
import com.paxti.hdrezkaapp.interfaces.IMsg
import com.paxti.hdrezkaapp.interfaces.IProgressState
import com.paxti.hdrezkaapp.models.FilmModel
import com.paxti.hdrezkaapp.db.toWatchLater
import com.paxti.hdrezkaapp.models.AppDatabase
import com.paxti.hdrezkaapp.objects.WatchLater
import com.paxti.hdrezkaapp.utils.ExceptionHelper.catchException
import com.paxti.hdrezkaapp.views.viewsInterface.WatchLaterView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchLaterPresenter(private val watchLaterView: WatchLaterView) {
    private var loadedWatchLaterList: ArrayList<WatchLater> = ArrayList()
    private var activeWatchLaterList: ArrayList<WatchLater> = ArrayList()
    private val ITEMS_PER_PAGE = 9
    private lateinit var db: AppDatabase

    fun initList(database: AppDatabase) {
        GlobalScope.launch {
            try {
                db = database;
                var data = db.watchLaterDAO()?.getAll()
                Log.d("TAG", data?.size.toString())
                if (data != null) {
                    loadedWatchLaterList = ArrayList(data.map { it.toWatchLater() })
                }


                withContext(Dispatchers.Main) {
                    activeWatchLaterList.clear()

                    if (loadedWatchLaterList.size > 0) {
                        watchLaterView.setWatchLaterList(activeWatchLaterList)
                        getNextWatchLater()
                    } else {
                        watchLaterView.showMsg(IMsg.MsgType.NOTHING_FOUND)
                    }
                }
            } catch (e: Exception) {
                catchException(e, watchLaterView)
                return@launch
            }
        }
    }

    fun getNextWatchLater() {
        watchLaterView.setProgressBarState(IProgressState.StateType.LOADING)

        val dataToLoad: ArrayList<WatchLater> = ArrayList()
        if (loadedWatchLaterList.size > 0) {
            for ((index, item) in (loadedWatchLaterList.clone() as ArrayList<WatchLater>).withIndex()) {
                dataToLoad.add(item)
                loadedWatchLaterList.removeAt(0)

                if (index == ITEMS_PER_PAGE - 1) {
                    break
                }
            }

            loadFilmData(dataToLoad)
        } else {
            watchLaterView.setProgressBarState(IProgressState.StateType.LOADED)
        }
    }

    private fun loadFilmData(dataToLoad: ArrayList<WatchLater>) {
        for ((index, item) in dataToLoad.withIndex()) {
            GlobalScope.launch {
                try {
                    item.posterPath = FilmModel.getFilmPosterByLink(item.filmLInk)

                    if (index == dataToLoad.size - 1) {
                        withContext(Dispatchers.Main) {
                            val itemsCount = activeWatchLaterList.size
                            activeWatchLaterList.addAll(dataToLoad)
                            watchLaterView.redrawWatchLaterList(itemsCount, dataToLoad.size, AdapterAction.ADD)
                            watchLaterView.setProgressBarState(IProgressState.StateType.LOADED)
                        }
                    }
                } catch (e: Exception) {
                    catchException(e, watchLaterView)
                    return@launch
                }
            }
        }
    }

    fun updateList() {
        activeWatchLaterList.clear()
    }

    fun deleteWatchLaterItem(id: String) {
        GlobalScope.launch {
            try {
                db.watchLaterDAO()?.removeFromWatchLater(id);
                withContext(Dispatchers.Main) {
                    val onItemsDownload = 4

                    if (activeWatchLaterList.size <= onItemsDownload) {
                        getNextWatchLater()
                    }

                }
            } catch (e: Exception) {
                catchException(e, watchLaterView)
                return@launch
            }
        }
    }
}