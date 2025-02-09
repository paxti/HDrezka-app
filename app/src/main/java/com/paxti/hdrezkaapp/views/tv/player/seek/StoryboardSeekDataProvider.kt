package com.paxti.hdrezkaapp.views.tv.player.seek

import android.content.Context
import android.graphics.Bitmap
import androidx.leanback.widget.PlaybackSeekDataProvider
import com.paxti.hdrezkaapp.objects.Voice

class StoryboardSeekDataProvider(translation: Voice, context: Context) : PlaybackSeekDataProvider() {
    private var mStoryboardManager: StoryboardManager? = null

    init {
        mStoryboardManager = StoryboardManager(context)
        translation.thumbnails?.let { mStoryboardManager?.setThumbnails(it) }
    }

    override fun getSeekPositions(): LongArray? {
        return mStoryboardManager?.getSeekPositions()
    }

    override fun getThumbnail(index: Int, callback: ResultCallback) {
        fun ready(bitmap: Bitmap?){
            callback.onThumbnailLoaded(bitmap, index)
        }

        mStoryboardManager?.getBitmap(index, ::ready)
    }
/*
    companion object{
        fun setSeekProvider(translation: Voice, glue: VideoPlayerGlue, context: Context) {
            if (glue.isPrepared) {
                glue.seekProvider = StoryboardSeekDataProvider(translation, context)
            } else {
                glue.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
                    val mGlue = glue

                    override fun onPreparedStateChanged(glue: PlaybackGlue) {
                        if (mGlue.isPrepared) {
                            mGlue.removePlayerCallback(this)
                            mGlue.seekProvider = StoryboardSeekDataProvider(translation, mGlue.context)
                        }
                    }
                })
            }
        }
    }*/
}
