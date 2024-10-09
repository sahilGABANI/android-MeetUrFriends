package com.meetfriend.app.ui.chatRoom.create.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.meetfriend.app.R
import com.meetfriend.app.api.subscription.model.TempPlanInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewPlanItemBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlanItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val planItemClickSubject: PublishSubject<TempPlanInfo> = PublishSubject.create()
    val planItemClicks: Observable<TempPlanInfo> = planItemClickSubject.hide()

    private lateinit var binding: ViewPlanItemBinding
    private lateinit var tempPlanInfo: TempPlanInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_plan_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewPlanItemBinding.bind(view)

        binding.apply {
            llPlanContainer.throttleClicks().subscribeAndObserveOnMainThread {
                planItemClickSubject.onNext(tempPlanInfo)
            }
        }
    }

    fun bind(tempPlanInfo: TempPlanInfo, isSelected: Boolean) {
        this.tempPlanInfo = tempPlanInfo

        binding.tvRoomType.text = tempPlanInfo.roomType
        binding.tvAmount.text = tempPlanInfo.amount
        binding.tvDuration.text = tempPlanInfo.duration

        if (isSelected) {
            binding.llPlanContainer.background = ContextCompat.getDrawable(
                context,
                R.drawable.selected_plan_bg
            )
        } else {
            binding.llPlanContainer.background = ContextCompat.getDrawable(
                context,
                R.drawable.bg_edittext
            )
        }
    }
}
