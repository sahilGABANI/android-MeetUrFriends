package com.meetfriend.app.api.cloudflare

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class CloudFlareModule {

    @Provides
    @Singleton
    fun provideCloudFlareRetrofitAPI(retrofit: Retrofit): CloudFlareRetrofitAPI {
        return retrofit.create(CloudFlareRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideCloudFlareRepository(
        cloudFlareRetrofitAPI: CloudFlareRetrofitAPI,
    ): CloudFlareRepository {
        return CloudFlareRepository(cloudFlareRetrofitAPI)
    }
}