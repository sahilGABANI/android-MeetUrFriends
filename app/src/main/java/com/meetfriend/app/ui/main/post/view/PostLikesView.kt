package com.meetfriend.app.ui.main.post.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.gift.model.GiftItemClickStates
import com.meetfriend.app.api.post.model.PostLikesInformation
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ItemviewTagPeopleItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostLikesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val giftsItemClicksSubject: PublishSubject<GiftItemClickStates> =
        PublishSubject.create()
    val giftsItemClicks: Observable<GiftItemClickStates> = giftsItemClicksSubject.hide()

    private lateinit var binding: ItemviewTagPeopleItemBinding
    private lateinit var postLikesInformation: PostLikesInformation

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.itemview_tag_people_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ItemviewTagPeopleItemBinding.bind(view)
    }

    fun bind(postLikesInformation: PostLikesInformation) {
        this.postLikesInformation = postLikesInformation

        Glide.with(this)
            .load(postLikesInformation.user?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfilePic)

        binding.tvUserName.text =
            postLikesInformation.user?.userName ?: postLikesInformation.user?.firstName?.plus("_")
                ?.plus(postLikesInformation.user.lastName)
    }
}
