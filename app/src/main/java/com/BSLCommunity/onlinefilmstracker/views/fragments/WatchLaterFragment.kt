package com.BSLCommunity.onlinefilmstracker.views.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.BSLCommunity.onlinefilmstracker.R
import com.BSLCommunity.onlinefilmstracker.models.UserModel
import com.BSLCommunity.onlinefilmstracker.objects.Film
import com.BSLCommunity.onlinefilmstracker.objects.WatchLater
import com.BSLCommunity.onlinefilmstracker.presenters.WatchLaterPresenter
import com.BSLCommunity.onlinefilmstracker.utils.FragmentOpener
import com.BSLCommunity.onlinefilmstracker.views.OnFragmentInteractionListener
import com.BSLCommunity.onlinefilmstracker.viewsInterface.WatchLaterView


class WatchLaterFragment : Fragment(), WatchLaterView {
    private lateinit var currentView: View
    private lateinit var listView: RecyclerView
    private lateinit var watchLaterPresenter: WatchLaterPresenter
    private lateinit var fragmentListener: OnFragmentInteractionListener
    private lateinit var progressBar: ProgressBar
    private lateinit var msgView: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentListener = context as OnFragmentInteractionListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(R.layout.fragment_watch_later_list, container, false)
        Log.d("FRAGMENT_TEST", "watch init")

        listView = currentView.findViewById(R.id.fragment_watch_later_list_rv)
        progressBar = currentView.findViewById(R.id.fragment_watch_later_list_pb_loading)
        msgView = currentView.findViewById<TextView>(R.id.fragment_watch_later_list_tv_msg)

        if (UserModel.isLoggedIn == true) {
            listView.layoutManager = LinearLayoutManager(context)
            watchLaterPresenter = WatchLaterPresenter(this)
            watchLaterPresenter.initList()
        } else {
            setDownloadingFailed("Данный раздел доступен только зарегистрированным пользователям")
        }

        return currentView
    }

    override fun setWatchLaterList(list: ArrayList<WatchLater>) {
        if (list.size > 0) {
            listView.adapter = WatchLaterRecyclerViewAdapter(list, ::listCallback)
            progressBar.visibility = View.GONE
        } else {
            setDownloadingFailed("В этот раздел попадают все просматриваемые тобой сериалы")
        }
    }

    private fun listCallback(film: Film) {
        FragmentOpener.openFilm(film, this, fragmentListener)
    }

    private fun setDownloadingFailed(msg: String) {
        msgView.text = msg
        msgView.visibility = View.VISIBLE
        listView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }
}