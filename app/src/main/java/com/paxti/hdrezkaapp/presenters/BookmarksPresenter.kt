package com.paxti.hdrezkaapp.presenters

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paxti.hdrezkaapp.constants.AdapterAction
import com.paxti.hdrezkaapp.constants.BookmarkFilterType
import com.paxti.hdrezkaapp.db.BookmarkEntity
import com.paxti.hdrezkaapp.interfaces.IProgressState
import com.paxti.hdrezkaapp.models.AppDatabase
import com.paxti.hdrezkaapp.models.FilmModel
import com.paxti.hdrezkaapp.objects.Film
import com.paxti.hdrezkaapp.utils.ExceptionHelper.catchException
import com.paxti.hdrezkaapp.utils.UrlUtils
import com.paxti.hdrezkaapp.views.viewsInterface.BookmarksView
import com.paxti.hdrezkaapp.views.viewsInterface.FilmsListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksPresenter(private val bookmarksView: BookmarksView, private val filmsListView: FilmsListView): ViewModel() {
    val sortFilters: ArrayList<String> = ArrayList(arrayListOf("added", "year", "popular"))

    private var selectedSortFilter: String? = null
    private var selectedShowFilter: String? = null
    private var loadedFilms: MutableList<Film> = mutableListOf()
    private var bookmarks: MutableList<BookmarkEntity> = mutableListOf()
    private var isLoading: Boolean = false
    private lateinit var db: AppDatabase

    init {
        // Initialize RecyclerView with empty adapter to prevent "No adapter attached" error
        filmsListView.setFilms(loadedFilms)
    }

    fun initBookmarks(database: AppDatabase) {
        db = database // Store the database reference
        bookmarksView.setBookmarksSpinner(ArrayList())
        filmsListView.setProgressBarState(IProgressState.StateType.LOADING)

        // Clear any existing data first
        loadedFilms.clear()
        bookmarks.clear()

        viewModelScope.launch {
            try {
                // Only fetch bookmark metadata, don't load film data yet
                withContext(Dispatchers.IO) {
                    bookmarks = (database.bookmarkDAO()?.getAll() ?: emptyList()).toMutableList()
                    Log.d("BookmarksPresenter", "initBookmarks - Found ${bookmarks.size} bookmarks in database")
                }

                withContext(Dispatchers.Main) {
                    if (bookmarks.isNotEmpty()) {
                        // Don't load films here - let getNextFilms handle it
                        // Just initialize the adapter with empty list
                        filmsListView.setFilms(loadedFilms)
                        // Now load the first batch of films
                        getNextFilms()
                    } else {
                        bookmarksView.setNoBookmarks()
                        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
                    }
                }
            } catch (e: Exception) {
                Log.e("BookmarksPresenter", "Error in initBookmarks", e)
                catchException(e, bookmarksView)
            }
        }
    }

    private suspend fun fetchBookmark(database: AppDatabase) = withContext(Dispatchers.IO) {
        // This method is no longer used - keeping for compatibility
        // All bookmark loading is now handled by initBookmarks + getNextFilms
    }

    private fun reset() {
        loadedFilms.clear()
        filmsListView.redrawFilms(0, loadedFilms.size, AdapterAction.DELETE, ArrayList())
    }


    fun getNextFilms(filter: String = sortFilters[0], database: AppDatabase? = null) {
        if (isLoading) {
            Log.d("BookmarksPresenter", "getNextFilms called but already loading, skipping")
            return
        }

        Log.d("BookmarksPresenter", "getNextFilms called - loadedFilms.size: ${loadedFilms.size}, bookmarks.size: ${bookmarks.size}")

        // Check if we have more bookmarks to load
        if (bookmarks.isNotEmpty() && loadedFilms.size < bookmarks.size) {
            isLoading = true
            Log.d("BookmarksPresenter", "Loading next batch of films from ${loadedFilms.size} to ${(loadedFilms.size + 10).coerceAtMost(bookmarks.size)}")

            try {
                bookmarksView.hideMsg()
                filmsListView.setProgressBarState(IProgressState.StateType.LOADING)
                viewModelScope.launch {
                    val start = loadedFilms.size
                    val end = (start + 10).coerceAtMost(bookmarks.size) // Load 10 at a time
                    val newFilms = mutableListOf<Film>()

                    withContext(Dispatchers.IO) {
                        for (i in start until end) {
                            // Build full URL using current provider
                            val fullUrl = UrlUtils.buildFullUrl(bookmarks[i].filmLInk)
                            val film = Film(fullUrl)

                            // Also set the poster path using current provider
                            film.posterPath = UrlUtils.buildFullPosterUrl(bookmarks[i].posterPath)

                            try {
                                newFilms.add(FilmModel.getMainData(film))
                            } catch (e: Exception) {
                                // Log and skip this bookmark if it fails to load
                                Log.e("BookmarksPresenter", "Failed to load bookmark: ${bookmarks[i].filmLInk}", e)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (newFilms.isNotEmpty()) {
                            // Add the new films to existing list
                            val startIndex = loadedFilms.size
                            loadedFilms.addAll(newFilms)
                            filmsListView.redrawFilms(startIndex, newFilms.size, AdapterAction.ADD, newFilms)
                            Log.d("BookmarksPresenter", "Added ${newFilms.size} new films, total now: ${loadedFilms.size}")
                        }
                        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                catchException(e, bookmarksView)
                isLoading = false
                return
            }
        } else if (bookmarks.isEmpty() && database != null) {
            // Only reinitialize if we don't have bookmarks loaded yet
            Log.d("BookmarksPresenter", "No bookmarks loaded, initializing...")
            initBookmarks(database)
        } else {
            // No more films to load
            Log.d("BookmarksPresenter", "No more films to load - loadedFilms: ${loadedFilms.size}, bookmarks: ${bookmarks.size}")
            filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
            isLoading = false
        }
    }

    private fun addFilms(films: MutableList<Film>) {
        // This method is now only used for initial loading in some cases
        isLoading = false
        filmsListView.setFilms(films)
        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
    }

    fun setFilter(filter: String, type: BookmarkFilterType) {
        when (type) {
            BookmarkFilterType.SORT -> selectedSortFilter = filter
            BookmarkFilterType.SHOW -> selectedShowFilter = filter
        }

        reset()
        getNextFilms(filter)
    }


    fun redrawBookmarks() {
        reset()
        bookmarks.clear();
        initBookmarks(db)
    }

    fun redrawBookmarksFilms() {
        Log.d("BookmarksPresenter", "redrawBookmarksFilms called")
        reset()
        // Don't call getNextFilms() here - just reset the UI
        // The films will be loaded when needed via scroll triggers
        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
    }

    fun clearAllBookmarks() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.bookmarkDAO()?.clearAll()
                }
                // Clear local data and update UI
                bookmarks.clear()
                loadedFilms.clear()
                withContext(Dispatchers.Main) {
                    filmsListView.redrawFilms(0, loadedFilms.size, AdapterAction.DELETE, ArrayList())
                    bookmarksView.setNoBookmarks()
                    filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
                }
                Log.d("BookmarksPresenter", "All bookmarks cleared successfully")
            } catch (e: Exception) {
                Log.e("BookmarksPresenter", "Failed to clear bookmarks", e)
                catchException(e, bookmarksView)
            }
        }
    }
}