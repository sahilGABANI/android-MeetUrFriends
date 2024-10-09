package com.meetfriend.app.ui.bottomsheet.auth.switchaccount.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.SwitchAccountUserViewBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SwitchUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val switchUserClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val switchUserItemClicks: Observable<MeetFriendUser> = switchUserClicksSubject.hide()

    private lateinit var binding: SwitchAccountUserViewBinding
    private lateinit var meetFriendUser: MeetFriendUser

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        MeetFriendApplication.component.inject(this)
        val view = View.inflate(context, R.layout.switch_account_user_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = SwitchAccountUserViewBinding.bind(view)

        binding.apply {
            mRow.throttleClicks().subscribeAndObserveOnMainThread {
                switchUserClicksSubject.onNext(meetFriendUser)
            }.autoDispose()
        }
    }

    fun bind(postLikesInformation: MeetFriendUser) {
        this.meetFriendUser = postLikesInformation

        Glide.with(this).load(postLikesInformation.profilePhoto).placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder).into(binding.ivUserProfilePic)

        binding.tvUserName.text = meetFriendUser.userName ?: ""
        if (meetFriendUser.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }
        binding.checkBoxRememberMe.isVisible = loggedInUserCache.getLoggedInUserId() == meetFriendUser.id
    }
}
