package com.meetfriend.app.ui.camerakit.view

import android.content.Context
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.post.model.MultipleImageDetails
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewMultipleMediaBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MultipleMediaView(context: Context) : ConstraintLayoutWithLifecycle(context) {
    private val chatRoomItemClickSubject: PublishSubject<MultipleImageDetails> =
        PublishSubject.create()
    val chatRoomItemClick: Observable<MultipleImageDetails> = chatRoomItemClickSubject.hide()

    private val closeClickSubject: PublishSubject<MultipleImageDetails> =
        PublishSubject.create()
    val closeClick: Observable<MultipleImageDetails> = closeClickSubject.hide()

    private lateinit var binding: ViewMultipleMediaBinding
    private lateinit var chatRoomInfo: MultipleImageDetails
    companion object {
        const val ZERO_FIVE = 0.5f
        const val THOUSAND_DURATION = 1000L
    }

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_multiple_media, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMultipleMediaBinding.bind(view)

        MeetFriendApplication.component.inject(this)

        binding.ivProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
            chatRoomItemClickSubject.onNext(chatRoomInfo)
        }.autoDispose()
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            closeClickSubject.onNext(chatRoomInfo)
        }.autoDispose()
    }

    fun bind(chatRoomInfo: MultipleImageDetails) {
        this.chatRoomInfo = chatRoomInfo
        binding.ivClose.isVisible = true
        if (chatRoomInfo.isVideo == true) {
            setIsVideo(chatRoomInfo)
        } else {
            if (chatRoomInfo.isMusicVideo == true) {
                binding.tvVideoDuration.isVisible = false
                Glide.with(this)
                    .load(chatRoomInfo.musicVideoPath)
                    .error(R.drawable.dummy_profile_pic)
                    .placeholder(R.drawable.dummy_profile_pic)
                    .into(binding.ivProfileImage)
                binding.tvVideoDuration.isVisible = true
                chatRoomInfo.musicVideoPath?.let {
                    binding.tvVideoDuration.text = getVideoDurationInSeconds(it)
                }
            } else {
                binding.tvVideoDuration.isVisible = false
                if (chatRoomInfo.editedImagePath.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(chatRoomInfo.mainImagePath)
                        .error(R.drawable.dummy_profile_pic)
                        .placeholder(R.drawable.dummy_profile_pic)
                        .into(binding.ivProfileImage)
                } else {
                    Glide.with(this)
                        .load(chatRoomInfo.editedImagePath)
                        .error(R.drawable.dummy_profile_pic)
                        .placeholder(R.drawable.dummy_profile_pic)
                        .into(binding.ivProfileImage)
                }
            }
        }
        if (chatRoomInfo.isSelected == true) {
            val borderWidthInDp = 2
            val scale = resources.displayMetrics.density
            val borderWidthInPx = (borderWidthInDp * scale + ZERO_FIVE).toInt()
            binding.ivProfileImage.borderWidth = borderWidthInPx.toFloat()
            binding.ivProfileImage.borderColor = ContextCompat.getColor(context, R.color.color_tab_purple)
        } else {
            // Hide the border
            binding.ivProfileImage.borderWidth = 0.0f
        }
    }
    private fun setIsVideo(chatRoomInfo: MultipleImageDetails) {
        if (chatRoomInfo.editedVideoPath.isNullOrEmpty()) {
            Glide.with(this)
                .load(chatRoomInfo.mainVideoPath)
                .error(R.drawable.dummy_profile_pic)
                .placeholder(R.drawable.dummy_profile_pic)
                .into(binding.ivProfileImage)
            binding.tvVideoDuration.isVisible = true
            chatRoomInfo.mainVideoPath?.let {
                binding.tvVideoDuration.text = getVideoDurationInSeconds(it)
            }
        } else {
            Glide.with(this)
                .load(chatRoomInfo.editedVideoPath)
                .error(R.drawable.dummy_profile_pic)
                .placeholder(R.drawable.dummy_profile_pic)
                .into(binding.ivProfileImage)
            binding.tvVideoDuration.isVisible = true
            chatRoomInfo.editedVideoPath?.let {
                binding.tvVideoDuration.text = getVideoDurationInSeconds(it)
            }
        }
    }

    fun getVideoDurationInSeconds(filePath: String): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMillis = durationStr?.toLongOrNull() ?: 0L
        retriever.release()
        val durationSeconds = durationMillis / THOUSAND_DURATION
        return "${durationSeconds}s"
    }
}
