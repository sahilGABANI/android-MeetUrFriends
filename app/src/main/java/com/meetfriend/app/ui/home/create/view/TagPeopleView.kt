package com.meetfriend.app.ui.home.create.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewTagPeopleBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TagPeopleView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userClicksSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val userClicks: Observable<MeetFriendUser> = userClicksSubject.hide()

    private lateinit var binding: ViewTagPeopleBinding
    private lateinit var data: MeetFriendUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_tag_people, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewTagPeopleBinding.bind(view)

        binding.apply {
            rlContainer.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(data)
            }
        }
    }

    fun bind(data: MeetFriendUser) {
        this.data = data
        binding.tvUserName.text = if (!data.userName.isNullOrEmpty() &&
            data.userName != "null"
        ) {
            data.userName
        } else {
            data.firstName.plus(" ").plus(data.lastName)
        }
        Glide.with(this)
            .load(data.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfilePic)

        if (data.isSelected) {
            binding.ivCheck.setImageResource(R.drawable.checked_img)
        } else {
            binding.ivCheck.setImageResource(R.drawable.bg_checkbox)
        }
    }
}
