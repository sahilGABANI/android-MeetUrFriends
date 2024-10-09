package com.meetfriend.app.api.search

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class SearchModule {

    @Provides
    @Singleton
    fun provideSearchRetrofitAPI(retrofit: Retrofit): SearchRetrofitAPI {
        return retrofit.create(SearchRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideSearchRepository(
        searchRetrofitAPI: SearchRetrofitAPI
    ): SearchRepository {
        return SearchRepository(searchRetrofitAPI)
    }
}