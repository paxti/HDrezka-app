package com.paxti.hdrezkaapp.views.viewsInterface

import com.paxti.hdrezkaapp.interfaces.IConnection

interface CategoriesView : IConnection{
    fun showList()

    fun showFilters()

    fun setFilters(years: ArrayList<String>, genres: ArrayList<String>)
}