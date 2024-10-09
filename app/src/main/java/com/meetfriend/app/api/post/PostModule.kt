package com.meetfriend.app.api.post

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class PostModule {

    @Provides
    @Singleton
    fun providePostRetrofitAPI(retrofit: Retrofit): PostRetrofitAPI {
        return retrofit.create(PostRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun providePostRepository(
        postRetrofitAPI: PostRetrofitAPI,
    ): PostRepository {
        return PostRepository(postRetrofitAPI)
    }
}