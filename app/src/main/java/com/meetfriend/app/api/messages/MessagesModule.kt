package com.meetfriend.app.api.messages

import com.meetfriend.app.socket.SocketDataManager
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class MessagesModule {

    @Provides
    @Singleton
    fun provideChatRetrofitAPI(retrofit: Retrofit): MessagesRetrofitAPI {
        return retrofit.create(MessagesRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideChatRepository(
        messagesRetrofitAPI: MessagesRetrofitAPI,
        socketDataManager: SocketDataManager
    ): MessagesRepository {
        return MessagesRepository(messagesRetrofitAPI, socketDataManager)
    }
}