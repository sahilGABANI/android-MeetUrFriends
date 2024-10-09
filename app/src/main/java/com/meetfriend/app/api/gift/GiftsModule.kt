package com.meetfriend.app.api.gift

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class GiftsModule {

    @Provides
    @Singleton
    fun provideGiftsRetrofitAPI(retrofit: Retrofit): GiftsRetrofitAPI {
        return retrofit.create(GiftsRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideGiftsRepository(
        giftsRetrofitAPI: GiftsRetrofitAPI,
    ): GiftsRepository {
        return GiftsRepository(giftsRetrofitAPI)
    }
}