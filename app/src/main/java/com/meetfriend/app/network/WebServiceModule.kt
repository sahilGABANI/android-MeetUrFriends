package com.meetfriend.app.network

import com.meetfriend.app.repositories.ApiRepository
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton
@Module
class WebServiceModule {
    @Provides
    @Singleton
    fun provideWebRetrofitAPI(retrofit: Retrofit): WebService {
        return retrofit.create(WebService::class.java)
    }

    @Provides
    @Singleton
    fun provideApiRepository(
        webService: WebService
    ): ApiRepository {
        return ApiRepository(webService)
    }
}
