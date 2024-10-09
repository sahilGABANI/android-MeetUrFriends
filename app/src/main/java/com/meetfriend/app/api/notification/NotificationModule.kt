package com.meetfriend.app.api.notification

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationRetrofitAPI(retrofit: Retrofit): NotificationRetrofitAPI {
        return retrofit.create(NotificationRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationRetrofitAPI: NotificationRetrofitAPI,
    ): NotificationRepository {
        return NotificationRepository(notificationRetrofitAPI)
    }
}
