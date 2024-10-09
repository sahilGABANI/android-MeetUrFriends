package com.meetfriend.app.api.userprofile


import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
class UserProfileModule {

    @Provides
    @Singleton
    fun provideUserProfileRetrofitAPI(retrofit: Retrofit): UserProfileRetrofitAPI {
        return retrofit.create(UserProfileRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        userProfileRetrofitAPI: UserProfileRetrofitAPI,
    ): UserProfileRepository {
        return UserProfileRepository(userProfileRetrofitAPI)
    }

}