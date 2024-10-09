package com.meetfriend.app.ui.chatRoom.profile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewPreviousProfileImageBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PreviousProfileImageView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val previousProfileImageViewClicksSubject: PublishSubject<ProfileItemInfo> =
        PublishSubject.create()
    val previousProfileImageViewClicks: Observable<ProfileItemInfo> =
        previousProfileImageViewClicksSubject.hide()

    private val deleteProfileImageViewClicksSubject: PublishSubject<ProfileItemInfo> =
        PublishSubject.create()
    val deleteProfileImageViewClicks: Observable<ProfileItemInfo> =
        deleteProfileImageViewClicksSubject.hide()

    private lateinit var binding: ViewPreviousProfileImageBinding
    private lateinit var profileItemInfo: ProfileItemInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_previous_profile_image, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewPreviousProfileImageBinding.bind(view)

        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        binding.apply {
            ivPreviousProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                previousProfileImageViewClicksSubject.onNext(profileItemInfo)
            }.autoDispose()

            ivDelete.throttleClicks().subscribeAndObserveOnMainThread {
                deleteProfileImageViewClicksSubject.onNext(profileItemInfo)
            }.autoDispose()
        }
    }

    fun bind(profileItemInfo: ProfileItemInfo) {
        this.profileItemInfo = profileItemInfo

        Glide.with(context)
            .load(profileItemInfo.filePath)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivPreviousProfileImage)
        binding.ivDelete.visibility = View.GONE
    }
}
