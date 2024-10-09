package com.meetfriend.app.ui.livestreaming.game.view

import android.content.Context
import android.graphics.Color
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.follow.model.FollowClickStates
import com.meetfriend.app.api.livestreaming.model.TopGifter
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewTopContributorBinding
import com.meetfriend.app.utils.Constant.FiXED_4_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class GameTopContributorView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val contributorClicksSubject: PublishSubject<FollowClickStates> =
        PublishSubject.create()
    val contributorClicks: Observable<FollowClickStates> = contributorClicksSubject.hide()

    private lateinit var binding: ViewTopContributorBinding
    private lateinit var topGifterInfo: TopGifter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_top_contributor, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewTopContributorBinding.bind(view)

        binding.apply {
        }
    }

    fun bind(topGifter: TopGifter) {
        this.topGifterInfo = topGifter

        Glide.with(this)
            .load(topGifter.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvName.text = topGifter.userName
        binding.tvTotalCoins.text = topGifter.coins.toString()
        binding.tvIndexNumber.text = topGifter.index.plus(1).toString()

        val mColors = arrayOf("#FEDA36", "#A1A1A1", "#CD7F32", "#333333")
        binding.tvIndexNumber.setTextColor(Color.parseColor(mColors[topGifter.index % FiXED_4_INT]))
    }
}
