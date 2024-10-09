package com.meetfriend.app.ui.chatRoom.view

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomListItemActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.base.view.ConstraintLayoutWithLifecycle
import com.meetfriend.app.databinding.ViewChatRoomListBinding
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import com.meetfriend.app.utils.Constant.FiXED_1_INT
import com.meetfriend.app.utils.Constant.FiXED_2_INT
import com.meetfriend.app.utils.Constant.FiXED_3_INT
import com.meetfriend.app.utils.Constant.FiXED_9_INT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.random.Random

class ChatRoomView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val chatRoomItemClickSubject: PublishSubject<ChatRoomListItemActionState> =
        PublishSubject.create()
    val chatRoomItemClick: Observable<ChatRoomListItemActionState> = chatRoomItemClickSubject.hide()

    private lateinit var binding: ViewChatRoomListBinding
    private lateinit var chatRoomInfo: ChatRoomInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_chat_room_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewChatRoomListBinding.bind(view)

        MeetFriendApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUserId()

        binding.apply {
            binding.rlChatRoomDataContainer.throttleClicks().subscribeAndObserveOnMainThread {
                if (!chatRoomInfo.isAdmin && chatRoomInfo.roomType != 2) {
                    if (chatRoomInfo.is_expired == true && chatRoomInfo.isJoin == true) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.unable_to_access_group),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        chatRoomItemClickSubject.onNext(
                            ChatRoomListItemActionState.ContainerClick(
                                chatRoomInfo
                            )
                        )
                    }
                } else {
                    chatRoomItemClickSubject.onNext(
                        ChatRoomListItemActionState.ContainerClick(
                            chatRoomInfo
                        )
                    )
                }
            }.autoDispose()

            binding.tvJoin.throttleClicks().subscribeAndObserveOnMainThread {
                if (binding.tvJoin.text.toString() == context.getString(R.string.label_join)) {
                    chatRoomItemClickSubject.onNext(
                        ChatRoomListItemActionState.JoinClick(
                            chatRoomInfo
                        )
                    )
                    binding.tvJoin.text = context.getString(R.string.label_requested)
                }
            }.autoDispose()
        }
    }

    fun bind(chatRoomInfo: ChatRoomInfo) {
        this.chatRoomInfo = chatRoomInfo

        setProfileImage(chatRoomInfo.filePath)
        setChatRoomName(chatRoomInfo)
        setUserCount(chatRoomInfo.noOfJoinCount ?: 0)
        handleRoomType(chatRoomInfo)
        setBackgroundRandomly()
        setJoinStatus(chatRoomInfo.isRequest)
    }

    private fun setProfileImage(filePath: String?) {
        Glide.with(this)
            .load(filePath)
            .error(R.drawable.ic_empty_profile_placeholder)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivProfileImage)
    }

    private fun setChatRoomName(chatRoomInfo: ChatRoomInfo) {
        if (chatRoomInfo.roomType == 2) {
            binding.tvChatRoomName.text = if (chatRoomInfo.receiver?.id == loggedInUserCache.getLoggedInUserId()) {
                chatRoomInfo.user?.firstName
            } else {
                chatRoomInfo.receiver?.firstName
            }
        } else {
            binding.tvChatRoomName.text = chatRoomInfo.roomName
        }
    }

    private fun setUserCount(noOfJoinCount: Int) {
        binding.tvUserCount.text = noOfJoinCount.toString()
    }

    private fun handleRoomType(chatRoomInfo: ChatRoomInfo) {
        when (chatRoomInfo.roomType) {
            0 -> handleRoomTypeZero(chatRoomInfo)
            1 -> handleRoomTypeOne(chatRoomInfo)
            2 -> hideJoinAndProfileImages()
        }
    }

    private fun handleRoomTypeZero(chatRoomInfo: ChatRoomInfo) {
        binding.tvJoin.visibility = View.GONE
        binding.llOtherProfileImage.visibility = View.VISIBLE

        chatRoomInfo.latestJoin?.let {
            when (it.size) {
                FiXED_1_INT -> loadProfileImage(it)
                FiXED_2_INT -> loadProfileImages(it)
                FiXED_3_INT -> loadProfileImages(it)
                else -> hideProfileImages()
            }
        }
    }

    private fun handleRoomTypeOne(chatRoomInfo: ChatRoomInfo) {
        if (chatRoomInfo.is_expired == true && chatRoomInfo.isJoin == false) {
            hideJoinAndProfileImages()
        } else {
            if (chatRoomInfo.isJoin == true) {
                binding.tvJoin.visibility = View.GONE
                binding.llOtherProfileImage.visibility = View.VISIBLE
            } else {
                binding.tvJoin.visibility = View.VISIBLE
                binding.llOtherProfileImage.visibility = View.GONE
            }
        }
    }

    private fun loadProfileImages(image1: ArrayList<String>) {
        Glide.with(this)
            .load(image1[0])
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivLatestImage1)

        Glide.with(this)
            .load(image1[1])
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivLatestImage2)
        if (image1.size > 2) {
            Glide.with(this)
                .load(image1[2])
                .error(R.drawable.ic_empty_profile_placeholder)
                .into(binding.ivLatestImage3)
        } else {
            binding.ivLatestImage3.visibility = View.GONE
        }

    }

    private fun loadProfileImage(image: ArrayList<String>) {
        Glide.with(this)
            .load(image[0])
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivLatestImage1)

        binding.ivLatestImage2.visibility = View.GONE
        binding.ivLatestImage3.visibility = View.GONE
        binding.addAppCompatImageView.visibility = View.GONE
    }

    private fun hideProfileImages() {
        binding.ivLatestImage1.visibility = View.GONE
        binding.ivLatestImage2.visibility = View.GONE
        binding.ivLatestImage3.visibility = View.GONE
        binding.addAppCompatImageView.visibility = View.GONE
    }

    private fun hideJoinAndProfileImages() {
        binding.tvJoin.visibility = View.GONE
        binding.llOtherProfileImage.visibility = View.GONE
    }

    private fun setBackgroundRandomly() {
        val listOfBackground = arrayListOf(
            R.drawable.bg_chat_room_brown,
            R.drawable.bg_chat_room_pink,
            R.drawable.bg_chat_room_yellow,
            R.drawable.bg_chat_room_green,
            R.drawable.bg_chat_room_grey,
            R.drawable.bg_chat_room_light_pink,
            R.drawable.bg_chat_room_list_view_purple,
            R.drawable.bg_chat_room_orange,
            R.drawable.bg_chat_room_sky
        )
        val randomBackground = listOfBackground[Random.nextInt(FiXED_9_INT)]
        binding.rlChatRoomDataContainer.background = ContextCompat.getDrawable(context, randomBackground)
    }

    private fun setJoinStatus(isRequest: Boolean?) {
        binding.tvJoin.text = if (isRequest == true) {
            context.getString(R.string.label_requested)
        } else {
            context.getString(R.string.label_join)
        }
    }
}
