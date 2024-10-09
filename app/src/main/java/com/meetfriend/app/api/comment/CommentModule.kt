package com.meetfriend.app.api.comment

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class CommentModule {

    @Provides
    @Singleton
    fun providesCommentRetrofitAPI(retrofit: Retrofit): CommentRetrofitAPI {
        return retrofit.create(CommentRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideCommentRepository(commentRetrofitAPI: CommentRetrofitAPI): CommentRepository {
        return CommentRepository(commentRetrofitAPI)
    }
}