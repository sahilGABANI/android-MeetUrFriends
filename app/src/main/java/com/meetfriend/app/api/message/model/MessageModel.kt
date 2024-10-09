package com.meetfriend.app.api.message.model

import com.google.gson.annotations.SerializedName
import com.meetfriend.app.api.chat.model.MessageInfo

data class EditMessageRequest(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("msg_type")
    val msgType: String,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("forward_ids")
    val forwardIds: String? = null,
)

sealed class MessageAction {
    data class ViewProfile(val messageInfo: MessageInfo) : MessageAction()
    data class Delete(val messageInfo: MessageInfo) : MessageAction()
    data class Reply(val messageInfo: MessageInfo) : MessageAction()
    data class Edit(val messageInfo: MessageInfo) : MessageAction()
    data class More(val messageInfo: MessageInfo) : MessageAction()
    data class Copy(val messageInfo: MessageInfo) : MessageAction()
    data class Forward(val messageInfo: MessageInfo) : MessageAction()
    data class Save(val messageInfo: MessageInfo) : MessageAction()
    data class ViewPhoto(val messageInfo: MessageInfo) : MessageAction()
    data class ViewVideo(val messageInfo: MessageInfo) : MessageAction()
    data class AcceptGiftRequest(val messageInfo: MessageInfo) : MessageAction()
    data class RejectGiftRequest(val messageInfo: MessageInfo) : MessageAction()
    data class StoryView(val messageInfo: MessageInfo) : MessageAction()


}