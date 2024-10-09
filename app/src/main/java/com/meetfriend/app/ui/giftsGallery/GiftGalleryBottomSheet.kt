package com.meetfriend.app.ui.giftsGallery

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.FragmentGiftGalleryBottomSheetBinding
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.ViewModelFactory
import com.meetfriend.app.newbase.extension.getViewModelFromFactory
import com.meetfriend.app.newbase.extension.showToast
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.ui.coins.CoinPlansActivity
import com.meetfriend.app.ui.giftsGallery.view.GiftItemAdapter
import com.meetfriend.app.ui.giftsGallery.viewmodel.GiftGalleryBottomSheetViewModel
import com.meetfriend.app.ui.giftsGallery.viewmodel.GiftsGalleryBottomSheetViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class GiftGalleryBottomSheet : BaseBottomSheetDialogFragment() {

    private val giftResponseStateSubject: PublishSubject<GiftItemClickStates> =
        PublishSubject.create()
    val giftResponseState: Observable<GiftItemClickStates> = giftResponseStateSubject.hide()

    companion object {

        private const val INTENT_TO_ID = "INTENT_TO_ID"
        private const val INTENT_POST_ID = "INTENT_POST_ID"
        private const val INTENT_IS_SEND = "INTENT_IS_SEND"
        private const val INTENT_IS_FROM_LIVE = "INTENT_IS_FROM_LIVE"
        private const val INTENT_IS_FROM_WHERE = "INTENT_IS_FROM_WHERE"
        const val INTENT_IS_FROM_SHORTS = "INTENT_IS_FROM_SHORTS"
        const val INTENT_IS_FROM_POST = "INTENT_IS_FROM_POST"
        const val INTENT_IS_FROM_WATCH_LIVE = "INTENT_IS_FROM_WATCH_LIVE"

        fun newInstance(
            toId: String, postId: String?, isSend: Boolean, isFromLive: Boolean = false, isFromWhere : String = ""
        ): GiftGalleryBottomSheet {
            val args = Bundle()
            toId.let { args.putString(INTENT_TO_ID, it) }
            postId.let { args.putString(INTENT_POST_ID, it) }
            isSend.let { args.putBoolean(INTENT_IS_SEND, it) }
            isFromLive.let { args.putBoolean(INTENT_IS_FROM_LIVE, it) }
            isFromWhere.let { args.putString(INTENT_IS_FROM_WHERE, it) }

            val fragment = GiftGalleryBottomSheet()
            fragment.arguments = args
            return fragment
        }

    }

    private var _binding: FragmentGiftGalleryBottomSheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftGalleryBottomSheetViewModel>
    private lateinit var giftGalleryBottomSheetViewModel: GiftGalleryBottomSheetViewModel

    private lateinit var giftItemAdapter: GiftItemAdapter
    private var toId: String = ""
    private var postId: String = ""
    private var totalCoins = 0.0
    private var isSend = false
    private var selectedGiftItem: GiftsItemInfo? = null
    private var isFromLive = false
    private var isFromWhere: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

        MeetFriendApplication.component.inject(this)
        giftGalleryBottomSheetViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGiftGalleryBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        toId = arguments?.getString(INTENT_TO_ID) ?: ""
        postId = arguments?.getString(INTENT_POST_ID) ?: ""
        isSend = arguments?.getBoolean(INTENT_IS_SEND) ?: false
        isFromLive = arguments?.getBoolean(INTENT_IS_FROM_LIVE) ?: false
        isFromWhere = arguments?.getString(INTENT_IS_FROM_WHERE) ?: ""
        listenToViewModel()
        listenToViewEvents()
        giftGalleryBottomSheetViewModel.resetPaginationForGifts()

    }

    private fun listenToViewEvents() {
        initAdapter()
    }

    private fun initAdapter() {
        giftItemAdapter = GiftItemAdapter(requireContext(), isFromLive).apply {
            giftsItemClicks.subscribeAndObserveOnMainThread {
                when (it) {
                    is GiftItemClickStates.GiftItemClick -> {
                        if (totalCoins >= it.data.coins!!) {
                            it.data.coins?.let { coins ->
                                if (isFromLive) {
                                    giftGalleryBottomSheetViewModel.sendGiftInLive(
                                        toId, postId,
                                        coins, it.data.id, it.data.quantity ?: 1
                                    )
                                } else {
                                    giftGalleryBottomSheetViewModel.sendGiftPost(
                                        toId, postId,
                                        coins, it.data.id
                                    )
                                }

                            }
                            selectedGiftItem = it.data

                        } else {
                            showToast(resources.getString(R.string.label_you_dont_have_enough_coins_to_send_this_gift))
                            startActivity(CoinPlansActivity.getIntent(requireContext()))
                        }

                    }
                    is GiftItemClickStates.SendGiftClick -> {


                    }
                    is GiftItemClickStates.RequestGiftClick -> {
                        giftResponseStateSubject.onNext(GiftItemClickStates.RequestGiftClick(it.data))
                        dismiss()
                    }

                    is GiftItemClickStates.GiftItemComboClick -> {
                    }
                    is GiftItemClickStates.ComboSent -> {
                        if(isFromWhere != INTENT_IS_FROM_SHORTS && isFromWhere != INTENT_IS_FROM_POST && isFromWhere.isNullOrEmpty()){
                            dismissAllowingStateLoss()
                        } else {
                        }
                    }
                    else -> {}
                }


            }.autoDispose()
        }
        binding.rvGiftGallery.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvGiftGallery.apply {
            adapter = giftItemAdapter
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
                            layoutManager.childCount > 0
                            && lastVisibleItemPosition >= adjAdapterItemCount - 2
                            && adjAdapterItemCount >= layoutManager.childCount
                        ) {
                            giftGalleryBottomSheetViewModel.loadMoreGifts()
                        }
                    }
                }
            })
        }
    }

    private fun listenToViewModel() {
        giftGalleryBottomSheetViewModel.giftsGalleryBottomSheetState.subscribeAndObserveOnMainThread { state ->
            when (state) {
                GiftsGalleryBottomSheetViewState.EmptyState -> {

                }
                is GiftsGalleryBottomSheetViewState.ErrorMessage -> {

                }
                is GiftsGalleryBottomSheetViewState.GiftsData -> {
                    binding.progressBar.isVisible = false
                    state.listOfGifts.forEach {
                        it.isSend = isSend
                    }
                    setGiftData(state.listOfGifts)
                }
                is GiftsGalleryBottomSheetViewState.LoadingState -> {
                    if (giftItemAdapter.listOfDataItems.isNullOrEmpty())
                        binding.progressBar.isVisible = state.isLoading
                }
                is GiftsGalleryBottomSheetViewState.MyEarningData -> {
                    totalCoins = state.myEarningInfo?.totalCurrentCoins ?: 0.0
                    binding.tvNumberOfCoins.text =
                        "%.2f".format(state.myEarningInfo?.totalCurrentCoins)
                }
                is GiftsGalleryBottomSheetViewState.SuccessMessage -> {
                    if (!isFromLive) {
                        showToast(state.successMessage)
                    }

                    selectedGiftItem?.let {
                        if (isFromLive) {
                            if(it.isCombo == 0) dismissAllowingStateLoss()
                            giftResponseStateSubject.onNext(
                                GiftItemClickStates.SendGiftInGameClick(
                                    it
                                )
                            )
                        } else {
                            dismissAllowingStateLoss()
                            giftResponseStateSubject.onNext(
                                GiftItemClickStates.SendGiftInChatClick(
                                    it
                                )
                            )
                        }
                    }
                }
            }
        }.autoDispose()

    }

    private fun setGiftData(listOfGifts: List<GiftsItemInfo>) {
        giftItemAdapter.listOfDataItems = listOfGifts
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    override fun onResume() {
        super.onResume()
        giftGalleryBottomSheetViewModel.getMyEarningInfo()
    }

}
