package com.meetfriend.app.api.viewmodelmodule

import com.meetfriend.app.api.authentication.AuthenticationRepository
import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.api.challenge.ChallengeRepository
import com.meetfriend.app.api.chat.ChatRepository
import com.meetfriend.app.api.cloudflare.CloudFlareRepository
import com.meetfriend.app.api.comment.CommentRepository
import com.meetfriend.app.api.follow.FollowRepository
import com.meetfriend.app.api.gift.GiftsRepository
import com.meetfriend.app.api.hashtag.HashtagRepository
import com.meetfriend.app.api.livestreaming.LiveRepository
import com.meetfriend.app.api.message.MessageRepository
import com.meetfriend.app.api.messages.MessagesRepository
import com.meetfriend.app.api.monetization.MonetizationRepository
import com.meetfriend.app.api.music.MusicRepository
import com.meetfriend.app.api.notification.NotificationRepository
import com.meetfriend.app.api.post.PostRepository
import com.meetfriend.app.api.profile.ProfileRepository
import com.meetfriend.app.api.redeem.RedeemRepository
import com.meetfriend.app.api.search.SearchRepository
import com.meetfriend.app.api.story.StoryRepository
import com.meetfriend.app.api.subscription.SubscriptionRepository
import com.meetfriend.app.api.userprofile.UserProfileRepository
import com.meetfriend.app.repositories.ApiRepository
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeCommentViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengePostCommentViewModel
import com.meetfriend.app.ui.challenge.viewmodel.ChallengeViewModel
import com.meetfriend.app.ui.challenge.viewmodel.CreateChallengeViewModel
import com.meetfriend.app.ui.chat.mychatroom.viewmodel.MyChatRoomViewModel
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ForwardMsgViewModel
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.OneToOneChatRoomViewModel
import com.meetfriend.app.ui.chat.onetoonechatroom.viewmodel.ViewOneToOneChatViewModel
import com.meetfriend.app.ui.chatRoom.create.viewmodel.CreateChatRoomViewModel
import com.meetfriend.app.ui.chatRoom.notification.viewmodel.NotificationViewModel
import com.meetfriend.app.ui.chatRoom.profile.viewmodel.ProfileViewModel
import com.meetfriend.app.ui.chatRoom.report.viewmodel.ReportUserViewModel
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.ChatRoomMessageViewModel
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.KickOutViewModel
import com.meetfriend.app.ui.chatRoom.roomview.viewmodel.ViewUserViewModel
import com.meetfriend.app.ui.chatRoom.subscription.viewmodel.SubscriptionViewModel
import com.meetfriend.app.ui.chatRoom.videoCall.viewmodel.VideoCallViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomCreateUserViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomInfoViewModel
import com.meetfriend.app.ui.chatRoom.viewmodel.ChatRoomViewModel
import com.meetfriend.app.ui.coins.viewmodel.CoinPlansViewModel
import com.meetfriend.app.ui.follow.request.viewmodel.FollowRequestViewModel
import com.meetfriend.app.ui.follow.viewmodel.FollowersViewModel
import com.meetfriend.app.ui.follow.viewmodel.FollowingViewModel
import com.meetfriend.app.ui.follow.viewmodel.SuggestedUserViewModel
import com.meetfriend.app.ui.giftsGallery.viewmodel.GiftGalleryBottomSheetViewModel
import com.meetfriend.app.ui.giftsGallery.viewmodel.GiftsGalleryViewModel
import com.meetfriend.app.ui.hashtag.viewmodel.HashTagViewModel
import com.meetfriend.app.ui.helpnsupport.viewmodel.HelpFormViewModel
import com.meetfriend.app.ui.helpnsupport.viewmodel.SendFeedbackViewModel
import com.meetfriend.app.ui.home.create.viewmodel.AddNewPostViewModel
import com.meetfriend.app.ui.home.shortdetails.viewmodel.ShortDetailsViewModel
import com.meetfriend.app.ui.home.shorts.comment.viewmodel.ViewPostViewModel
import com.meetfriend.app.ui.home.shorts.report.viewmodel.ReportDialogViewModel
import com.meetfriend.app.ui.home.shorts.viewmodel.ShortsViewModel
import com.meetfriend.app.ui.home.viewmodel.MainHomeViewModel
import com.meetfriend.app.ui.home.viewmodel.SharePostViewModel
import com.meetfriend.app.ui.livestreaming.inviteuser.viewmodel.InviteUserInLiveViewModel
import com.meetfriend.app.ui.livestreaming.settings.viewmodel.LiveStreamCreateEventSettingViewModel
import com.meetfriend.app.ui.livestreaming.videoroom.viewmodel.VideoRoomsViewModel
import com.meetfriend.app.ui.livestreaming.viewmodel.LiveStreamingViewModel
import com.meetfriend.app.ui.livestreaming.viewmodel.UpdateInviteCoHostStatusViewModel
import com.meetfriend.app.ui.livestreaming.watchlive.viewmodel.LiveWatchingUserViewModel
import com.meetfriend.app.ui.livestreaming.watchlive.viewmodel.WatchLiveVideoViewModel
import com.meetfriend.app.ui.login.viewmodel.LoginViewModel
import com.meetfriend.app.ui.main.viewmodel.HomeViewModel
import com.meetfriend.app.ui.messages.viewmodel.CreateMessageViewModel
import com.meetfriend.app.ui.messages.viewmodel.MessageChatRoomViewModel
import com.meetfriend.app.ui.monetization.earnings.viewmodel.EarningsViewModel
import com.meetfriend.app.ui.monetization.viewmodel.HubRequestViewModel
import com.meetfriend.app.ui.monetization.viewmodel.MonetizationViewModel
import com.meetfriend.app.ui.more.blockedUser.viewmodel.BlockedUserViewModel
import com.meetfriend.app.ui.music.viewmodel.MusicViewModel
import com.meetfriend.app.ui.mygifts.viewmodel.GiftWeeklySummaryViewModel
import com.meetfriend.app.ui.mygifts.viewmodel.GiftsTransactionViewModel
import com.meetfriend.app.ui.myprofile.report.viewmodel.ReportMainUserViewModel
import com.meetfriend.app.ui.myprofile.viewmodel.UserProfileViewModel
import com.meetfriend.app.ui.redeem.history.RedeemHistoryViewModel
import com.meetfriend.app.ui.redeem.viewmodel.RedeemAmountViewModel
import com.meetfriend.app.ui.register.viewmodel.NewRegisterViewModel
import com.meetfriend.app.ui.search.viewmodel.SearchListViewModel
import com.meetfriend.app.ui.trends.viewmodel.CountryHashTagViewModel
import com.meetfriend.app.viewmodal.RegisterViewModal
import com.meetfriend.app.viewmodal.SettingViewModal
import dagger.Module
import dagger.Provides

@Module
class MeetFriendViewModelProvider {

    @Provides
    fun provideLoginViewModel(
        authenticationRepository: AuthenticationRepository,
    ): LoginViewModel {
        return LoginViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideRegisterViewModel(
        authenticationRepository: AuthenticationRepository,
    ): NewRegisterViewModel {
        return NewRegisterViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideWEBRegisterViewModel(
        apiRepository: ApiRepository,
    ): RegisterViewModal {
        return RegisterViewModal(apiRepository)
    }

    @Provides
    fun provideSettingViewModel(
        apiRepository: ApiRepository,
    ): SettingViewModal {
        return SettingViewModal(
            apiRepository
        )
    }

    @Provides
    fun provideChatViewModel(
        chatRepository: ChatRepository,
        profileRepository: ProfileRepository,
        notificationRepository: NotificationRepository,
    ): ChatRoomViewModel {
        return ChatRoomViewModel(
            chatRepository,
            profileRepository,
            notificationRepository,
        )
    }

    @Provides
    fun provideProfileViewModel(
        profileRepository: ProfileRepository,
        cloudFlareRepository: CloudFlareRepository,
        loggedInUserCache: LoggedInUserCache,
        chatRepository: ChatRepository,
        followRepository: FollowRepository
    ): ProfileViewModel {
        return ProfileViewModel(
            profileRepository,
            cloudFlareRepository,
            loggedInUserCache,
            chatRepository,
            followRepository
        )
    }

    @Provides
    fun provideChatRoomMessageViewModel(
        chatRepository: ChatRepository,
        loggedInUserCache: LoggedInUserCache
    ): ChatRoomMessageViewModel {
        return ChatRoomMessageViewModel(
            chatRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideNotificationViewModel(
        notificationRepository: NotificationRepository,
    ): NotificationViewModel {
        return NotificationViewModel(
            notificationRepository,
        )
    }

    @Provides
    fun provideSubscriptionViewModel(
        subscriptionRepository: SubscriptionRepository,
    ): SubscriptionViewModel {
        return SubscriptionViewModel(
            subscriptionRepository,
        )
    }

    @Provides
    fun provideChatRoomInfoViewModel(
        chatRepository: ChatRepository,
        cloudFlareRepository: CloudFlareRepository,
        loggedInUserCache: LoggedInUserCache,
    ): ChatRoomInfoViewModel {
        return ChatRoomInfoViewModel(
            chatRepository,
            cloudFlareRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideMyChatRoomViewModel(
        chatRepository: ChatRepository,
    ): MyChatRoomViewModel {
        return MyChatRoomViewModel(
            chatRepository,

        )
    }

    @Provides
    fun provideOneToOneChatRoomViewModel(
        chatRepository: ChatRepository,
    ): OneToOneChatRoomViewModel {
        return OneToOneChatRoomViewModel(
            chatRepository,
        )
    }

    @Provides
    fun provideCreateChatRoomViewModel(
        chatRepository: ChatRepository,
        cloudFlareRepository: CloudFlareRepository,
        loginUserCache: LoggedInUserCache,
    ): CreateChatRoomViewModel {
        return CreateChatRoomViewModel(
            chatRepository,
            cloudFlareRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideViewUserViewModel(
        chatRepository: ChatRepository,
        loggedInUserCache: LoggedInUserCache,
    ): ViewUserViewModel {
        return ViewUserViewModel(
            chatRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideKickOutViewModel(
        chatRepository: ChatRepository,
        loggedInUserCache: LoggedInUserCache,
    ): KickOutViewModel {
        return KickOutViewModel(
            chatRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideReportUserViewModel(
        chatRepository: ChatRepository,
        hashtagRepository: HashtagRepository
    ): ReportUserViewModel {
        return ReportUserViewModel(
            chatRepository,
            hashtagRepository
        )
    }

    @Provides
    fun provideVideoCallViewModel(
        chatRepository: ChatRepository,
        loggedInUserCache: LoggedInUserCache,
        notificationRepository: NotificationRepository,
    ): VideoCallViewModel {
        return VideoCallViewModel(
            chatRepository,
            loggedInUserCache,
            notificationRepository
        )
    }

    @Provides
    fun provideViewOneToOneChatViewModel(
        chatRepository: ChatRepository,
        notificationRepository: NotificationRepository,
        messageRepository: MessageRepository,
        cloudFlareRepository: CloudFlareRepository,
        loginUserCache: LoggedInUserCache,
        giftsRepository: GiftsRepository
    ): ViewOneToOneChatViewModel {
        return ViewOneToOneChatViewModel(
            chatRepository,
            notificationRepository,
            messageRepository,
            cloudFlareRepository,
            loginUserCache,
            giftsRepository
        )
    }

    @Provides
    fun provideForwardMsgViewModel(
        chatRepository: ChatRepository,
        messagesRepository: MessagesRepository
    ): ForwardMsgViewModel {
        return ForwardMsgViewModel(
            chatRepository,
            messagesRepository
        )
    }

    @Provides
    fun provideLiveStreamingViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): LiveStreamingViewModel {
        return LiveStreamingViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideVideoRoomsViewModel(
        liveRepository: LiveRepository
    ): VideoRoomsViewModel {
        return VideoRoomsViewModel(
            liveRepository
        )
    }

    @Provides
    fun provideWatchLiveVideoViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache,
        followRepository: FollowRepository,
        giftsRepository: GiftsRepository
    ): WatchLiveVideoViewModel {
        return WatchLiveVideoViewModel(
            liveRepository,
            loggedInUserCache,
            followRepository,
            giftsRepository
        )
    }

    @Provides
    fun provideLiveStreamCreateEventSettingViewModel(
        liveRepository: LiveRepository
    ): LiveStreamCreateEventSettingViewModel {
        return LiveStreamCreateEventSettingViewModel(
            liveRepository
        )
    }

    @Provides
    fun provideLiveWatchingUserViewModel(
        liveRepository: LiveRepository,
    ): LiveWatchingUserViewModel {
        return LiveWatchingUserViewModel(
            liveRepository
        )
    }

    @Provides
    fun provideAddNewPostViewModel(
        cloudFlareRepository: CloudFlareRepository,
        postRepository: PostRepository,
        loginUserCache: LoggedInUserCache,
    ): AddNewPostViewModel {
        return AddNewPostViewModel(
            cloudFlareRepository,
            postRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideViewPostViewModel(
        postRepository: PostRepository,
        commentRepository: CommentRepository,
        followRepository: FollowRepository
    ): ViewPostViewModel {
        return ViewPostViewModel(
            postRepository,
            commentRepository,
            followRepository
        )
    }

    @Provides
    fun provideFollowingViewModel(
        followRepository: FollowRepository,
    ): FollowingViewModel {
        return FollowingViewModel(
            followRepository,
        )
    }

    @Provides
    fun provideMainHomeViewModel(
        postRepository: PostRepository,
        storyRepository: StoryRepository,
        cloudFlareRepository: CloudFlareRepository,
        loginUserCache: LoggedInUserCache
    ): MainHomeViewModel {
        return MainHomeViewModel(
            postRepository,
            storyRepository,
            cloudFlareRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideFollowersViewModel(
        followRepository: FollowRepository,
    ): FollowersViewModel {
        return FollowersViewModel(
            followRepository,
        )
    }

    @Provides
    fun provideInviteViewModel(
        followRepository: FollowRepository,
    ): InviteUserInLiveViewModel {
        return InviteUserInLiveViewModel(
            followRepository,
        )
    }

    @Provides
    fun providesShortsViewModel(
        postRepository: PostRepository,
        followRepository: FollowRepository
    ): ShortsViewModel {
        return ShortsViewModel(
            postRepository,
            followRepository
        )
    }

    @Provides
    fun provideSuggestedUserViewModel(
        followRepository: FollowRepository,
    ): SuggestedUserViewModel {
        return SuggestedUserViewModel(
            followRepository,
        )
    }

    @Provides
    fun provideUpdateInviteUserViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): UpdateInviteCoHostStatusViewModel {
        return UpdateInviteCoHostStatusViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun providesUserProfileViewModel(
        userProfileRepository: UserProfileRepository,
        loggedInUserCache: LoggedInUserCache,
        postRepository: PostRepository,
        chatRepository: MessagesRepository,
        followRepository: FollowRepository
    ): UserProfileViewModel {
        return UserProfileViewModel(
            userProfileRepository,
            loggedInUserCache,
            postRepository,
            chatRepository,
            followRepository
        )
    }

    @Provides
    fun provideFollowRequestViewModel(
        followRepository: FollowRepository,
    ): FollowRequestViewModel {
        return FollowRequestViewModel(
            followRepository,
        )
    }

    @Provides
    fun provideHomeViewModel(
        notificationRepository: NotificationRepository,
        monetizationRepository: MonetizationRepository,
        cloudFlareRepository: CloudFlareRepository,
        loggedInUserCache: LoggedInUserCache,
        postRepository: PostRepository,
        authenticationRepository: AuthenticationRepository
    ): HomeViewModel {
        return HomeViewModel(
            notificationRepository,
            monetizationRepository,
            cloudFlareRepository,
            loggedInUserCache,
            postRepository,
            authenticationRepository
        )
    }

    @Provides
    fun provideShortDetailsViewModel(
        postRepository: PostRepository,
        followRepository: FollowRepository
    ): ShortDetailsViewModel {
        return ShortDetailsViewModel(
            postRepository,
            followRepository
        )
    }

    @Provides
    fun provideReportMainUserViewModel(
        userProfileRepository: UserProfileRepository,
    ): ReportMainUserViewModel {
        return ReportMainUserViewModel(
            userProfileRepository,
        )
    }

    @Provides
    fun provideReportDialogViewModel(
        postRepository: PostRepository,
        storyRepository: StoryRepository
    ): ReportDialogViewModel {
        return ReportDialogViewModel(
            postRepository,
            storyRepository
        )
    }

    @Provides
    fun provideMessagesChatViewModel(
        messagesRepository: MessagesRepository,
    ): MessageChatRoomViewModel {
        return MessageChatRoomViewModel(
            messagesRepository,
        )
    }

    @Provides
    fun provideChatRoomCreateUserViewModel(
        profileRepository: ProfileRepository,
        cloudFlareRepository: CloudFlareRepository,
        loggedInUserCache: LoggedInUserCache,
    ): ChatRoomCreateUserViewModel {
        return ChatRoomCreateUserViewModel(
            profileRepository,
            cloudFlareRepository,
            loggedInUserCache,
        )
    }

    @Provides
    fun provideSearchUserViewModel(
        searchRepository: SearchRepository,
        followRepository: FollowRepository
    ): SearchListViewModel {
        return SearchListViewModel(
            searchRepository,
            followRepository
        )
    }

    @Provides
    fun provideSharePostViewModel(
        postRepository: PostRepository
    ): SharePostViewModel {
        return SharePostViewModel(
            postRepository,
        )
    }

    @Provides
    fun provideCreateMessageViewModel(
        followRepository: FollowRepository,
        messagesRepository: MessagesRepository,
        postRepository: PostRepository
    ): CreateMessageViewModel {
        return CreateMessageViewModel(
            followRepository,
            messagesRepository,
            postRepository,
        )
    }

    @Provides
    fun provideGiftsGalleryViewModel(
        giftsRepository: GiftsRepository
    ): GiftsGalleryViewModel {
        return GiftsGalleryViewModel(
            giftsRepository,
        )
    }

    @Provides
    fun provideCoinPlansViewModel(
        giftsRepository: GiftsRepository
    ): CoinPlansViewModel {
        return CoinPlansViewModel(
            giftsRepository,
        )
    }

    @Provides
    fun provideGiftsTransactionViewModel(
        giftsRepository: GiftsRepository,
        liveRepository: LiveRepository
    ): GiftsTransactionViewModel {
        return GiftsTransactionViewModel(
            giftsRepository,
            liveRepository
        )
    }

    @Provides
    fun provideGiftGalleryBottomSheetViewModel(
        giftsRepository: GiftsRepository
    ): GiftGalleryBottomSheetViewModel {
        return GiftGalleryBottomSheetViewModel(
            giftsRepository
        )
    }

    @Provides
    fun provideBlockedUserViewModel(
        followRepository: FollowRepository
    ): BlockedUserViewModel {
        return BlockedUserViewModel(
            followRepository
        )
    }

    @Provides
    fun provideChallengeViewModel(
        challengeRepository: ChallengeRepository,
        followRepository: FollowRepository
    ): ChallengeViewModel {
        return ChallengeViewModel(challengeRepository, followRepository)
    }

    @Provides
    fun provideCreateChallengeViewModel(
        challengeRepository: ChallengeRepository,
        postRepository: PostRepository
    ): CreateChallengeViewModel {
        return CreateChallengeViewModel(
            challengeRepository,
            postRepository
        )
    }

    @Provides
    fun provideChallengeCommentViewModel(
        challengeRepository: ChallengeRepository,
        postRepository: PostRepository
    ): ChallengeCommentViewModel {
        return ChallengeCommentViewModel(
            challengeRepository,
            postRepository
        )
    }

    @Provides
    fun provideChallengePostCommentViewModel(
        challengeRepository: ChallengeRepository,
        postRepository: PostRepository
    ): ChallengePostCommentViewModel {
        return ChallengePostCommentViewModel(
            challengeRepository,
            postRepository
        )
    }

    @Provides
    fun provideSendFeedbackViewModel(
        profileRepository: ProfileRepository
    ): SendFeedbackViewModel {
        return SendFeedbackViewModel(
            profileRepository,
        )
    }

    @Provides
    fun provideHelpFormViewModel(
        profileRepository: ProfileRepository,
        cloudFlareRepository: CloudFlareRepository,
        loggedInUserCache: LoggedInUserCache
    ): HelpFormViewModel {
        return HelpFormViewModel(
            profileRepository,
            cloudFlareRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideHashTagViewModel(
        hashtagRepository: HashtagRepository,
        followRepository: FollowRepository
    ): HashTagViewModel {
        return HashTagViewModel(
            hashtagRepository,
            followRepository
        )
    }

    @Provides
    fun provideCountryHashTagViewModel(
        hashtagRepository: HashtagRepository
    ): CountryHashTagViewModel {
        return CountryHashTagViewModel(
            hashtagRepository,
        )
    }

    @Provides
    fun provideMonetizationViewModel(
        profileRepository: ProfileRepository
    ): MonetizationViewModel {
        return MonetizationViewModel(
            profileRepository
        )
    }

    @Provides
    fun provideHubRequestViewModel(
        monetizationRepository: MonetizationRepository
    ): HubRequestViewModel {
        return HubRequestViewModel(
            monetizationRepository
        )
    }

    @Provides
    fun provideEarningsViewModel(
        monetizationRepository: MonetizationRepository,
        giftsRepository: GiftsRepository
    ): EarningsViewModel {
        return EarningsViewModel(
            monetizationRepository,
            giftsRepository
        )
    }

    @Provides
    fun provideGiftWeeklySummaryViewModel(
        giftsRepository: GiftsRepository
    ): GiftWeeklySummaryViewModel {
        return GiftWeeklySummaryViewModel(
            giftsRepository
        )
    }

    @Provides
    fun provideRedeemAmountViewModel(
        redeemRepository: RedeemRepository
    ): RedeemAmountViewModel {
        return RedeemAmountViewModel(
            redeemRepository
        )
    }

    @Provides
    fun provideRedeemHistoryViewModel(
        redeemRepository: RedeemRepository
    ): RedeemHistoryViewModel {
        return RedeemHistoryViewModel(
            redeemRepository
        )
    }

    @Provides
    fun provideMusicViewModel(
        musicRepository: MusicRepository
    ): MusicViewModel {
        return MusicViewModel(
            musicRepository
        )
    }
}
