package com.meetfriend.app.api.follow

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class FollowModule {
    @Provides
    @Singleton
    fun provideFollowRetrofitAPI(retrofit: Retrofit): FollowRetrofitAPI {
        return retrofit.create(FollowRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideFollowRepository(
        followRetrofitAPI: FollowRetrofitAPI,
    ): FollowRepository {
        return FollowRepository(followRetrofitAPI)
    }
}