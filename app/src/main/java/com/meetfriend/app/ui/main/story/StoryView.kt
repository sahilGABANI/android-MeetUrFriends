package com.meetfriend.app.ui.main.story

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.MainStoryViewBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.storywork.models.HomePageStoryInfoState
import com.meetfriend.app.ui.storywork.models.ResultListResult
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class StoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val storyViewClickSubject: PublishSubject<HomePageStoryInfoState> = PublishSubject.create()
    val storyViewClick: Observable<HomePageStoryInfoState> = storyViewClickSubject.hide()

    private var binding: MainStoryViewBinding? = null
    private lateinit var storyResponse: ResultListResult

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        MeetFriend.component.inject(this)

        val view = View.inflate(context, R.layout.main_story_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = MainStoryViewBinding.bind(view)
        binding?.apply {
            ivAdd.throttleClicks().subscribeAndObserveOnMainThread {
                storyViewClickSubject.onNext(HomePageStoryInfoState.AddStoryResponseInfo(""))
            }
            storyItem.throttleClicks().subscribeAndObserveOnMainThread {
                storyViewClickSubject.onNext(HomePageStoryInfoState.StoryResponseData(storyResponse))
            }
        }
    }

    fun bindAddInfo() {
        binding?.apply {
            addStoryLinearLayout.visibility = View.VISIBLE
            storyItem.visibility = View.GONE

            Glide.with(context)
                .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.profilePhoto)
                .placeholder(R.drawable.image_placeholder)
                .fitCenter()
                .into(addStoryImage)
        }
    }

    fun bind(storyListResponse: ResultListResult) {
        this.storyResponse = storyListResponse

        binding?.apply {
            addStoryLinearLayout.visibility = View.GONE
            storyItem.visibility = View.VISIBLE

            Glide.with(context)
                .load(storyListResponse.stories.last().file_path)
                .placeholder(R.drawable.place_holder_image)
                .into(storyRoundedImageView)
            if (loggedInUserCache.getLoggedInUserId() == storyListResponse.stories[0].user_id) {
                storyNameAppCompatTextView.text = resources.getString(R.string.label_your_story)
            } else if (!storyListResponse.userName.isNullOrEmpty() && storyListResponse.userName != "null") {
                storyNameAppCompatTextView.text = storyListResponse.userName
            } else {
                storyNameAppCompatTextView.text = "${storyListResponse.firstName} ${storyListResponse.lastName}}"
            }

            if (storyListResponse.stories.get(0).user?.isVerified == 1) {
                ivAccountVerified.visibility = View.VISIBLE
            } else {
                ivAccountVerified.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}
