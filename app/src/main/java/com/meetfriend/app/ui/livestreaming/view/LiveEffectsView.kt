package com.meetfriend.app.ui.livestreaming.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.effect.model.EffectResponse
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.LiveEffectItemsBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveEffectsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val effectClicksSubject: PublishSubject<EffectResponse> = PublishSubject.create()
    val effectClicks: Observable<EffectResponse> = effectClicksSubject.hide()

    private lateinit var binding: LiveEffectItemsBinding

    private lateinit var effectResponse: EffectResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.live_effect_items, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = LiveEffectItemsBinding.bind(view)

        binding.apply {
            effectRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
                effectClicksSubject.onNext(effectResponse)
            }
        }
    }

    fun bind(effects: EffectResponse) {
        effectResponse = effects

        binding.let {
            Glide.with(context).load(resources.getDrawable(effects.effectImageName, null))
                .placeholder(R.drawable.ic_empty_profile_placeholder)
                .into(it.effectRoundedImageView)
        }
    }
}
