package com.paxti.hdrezkaapp.views.tv.grid

import android.content.Context
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Presenter
import com.paxti.hdrezkaapp.R
import com.paxti.hdrezkaapp.objects.Film
import java.lang.RuntimeException

class CardPresenterSelector(private val mContext: Context) : PresenterSelector() {
    override fun getPresenter(item: Any): Presenter {
        if (item !is Film) throw RuntimeException(String.format("The PresenterSelector only supports data items of type '%s'", Film::class.java.getName()))
        return VideoCardViewPresenter(mContext, R.style.VideoGridCardTheme)
    }
}