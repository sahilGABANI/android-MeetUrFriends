package com.meetfriend.app.ui.chatRoom.profile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.profile.model.ProfileItemInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewViewUserProfileBinding

class ViewUserprofileView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewViewUserProfileBinding
    private lateinit var profileItemInfo: ProfileItemInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_view_user_profile, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding = ViewViewUserProfileBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(profileItemInfo: ProfileItemInfo) {
        this.profileItemInfo = profileItemInfo

        Glide.with(context)
            .load(profileItemInfo.filePath)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivProfileImage)


    }
}