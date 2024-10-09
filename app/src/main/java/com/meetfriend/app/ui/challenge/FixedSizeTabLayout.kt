package com.meetfriend.app.ui.challenge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import com.google.android.material.tabs.TabLayout
import com.meetfriend.app.R

class FixedSizeTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {
    init {
        // Hide the system tab indicator by setting a transparent drawable
        this.setSelectedTabIndicator(R.drawable.new_tab_layout_background)
    }
    private val indicatorWidth = context.resources.getDimensionPixelSize(R.dimen.tab_indicator_width)
    private val indicatorHeight = context.resources.getDimensionPixelSize(R.dimen.tab_indicator_height)
    private val indicatorDrawable: Drawable? = context.getDrawable(R.drawable.tab_indicator_white)
    private val rect = Rect()

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val tabView = (getChildAt(0) as? LinearLayout)?.getChildAt(selectedTabPosition)
        tabView?.let {
            val left = it.left + (it.width - indicatorWidth) / 2
            val right = left + indicatorWidth

            rect.left = left
            rect.right = right
            rect.top = height - indicatorHeight // Position at the bottom of the TabLayout
            rect.bottom = height

            indicatorDrawable?.bounds = rect
            indicatorDrawable?.draw(canvas)
        }
    }
}