package com.meetfriend.app.ui.livestreaming.inviteuser.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.SelectedItemsBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectedGroupView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val removeItemClickStateSubject: PublishSubject<MeetFriendUser> = PublishSubject.create()
    val removeItemClick: Observable<MeetFriendUser> = removeItemClickStateSubject.hide()

    private var binding: SelectedItemsBinding? = null
    private lateinit var userInfo: MeetFriendUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.selected_items, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = SelectedItemsBinding.bind(view)

        binding?.apply {
            removeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                removeItemClickStateSubject.onNext(userInfo)
            }.autoDispose()
        }
    }

    fun bind(user: MeetFriendUser) {
        this.userInfo = user
        binding?.let {
            Glide.with(context)
                .load(user.profilePhoto)
                .centerCrop()
                .placeholder(resources.getDrawable(R.drawable.ic_placer_holder_image_new, null))
                .into(it.ivUserProfileImage)
        }
        binding?.tvUSerName?.text = user.userName
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
