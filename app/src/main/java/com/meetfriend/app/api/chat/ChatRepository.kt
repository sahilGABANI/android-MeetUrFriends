package com.meetfriend.app.api.chat

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.chat.model.AgoraSDKVoiceCallTokenInfo
import com.meetfriend.app.api.chat.model.AgoraVoiceCallChannelInfo
import com.meetfriend.app.api.chat.model.AgoraVoiceCallChannelInfoRequest
import com.meetfriend.app.api.chat.model.ChatRoom
import com.meetfriend.app.api.chat.model.ChatRoomInfo
import com.meetfriend.app.api.chat.model.ChatRoomMessage
import com.meetfriend.app.api.chat.model.ChatRoomUser
import com.meetfriend.app.api.chat.model.CreateChatRoomRequest
import com.meetfriend.app.api.chat.model.CreateOneToOneChatRequest
import com.meetfriend.app.api.chat.model.GetChatMessageRequest
import com.meetfriend.app.api.chat.model.GetChatRoomAdminRequest
import com.meetfriend.app.api.chat.model.GetMentionUserRequest
import com.meetfriend.app.api.chat.model.GetMiceAccessRequest
import com.meetfriend.app.api.chat.model.JoinChatRoomResponse
import com.meetfriend.app.api.chat.model.JoinRoomRequest
import com.meetfriend.app.api.chat.model.MessageInfo
import com.meetfriend.app.api.chat.model.MiceAccessInfo
import com.meetfriend.app.api.chat.model.ReportUserInfo
import com.meetfriend.app.api.chat.model.ReportUserRequest
import com.meetfriend.app.api.chat.model.RestrictRequest
import com.meetfriend.app.api.chat.model.RestrictUserRequest
import com.meetfriend.app.api.chat.model.SeenMsgRequest
import com.meetfriend.app.api.chat.model.SendJoinChatRoomRequestRequest
import com.meetfriend.app.api.chat.model.SendMicAccessRequestSocketRequest
import com.meetfriend.app.api.chat.model.SendPrivateMessageRequest
import com.meetfriend.app.api.chat.model.TypingRequest
import com.meetfriend.app.api.chat.model.UpdateChatRoomRequest
import com.meetfriend.app.api.chat.model.VoiceCallEndSocketRequest
import com.meetfriend.app.api.chat.model.VoiceCallStartSocketRequest
import com.meetfriend.app.api.gift.model.AcceptRejectGiftRequest
import com.meetfriend.app.newbase.network.MeetFriendResponseConverter
import com.meetfriend.app.newbase.network.model.MeetFriendCommonResponse
import com.meetfriend.app.newbase.network.model.MeetFriendCommonStoryResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponse
import com.meetfriend.app.newbase.network.model.MeetFriendResponseForChat
import com.meetfriend.app.socket.SocketDataManager
import com.meetfriend.app.ui.storywork.models.ResultListResult
import com.meetfriend.app.utils.Constant.FiXED_230_INT
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class ChatRepository(
    private val chatRetrofitAPI: ChatRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache,
    private val socketDataManager: SocketDataManager,
) {
    var listOfConversationId: MutableList<Int> = mutableListOf(FiXED_230_INT)
    private val meetFriendResponseConverter: MeetFriendResponseConverter = MeetFriendResponseConverter()

    fun createChatRoom(createChatRoomRequest: CreateChatRoomRequest): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return chatRetrofitAPI.createChatRoom(createChatRoomRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun getPrivateChatRoom(page: Int, perPage: Int): Single<MeetFriendResponse<ChatRoom>> {
        return chatRetrofitAPI.getPrivateChatRoom(page, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getPublicChatRoom(page: Int, perPage: Int): Single<MeetFriendResponse<ChatRoom>> {
        return chatRetrofitAPI.getPublicChatRoom(page, perPage)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getChatRoomMessage(
        chatMessageRequest: GetChatMessageRequest,
        pageNo: Int
    ): Single<MeetFriendResponse<ChatRoomMessage>> {
        return chatRetrofitAPI.getChatRoomMessage(chatMessageRequest, pageNo)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getChatRoomInfo(conversationId: Int): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return chatRetrofitAPI.chatRoomInfo(conversationId)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable {
        return socketDataManager.joinRoom(joinRoomRequest)
    }

    fun observeRoomJoined(): Observable<JoinChatRoomResponse> {
        return socketDataManager.roomJoined()
    }

    fun sendPrivateMessage(sendPrivateMessageRequest: SendPrivateMessageRequest): Completable {
        return socketDataManager.sendPrivateMessage(sendPrivateMessageRequest)
    }

    fun observeNewMessage(): Observable<MessageInfo> {
        return socketDataManager.privateMessage()
    }

    fun sendJoinRoomRequest(sendJoinChatRoomRequest: SendJoinChatRoomRequestRequest): Completable {
        return socketDataManager.sendJoinRoomRequest(sendJoinChatRoomRequest)
    }

    fun observeJoinRoomRequest(): Observable<SendJoinChatRoomRequestRequest> {
        return socketDataManager.receiveJoinRoomRequest()
    }

    fun observeKickedOutUser(): Observable<MessageInfo> {
        return socketDataManager.kickedOutUser()
    }

    fun observeBannedUser(): Observable<MessageInfo> {
        return socketDataManager.bannedUser()
    }

    fun observeRestrictUser(): Observable<RestrictRequest> {
        return socketDataManager.restrictUser()
    }

    fun observeVoiceCallStarted(): Observable<VoiceCallStartSocketRequest> {
        return socketDataManager.voiceCallStarted()
    }

    fun observeVoiceCallEnded(): Observable<VoiceCallEndSocketRequest> {
        return socketDataManager.voiceCallEnded()
    }

    fun startVoiceCall(voiceCallStartSocketRequest: VoiceCallStartSocketRequest): Completable {
        listOfConversationId.add(voiceCallStartSocketRequest.conversationId)
        return socketDataManager.voiceCallStart(voiceCallStartSocketRequest)
    }

    fun endVoiceCall(endVoiceCallEndSocketRequest: VoiceCallEndSocketRequest): Completable {
        listOfConversationId.remove(endVoiceCallEndSocketRequest.conversationId)
        return socketDataManager.voiceCallEnd(endVoiceCallEndSocketRequest)
    }

    fun off(name: String) {
        socketDataManager.off(name)
    }

    fun getAgoraChannelName(
        agoraVoiceCallChannelInfoRequest: AgoraVoiceCallChannelInfoRequest
    ): Single<AgoraVoiceCallChannelInfo> {
        return chatRetrofitAPI.getAgoraChannelName(agoraVoiceCallChannelInfoRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithResponseForCreateChatRoom(it) }
    }

    fun getAgoraSDKToken(channelName: String, tokenRole: Int): Single<AgoraSDKVoiceCallTokenInfo> {
        val loggedInUserId = loggedInUserCache.getLoggedInUserId()
        return chatRetrofitAPI.getAgoraCallToken(channelName, tokenRole, loggedInUserId)
    }

    fun createOneToOneChat(
        createOneToOneChatRequest: CreateOneToOneChatRequest
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return chatRetrofitAPI.createOneToOneChat(createOneToOneChatRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun updateRoom(
        conversationId: Int,
        updateChatRoomRequest: UpdateChatRoomRequest
    ): Single<MeetFriendResponseForChat<ChatRoomInfo>> {
        return chatRetrofitAPI.updateRoom(conversationId, updateChatRoomRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun getOneToOneChatRoom(perPage: Int, page: Int): Single<MeetFriendResponse<ChatRoom>> {
        return chatRetrofitAPI.getOneToOneChatRoom(perPage, page)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getMyPrivateChatRoom(perPage: Int, page: Int): Single<MeetFriendResponse<ChatRoom>> {
        return chatRetrofitAPI.getMyPrivateChatRoom(perPage, page)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun deleteChatRoom(conversationId: Int): Single<MeetFriendCommonResponse> {
        return chatRetrofitAPI.deleteChatRoom(conversationId)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun getChatRoomAdmin(
        getChatRoomAdminRequest: GetChatRoomAdminRequest
    ): Single<MeetFriendResponseForChat<List<ChatRoomUser>>> {
        return chatRetrofitAPI.getChatRoomAdmin(getChatRoomAdminRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForCreateChatRoom(it) }
    }

    fun getUserForMention(
        getMentionUserRequest: GetMentionUserRequest
    ): Single<MeetFriendResponse<List<MeetFriendUser>>> {
        return chatRetrofitAPI.getUserForMention(getMentionUserRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun restrictUser(restrictUserRequest: RestrictUserRequest): Single<MeetFriendCommonResponse> {
        return chatRetrofitAPI.restrictUser(restrictUserRequest)
            .flatMap { meetFriendResponseConverter.convertCommonResponse(it) }
    }

    fun sendMicAccessRequest(micAccessRequestSocketRequest: SendMicAccessRequestSocketRequest): Completable {
        return socketDataManager.sendMiceAccessRequest(micAccessRequestSocketRequest)
    }

    fun observeMicAccessRequest(): Observable<SendMicAccessRequestSocketRequest> {
        return socketDataManager.miceAccessRequest()
    }

    fun acceptMicAccessRequest(sendMicAccessRequestSocketRequest: SendMicAccessRequestSocketRequest): Completable {
        return socketDataManager.acceptMicRequest(sendMicAccessRequestSocketRequest)
    }

    fun observeAcceptMicAccessRequest(): Observable<SendMicAccessRequestSocketRequest> {
        return socketDataManager.acceptedRequest()
    }

    fun revokeMicAccess(revokeMicAccessRequest: SendMicAccessRequestSocketRequest): Completable {
        return socketDataManager.revokeMicAccess(revokeMicAccessRequest)
    }

    fun observeRevokeMicAccess(): Observable<SendMicAccessRequestSocketRequest> {
        return socketDataManager.micAccessRevoked()
    }

    fun getMiceAccessInfo(
        getMiceAccessRequest: GetMiceAccessRequest
    ): Single<MeetFriendResponse<List<MiceAccessInfo>>> {
        return chatRetrofitAPI.getMiceAccessInfo(getMiceAccessRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun reportUser(reportUserRequest: ReportUserRequest): Single<MeetFriendResponse<ReportUserInfo>> {
        return chatRetrofitAPI.reportUser(reportUserRequest)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun storyByUser(userId: Int): Single<MeetFriendCommonStoryResponse<ResultListResult>> {
        return chatRetrofitAPI.storyByUser(userId)
            .flatMap { meetFriendResponseConverter.convertToSingleWithFullResponseForStory(it) }
    }

    fun leaveOneToOneRoom(leaveRoomRequest: GetChatRoomAdminRequest): Completable {
        return socketDataManager.leaveOneToOneRoom(leaveRoomRequest)
    }

    fun endVideoCall(endVoiceCallEndSocketRequest: VoiceCallEndSocketRequest): Completable {
        return socketDataManager.videoCallEnd(endVoiceCallEndSocketRequest)
    }

    fun observeVideoCallEnded(): Observable<VoiceCallEndSocketRequest> {
        return socketDataManager.videoCallEnded()
    }

    fun acceptRejectGiftRequest(acceptRejectGiftRequest: AcceptRejectGiftRequest): Completable {
        return socketDataManager.acceptRejectGiftRequest(acceptRejectGiftRequest)
    }

    fun observeGiftRequest(): Observable<MessageInfo> {
        return socketDataManager.observeGiftRequest()
    }

    fun startTyping(typingRequest: TypingRequest): Completable {
        return socketDataManager.startTyping(typingRequest)
    }

    fun observeTyping(): Observable<TypingRequest> {
        return socketDataManager.observeStartTyping()
    }
    fun stopTyping(typingRequest: TypingRequest): Completable {
        return socketDataManager.stopTyping(typingRequest)
    }

    fun observeStopTyping(): Observable<TypingRequest> {
        return socketDataManager.observeStopTyping()
    }

    fun seenMsg(seenMsgRequest: SeenMsgRequest): Completable {
        return socketDataManager.seenMsg(seenMsgRequest)
    }

    fun observeMsgSeen(): Observable<Unit> {
        return socketDataManager.observeMsgSeen()
    }
}
