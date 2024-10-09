package com.meetfriend.app.ui.main.story

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.databinding.ActivityStoryInfoBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.utilclasses.ads.AdsManager
import javax.inject.Inject

class StoryInfoActivity : BasicActivity() {

    private lateinit var binding: ActivityStoryInfoBinding
    private var list = ArrayList<ResultListResult>()
    private var mCurrentPosition = -1

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        const val LIST_OF_STORY = "LIST_OF_STORY"
        const val FIVE_HUNDRED_MILLISECONDS = 500L
        fun getIntent(context: Context, listOfStories: ArrayList<ResultListResult>): Intent {
            val intent = Intent(context, StoryInfoActivity::class.java)
            intent.putExtra(LIST_OF_STORY, listOfStories)
            return intent
        }
    }

    private val hasNextItem: Boolean
        get() = mCurrentPosition < list.size - 1

    private val nextVideoUrls: List<String>?
        get() {
            val listOfItems = list
            if (listOfItems.isNotEmpty() && hasNextItem) {
                val listData: ArrayList<String> = arrayListOf()

                listOfItems.forEach {
                    it.stories.forEach {
                        if ("video" == it.type) {
                            listData.add(it.video_url.toString().plus("?clientBandwidthHint=2.5"))
                        }
                    }
                }

                listData.remove("")
                return listData
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loggedInUserCache = loggedInUserCacheBase

        initUI()
    }

    private fun initUI() {
        intent?.let {
            list = intent?.getParcelableArrayListExtra(LIST_OF_STORY) ?: arrayListOf()
            nextVideoUrls?.let {
                MeetFriend.exoCacheManager.prepareCacheVideos(it)
            }
        }
        val size = list.size
        binding.viewpager2INfo.setPageTransformer(CubeTransformer())

        val storyListAdapter = StoryListAdapter({
            if (list.size - 1 == it) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                mCurrentPosition = it
                nextVideoUrls?.let {
                    MeetFriend.exoCacheManager.prepareCacheVideos(it)
                }
                binding.viewpager2INfo.setCurrentItems(
                    binding.viewpager2INfo.currentItem + 1,
                    FIVE_HUNDRED_MILLISECONDS
                )
            }
        }, {
            mCurrentPosition = it
            nextVideoUrls?.let {
                MeetFriend.exoCacheManager.prepareCacheVideos(it)
            }
            binding.viewpager2INfo.setCurrentItems(binding.viewpager2INfo.currentItem - 1, FIVE_HUNDRED_MILLISECONDS)
        }, size, list, this@StoryInfoActivity)

        binding.viewpager2INfo.adapter = storyListAdapter

        val selectedItem = list.find { it.isSelected }
        val index = list.indexOf(selectedItem)
        binding.viewpager2INfo.setCurrentItem(index, false)
    }

    fun ViewPager2.setCurrentItems(
        item: Int,
        duration: Long,
        interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
        pagePxWidth: Int = width
    ) {
        val pxToDrag: Int = pagePxWidth * (item - currentItem)
        val animator = ValueAnimator.ofInt(0, pxToDrag)
        var previousValue = 0
        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            val currentPxToDrag = (currentValue - previousValue).toFloat()
            fakeDragBy(-currentPxToDrag)
            previousValue = currentValue
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                beginFakeDrag()
            }

            override fun onAnimationEnd(p0: Animator) {
                endFakeDrag()
            }

            override fun onAnimationCancel(p0: Animator) {
                return
            }
            override fun onAnimationRepeat(p0: Animator) {
                return
            }
        })
        animator.interpolator = interpolator
        animator.duration = duration
        animator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdsManager.getAdsManager()
            .openInterstitialAds(
                this,
                loggedInUserCache.getInterstitialAdId(),
                object : AdsManager.InterstitialAdsCallback {
                    override fun InterstitialResponse(nativeAd: InterstitialAd?) {
                        if (nativeAd != null) {
                            nativeAd.show(this@StoryInfoActivity)
                            nativeAd.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdClicked() {
                                        return
                                    }
                                }
                        }
                    }
                    override fun adsOnLoaded() {
                        return
                    }
                }
            )
    }
}
