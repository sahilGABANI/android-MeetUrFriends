package com.meetfriend.app.api.message

import com.meetfriend.app.socket.SocketDataManager
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class MessageModule {
    @Provides
    @Singleton
    fun provideMessageRetrofitAPI(retrofit: Retrofit): MessageRetrofitAPI {
        return retrofit.create(MessageRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideChatRepository(
        messageRetrofitAPI: MessageRetrofitAPI,
        socketDataManager: SocketDataManager,
    ): MessageRepository {
        return MessageRepository(messageRetrofitAPI, socketDataManager)
    }
}