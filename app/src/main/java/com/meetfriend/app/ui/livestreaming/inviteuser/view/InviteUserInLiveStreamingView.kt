package com.meetfriend.app.ui.livestreaming.inviteuser.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewInviteUserInLiveStramingBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

@SuppressLint("ViewConstructor")
class InviteUserInLiveStreamingView(
    context: Context,
    private val isOpenCreateScreen: Boolean,
    private val isAllowPlayGame: Int,
    private val isOpenFrom: String
) : ConstraintLayoutWithLifecycle(context) {

    private val inviteButtonViewClicksSubject: PublishSubject<MeetFriendUser> =
        PublishSubject.create()
    val inviteButtonViewClicks: Observable<MeetFriendUser> = inviteButtonViewClicksSubject.hide()

    private val invitedButtonViewClicksSubject: PublishSubject<MeetFriendUser> =
        PublishSubject.create()
    val invitedButtonViewClicks: Observable<MeetFriendUser> = invitedButtonViewClicksSubject.hide()

    private val createChatViewClicksSubject: PublishSubject<MeetFriendUser> =
        PublishSubject.create()
    val createChatViewClicks: Observable<MeetFriendUser> = createChatViewClicksSubject.hide()

    private val userClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val userClicks: Observable<MeetFriendUser> = userClicksSubject.hide()

    private lateinit var binding: ViewInviteUserInLiveStramingBinding
    private lateinit var meetFriendUser: MeetFriendUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_invite_user_in_live_straming, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewInviteUserInLiveStramingBinding.bind(view)

        binding.apply {
            tvInvite.throttleClicks().subscribeAndObserveOnMainThread {
                inviteButtonViewClicksSubject.onNext(meetFriendUser)
            }.autoDispose()

            tvInvited.throttleClicks().subscribeAndObserveOnMainThread {
                inviteButtonViewClicksSubject.onNext(meetFriendUser)
            }.autoDispose()

            rlChatRoomDataContainer.throttleClicks().subscribeAndObserveOnMainThread {
                if (isOpenFrom == "WhoCanWatch") {
                    userClicksSubject.onNext(meetFriendUser)
                } else {
                    createChatViewClicksSubject.onNext(meetFriendUser)
                }
            }.autoDispose()

            ivCheck.throttleClicks().subscribeAndObserveOnMainThread {
                if (isOpenFrom == "WhoCanWatch") {
                    userClicksSubject.onNext(meetFriendUser)
                }
            }.autoDispose()
        }
    }

    fun bind(mentionUserInfo: MeetFriendUser) {
        this.meetFriendUser = mentionUserInfo
        if (isOpenCreateScreen) binding.flInviteContainer.visibility = View.GONE
        binding.ivCheck.isVisible = isOpenFrom == "WhoCanWatch"
        binding.apply {
            tvName.text = if (!meetFriendUser.userName.isNullOrEmpty() && meetFriendUser.userName != "null") {
                meetFriendUser.userName
            } else {
                meetFriendUser.firstName.plus(" ").plus(meetFriendUser.lastName)
            }
            tvUserName.text =
                if (!meetFriendUser.userName.isNullOrEmpty() && meetFriendUser.userName != "null") {
                    resources.getString(
                        R.string.sign_at_the_rate
                    ).plus(meetFriendUser.userName)
                } else {
                    ""
                }
            Glide.with(context)
                .load(mentionUserInfo.profilePhoto ?: "")
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(ivProfileImage)
                binding.ivAccountVerified.isVisible = meetFriendUser.isVerified == 1
            if (meetFriendUser.isSelected) {
                binding.ivCheck.setImageResource(R.drawable.checked_img)
            } else {
                binding.ivCheck.setImageResource(R.drawable.bg_checkbox)
            }
            if (isAllowPlayGame == 1) {
                if (mentionUserInfo.isAlreadyInvited) {
                    tvInvite.visibility = View.GONE
                    tvInvited.visibility = View.VISIBLE
                } else {
                    if (mentionUserInfo.isInvited) {
                        tvInvite.visibility = View.GONE
                        tvInvited.visibility = View.VISIBLE
                    } else {
                        tvInvite.visibility = View.VISIBLE
                        tvInvited.visibility = View.GONE
                    }
                }
            } else {
                if (mentionUserInfo.isAlreadyInvited) {
                    tvInvite.visibility = View.GONE
                    tvInvited.visibility = View.VISIBLE
                } else {
                    if (mentionUserInfo.isInvited) {
                        tvInvite.visibility = View.GONE
                        tvInvited.visibility = View.VISIBLE
                    } else {
                        tvInvite.visibility = View.VISIBLE
                        tvInvited.visibility = View.GONE
                    }
                }
            }
        }
    }
}
