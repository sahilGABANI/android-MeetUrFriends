package com.meetfriend.app.api.livestreaming

import com.meetfriend.app.socket.SocketDataManager
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class LiveModule {

    @Provides
    @Singleton
    fun provideLiveRetrofitAPI(retrofit: Retrofit): LiveRetrofitAPI {
        return retrofit.create(LiveRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideLiveRepository(
        liveRetrofitAPI: LiveRetrofitAPI,
        socketDataManager: SocketDataManager,
    ): LiveRepository {
        return LiveRepository(liveRetrofitAPI, socketDataManager)
    }
}
