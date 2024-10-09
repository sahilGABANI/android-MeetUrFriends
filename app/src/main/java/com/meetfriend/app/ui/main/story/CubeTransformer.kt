package com.meetfriend.app.ui.main.story

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class CubeTransformer : ViewPager2.PageTransformer {
    companion object {
        const val MAX_ROTATION_Y = 45F
        const val DELTA_Y = 0.5F
    }
    private val maxRotationY = MAX_ROTATION_Y
    override fun transformPage(view: View, position: Float) {
        val deltaY = DELTA_Y
        val rotationAngle = maxRotationY * position
        view.pivotX = if (position < 0F) view.width.toFloat() else 0F
        view.pivotY = view.height * deltaY
        view.rotationY = rotationAngle
    }
}