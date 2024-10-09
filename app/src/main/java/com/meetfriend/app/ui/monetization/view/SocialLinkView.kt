package com.meetfriend.app.ui.monetization.view

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.recyclerview.widget.LinearLayoutManager
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.gift.model.GiftTransactionInfo
import com.meetfriend.app.api.monetization.model.AmountData
import com.meetfriend.app.api.monetization.model.SocialLinkInfo
import com.meetfriend.app.api.monetization.model.SocialType
import com.meetfriend.app.api.monetization.model.TargetInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemViewEarningHistoryBinding
import com.meetfriend.app.databinding.ItemViewEarningTargetBinding
import com.meetfriend.app.databinding.ViewSocialLinkBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant
import contractorssmart.app.utilsclasses.PreferenceHandler.readString
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

class SocialLinkView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val socialLinkClicksSubject: PublishSubject<SocialLinkInfo> =
        PublishSubject.create()
    val socialLinkClicks: Observable<SocialLinkInfo> = socialLinkClicksSubject.hide()

    private var binding: ViewSocialLinkBinding? = null
    private var socialLinkInfo: SocialLinkInfo? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_social_link, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewSocialLinkBinding.bind(view.rootView)

        binding?.apply {
            ivRemove.throttleClicks().subscribeAndObserveOnMainThread {

                socialLinkInfo?.let {
                    socialLinkClicksSubject.onNext(it)
                }
            }.autoDispose()
        }
    }

    fun bind(info: SocialLinkInfo) {
        socialLinkInfo = info
        binding?.apply {
            tvLink.text = info.socialLink
            when (info.socialType) {
                SocialType.facebook.toString() -> {
                    ivSocialType.setImageResource(R.drawable.ic_facebook)
                }

                SocialType.instagram.toString() -> {
                    ivSocialType.setImageResource(R.drawable.ic_instagram)
                }

                SocialType.twitter.toString() -> {
                    ivSocialType.setImageResource(R.drawable.ic_twitter)
                }

                SocialType.tiktok.toString() -> {
                    ivSocialType.setImageResource(R.drawable.ic_tik_tok)
                }
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

}