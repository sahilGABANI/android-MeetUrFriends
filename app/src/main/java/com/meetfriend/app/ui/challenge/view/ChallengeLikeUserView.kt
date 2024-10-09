package com.meetfriend.app.ui.challenge.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.challenge.model.ChallengeUserModel
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.NewLikedUserRowBinding

class ChallengeLikeUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: NewLikedUserRowBinding

    private lateinit var result: ChallengeUserModel

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_liked_user_row, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = NewLikedUserRowBinding.bind(view)
    }

    fun bind(result: ChallengeUserModel) {
        this.result = result
        binding.tvUsername.text = result.user?.userName ?: result.user?.firstName?.plus("_")
            ?.plus(result.user.lastName)


        Glide.with(this)
            .load(result.user?.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserPicture)

    }

}