package com.meetfriend.app.ui.chatRoom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.Window
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.api.notification.model.NotificationActionState
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.DialogFragmentJoinChatRoomRequestBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class JoinChatRoomRequestDialogFragment : BaseDialogFragment() {

    private val notificationStateSubject: PublishSubject<NotificationActionState> =
        PublishSubject.create()
    val notificationState: Observable<NotificationActionState> = notificationStateSubject.hide()

    companion object {

        private const val RECEIVED_REQUEST_DATA = "receivedRequestData"

        fun newInstance(receivedRequestData: SendJoinChatRoomRequestRequest): JoinChatRoomRequestDialogFragment {
            val args = Bundle()
            receivedRequestData.let { args.putParcelable(RECEIVED_REQUEST_DATA, it) }
            val fragment = JoinChatRoomRequestDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: DialogFragmentJoinChatRoomRequestBinding? = null
    private val binding get() = _binding!!

    private var receivedRequestData: SendJoinChatRoomRequestRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogFragmentJoinChatRoomRequestBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MeetFriendApplication.component.inject(this)

        receivedRequestData = arguments?.getParcelable(RECEIVED_REQUEST_DATA)
            ?: throw IllegalStateException("No args provided")
        loadDataFromIntent()
        listenToViewEvent()
    }

    private fun loadDataFromIntent() {

        binding.tvUserName.text = receivedRequestData?.senderName

        Glide.with(this)
            .load(receivedRequestData?.senderProfile)
            .placeholder(R.drawable.ic_empty_profile_placeholder)
            .error(R.drawable.ic_empty_profile_placeholder)
            .into(binding.ivUserProfileImage)

        binding.tvTime.text = getString(R.string.desc_want_to_join_chat_room)
        binding.tvRoomName.text =
            getString(R.string.label_room_name_bold, receivedRequestData?.conversationName)
    }

    private fun listenToViewEvent() {

        binding.subtitleAppCompatTextView.visibility = GONE

        binding.tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
            receivedRequestData?.let {
                notificationStateSubject.onNext(NotificationActionState.AcceptFromPopup(it))
            }

        }.autoDispose()

        binding.tvReject.throttleClicks().subscribeAndObserveOnMainThread {
            receivedRequestData?.let {
                notificationStateSubject.onNext(NotificationActionState.RejectFromPopup(it))
            }
        }.autoDispose()
    }
}