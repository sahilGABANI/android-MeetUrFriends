package com.meetfriend.app.api.redeem

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class RedeemModule {
    @Provides
    @Singleton
    fun provideRedeemRetrofitAPI(retrofit: Retrofit): RedeemRetrofitAPI {
        return retrofit.create(RedeemRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideSearchRepository(
        redeemRetrofitAPI: RedeemRetrofitAPI
    ): RedeemRepository {
        return RedeemRepository(redeemRetrofitAPI)
    }
}