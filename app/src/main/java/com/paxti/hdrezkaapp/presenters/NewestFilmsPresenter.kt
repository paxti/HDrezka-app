package com.paxti.hdrezkaapp.presenters

import android.util.ArrayMap
import com.paxti.hdrezkaapp.constants.AppliedFilter
import com.paxti.hdrezkaapp.models.NewestFilmsModel
import com.paxti.hdrezkaapp.objects.Film
import com.paxti.hdrezkaapp.objects.SettingsData
import com.paxti.hdrezkaapp.utils.ExceptionHelper
import com.paxti.hdrezkaapp.views.viewsInterface.FilmsListView
import com.paxti.hdrezkaapp.views.viewsInterface.NewestFilmsView

class NewestFilmsPresenter(
    private val newestFilmsView: NewestFilmsView,
    private val filmsListView: FilmsListView
) : FilmsListPresenter.IFilmsList {
    var filmsListPresenter: FilmsListPresenter = FilmsListPresenter(filmsListView, newestFilmsView, this)
    private var appliedFilters: ArrayMap<AppliedFilter, String> = ArrayMap()
    private var currentPage: Int = 1 // newest film page

    fun initFilms() {
        SettingsData.defaultSort?.let {
            appliedFilters[AppliedFilter.SORT] = NewestFilmsModel.SORTS[it]
        }
        appliedFilters[AppliedFilter.TYPE] = NewestFilmsModel.TYPES[0]
        filmsListView.setFilms(filmsListPresenter.activeFilms)
        filmsListPresenter.getNextFilms()
    }

    override fun getMoreFilms(): ArrayList<Film> {
        return try {
            var films: ArrayList<Film> = ArrayList()

            if (appliedFilters[AppliedFilter.SORT]!! == NewestFilmsModel.SORTS[0]) {
                if (currentPage == 1) {
                    // films from slider
                    films = NewestFilmsModel.getNewFilms(appliedFilters[AppliedFilter.TYPE]!!)
                }
            } else if (appliedFilters[AppliedFilter.SORT]!! == NewestFilmsModel.SORTS[5]) {
                films = NewestFilmsModel.getFreshFilms(currentPage, appliedFilters[AppliedFilter.TYPE]!!)
            } else {
                films = NewestFilmsModel.getNewestFilms(currentPage, appliedFilters[AppliedFilter.SORT]!!, appliedFilters[AppliedFilter.TYPE]!!)
            }

            currentPage++
            films
        } catch (e: Exception) {
            ExceptionHelper.catchException(e, newestFilmsView)
            ArrayList()
        }
    }

    fun setFilter(type: AppliedFilter, pos: Int) {
        appliedFilters[type] = when (type) {
            AppliedFilter.TYPE -> NewestFilmsModel.TYPES[pos]
            AppliedFilter.SORT -> NewestFilmsModel.SORTS[pos]
            else -> ""
        }
    }

    fun applyFilters() {
        filmsListPresenter.reset()
        filmsListPresenter.filmList.clear()
        currentPage = 1
        filmsListPresenter.getNextFilms()
    }
}