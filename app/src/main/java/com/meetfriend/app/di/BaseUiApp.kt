package com.meetfriend.app.di

import android.app.Application
import com.meetfriend.app.newbase.ActivityManager
import com.meetfriend.app.newbase.BaseBottomSheetDialogFragment
import com.meetfriend.app.newbase.BasicActivity
import com.meetfriend.app.ui.activities.LoginActivity
import com.meetfriend.app.ui.activities.ResetPasswordActivity
import com.meetfriend.app.ui.activities.SettingActivity
import com.meetfriend.app.ui.activities.TagPeopleListActivity
import com.meetfriend.app.ui.activities.WelcomeActivity
import com.meetfriend.app.ui.bottomsheet.SecurityManagmentBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.ForgotPassBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.LoginBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.RegisterBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.switchaccount.SwitchAccountBottomSheet
import com.meetfriend.app.ui.bottomsheet.auth.switchaccount.view.SwitchUserView
import com.meetfriend.app.ui.camerakit.SnapEditorActivity
import com.meetfriend.app.ui.camerakit.SnapPreviewActivity
import com.meetfriend.app.ui.camerakit.SnapkitActivity
import com.meetfriend.app.ui.camerakit.view.MultipleMediaView
import com.meetfriend.app.ui.challenge.*
import com.meetfriend.app.ui.challenge.bottomsheet.*
import com.meetfriend.app.ui.challenge.view.ChallengeCommentReplayView
import com.meetfriend.app.ui.challenge.view.ChallengeCommentView
import com.meetfriend.app.ui.challenge.view.PlayChallengeReplayView
import com.meetfriend.app.ui.challenge.view.PlayChallengeView
import com.meetfriend.app.ui.chat.joinchatroom.JoinChatRoomFragment
import com.meetfriend.app.ui.chat.mychatroom.MyChatRoomFragment
import com.meetfriend.app.ui.chat.mychatroom.view.MyRoomListView
import com.meetfriend.app.ui.chat.onetoonechatroom.ForwardUserListBottomSheet
import com.meetfriend.app.ui.chat.onetoonechatroom.OneToOneChatRoomFragment
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewOneToOneChatRoomActivity
import com.meetfriend.app.ui.chat.onetoonechatroom.ViewVideoMessageActivity
import com.meetfriend.app.ui.chat.onetoonechatroom.view.*
import com.meetfriend.app.ui.chat.profile.ChatRoomProfileFragment
import com.meetfriend.app.ui.chatRoom.ChatRoomActivity
import com.meetfriend.app.ui.chatRoom.ChatRoomInfoActivity
import com.meetfriend.app.ui.chatRoom.ChatRoomUserBottomSheet
import com.meetfriend.app.ui.chatRoom.JoinChatRoomRequestDialogFragment
import com.meetfriend.app.ui.chatRoom.create.CreateRoomActivity
import com.meetfriend.app.ui.chatRoom.notification.ChatRoomNotificationActivity
import com.meetfriend.app.ui.chatRoom.profile.ChatRoomProfileActivity
import com.meetfriend.app.ui.chatRoom.profile.OtherUserFullProfileActivity
import com.meetfriend.app.ui.chatRoom.profile.OtherUserMoreBottomSheet
import com.meetfriend.app.ui.chatRoom.profile.ViewUserProfileActivity
import com.meetfriend.app.ui.chatRoom.profile.view.PreviousProfileImageView
import com.meetfriend.app.ui.chatRoom.report.ReportUserBottomSheet
import com.meetfriend.app.ui.chatRoom.roomview.*
import com.meetfriend.app.ui.chatRoom.roomview.service.VoiceCallEndService
import com.meetfriend.app.ui.chatRoom.subscription.SubscriptionActivity
import com.meetfriend.app.ui.chatRoom.videoCall.OneToOneVideoCallActivity
import com.meetfriend.app.ui.chatRoom.view.ChatRoomView
import com.meetfriend.app.ui.coins.CoinPlansActivity
import com.meetfriend.app.ui.hashtag.HashTagListActivity
import com.meetfriend.app.ui.dual.DualCameraActivity
import com.meetfriend.app.ui.follow.FollowersFragment
import com.meetfriend.app.ui.follow.FollowingFragment
import com.meetfriend.app.ui.follow.ProfileFollowingActivity
import com.meetfriend.app.ui.follow.SuggestedFragment
import com.meetfriend.app.ui.follow.request.FollowRequestActivity
import com.meetfriend.app.ui.follow.view.FollowersListView
import com.meetfriend.app.ui.follow.view.FollowingListView
import com.meetfriend.app.ui.fragments.MoreFragment
import com.meetfriend.app.ui.fragments.SplashFragment
import com.meetfriend.app.ui.fragments.settings.SecurityManagmentFragment
import com.meetfriend.app.ui.giftsGallery.GiftGalleryBottomSheet
import com.meetfriend.app.ui.giftsGallery.GiftsGalleryActivity
import com.meetfriend.app.ui.giftsGallery.SendGiftCoinBottomsheet
import com.meetfriend.app.ui.helpnsupport.HelpFormActivity
import com.meetfriend.app.ui.helpnsupport.SendFeedbackActivity
import com.meetfriend.app.ui.home.ProgressDialogFragment
import com.meetfriend.app.ui.home.SharePostBottomSheet
import com.meetfriend.app.ui.home.create.AddNewPostInfoActivity
import com.meetfriend.app.ui.home.shortdetails.ShortDetailsActivity
import com.meetfriend.app.ui.home.shortdetails.view.PlayShortsDetailView
import com.meetfriend.app.ui.home.shorts.FollowingShortsFragment
import com.meetfriend.app.ui.home.shorts.ForYouShortsFragment
import com.meetfriend.app.ui.home.shorts.ShortsCommentBottomSheet
import com.meetfriend.app.ui.home.shorts.ShortsFragment
import com.meetfriend.app.ui.home.shorts.comment.view.ShortsCommentReplyView
import com.meetfriend.app.ui.home.shorts.comment.view.ShortsCommentView
import com.meetfriend.app.ui.home.shorts.report.ReportDialogFragment
import com.meetfriend.app.ui.home.shorts.view.PlayShortsView
import com.meetfriend.app.ui.home.suggestion.UserSuggestionActivity
import com.meetfriend.app.ui.livestreaming.LiveStreamAcceptRejectDialog
import com.meetfriend.app.ui.livestreaming.LiveStreamingActivity
import com.meetfriend.app.ui.livestreaming.LiveSummaryBottomSheet
import com.meetfriend.app.ui.livestreaming.inviteuser.InviteUserInLiveStreamingBottomSheet
import com.meetfriend.app.ui.livestreaming.settings.StartLiveStreamingSettingsBottomSheet
import com.meetfriend.app.ui.livestreaming.videoroom.LiveRoomActivity
import com.meetfriend.app.ui.livestreaming.watchlive.LiveWatchBottomSheetFragment
import com.meetfriend.app.ui.livestreaming.watchlive.WatchLiveStreamingActivity
import com.meetfriend.app.ui.location.AddLocationActivity
import com.meetfriend.app.ui.main.MainHomeActivity
import com.meetfriend.app.ui.main.MainHomeFragment
import com.meetfriend.app.ui.main.story.AddStoryActivity
import com.meetfriend.app.ui.main.story.HomePageStoryView
import com.meetfriend.app.ui.main.story.StoryListFragment
import com.meetfriend.app.ui.main.story.StoryView
import com.meetfriend.app.ui.main.view.MainVideoPostView
import com.meetfriend.app.ui.messages.MessageListActivity
import com.meetfriend.app.ui.messages.create.CreateMessageActivity
import com.meetfriend.app.ui.monetization.HubRequestFormActivity
import com.meetfriend.app.ui.monetization.NewMonetizationActivity
import com.meetfriend.app.ui.monetization.SelectTaskActivity
import com.meetfriend.app.ui.monetization.earnings.EarningsActivity
import com.meetfriend.app.ui.monetization.earnings.bottomsheet.DateFilterBottomSheet
import com.meetfriend.app.ui.more.blockedUser.BlockedUserActivity
import com.meetfriend.app.ui.music.AddMusicActivity
import com.meetfriend.app.ui.mygifts.SentGiftsFragment
import com.meetfriend.app.ui.mygifts.ReceivedGiftFragment
import com.meetfriend.app.ui.mygifts.GiftWeeklySummaryFragment
import com.meetfriend.app.ui.mygifts.GiftsTransactionActivity
import com.meetfriend.app.ui.myprofile.*
import com.meetfriend.app.ui.redeem.RedeemAmountBottomSheet
import com.meetfriend.app.ui.redeem.history.RedeemHistoryActivity
import com.meetfriend.app.ui.redeem.summary.RedeemSummaryBottomSheet
import com.meetfriend.app.ui.search.SearchListActivity
import com.meetfriend.app.ui.storywork.StoryDetailActivity
import com.meetfriend.app.ui.trends.CountryHashTagFragment
import com.meetfriend.app.ui.trends.HashtagListFragment
import com.meetfriend.app.ui.trends.HashtagShortsFragment
import com.meetfriend.app.ui.trends.UserListFragment
import com.meetfriend.app.ui.trends.view.UserListView
import com.meetfriend.app.utilclasses.MyFcmListenerService
import com.meetfriend.app.utilclasses.ads.AdsManager


/**
 *
 * This base app class will be extended by either Main or Demo project.
 *
 * It then will provide library project app component accordingly.
 *
 */
abstract class BaseUiApp : Application() {
    abstract fun getAppComponent(): BaseAppComponent
    abstract fun setAppComponent(baseAppComponent: BaseAppComponent)
}

/**
 * Base app component
 *
 * This class should have all the inject targets classes
 *
 */
interface BaseAppComponent {
    fun inject(app: Application)
    fun inject(createRoomActivity: CreateRoomActivity)
    fun inject(chatRoomUserBottomSheet: ChatRoomUserBottomSheet)
    fun inject(chatRoomProfileActivity: ChatRoomProfileActivity)
    fun inject(chatRoomActivity: ChatRoomActivity)
    fun inject(viewChatRoomActivity: ViewChatRoomActivity)
    fun inject(moreFragment: MoreFragment)
    fun inject(splashFragment: SplashFragment)
    fun inject(chatRoomNotificationActivity: ChatRoomNotificationActivity)
    fun inject(viewUserBottomSheet: ViewUserBottomSheet)
    fun inject(kickOutDialogFragment: KickOutDialogFragment)
    fun inject(joinChatRoomRequestDialogFragment: JoinChatRoomRequestDialogFragment)

    fun inject(basicActivity: BasicActivity)
    fun inject(welcomeActivity: WelcomeActivity)
    fun inject(securityManagmentBottomSheet: SecurityManagmentBottomSheet)
    fun inject(securityManagmentFragment: SecurityManagmentFragment)
    fun inject(previousProfileImageView: PreviousProfileImageView)
    fun inject(subscriptionActivity: SubscriptionActivity)
    fun inject(chatRoomInfoActivity: ChatRoomInfoActivity)
    fun inject(otherUserMoreBottomSheet: OtherUserMoreBottomSheet)
    fun inject(viewUserProfileActivity: ViewUserProfileActivity)
    fun inject(chatRoomView: ChatRoomView)
    fun inject(voiceCallEndService: VoiceCallEndService)


    fun inject(joinChatRoomFragment: JoinChatRoomFragment)
    fun inject(chatRoomProfileFragment: ChatRoomProfileFragment)
    fun inject(myChatRoomFragment: MyChatRoomFragment)
    fun inject(myRoomListView: MyRoomListView)
    fun inject(storyListFragment: StoryListFragment)
    fun inject(homePageStoryView: HomePageStoryView)
    fun inject(storyView: StoryView)

    fun inject(oneToOneChatRoomView: OneToOneChatRoomView)
    fun inject(oneToOneChatRoomFragment: OneToOneChatRoomFragment)
    fun inject(micAccessRequestDialogFragment: MicAccessRequestDialogFragment)
    fun inject(viewMicAccessUserBottomSheet: ViewMicAccessUserBottomSheet)
    fun inject(reportUserBottomSheet: ReportUserBottomSheet)
    fun inject(oneToOneVideoCallActivity: OneToOneVideoCallActivity)
    fun inject(viewOneToOneChatRoomActivity: ViewOneToOneChatRoomActivity)
    fun inject(myFcmListenerService: MyFcmListenerService)
    fun inject(oneToOneChatMessageView: OneToOneChatMessageView)
    fun inject(forwardUserListBottomSheet: ForwardUserListBottomSheet)
    fun inject(forwardPeopleListView: ForwardPeopleListView)
    fun inject(viewReplyMessage: OneToOneChatReplyMessageView)
    fun inject(oneToOneChatImageMessageView: OneToOneChatImageMessageView)
    fun inject(viewVideoMessageActivity: ViewVideoMessageActivity)
    fun inject(otherUserFullProfileActivity: OtherUserFullProfileActivity)
    fun inject(liveStreamingActivity: LiveStreamingActivity)
    fun inject(watchLiveStreamingActivity: WatchLiveStreamingActivity)
    fun inject(liveRoomActivity: LiveRoomActivity)
    fun inject(startLiveStreamingSettingsBottomSheet: StartLiveStreamingSettingsBottomSheet)
    fun inject(liveWatchBottomSheetFragment: LiveWatchBottomSheetFragment)
    fun inject(addNewPostInfoActivity: AddNewPostInfoActivity)
    fun inject(sharePostBottomSheet: SharePostBottomSheet)
    fun inject(mainHomeFragment: MainHomeFragment)
    fun inject(playShortsView: PlayShortsView)
    fun inject(shortsFragment: ShortsFragment)
    fun inject(shortsCommentView: ShortsCommentView)
    fun inject(shortsCommentReplyView: ShortsCommentReplyView)
    fun inject(myProfileActivity: MyProfileActivity)
    fun inject(postFragment: PostFragment)
    fun inject(shortFragment: ShortFragment)
    fun inject(likesFragment: LikesFragment)
    fun inject(shortDetailsActivity: ShortDetailsActivity)
    fun inject(editProfileActivity: EditProfileActivity)
    fun inject(likeChallengeOptionBottomSheet: LikeChallengeOptionBottomSheet)
    fun inject(followingFragment: FollowingFragment)
    fun inject(followersFragment: FollowersFragment)
    fun inject(followingListView: FollowingListView)
    fun inject(followersListView: FollowersListView)
    fun inject(suggestedFragment: SuggestedFragment)
    fun inject(followingShortsFragment: FollowingShortsFragment)
    fun inject(forYouShortsFragment: ForYouShortsFragment)
    fun inject(storyDetailActivity: StoryDetailActivity)
    fun inject(tagPeopleListActivity: TagPeopleListActivity)
    fun inject(inviteUserInLiveStreamingBottomSheet: InviteUserInLiveStreamingBottomSheet)
    fun inject(liveStreamAcceptRejectDialog: LiveStreamAcceptRejectDialog)
    fun inject(followRequestActivity: FollowRequestActivity)
    fun inject(mainHomeActivity: MainHomeActivity)
    fun inject(resetPasswordActivity: ResetPasswordActivity)
    fun inject(userSuggestionActivity: UserSuggestionActivity)
    fun inject(playShortsDetailView: PlayShortsDetailView)
    fun inject(reportMainUserBottomSheet: ReportMainUserBottomSheet)
    fun inject(reportDialogFragment: ReportDialogFragment)
    fun inject(messageListActivity: MessageListActivity)
    fun inject(searchListActivity: SearchListActivity)
    fun inject(createMessageActivity: CreateMessageActivity)
    fun inject(giftsGalleryActivity: GiftsGalleryActivity)
    fun inject(coinPlansActivity: CoinPlansActivity)
    fun inject(giftsTransactionActivity: GiftsTransactionActivity)
    fun inject(giftGalleryBottomSheet: GiftGalleryBottomSheet)
    fun inject(blockedUserActivity: BlockedUserActivity)
    fun inject(giftMessageView: GiftMessageView)
    fun inject(liveSummaryBottomSheet: LiveSummaryBottomSheet)
    fun inject(allChallengeFragment: AllChallengeFragment)
    fun inject(challengeShortsView: PlayChallengeView)
    fun inject(challengeCommentBottomSheetFragment: ChallengeCommentBottomSheetFragment)
    fun inject(challengeCommentView: ChallengeCommentView)
    fun inject(createChallengeActivity: CreateChallengeActivity)
    fun inject(chooseCountryFragment: ChooseCountryFragment)
    fun inject(chooseStateFragment: ChooseStateFragment)
    fun inject(chooseCityFragment: ChooseCityFragment)
    fun inject(reportOptionBottomSheet: ReportOptionBottomSheet)
    fun inject(moreOptionBottomSheet: MoreOptionBottomSheet)
    fun inject(challengeBottomSheetFragment: ChallengeBottomSheetFragment)
    fun inject(challengeReplayFragment: ChallengeReplayFragment)
    fun inject(playChallengeReplayView: PlayChallengeReplayView)
    fun inject(challengeDetailsActivity: ChallengeDetailsActivity)
    fun inject(challengeCommentReplayView: ChallengeCommentReplayView)
    fun inject(challengeReplyActivity: ChallengeReplyActivity)
    fun inject(addStoryActivity: AddStoryActivity)

    fun inject(sendFeedbackActivity: SendFeedbackActivity)
    fun inject(HelpFormActivity: HelpFormActivity)
    fun inject(challengeFragment: ChallengeFragment)
    fun inject(hashTagListActivity: HashTagListActivity)
    fun inject(hashtagListFragment: HashtagListFragment)
    fun inject(progressDialogFragment: ProgressDialogFragment)
    fun inject(settingActivity: SettingActivity)

    fun inject(hashtagShortsFragment: HashtagShortsFragment)
    fun inject(shortsCommentBottomSheet: ShortsCommentBottomSheet)
    fun inject(userListFragment: UserListFragment)
    fun inject(userListView: UserListView)
    fun inject(countryHashTagFragment: CountryHashTagFragment)
    fun inject(newMonetizationActivity: NewMonetizationActivity)
    fun inject(hubRequestFormActivity: HubRequestFormActivity)
    fun inject(earningsActivity: EarningsActivity)
    fun inject(dateFilterBottomSheet: DateFilterBottomSheet)
    fun inject(selectTaskActivity: SelectTaskActivity)
    fun inject(addLocationActivity: AddLocationActivity)
    fun inject(receivedGiftFragment: ReceivedGiftFragment)
    fun inject(giftWeeklySummaryFragment: GiftWeeklySummaryFragment)
    fun inject(sentGiftsFragment: SentGiftsFragment)
    fun inject(redeemAmountBottomSheet: RedeemAmountBottomSheet)
    fun inject(sendGiftCoinBottomsheet: SendGiftCoinBottomsheet)
    fun inject(redeemHistoryActivity: RedeemHistoryActivity)
    fun inject(redeemSummaryBottomSheet: RedeemSummaryBottomSheet)
    fun inject(oneToOneChatSenderMessageView: OneToOneChatSenderMessageView)
    fun inject(oneToOneChatSenderReplayStoryView: OneToOneChatSenderReplayStoryView)
    fun inject(oneToOneChatReceiverStoryReplayView: OneToOneChatReceiverStoryReplayView)
    fun inject(oneToOneChatReplySenderMessageView: OneToOneChatReplySenderMessageView)
    fun inject(oneToOneChatSenderVideoMessageView: OneToOneChatSenderImageMessageView)
    fun inject(giftSenderMessageView: GiftSenderMessageView)
    fun inject(registerBottomSheet: RegisterBottomSheet)
    fun inject(loginBottomSheet: LoginBottomSheet)
    fun inject(mainVideoPostView: MainVideoPostView)
    fun inject(app: AdsManager)
    fun inject(activityManager: ActivityManager)
    fun inject(profileFollowingActivity: ProfileFollowingActivity)
    fun inject(baseBottomSheetDialogFragment: BaseBottomSheetDialogFragment)
    fun inject(loginActivity: LoginActivity)
    fun inject(switchAccountBottomSheet: SwitchAccountBottomSheet)
    fun inject(switchUserView: SwitchUserView)
    fun inject(snapkitActivity: SnapkitActivity)
    fun inject(snapPreviewActivity: SnapPreviewActivity)
    fun inject(dualCameraActivity: DualCameraActivity)
    fun inject(snapEditorActivity: SnapEditorActivity)
    fun inject(multipleMediaView: MultipleMediaView)
    fun inject(addMusicActivity: AddMusicActivity)
    fun inject(forgotPassBottomSheet: ForgotPassBottomSheet)


}

/**
 * Extension for getting component more easily
 */
fun BaseUiApp.getComponent(): BaseAppComponent {
    return this.getAppComponent()
}
