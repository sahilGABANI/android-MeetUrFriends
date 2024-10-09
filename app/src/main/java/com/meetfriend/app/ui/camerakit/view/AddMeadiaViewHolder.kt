package com.meetfriend.app.ui.camerakit.view

import android.content.Context
import android.view.View
import com.meetfriend.app.R
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.AddMediaBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddMeadiaViewHolder (context: Context) : ConstraintLayoutWithLifecycle(context) {
    private val addMediaClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val addMediaClick: Observable<Unit> = addMediaClickSubject.hide()
    private lateinit var binding: AddMediaBinding
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.add_media, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = AddMediaBinding.bind(view)

        binding.addMediaAppCompatImageView.setOnClickListener {
            addMediaClickSubject.onNext(Unit)
        }
    }
    fun bind() {
    }
}