package com.meetfriend.app.ui.myprofile.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.MediaViewItemBinding
import com.meetfriend.app.newbase.extension.prettyCount
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.responseclasses.photos.Data
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val postViewClicksSubject: PublishSubject<Data> = PublishSubject.create()
    val postViewClicks: Observable<Data> = postViewClicksSubject.hide()

    private var binding: MediaViewItemBinding? = null
    private lateinit var postDetails: Data

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.media_view_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = MediaViewItemBinding.bind(view)

        binding?.apply {
            detailsContainer.throttleClicks().subscribeAndObserveOnMainThread {
                postViewClicksSubject.onNext(postDetails)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(post: Data) {
        this.postDetails = post
        binding?.apply {

            watchingCountAppCompatTextView.text = (post.short_views?.prettyCount() ?: 0).toString()
            if (post.thumbnail.isNullOrEmpty()) {
                Glide.with(context)
                    .load(post.file_path)
                    .placeholder(resources.getDrawable(R.drawable.image_placeholder, null))
                    .into(thumbnailNewSquareImageView)

                videoAppCompatImageView.visibility = View.GONE
                watchingCountAppCompatTextView.visibility = View.GONE
            } else {
                Glide.with(context)
                    .load(post.thumbnail)
                    .placeholder(resources.getDrawable(R.drawable.image_placeholder, null))
                    .into(thumbnailNewSquareImageView)

                videoAppCompatImageView.visibility = View.VISIBLE
                watchingCountAppCompatTextView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}