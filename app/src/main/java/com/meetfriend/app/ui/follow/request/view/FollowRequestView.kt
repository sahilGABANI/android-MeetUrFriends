package com.meetfriend.app.ui.follow.request.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowRequestClickStates
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewFollowRequestBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowRequestView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userClicksSubject: PublishSubject<FollowRequestClickStates> =
        PublishSubject.create()
    val userClicks: Observable<FollowRequestClickStates> = userClicksSubject.hide()

    private lateinit var binding: ViewFollowRequestBinding
    private lateinit var userInfo: MeetFriendUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_follow_request, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewFollowRequestBinding.bind(view)

        binding.apply {
            tvAcceptRequest.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(FollowRequestClickStates.AcceptClick(userInfo))
            }.autoDispose()

            tvRejectRequest.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(FollowRequestClickStates.RejectClick(userInfo))
            }.autoDispose()

            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(FollowRequestClickStates.ProfileClick(userInfo))
            }.autoDispose()

            llNameContainer.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(FollowRequestClickStates.ProfileClick(userInfo))
            }.autoDispose()
        }
    }

    fun bind(userInfo: MeetFriendUser) {
        this.userInfo = userInfo

        binding.tvName.text =  if(!userInfo.userName.isNullOrEmpty() && userInfo.userName != "null" ) userInfo.userName
            else userInfo.firstName.plus(" ").plus(userInfo.lastName)
        binding.tvUserName.text =
            resources.getString(R.string.sign_at_the_rate).plus(userInfo.userName)
        Glide.with(this)
            .load(userInfo.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        if (userInfo.isVerified == 1){
            binding.ivAccountVerified.visibility = View.VISIBLE
        }else{
            binding.ivAccountVerified.visibility= View.GONE
        }
    }
}