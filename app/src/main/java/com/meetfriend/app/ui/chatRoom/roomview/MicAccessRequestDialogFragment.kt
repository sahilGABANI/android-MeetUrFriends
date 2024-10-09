package com.meetfriend.app.ui.chatRoom.roomview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.bumptech.glide.Glide
import com.meetfriend.app.R
import com.meetfriend.app.api.chat.model.SendMicAccessRequestSocketRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.databinding.DialogFragmentJoinChatRoomRequestBinding
import com.meetfriend.app.newbase.BaseDialogFragment
import com.meetfriend.app.newbase.extension.subscribeAndObserveOnMainThread
import com.meetfriend.app.newbase.extension.throttleClicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MicAccessRequestDialogFragment : BaseDialogFragment() {

    private val sendMicAccessStateSubject: PublishSubject<SendMicAccessRequestSocketRequest> =
        PublishSubject.create()
    val sendMicAccessState: Observable<SendMicAccessRequestSocketRequest> =
        sendMicAccessStateSubject.hide()

    companion object {

        private const val RECEIVED_REQUEST_DATA = "receivedRequestData"

        fun newInstance(receivedRequestData: SendMicAccessRequestSocketRequest): MicAccessRequestDialogFragment {
            val args = Bundle()
            receivedRequestData.let { args.putParcelable(RECEIVED_REQUEST_DATA, it) }
            val fragment = MicAccessRequestDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: DialogFragmentJoinChatRoomRequestBinding? = null
    private val binding get() = _binding!!

    private var receivedRequestData: SendMicAccessRequestSocketRequest? = null

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

        binding.joinchatAppCompatTextView.text =
            resources.getString(R.string.label_mic_access_request)
        binding.subtitleAppCompatTextView.text =
            resources.getString(R.string.label_you_received_new_mic_access_request)
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

        binding.tvTime.text =
            getString(R.string.desc_want_mic_access).plus(" ")
                .plus(receivedRequestData?.conversationName).plus(".")
    }

    private fun listenToViewEvent() {

        binding.tvAccept.throttleClicks().subscribeAndObserveOnMainThread {
            receivedRequestData?.let {
                sendMicAccessStateSubject.onNext(it)
            }

        }.autoDispose()

        binding.tvReject.throttleClicks().subscribeAndObserveOnMainThread {
            receivedRequestData?.let {
                dismiss()
            }
        }.autoDispose()
    }
}