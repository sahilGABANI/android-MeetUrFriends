package com.meetfriend.app.api.chat

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.socket.SocketDataManager
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ChatModule {

    @Provides
    @Singleton
    fun provideChatRetrofitAPI(retrofit: Retrofit): ChatRetrofitAPI {
        return retrofit.create(ChatRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideChatRepository(
        chatRetrofitAPI: ChatRetrofitAPI,
        loggedInUserCache: LoggedInUserCache,
        socketDataManager: SocketDataManager
    ): ChatRepository {
        return ChatRepository(chatRetrofitAPI, loggedInUserCache,socketDataManager)
    }
}