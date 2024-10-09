package com.meetfriend.app.ui.camerakit.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar

class CustomSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SeekBar(context, attrs, defStyleAttr) {

    init {
        thumb = ThumbDrawable(context)
        setPadding(0, 0, 0, 0);
    }

    override fun invalidate() {
        super.invalidate()
        if (thumb is ThumbDrawable) (thumb as ThumbDrawable).progress = progress

    }



}