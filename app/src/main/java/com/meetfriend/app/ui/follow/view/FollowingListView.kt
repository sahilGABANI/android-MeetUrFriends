package com.meetfriend.app.ui.follow.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemviewFollowingListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class FollowingListView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val followingItemClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    val followingItemClicks: Observable<FollowClickStates> = followingItemClicksSubject.hide()

    private lateinit var binding: ItemviewFollowingListBinding
    private lateinit var userInfo: MeetFriendUser

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.itemview_following_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemviewFollowingListBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.apply {

            tvFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                followingItemClicksSubject.onNext(FollowClickStates.FollowingClick(userInfo))
            }.autoDispose()

            tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
                followingItemClicksSubject.onNext(FollowClickStates.FollowClick(userInfo))
            }.autoDispose()

            tvFollowBack.throttleClicks().subscribeAndObserveOnMainThread {
                followingItemClicksSubject.onNext(FollowClickStates.FollowClick(userInfo))
            }.autoDispose()

            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                followingItemClicksSubject.onNext(FollowClickStates.ProfileClick(userInfo))
            }.autoDispose()

            llUserNameContainer.throttleClicks().subscribeAndObserveOnMainThread {
                followingItemClicksSubject.onNext(FollowClickStates.ProfileClick(userInfo))
            }
        }
    }

    fun bind(followingInfo: MeetFriendUser, userId: Int) {
        this.userInfo = followingInfo
        binding.tvName.text =  if(!followingInfo.userName.isNullOrEmpty() && followingInfo.userName != "null" ) followingInfo.userName
        else followingInfo.firstName.plus(" ").plus(followingInfo.lastName)

        if (followingInfo.isVerified == 1){
            binding.ivAccountVerified.visibility= View.VISIBLE
        }else{
            binding.ivAccountVerified.visibility = View.GONE
        }

        binding.tvUserName.text =
            resources.getString(R.string.sign_at_the_rate).plus(followingInfo.userName)

        Glide.with(this)
            .load(followingInfo.profilePhoto)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        updateFollowUnfollowButton(userId)
    }

    private fun updateFollowUnfollowButton(userId: Int) {
        binding.apply {
            if (loggedInUserCache.getLoggedInUserId() != userInfo.id && userInfo.userBlockedYou == 0) {
                if (userId != loggedInUserCache.getLoggedInUserId()) {
                    if (userInfo.isPrivate == 1) {
                        if (userInfo.isRequested == 1) {
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
                } else {
                    publicFollowStatus()
                }
            } else if (loggedInUserCache.getLoggedInUserId() == userInfo.id) {
                tvFollow.visibility = View.INVISIBLE
                tvFollowing.visibility = View.INVISIBLE
                tvFollowBack.visibility = View.INVISIBLE
                tvRequested.visibility = View.INVISIBLE

            } else {
                publicFollowStatus()
            }
        }
    }

    private fun publicFollowStatus() {
        binding.apply {
            if (userInfo.followStatus == 0 && userInfo.followingStatus == 0) {
                tvFollow.visibility = View.VISIBLE
                tvFollowing.visibility = View.GONE
                tvFollowBack.visibility = View.GONE
                tvRequested.visibility = View.GONE

            } else if (userInfo.followStatus == 0 && userInfo.followingStatus == 1) {
                tvFollowBack.visibility = View.VISIBLE
                tvFollow.visibility = View.GONE
                tvFollowing.visibility = View.GONE
                tvRequested.visibility = View.GONE

            } else if (userInfo.followStatus == 1 && userInfo.followingStatus == 0) {
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