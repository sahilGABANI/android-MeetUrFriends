package com.meetfriend.app.ui.more.blockedUser.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.follow.model.BlockInfo
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewBlockedUserBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BlockedUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userClicksSubject: PublishSubject<BlockInfo> = PublishSubject.create()
    val userClicks: Observable<BlockInfo> = userClicksSubject.hide()

    private lateinit var binding: ViewBlockedUserBinding
    private lateinit var blockInfo: BlockInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_blocked_user, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewBlockedUserBinding.bind(view)

        binding.apply {

            tvUpdateInfo.throttleClicks().subscribeAndObserveOnMainThread {
                userClicksSubject.onNext(blockInfo)
            }
        }
    }

    fun bind(blockInfo: BlockInfo) {
        this.blockInfo = blockInfo
        binding.tvName.text = if(!blockInfo.accepted_user.userName.isNullOrEmpty() && blockInfo.accepted_user.userName != "null") blockInfo.accepted_user.userName
            else blockInfo.accepted_user.firstName.plus(" ").plus(blockInfo.accepted_user.lastName)
        binding.tvUserName.text =
            resources.getString(R.string.sign_at_the_rate).plus(blockInfo.accepted_user.userName)
        Glide.with(this)
            .load(blockInfo.accepted_user.profilePhoto)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfilePic)

        if (blockInfo.accepted_user.isVerified == 1){
            binding.ivAccountVerified.visibility = View.VISIBLE
        }else{
            binding.ivAccountVerified.visibility= View.GONE
        }
    }
}