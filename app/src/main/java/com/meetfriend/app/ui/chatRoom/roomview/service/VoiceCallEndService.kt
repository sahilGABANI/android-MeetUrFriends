package com.meetfriend.app.ui.chatRoom.roomview.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.chat.model.VoiceCallEndSocketRequest
import com.meetfriend.app.application.MeetFriendApplication
import com.meetfriend.app.newbase.ActivityManager
import com.meetfriend.app.newbase.extension.subscribeOnIoAndObserveOnMainThread
import timber.log.Timber
import javax.inject.Inject

class VoiceCallEndService : Service() {
    @Inject
    lateinit var chatRepository: ChatRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        MeetFriendApplication.component.inject(this)
        super.onCreate()
        if (ActivityManager.getInstance().foregroundActivity == null) {
            chatRepository.apply {
                listOfConversationId.forEach { conversationId ->
                    this.endVoiceCall(VoiceCallEndSocketRequest(conversationId))
                        .subscribeOnIoAndObserveOnMainThread({
                            stopSelf()
                        }, {
                            stopSelf()
                            Timber.e(it)
                        })
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}