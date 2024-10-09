package com.meetfriend.app.utils.penview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var drawPath: Path = Path()
    private var drawPaint: Paint = Paint()
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null

    private val paths = mutableListOf<Pair<Path, Paint>>()
    private val undonePaths = mutableListOf<Pair<Path, Paint>>()

    private var isDrawingEnabled = false
    private var isDrawn = false // Flag to track if any drawing has occurred

    private var drawListener: DrawListener? = null // Listener for draw events

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.color = Color.BLACK
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 10f
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
    }

    fun enableDrawing() {
        isDrawingEnabled = true
    }

    fun disableDrawing() {
        isDrawingEnabled = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)
        for ((path, paint) in paths) {
            canvas.drawPath(path, paint)
        }
        canvas.drawPath(drawPath, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(touchX, touchY)
                undonePaths.clear()
                drawListener?.onDrawStart()
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                val newPaint = Paint(drawPaint)
                paths.add(Pair(Path(drawPath), newPaint))
                isDrawn = true // Set the flag to true when a path is added
                drawPath.reset()
                drawListener?.onDrawEnd()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setColor(newColor: Int) {
        drawPaint.color = newColor
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            undonePaths.add(paths.removeAt(paths.size - 1))
            if (paths.isEmpty()) {
                isDrawn = false // Reset the flag if no paths remain
            }
            invalidate()
        }
    }
    // Method to check if any drawing has occurred
    fun hasDrawingOccurred(): Boolean {
        return isDrawn
    }

    fun setDrawListener(listener: DrawListener) {
        this.drawListener = listener
    }

    // Interface for draw events
    interface DrawListener {
        fun onDrawStart()
        fun onDrawEnd()
    }
    fun saveDrawing(): Bitmap? {
        return canvasBitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }
    fun restoreDrawing(bitmap: Bitmap?) {
        if (bitmap != null) {
            canvasBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            drawCanvas = Canvas(canvasBitmap!!)
            invalidate()
        }
    }
}