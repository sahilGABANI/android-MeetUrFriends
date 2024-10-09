package com.meetfriend.app.api.profile

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.socket.SocketDataManager
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ProfileModule {

    @Provides
    @Singleton
    fun provideProfileRetrofitAPI(retrofit: Retrofit): ProfileRetrofitAPI {
        return retrofit.create(ProfileRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        profileRetrofitAPI: ProfileRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): ProfileRepository {
        return ProfileRepository(profileRetrofitAPI, loggedInUserCache)
    }
}