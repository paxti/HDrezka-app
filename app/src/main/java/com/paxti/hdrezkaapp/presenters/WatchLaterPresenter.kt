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
import com.paxti.hdrezkaapp.utils.UrlUtils
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
                    // Convert relative URL to complete URL if needed
                    val fullUrl = UrlUtils.buildFullUrl(item.filmLInk)
                    item.posterPath = FilmModel.getFilmPosterByLink(fullUrl)

                    if (index == dataToLoad.size - 1) {
                        withContext(Dispatchers.Main) {
                            val itemsCount = activeWatchLaterList.size
                            activeWatchLaterList.addAll(dataToLoad)
                            watchLaterView.redrawWatchLaterList(itemsCount, dataToLoad.size, AdapterAction.ADD)
                            watchLaterView.setProgressBarState(IProgressState.StateType.LOADED)
                        }
                    }
                } catch (e: Exception) {
                    // Log the error but don't crash the app
                    Log.e("WatchLaterPresenter", "Failed to load poster for: ${item.filmLInk}", e)
                    // Set a default poster path or leave it null
                    item.posterPath = null

                    if (index == dataToLoad.size - 1) {
                        withContext(Dispatchers.Main) {
                            val itemsCount = activeWatchLaterList.size
                            activeWatchLaterList.addAll(dataToLoad)
                            watchLaterView.redrawWatchLaterList(itemsCount, dataToLoad.size, AdapterAction.ADD)
                            watchLaterView.setProgressBarState(IProgressState.StateType.LOADED)
                        }
                    }
                }
            }
        }
    }

    fun updateList() {
        activeWatchLaterList.clear()
    }
}