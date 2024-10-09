package com.meetfriend.app.api.hashtag

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class HashtagModule {

    @Provides
    @Singleton
    fun provideHashtagRetrofitAPI(retrofit: Retrofit): HashtagRetrofitAPI {
        return retrofit.create(HashtagRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideHashtagRepository(
        hashtagRetrofitAPI: HashtagRetrofitAPI
    ): HashtagRepository {
        return HashtagRepository(hashtagRetrofitAPI)
    }
}