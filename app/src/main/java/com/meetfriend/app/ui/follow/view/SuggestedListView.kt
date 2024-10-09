package com.meetfriend.app.ui.follow.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemviewSuggestedListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val suggestedItemClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    val suggestedItemClicks: Observable<FollowClickStates> = suggestedItemClicksSubject.hide()

    private lateinit var binding: ItemviewSuggestedListBinding
    private lateinit var user: MeetFriendUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.itemview_suggested_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemviewSuggestedListBinding.bind(view)

        binding.apply {
            binding.tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedItemClicksSubject.onNext(FollowClickStates.FollowClick(user))
            }.autoDispose()

            binding.tvFollowBack.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedItemClicksSubject.onNext(FollowClickStates.FollowClick(user))
            }

            binding.tvFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedItemClicksSubject.onNext(FollowClickStates.FollowingClick(user))
            }.autoDispose()

            binding.ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedItemClicksSubject.onNext(FollowClickStates.ProfileClick(user))
            }.autoDispose()

            binding.llUserNameContainer.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedItemClicksSubject.onNext(FollowClickStates.ProfileClick(user))
            }.autoDispose()
            binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
                suggestedItemClicksSubject.onNext(FollowClickStates.CancelClick(user))
            }.autoDispose()
        }

    }

    fun bind(user: MeetFriendUser) {
        this.user = user

        val tvName = when {
            !user.userName.isNullOrEmpty() && user.userName != "null" -> user.userName.toString()
            !user.firstName.isNullOrEmpty() && !user.lastName.isNullOrEmpty() -> "${user.firstName} ${user.lastName}"
            else -> user.firstName.toString()
        }
        binding.tvName.text = tvName

        if (user.isVerified == 1){
            binding.ivAccountVerified.visibility= View.VISIBLE
        }else{
            binding.ivAccountVerified.visibility = View.GONE
        }

        binding.tvUserName.text = resources.getString(R.string.sign_at_the_rate).plus(user.userName)
        Glide.with(this)
            .load(user.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        updateFollowStatus()
    }

    private fun updateFollowStatus() {
        binding.apply {

            if (user.isPrivate == 1) {
                if (user.isRequested == 1) {
                    tvRequested.visibility = View.VISIBLE
                    tvFollow.visibility = View.INVISIBLE
                    tvFollowing.visibility = View.INVISIBLE
                    tvFollowBack.visibility = View.INVISIBLE
                } else {
                    publicFollowStatus()
                }

            } else {
                publicFollowStatus()
            }
        }
    }

    private fun publicFollowStatus() {
        binding.apply {
            if (user.followStatus == 0 && user.followingStatus == 0) {
                tvFollow.visibility = View.VISIBLE
                tvFollowing.visibility = View.GONE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE

            } else if (user.followStatus == 0 && user.followingStatus == 1) {
                tvFollowBack.visibility = View.VISIBLE
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.GONE
                tvRequested.visibility = View.GONE

            } else if (user.followStatus == 1 && user.followingStatus == 0) {
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.VISIBLE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE


            } else {
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.VISIBLE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE

            }
        }
    }

}