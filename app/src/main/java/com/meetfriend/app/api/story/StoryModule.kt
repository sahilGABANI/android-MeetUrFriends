package com.meetfriend.app.api.story

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class StoryModule {

    @Provides
    @Singleton
    fun provideStoryRetrofitAPI(retrofit: Retrofit) : StoryRetrofitAPI{
        return retrofit.create(StoryRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideStoryRepository(storyRetrofitAPI: StoryRetrofitAPI) :StoryRepository {
        return StoryRepository(storyRetrofitAPI)
    }
}