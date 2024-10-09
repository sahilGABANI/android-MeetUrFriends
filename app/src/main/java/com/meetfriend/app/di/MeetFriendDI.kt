package com.meetfriend.app.di

import android.app.Application
import android.content.Context
import com.meetfriend.app.api.authentication.AuthenticationModule
import com.meetfriend.app.api.challenge.ChallengeModule
import com.meetfriend.app.api.chat.ChatModule
import com.meetfriend.app.api.cloudflare.CloudFlareModule
import com.meetfriend.app.api.comment.CommentModule
import com.meetfriend.app.api.follow.FollowModule
import com.meetfriend.app.api.gift.GiftsModule
import com.meetfriend.app.api.hashtag.HashtagModule
import com.meetfriend.app.api.livestreaming.LiveModule
import com.meetfriend.app.api.message.MessageModule
import com.meetfriend.app.api.messages.MessagesModule
import com.meetfriend.app.api.monetization.MonetizationModule
import com.meetfriend.app.api.music.MusicModule
import com.meetfriend.app.api.notification.NotificationModule
import com.meetfriend.app.api.post.PostModule
import com.meetfriend.app.api.profile.ProfileModule
import com.meetfriend.app.api.redeem.RedeemModule
import com.meetfriend.app.api.search.SearchModule
import com.meetfriend.app.api.story.StoryModule
import com.meetfriend.app.api.subscription.SubscriptionModule
import com.meetfriend.app.api.userprofile.UserProfileModule
import com.meetfriend.app.api.viewmodelmodule.MeetFriendViewModelProvider
import com.meetfriend.app.application.MeetFriend
import com.meetfriend.app.network.WebServiceModule
import com.meetfriend.app.newbase.network.NetworkModule
import com.meetfriend.app.newbase.prefs.PrefsModule
import com.meetfriend.app.socket.SocketManagerModule
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MeetFriendAppModule(val app: Application) {
    @Provides
    @Singleton
    fun provideApplication(): Application {
        return app
    }

    @Provides
    @Singleton
    fun provideContext(): Context {
        return app
    }
}

@Singleton
@Component(
    modules = [
        MeetFriendAppModule::class,
        NetworkModule::class,
        PrefsModule::class,
        MeetFriendViewModelProvider::class,
        CloudFlareModule::class,
        SocketManagerModule::class,
        AuthenticationModule::class,
        ChatModule::class,
        ProfileModule::class,
        NotificationModule::class,
        SubscriptionModule::class,
        MessageModule::class,
        LiveModule::class,
        PostModule::class,
        FollowModule::class,
        StoryModule::class,
        CommentModule::class,
        UserProfileModule::class,
        MessagesModule::class,
        SearchModule::class,
        GiftsModule::class,
        ChallengeModule::class,
        HashtagModule::class,
        MonetizationModule::class,
        RedeemModule::class,
        MusicModule::class,
        WebServiceModule::class
    ]
)

interface MeetFriendAppComponent : BaseAppComponent {
    fun inject(app: MeetFriend)
}