package com.meetfriend.app.socket

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.newbase.extension.getSocketBaseUrl
import io.reactivex.Completable
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.json.JSONObject
import timber.log.Timber

class SocketService(
    private val loggedInUserCache: LoggedInUserCache
) {
    private lateinit var socket: Socket
    private lateinit var options: IO.Options
    private var gson: Gson = GsonBuilder().create()
    val isConnected: Boolean get() = socket.connected()

    init {
        initSocket()
    }

    private fun initSocket() {
        val socketOptionBuilder = IO.Options.builder().apply {
            setReconnection(true)
        }

        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(
                Interceptor { chain ->
                    val original = chain.request()
                    try {
                        val requestBuilder = original.newBuilder()
                        requestBuilder.addHeader("userId", loggedInUserCache.getLoggedInUserId().toString())
                        requestBuilder.addHeader(
                            "username",
                            (loggedInUserCache.getLoggedInUser()?.loggedInUser?.firstName.toString())
                        )
                        chain.proceed(requestBuilder.build())
                    } catch (e: IllegalArgumentException) {
                        // Handle the exception, you may log it
                        e.printStackTrace()

                        // Create a new request without the problematic header and proceed
                        val newRequest = original.newBuilder()
                            .removeHeader("username") // Remove the problematic header
                            .build()
                        chain.proceed(newRequest)
                    }
                }
            ).build()

        options = socketOptionBuilder.build()
        options.webSocketFactory = httpClient
        options.callFactory = httpClient
        socket = IO.socket(getSocketBaseUrl(), options)
        socket.apply {
            io().timeout(-1)
        }
    }

    fun connect() {
        if (!isConnected && !loggedInUserCache.getLoginUserToken().isNullOrEmpty()) {
            Timber.tag(SOCKET_TAG).i("Call socket connection")
            socket.connect()
        } else {
            Timber.e(
                "Socket connected :::: %s  User token :::: %s ",
                isConnected,
                !loggedInUserCache.getLoginUserToken().isNullOrEmpty()
            )
        }
    }

    fun disconnect() {
        Timber.tag(SOCKET_TAG).i("Call socket disconnect")
        socket.disconnect()
    }

    fun getGson() = gson

    companion object {
        const val SOCKET_TAG = "SocketTag"
        const val EVENT_CONNECT = Socket.EVENT_CONNECT
        const val EVENT_CONNECT_ERROR = Socket.EVENT_CONNECT_ERROR
        const val EVENT_DISCONNECT = Socket.EVENT_DISCONNECT
        const val EVENT_ROOM = "room"
        const val EVENT_ROOM_JOIN = "roomJoin"
        const val EVENT_SEND_PRIVATE_MESSAGE = "sendPrivateMessage"
        const val EVENT_PRIVATE_MESSAGE = "receivePrivateMessage"
        const val EVENT_SEND_JOIN_ROOM_REQUEST = "sendJoinRequest"
        const val EVENT_RECEIVE_JOIN_ROOM_REQUEST = "receiveJoinRequest"
        const val EVENT_KICKED_OUT = "kickedOut"
        const val EVENT_BANNED_USER = "bannedUser"
        const val EVENT_VOICE_CALL_START = "callStart"
        const val EVENT_VOICE_CALL_END = "callEnd"
        const val EVENT_VOICE_CALL_START_LISTEN = "callJoin"
        const val EVENT_VOICE_CALL_END_LISTEN = "callLeave"
        const val EVENT_RESTRICT_USER_LISTEN = "restrictRequest"
        const val EVENT_MICE_REQUEST = "miceRequest"
        const val EVENT_RECEIVE_MICE_REQUESTED = "miceRequested"
        const val EVENT_REQUEST_ACCEPT = "miceRequestAccepted"
        const val EVENT_REQUEST_ACCEPTED = "miceRequestAccepted"
        const val EVENT_REVOKE_MIC_ACCESS = "revokeMiceAccess"
        const val EVENT_LEAVE_ONE_TO_ONE_CHAT = "leaveOnetoOne"
        const val EVENT_ACCEPT_REJECT_GIFT_REQUEST = "accprtRejectPrivateMessage"

        // Live
        const val EVENT_CREATE_LIVE_ROOM = "createRoom"
        const val EVENT_ADDED_NEW_ROOM = "addedNewRoom"
        const val EVENT_JOIN_LIVE_ROOM = "joinRoom"
        const val EVENT_UPDATE_LIVE_COUNTER = "updateLiveCounter"
        const val EVENT_ATTENDEE_JOIN_LIVE_ROOM = "attendeeJoinRoom"
        const val EVENT_SEND_HEART = "onHeart"
        const val EVENT_LIVE_HEART = "updateHeartCounter"
        const val EVENT_LIVE_COMMENT = "liveComment"
        const val EVENT_LIVE_NEW_MESSAGE = "onNewMessage"
        const val EVENT_JOIN_ROOM_AS_CO_HOST = "joinRoomAsCoHost"
        const val EVENT_LIVE_CO_HOST = "liveCohost"
        const val EVENT_KICK_USER = "kickUser"
        const val EVENT_KICKED_USER = "kickedUser"
        const val EVENT_RESTRICT_USER = "restrictUser"
        const val EVENT_LEAVE_LIVE_ROOM = "leaveRoom"
        const val EVENT_LIVE_END = "liveEnd"
        const val EVENT_UPDATE_VIEWER_COUNTER = "decrementViewerCounter"

        // game in live
        const val EVENT_SEND_GIFT = "sendGift"
        const val EVENT_RECEIVE_GIFT = "userReceiveGifts"
        const val EVENT_TOP_GIFTER = "topGifter"
        const val EVENT_START_GAME = "startGame"
        const val EVENT_GAME_STARTED = "gameStarted"
        const val EVENT_END_GAME = "endGame"
        const val EVENT_GAME_ENDED = "gameEnded"
        const val EVENT_WANT_TO_PLAY_GAME = "wantToPlayGame"
        const val EVENT_REMOVE_CO_HOST_FROM_LIVE = "removeCohost"

        // Video call
        const val EVENT_VIDEO_CALL_END = "videoCallEnd"
        const val EVENT_VIDEO_CALL_END_LISTEN = "videoCallLeave"

        // Chatroom typing
        const val EVENT_TYPING = "typing"
        const val EVENT_STOP_TYPING = "stopTyping"

        // Chatroom msg seen
        const val EVENT_MSG_SEEN = "seenMessage"
    }

    fun request(name: String, jSONObject: JSONObject): Completable =
        if (isConnected) {
            Timber.tag(SOCKET_TAG).i("Request Event Name : $name")
            Timber.tag(SOCKET_TAG).i("Request Event RequestJson : $jSONObject")
            socket.emit(name, jSONObject)
            Completable.complete()
        } else {
            Completable.error(SocketNotConnectedException("Socket is not connected while calling $name event"))
        }

    fun on(name: String, listener: Emitter.Listener) {
        socket.on(name, listener)
    }

    fun off(name: String) {
        Timber.tag(SOCKET_TAG).i("Remove(off) Event Name : $name")
        socket.off(name)
    }

}

class SocketNotConnectedException(message: String) : Throwable(message)