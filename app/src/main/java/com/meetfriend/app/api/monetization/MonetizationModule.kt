package com.meetfriend.app.api.monetization

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class MonetizationModule {


    @Provides
    @Singleton
    fun provideMonetizationRetrofitAPI(retrofit: Retrofit): MonetizationRetrofitAPI {
        return retrofit.create(MonetizationRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideAuthenticationRepository(
        monetizationRetrofitAPI: MonetizationRetrofitAPI
    ): MonetizationRepository {
        return MonetizationRepository(monetizationRetrofitAPI)
    }
}