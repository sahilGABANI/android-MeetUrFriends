package com.meetfriend.app.api.challenge

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ChallengeModule {
    @Provides
    @Singleton
    fun provideChallengeRetrofitAPI(retrofit: Retrofit): ChallengeRetrofitAPI {
        return retrofit.create(ChallengeRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideChallengeRepository(
        challengeRetrofitAPI: ChallengeRetrofitAPI,
    ): ChallengeRepository {
        return ChallengeRepository(challengeRetrofitAPI)
    }
}
