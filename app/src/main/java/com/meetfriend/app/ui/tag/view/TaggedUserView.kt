package com.meetfriend.app.ui.tag.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.TaggedUser
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemviewTagPeopleItemBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TaggedUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userClicksSubject: PublishSubject<TaggedUser> = PublishSubject.create()
    val userClicks: Observable<TaggedUser> = userClicksSubject.hide()


    private lateinit var binding: ItemviewTagPeopleItemBinding
    private lateinit var taggedUser: TaggedUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.itemview_tag_people_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemviewTagPeopleItemBinding.bind(view)

        binding.apply {
            mRow.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(taggedUser)
            }.autoDispose()
        }

    }

    fun bind(taggedUser: TaggedUser) {
        this.taggedUser = taggedUser

        binding.tvUserName.text = taggedUser.user.userName

        if (taggedUser.user.isVerified == 1) {
            binding.ivAccountVerified.visibility = View.VISIBLE
        } else {
            binding.ivAccountVerified.visibility = View.GONE
        }

        Glide.with(this)
            .load(taggedUser.user.profilePhoto)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfilePic)

    }
}