package com.meetfriend.app.ui.camerakit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.meetfriend.app.R

class CaptureButtonWithProgress(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var progressColor: Int = Color.RED
    private var progressWidth: Float = 8f
    private var progress = 0

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CaptureButtonWithProgress,
            0, 0).apply {

            try {
                progressColor = getColor(R.styleable.CaptureButtonWithProgress_progressColor, progressColor)
                progressWidth = getDimension(R.styleable.CaptureButtonWithProgress_progressWidth, progressWidth)
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the outer circle for the button
        paint.color = Color.BLACK
        paint.strokeWidth = progressWidth
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.isAntiAlias = true

        val radius = (width / 2).toFloat()
        val centerX = (width / 2).toFloat()
        val centerY = (height / 2).toFloat()

        canvas.drawCircle(centerX, centerY, radius - progressWidth / 2, paint)

        // Draw the progress arc
        paint.color = progressColor
        val sweepAngle = 360 * progress / 100f
        canvas.drawArc(
            progressWidth / 2,
            progressWidth / 2,
            width - progressWidth / 2,
            height - progressWidth / 2,
            -90f,
            sweepAngle,
            false,
            paint
        )
    }
}