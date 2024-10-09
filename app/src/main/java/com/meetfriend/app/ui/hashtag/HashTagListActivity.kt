package com.meetfriend.app.ui.hashtag

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.meetfriend.app.utils.blurview.FKBlurView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.hashtag.model.HashtagResponse
import com.meetfriend.app.api.hashtag.model.ReportHashtagRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityHashTagListBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.RxBus
import com.meetfriend.app.newbase.RxEvent
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.chatRoom.report.ReportUserBottomSheet
import com.meetfriend.app.ui.hashtag.view.HashTagTabAdapter
import com.meetfriend.app.ui.hashtag.viewmodel.HashTagViewModel
import com.meetfriend.app.ui.hashtag.viewmodel.HashTagsViewState
import javax.inject.Inject


class HashTagListActivity : BasicActivity() {

    companion object {
        val HASH_TAG_ID = "HASH_TAG_ID"
        val INTENT_IS_FROM = "INTENT_IS_FROM"
        val INTENT_TAG_NAME = "INTENT_TAG_NAME"
        fun getIntent(context: Context, hashTagId: Int,hashtagName:String,isFrom:Int ? = 0): Intent {
            var intent = Intent(context, HashTagListActivity::class.java)
            intent.putExtra(HASH_TAG_ID, hashTagId)
            intent.putExtra(INTENT_IS_FROM, isFrom)
            intent.putExtra(INTENT_TAG_NAME, hashtagName)

            return intent
        }
    }

    private lateinit var binding: ActivityHashTagListBinding
    private lateinit var hashTagTabAdapter: HashTagTabAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HashTagViewModel>
    private lateinit var hashTagViewModel: HashTagViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var hashtagId: Int = 0
    private var isBlocked: Int = 0
    private var isFrom:Int? = 0
    private var hashtagName:String? = ""
    private var hashTagInfo : HashtagResponse ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeetFriendApplication.component.inject(this)

        binding = ActivityHashTagListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hashTagViewModel = getViewModelFromFactory(viewModelFactory)
        initUI()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        hashTagViewModel.hashTagsState.subscribeAndObserveOnMainThread {
            when (it) {
                is HashTagsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is HashTagsViewState.LoadingState -> {
                    binding.loadingApiProgressBar.visibility =
                        if (it.isLoading) View.VISIBLE else View.GONE
                }
                is HashTagsViewState.HashtagResponses -> {
                    hashTagInfo = it.hashtagResponse
                    setupUI(it.hashtagResponse)
                }
                is HashTagsViewState.BlockHashtag -> {
                    finish()
                }
                else -> {}
            }
        }
    }

    private fun setupUI(hashtagResponse: HashtagResponse?) {
        isBlocked = hashtagResponse?.isBlock ?: 0
        Glide.with(this@HashTagListActivity)
            .load(hashtagResponse?.user?.profilePhoto)
            .placeholder(R.drawable.image_placeholder)
            .into(binding.profileRoundedImageView)

        Glide.with(this@HashTagListActivity)
            .load(hashtagResponse?.user?.profilePhoto)
            .placeholder(R.drawable.image_placeholder)
            .listener(object : RequestListener<Drawable>{
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    val blurView = findViewById<FKBlurView>(R.id.fkBlurView)
                    blurView.setBlur(this@HashTagListActivity, blurView,80)
                    return false
                }
            })
            .into(binding.ivBackProfile)

        binding.moreAppCompatImageView.visibility = View.VISIBLE
        binding.profileUsernameAppCompatTextView.text = hashtagResponse?.user?.userName
        binding.createAtAppCompatTextView.text = hashtagResponse?.updatedAt ?: ""
        binding.likeCountAtAppCompatTextView.text = hashtagResponse?.likeCount.toString()
        binding.postCountCountAtAppCompatTextView.text = hashtagResponse?.totalPosts.toString()
        binding.postsAtAppCompatTextView.text = resources.getString(R.string.post)
        binding.likesAtAppCompatTextView.text = resources.getString(R.string.label_liked_users)

        if (hashtagResponse?.user?.isVerified == 1){
            binding.ivAccountVerified.visibility= View.VISIBLE
        }else{
            binding.ivAccountVerified.visibility= View.GONE
        }


        if (hashtagResponse?.followStatus == 0) {
            binding.tvFollow.visibility = View.VISIBLE
            binding.tvFollowing.visibility = View.GONE

            binding.tvFollow.text =
                if (hashtagResponse.followingStatus == 0) "Follow" else "Follow back"
        } else if (hashtagResponse?.followStatus == 1) {
            binding.tvFollow.visibility = View.GONE
            binding.tvFollowing.visibility = View.VISIBLE

            binding.tvFollow.text =
                if (hashtagResponse.isPrivate == 1 && hashtagResponse.isRequested == 1) "Requested" else "Following"
        }

        binding.followStatusFrameLayout.visibility =
            if ((loggedInUserCache.getLoggedInUserId()) == hashtagResponse?.userId ?: 0) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }


        binding.tvFollow.throttleClicks().subscribeAndObserveOnMainThread {
            hashtagResponse?.userId?.let {
                hashTagViewModel.followUnfollow(it, hashtagResponse.id)
            }
        }

        binding.tvFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            hashtagResponse?.userId?.let {
                hashTagViewModel.followUnfollow(it, hashtagResponse.id)
            }
        }

    }

    private fun initUI() {
        intent?.let {
            hashtagId = it.getIntExtra(HASH_TAG_ID, -1)
            isFrom = it.getIntExtra(INTENT_IS_FROM,0)
            hashtagName = it.getStringExtra(INTENT_TAG_NAME)
           hashTagViewModel.getHashtagDetails(hashtagId)
        }
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(isFrom ?: 0))
        binding.titleAppCompatTextView.text = "#${hashtagName?.replace("#", "")}"


        hashTagTabAdapter =
            HashTagTabAdapter(this, hashtagId ?: 0, hashtagName ?: "")
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = hashTagTabAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = resources.getString(R.string.post)
                }
                1 -> {
                    tab.text = resources.getString(R.string.shorts)
                }
                2 -> {
                    tab.text = resources.getString(R.string.challenges)
                }
            }
        }.attach()

        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(isFrom ?: 0))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.text == resources.getString(R.string.shorts)) {
                    RxBus.publish(RxEvent.RefreshForYouPlayFragment)
                }
                when(tab.position) {
                    0 -> {
                        binding.postsAtAppCompatTextView.text = resources.getString(R.string.post)
                        binding.postCountCountAtAppCompatTextView.text = hashTagInfo?.totalPosts.toString()
                    }
                    1 -> {
                        binding.postsAtAppCompatTextView.text = resources.getString(R.string.shorts)
                        binding.postCountCountAtAppCompatTextView.text = hashTagInfo?.totalShorts.toString()
                    }
                    2 -> {
                        binding.postsAtAppCompatTextView.text = resources.getString(R.string.challenges)
                        binding.postCountCountAtAppCompatTextView.text = hashTagInfo?.totalChallenges.toString()
                    }

                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.moreAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val wrapper = ContextThemeWrapper(this@HashTagListActivity, R.style.CustomPopupMenu)
            val popupMenu = PopupMenu(wrapper, binding.moreAppCompatImageView)

            popupMenu.menuInflater.inflate(R.menu.more_menu, popupMenu.menu)
            showMenuIcons(popupMenu)

            val popupSubMenu: Menu = popupMenu.menu
            popupSubMenu.findItem(R.id.block).isVisible = isBlocked != 1
            popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                    // Toast message on menu item clicked
                    if (menuItem.itemId == R.id.report) {
                        val reportUserBottomSheet = ReportUserBottomSheet.newInstanceReportUser(challengeId = hashtagId)
                        reportUserBottomSheet.show(
                            supportFragmentManager,
                            ReportUserBottomSheet::class.java.name
                        )

                    } else if (menuItem.itemId == R.id.block) {
                        hashTagViewModel.blockHashtag(ReportHashtagRequest(hashtagId))
                    } else {
                        return false;
                    }
                    return true
                }
            })

            popupMenu.show()
        }.autoDispose()
    }

    private fun showMenuIcons(popupMenu: PopupMenu) {
        try {
            val fields = popupMenu.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popupMenu)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}