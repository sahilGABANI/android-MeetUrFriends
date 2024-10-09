package com.meetfriend.app.ui.livestreaming.watchlive.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.livestreaming.model.LiveJoinResponse
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewLiveWatchingUsersBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class WatchUserListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val watchUsersViewClicksSubject: PublishSubject<LiveJoinResponse> =
        PublishSubject.create()
    val watchUsersViewClicks: Observable<LiveJoinResponse> = watchUsersViewClicksSubject.hide()

    private lateinit var binding: ViewLiveWatchingUsersBinding
    private lateinit var liveJoinResponse: LiveJoinResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_watching_users, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveWatchingUsersBinding.bind(view)

        binding.apply {
        }
    }

    fun bind(liveJoinResponse: LiveJoinResponse) {
        this.liveJoinResponse = liveJoinResponse
        binding.apply {
            tvUserName.text = liveJoinResponse.username ?: ""

            Glide.with(context)
                .load(liveJoinResponse.profileUrl ?: "")
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(ivProfileImage)
        }
    }
}
