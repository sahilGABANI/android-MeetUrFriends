package com.meetfriend.app.ui.giftsGallery.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.gift.model.GiftsItemInfo
import com.meetfriend.app.api.livestreaming.model.Time
import com.meetfriend.app.api.livestreaming.model.secondToTime
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewGiftsItemBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

class GiftItemView(context: Context, private val isFromLive: Boolean) :
    ConstraintLayoutWithLifecycle(context) {

    private val giftsItemClicksSubject: PublishSubject<GiftItemClickStates> =
        PublishSubject.create()
    val giftsItemClicks: Observable<GiftItemClickStates> = giftsItemClicksSubject.hide()


    private lateinit var binding: ViewGiftsItemBinding
    private lateinit var giftsItemInfo: GiftsItemInfo
    private var countTimerDisposable: Disposable? = null
    var countTime = 1


    init {
        inflateUi()
    }

    @SuppressLint("ClickableViewAccessibility")

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_gifts_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewGiftsItemBinding.bind(view)

        binding.apply {
            llGiftItemContainer.throttleClicks().subscribeAndObserveOnMainThread {
                if (!giftsItemInfo.isSend) {
                    giftsItemClicksSubject.onNext(GiftItemClickStates.RequestGiftClick(giftsItemInfo))
                } else {
                     sendGiftClick(0)
                }
            }.autoDispose()

            if (isFromLive) {
                llGiftItemContainer.setOnLongClickListener {
                    startTimer(0)
                    true
                }
            }

            llGiftItemContainer.setOnTouchListener { v, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                    }
                    MotionEvent.ACTION_UP -> {
                        countTime = 0
                        countTimerDisposable?.dispose()
                        if(giftsItemInfo.isCombo == 1) {
                            Observable.interval(500, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                                giftsItemClicksSubject.onNext(GiftItemClickStates.ComboSent)
                            }.autoDispose()
                        }

                    }

                    MotionEvent.ACTION_CANCEL -> {
                        countTimerDisposable?.dispose()
                        binding.tvComboCount.isVisible = false
                        binding.comboAnimation.isVisible = false
                        binding.tvCombo.isVisible = false
                        binding.llGiftItemContainer.visibility = View.VISIBLE
                        giftsItemClicksSubject.onNext(GiftItemClickStates.ComboSent)
                    }
                }
                v?.onTouchEvent(event) ?: true
            }

        }

    }

    private fun sendGiftClick(isCombo : Int) {

        if (giftsItemInfo.isSend) {
            giftsItemInfo.quantity = if (countTime > 0) countTime else 1
            giftsItemInfo.isCombo = isCombo
            giftsItemClicksSubject.onNext(GiftItemClickStates.GiftItemClick(giftsItemInfo))
        }

    }

    fun bind(giftsItemInfo: GiftsItemInfo, selected: Boolean) {
        this.giftsItemInfo = giftsItemInfo

        Glide.with(this)
            .load(giftsItemInfo.file_path)
            .placeholder(R.drawable.giftbox_placeholder)
            .into(binding.giftAppCompatImageView)

        val nf: NumberFormat = DecimalFormat("##.###")
        binding.coinAppCompatTextView.text = nf.format(giftsItemInfo.coins).toString()
        binding.giftNameAppCompatTextView.text = giftsItemInfo.name

    }

    private fun updateTimerText(secondToTime: Time) {
        val secondString = secondToTime.second.toString()
        binding.tvComboCount.isVisible = true
        binding.tvCombo.isVisible = true
        binding.comboAnimation.isVisible = true
        binding.llGiftItemContainer.visibility = View.INVISIBLE
        binding.tvComboCount.text = secondString
        sendGiftClick(1)
    }

    private fun startTimer(time: Int) {
        countTime = time
        countTimerDisposable?.dispose()
        countTimerDisposable =
            Observable.interval(1, TimeUnit.SECONDS).subscribeAndObserveOnMainThread {
                countTime++
                updateTimerText(countTime.secondToTime())

                android.view.ViewConfiguration.getLongPressTimeout()

                if (countTime >= 20) {
                    countTime = 0
                    countTimerDisposable?.dispose()
                    binding.tvComboCount.isVisible = false
                    binding.tvCombo.isVisible = false
                    binding.comboAnimation.isVisible = false
                    binding.llGiftItemContainer.visibility = View.VISIBLE

                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countTimerDisposable?.dispose()

    }

}