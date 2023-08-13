package com.paxti.hdrezkaapp.interfaces

import com.paxti.hdrezkaapp.constants.UpdateItem

interface IPagerView {
    fun updatePager()

    fun redrawPage(item: UpdateItem)
}