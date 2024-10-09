package com.meetfriend.app.api.subscription


import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class SubscriptionModule {

    @Provides
    @Singleton
    fun provideSubscriptionRetrofitAPI(retrofit: Retrofit): SubscriptionRetrofitAPI {
        return retrofit.create(SubscriptionRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        subscriptionRetrofitAPI: SubscriptionRetrofitAPI
    ): SubscriptionRepository {
        return SubscriptionRepository(subscriptionRetrofitAPI)
    }
}