package com.meetfriend.app.ui.location.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.places.model.ResultResponse
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.LocationItemViewBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LocationView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val locationItemClickSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val locationItemClick: Observable<ResultResponse> = locationItemClickSubject.hide()

    private lateinit var binding: LocationItemViewBinding
    private lateinit var resultResponse: ResultResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.location_item_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = LocationItemViewBinding.bind(view)

        binding.apply {
            binding.rlLocation.throttleClicks().subscribeAndObserveOnMainThread {
                locationItemClickSubject.onNext(resultResponse)
            }.autoDispose()
        }
    }

    fun bind(resultResponse: ResultResponse) {
        this.resultResponse = resultResponse
        binding.apply {
            tvLocationFormatted.text = resultResponse.formattedAddress
            tvLocationName.text = resultResponse.name
        }
    }
}
