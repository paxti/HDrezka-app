package com.paxti.hdrezkaapp.presenters

import android.util.ArrayMap
import com.paxti.hdrezkaapp.constants.AppliedFilter
import com.paxti.hdrezkaapp.models.CategoriesModel
import com.paxti.hdrezkaapp.objects.Category
import com.paxti.hdrezkaapp.objects.Film
import com.paxti.hdrezkaapp.utils.ExceptionHelper.catchException
import com.paxti.hdrezkaapp.views.viewsInterface.CategoriesView
import com.paxti.hdrezkaapp.views.viewsInterface.FilmsListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesPresenter(private val categoriesView: CategoriesView, private val filmsListView: FilmsListView) : FilmsListPresenter.IFilmsList {
    var filmsListPresenter: FilmsListPresenter = FilmsListPresenter(filmsListView, categoriesView, this)

    private var currentPage = 1
    private var link = ""

    var category: Category? = null
    private var appliedFilters: ArrayMap<AppliedFilter, String> = ArrayMap()
    private var selectedTypePos = 3

    init {
        filmsListView.setFilms(filmsListPresenter.activeFilms)
    }

    fun initCategories() {
        GlobalScope.launch {
            try {
                category = CategoriesModel.getCategories()

                withContext(Dispatchers.Main) {
                    categoriesView.showFilters()
                }
            } catch (e: Exception) {
                catchException(e, categoriesView)
                return@launch
            }
        }
    }

    fun setFilter(type: AppliedFilter, pos: Int) {
        if (category != null && category!!.categories.size > 0) {
            when (type) {
                AppliedFilter.TYPE -> {
                    appliedFilters.remove(AppliedFilter.GENRES)
                    //appliedFilters.remove(AppliedFilter.YEARS)
                    appliedFilters[type] = category!!.categories.keyAt(pos).second
                    selectedTypePos = pos

                    val genres: ArrayList<String> = ArrayList()
                    for (genre in category!!.categories.valueAt(selectedTypePos)) {
                        genres.add(genre.first)
                    }
                    categoriesView.setFilters(category!!.years, genres)
                }
                AppliedFilter.GENRES -> appliedFilters[type] = category!!.categories.valueAt(selectedTypePos)[pos].second
                AppliedFilter.YEARS -> appliedFilters[type] = category!!.years[pos]
            }
        }
    }

    fun applyFilters() {
        link = ""

        link += appliedFilters[AppliedFilter.TYPE] + "best/"

        if (appliedFilters[AppliedFilter.GENRES]?.isNotEmpty() == true) {
            link = appliedFilters[AppliedFilter.GENRES]!!
        }

        if (appliedFilters[AppliedFilter.YEARS]?.isNotEmpty() == true) {
            val year = appliedFilters[AppliedFilter.YEARS]!!

            if (year != "за все время") {
                link += "$year/"
            }
        }

        updateCategories()
    }


    private fun updateCategories() {
        if (link.isNotEmpty()) {
            currentPage = 1
            filmsListPresenter.reset()
            filmsListPresenter.filmList.clear()
            categoriesView.showList()
            filmsListPresenter.setToken(link)
            filmsListPresenter.getNextFilms()
        }
    }

    override fun getMoreFilms(): ArrayList<Film> {
        val films = CategoriesModel.getFilmsFromCategory(link, currentPage)
        currentPage++

        return films
    }
}