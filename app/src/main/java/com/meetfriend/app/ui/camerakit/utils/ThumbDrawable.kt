package com.meetfriend.app.ui.camerakit.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.meetfriend.app.R
import timber.log.Timber


class ThumbDrawable(context: Context) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect()

    private var shadowColor = context.resources.getColor(R.color.color_text_black,null)
    private val size = 20.toFloat()
    private val textSize = 40f
    var progress: Int = 0

    init {
        val accentColor = context.resources.getColor(R.color.color_text_black,null)
        paint.color = accentColor
        textPaint.color = accentColor
        textPaint.textSize = textSize
        paint.setShadowLayer(size / 2, 0f, 0f, shadowColor)
    }

    override fun draw(canvas: Canvas) {
        val minutes = progress / 60
        val seconds = progress % 60
        val time = String.format("%02d:%02d", minutes, seconds)
        val progressAsString = time.toString()

        canvas.drawRect(bounds.left.toFloat()+40, bounds.top.toFloat()+20, (bounds.left.toFloat()) + 40 ,  bounds.top.toFloat()+40,paint)


        textPaint.getTextBounds(progressAsString, 0, progressAsString.length, textBounds)
        canvas.drawText(progressAsString, bounds.left.toFloat() - textBounds.width() * 0.6f, bounds.top.toFloat() - size * 5, textPaint)
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

}