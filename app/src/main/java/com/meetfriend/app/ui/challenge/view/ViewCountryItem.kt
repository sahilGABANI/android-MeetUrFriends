package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.api.challenge.model.CountryModel
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewCountryItemBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ViewCountryItem(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val countryItemClickSubject: PublishSubject<CountryModel> = PublishSubject.create()
    val countryItemClick: Observable<CountryModel> = countryItemClickSubject.hide()
    private lateinit var binding: ViewCountryItemBinding

    private lateinit var result: CountryModel

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_country_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCountryItemBinding.bind(view)

        binding.apply {
            checkBox.throttleClicks().subscribeAndObserveOnMainThread {
                countryItemClickSubject.onNext(result)
            }.autoDispose()
        }
    }

    fun bind(result: CountryModel) {
        this.result = result
        binding.txtName.text = result.name
        binding.checkBox.isChecked = result.isSelected
    }
}
