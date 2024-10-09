package com.meetfriend.app.ui.livestreaming.videoroom.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewLiveRoomBinding
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveRoomView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val roomClickSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val roomClicks: Observable<LiveEventInfo> = roomClickSubject.hide()

    private lateinit var binding: ViewLiveRoomBinding
    private lateinit var liveEventInfo: LiveEventInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_room, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveRoomBinding.bind(view)

        binding.apply {
            liveUserImage.throttleClicks().subscribeAndObserveOnMainThread {
                roomClickSubject.onNext(liveEventInfo)
            }
        }
    }

    fun bind(liveEventInfo: LiveEventInfo) {
        this.liveEventInfo = liveEventInfo
        binding.apply {
            tvLiveUserName.text = liveEventInfo.userName
            watchingCountAppCompatTextView.text =
                liveEventInfo.liveViews?.prettyCount() ?: "".toString()
            Glide.with(context)
                .load(liveEventInfo.profilePhoto ?: "")
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .centerCrop()
                .into(liveUserImage)

            if (liveEventInfo.isVerified == 1) {
                ivAccountVerified.visibility = View.VISIBLE
            } else {
                ivAccountVerified.visibility = View.GONE
            }
        }
    }
}
