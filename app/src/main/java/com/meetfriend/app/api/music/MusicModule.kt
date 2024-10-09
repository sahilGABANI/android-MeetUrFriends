package com.meetfriend.app.api.music

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class MusicModule {
    @Provides
    @Singleton
    fun provideMusicRetrofitAPI(retrofit: Retrofit): MusicRetrofitAPI {
        return retrofit.create(MusicRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        musicRetrofitAPI: MusicRetrofitAPI
    ): MusicRepository {
        return MusicRepository(musicRetrofitAPI)
    }
}