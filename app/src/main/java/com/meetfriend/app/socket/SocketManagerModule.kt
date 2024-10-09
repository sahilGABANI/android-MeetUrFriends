package com.meetfriend.app.socket

import com.meetfriend.app.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SocketManagerModule {
    @Provides
    @Singleton
    fun provideSocketService(loggedInUserCache: LoggedInUserCache): SocketService {
        return SocketService(loggedInUserCache)
    }

    @Provides
    @Singleton
    fun provideSocketDataManager(socketService: SocketService): SocketDataManager {
        return SocketDataManager(socketService)
    }
}
