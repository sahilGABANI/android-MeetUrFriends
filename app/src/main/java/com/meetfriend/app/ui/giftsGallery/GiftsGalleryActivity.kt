package com.meetfriend.app.ui.giftsGallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.ActivityGiftsGalleryBinding
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.ui.coins.CoinPlansActivity
import com.meetfriend.app.ui.giftsGallery.view.GiftItemAdapter
import com.meetfriend.app.ui.giftsGallery.viewmodel.GiftsGalleryViewModel
import com.meetfriend.app.ui.giftsGallery.viewmodel.GiftsGalleryViewState
import com.meetfriend.app.ui.messages.create.CreateMessageActivity
import com.meetfriend.app.utilclasses.CallProgressWheel.showLoadingDialog
import com.meetfriend.app.utils.Constant
import com.meetfriend.app.utils.Constant.FiXED_4_INT
import contractorssmart.app.utilsclasses.CommonMethods.showToastMessageAtTop
import contractorssmart.app.utilsclasses.PreferenceHandler.writeString
import java.util.Locale
import javax.inject.Inject

class GiftsGalleryActivity : BasicActivity() {

    companion object {
        private const val INTENT_FROM = "INTENT_FROM"
        private const val INTENT_TO_ID = "INTENT_TO_ID"
        private const val INTENT_POST_ID = "INTENT_POST_ID"

        fun getIntent(context: Context, isFrom: String, toId: String?, postId: String?): Intent {
            val intent = Intent(context, GiftsGalleryActivity::class.java)
            intent.putExtra(INTENT_FROM, isFrom)
            intent.putExtra(INTENT_TO_ID, toId)
            intent.putExtra(INTENT_POST_ID, postId)
            return intent
        }
    }

    lateinit var binding: ActivityGiftsGalleryBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftsGalleryViewModel>
    private lateinit var giftsGalleryViewModel: GiftsGalleryViewModel

    private lateinit var giftItemAdapter: GiftItemAdapter
    private var listOfGifts: List<GiftsItemInfo> = listOf()

    private var isFrom: String = ""
    private var toId: String = ""
    private var postId: String = ""
    private var totalCoins = 0.0
    private var temp: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGiftsGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MeetFriendApplication.component.inject(this)
        giftsGalleryViewModel = getViewModelFromFactory(viewModelFactory)

        intent?.let {
            isFrom = it.getStringExtra(INTENT_FROM) ?: ""
            toId = it.getStringExtra(INTENT_TO_ID) ?: ""
            postId = it.getStringExtra(INTENT_POST_ID) ?: ""
        }

        listenToViewModel()
        listenToViewEvent()
    }

    fun listenToViewModel() {
        giftsGalleryViewModel.giftsGalleryState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftsGalleryViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is GiftsGalleryViewState.GiftsData -> {
                    if (!it.listOfGifts.isNullOrEmpty()) {
                        listOfGifts = it.listOfGifts
                    }
                    giftItemAdapter.listOfDataItems = it.listOfGifts
                    hideShowNoData(listOfGifts)
                    hideLoading()
                }
                is GiftsGalleryViewState.LoadingState -> {
                    if (giftItemAdapter.listOfDataItems.isNullOrEmpty()) {
                        showLoading(it.isLoading)
                    }
                }
                is GiftsGalleryViewState.SuccessMessage -> {
                }
                GiftsGalleryViewState.EmptyState -> {
                    hideShowNoData(listOfGifts)
                    hideLoading()
                }
                is GiftsGalleryViewState.MyEarningData -> {
                    totalCoins = it.myEarningInfo?.totalCurrentCoins ?: 0.0
                    temp = it.myEarningInfo?.totalCurrentCoins ?: 0.0
                    binding.availableCoinsAppCompatTextView.text =
                        String.format(
                            Locale.US,
                            "%.2f",
                            it.myEarningInfo?.totalCurrentCoins
                        )
                }
            }
        }.autoDispose()
    }

    fun listenToViewEvent() {
        giftItemAdapter = GiftItemAdapter(this, false).apply {
            giftsItemClicks.subscribeAndObserveOnMainThread {
                when (it) {
                    is GiftItemClickStates.GiftItemClick -> {
                    }
                    is GiftItemClickStates.SendGiftClick -> {
                        handleSendGiftClick(it)
                    }
                    else -> {}
                }
            }.autoDispose()
        }

        binding.giftsRecyclerView.apply {
            adapter = giftItemAdapter
            layoutManager =
                GridLayoutManager(this@GiftsGalleryActivity, FiXED_4_INT, GridLayoutManager.VERTICAL, false)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        var lastVisibleItemPosition = 0
                        if (layoutManager is GridLayoutManager) {
                            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        }
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (
                            layoutManager.childCount > 0 &&
                            lastVisibleItemPosition >= adjAdapterItemCount - 2 &&
                            adjAdapterItemCount >= layoutManager.childCount
                        ) {
                            giftsGalleryViewModel.loadMoreGifts()
                        }
                    }
                }
            })
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

        binding.buyCoinsAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(CoinPlansActivity.getIntent(this))
            openInterstitialAds()
            Constant.CLICK_COUNT++
        }.autoDispose()

        binding.tvSendCoin.throttleClicks().subscribeAndObserveOnMainThread {
            val totalCoins = binding.availableCoinsAppCompatTextView.text
            startActivity(
                CreateMessageActivity.getIntentWithDataS(
                    this@GiftsGalleryActivity,
                    "sendCoin",
                    totalCoins as String
                )
            )
        }.autoDispose()
    }

    private fun handleSendGiftClick(it: GiftItemClickStates.SendGiftClick) {
        if (isFrom.equals("chat", ignoreCase = true) || isFrom.equals(
                "gift",
                ignoreCase = true
            )
        ) {
            if (totalCoins >= it.data.coins!!) {
                if (isFrom.equals("chat", ignoreCase = true)) {
                    writeString(
                        this@GiftsGalleryActivity,
                        "coin",
                        it.data.coins.toString()
                    )
                    writeString(
                        this@GiftsGalleryActivity,
                        "filePath",
                        it.data.file_path.toString()
                    )
                    writeString(this@GiftsGalleryActivity, "isSend", "1")
                }
                showLoadingDialog(this@GiftsGalleryActivity)
                val mHashMap = HashMap<String, Any>()
                mHashMap["to_id"] = toId
                mHashMap["post_id"] = postId
                mHashMap["coins"] = it.data.coins.toString()
                mHashMap["gift_id"] = it.data.id
                giftsGalleryViewModel.sendGiftPost(
                    toId,
                    postId,
                    it.data.coins!!,
                    it.data.id
                )
            } else {
                showToastMessageAtTop(
                    this@GiftsGalleryActivity,
                    resources.getString(R.string.label_you_dont_have_enough_coins_to_send_this_gift)
                )
                startActivity(CoinPlansActivity.getIntent(this@GiftsGalleryActivity))
            }
        } else if (isFrom.equals("request", ignoreCase = true)) {
            writeString(this@GiftsGalleryActivity, "coin_id", it.data.id.toString())
            writeString(this@GiftsGalleryActivity, "coin", it.data.coins.toString())
            writeString(
                this@GiftsGalleryActivity,
                "filePath",
                it.data.file_path.toString()
            )
            writeString(this@GiftsGalleryActivity, "isSend", "1")
            finish()
        }
    }

    private fun hideShowNoData(listOfGifts: List<GiftsItemInfo>) {
        if (listOfGifts.isNotEmpty()) {
            binding.tvEmptyState.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        giftsGalleryViewModel.resetPaginationForGifts()
        giftsGalleryViewModel.getMyEarningInfo()
    }
}
