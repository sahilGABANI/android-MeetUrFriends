package com.meetfriend.app.api.authentication

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.meetfriend.app.api.authentication.model.ChatUser
import com.meetfriend.app.api.authentication.model.LoggedInUser
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import com.meetfriend.app.api.livestreaming.model.LiveEventInfo
import com.meetfriend.app.newbase.prefs.LocalPrefs
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.IOException

/**
 *
 * This class is responsible for caching the logged in user so that the user can
 * be accessed without having to contact the server meaning it's faster and can
 * be accessed offline
 */
class LoggedInUserCache(
    private val localPrefs: LocalPrefs,
) {

    private var meetFriendUser: MeetFriendUser? = null
    private var chatuserProfile: ChatUser? = null

    private val loggedInUserCacheUpdatesSubject = PublishSubject.create<Unit>()

    private val userAuthenticationFailSubject = PublishSubject.create<Unit>()
    val userAuthenticationFail: Observable<Unit> = userAuthenticationFailSubject.hide()

    private val invitedAsCoHostSubject = PublishSubject.create<LiveEventInfo>()
    val invitedAsCoHost: Observable<LiveEventInfo> = invitedAsCoHostSubject.hide()

    var userUnauthorized: Boolean = false

    enum class PreferenceKey(val identifier: String) {
        LOGGED_IN_USER_JSON_KEY("loggedInUser"),
        LOGGED_IN_USER_TOKEN("token"),
        USER_FIRST_TIME_LOGIN("userFirstTimeLogin"),
        PROFILE_UPDATED_STATUS("profileUpdatedStatus"),
        LOGGED_IN_USER_SOCKET_TOKEN("socket_token"),
        CHAT_USER_PROFILE_IMAGE("chatUserProfileImage"),
        CHAT_USER_NAME("chatUserName"),
        IS_CHAT_USER_CREATED("isChatUserCreated"),
        CHAT_USER_INFO("chatUserProfile"),
        LOGGED_IN_USER_CHANNEL_ID("channel_id"),
        LOGGED_IN_USER_EMAIL_OR_PHONE("email_or_phone"),
        LOGGED_IN_USER_PASSWORD("password"),
        LOGGED_IN_USER_REMEMBER_ME("remember"),
        LOCATION_PERMISSION("locationPermission"),
        HUB_REQUEST_SENT("hub_request_sent"),
        PLACE_API_KEY("place_api_key"),
        NATIVE_AD_ID("native_ad_id"),
        INTERSTITIAL_AD_ID("interstitial_ad_id"),
        BANNER_AD_ID("banner_ad_id"),
        APP_OPEN_AD_ID("app_open_ad_id"),
        CLICK_COUNT_ADS("click_count_ads"),
        SCROLL_COUNT_ADS("scroll_count_ads"),
        HOME_LOCATION_PERMISSION("home_location_permission"),
        SNAP_AGREEMENT("snap_agreement")
    }

    init {
        userUnauthorized = false
        loadLoggedInUserFromLocalPrefs()
        loadChatUserFromLocalPrefs()
    }

    private var loggedInUserTokenLocalPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier, value)
        }

    private var userFirstTimeLoginPref: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.USER_FIRST_TIME_LOGIN.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.USER_FIRST_TIME_LOGIN.identifier, value)
        }

    private var profileUpdatedStatus: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.PROFILE_UPDATED_STATUS.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.PROFILE_UPDATED_STATUS.identifier, value)
        }
    private var chatUserProfileImagePref: String?
        get() {
            return localPrefs.getString(PreferenceKey.CHAT_USER_PROFILE_IMAGE.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.CHAT_USER_PROFILE_IMAGE.identifier, value)
        }

    private var isChatUserCreatedPref: Boolean?
        get() {
            return localPrefs.getBoolean(PreferenceKey.IS_CHAT_USER_CREATED.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.USER_FIRST_TIME_LOGIN.identifier, false)
        }

    private var loggedInUserEmailOrPhoneLocalPref: String?
        get() {
            return localPrefs.getString(
                PreferenceKey.LOGGED_IN_USER_EMAIL_OR_PHONE.identifier,
                null
            )
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOGGED_IN_USER_EMAIL_OR_PHONE.identifier, value)
        }

    private var loggedInUserPasswordLocalPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOGGED_IN_USER_PASSWORD.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOGGED_IN_USER_PASSWORD.identifier, value)
        }

    private var loggedInUserRememberMeLocalPref: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.LOGGED_IN_USER_REMEMBER_ME.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.LOGGED_IN_USER_REMEMBER_ME.identifier, value)
        }

    private var locationPermissionAskedPref: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.LOCATION_PERMISSION.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.LOCATION_PERMISSION.identifier, value)
        }

    private var hubRequestSentPref: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.HUB_REQUEST_SENT.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.HUB_REQUEST_SENT.identifier, value)
        }
    private var placeApiKeyPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.PLACE_API_KEY.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.PLACE_API_KEY.identifier, value)
        }

    private var nativeAdIdPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.NATIVE_AD_ID.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.NATIVE_AD_ID.identifier, value)
        }

    private var interstitialAdIdPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.INTERSTITIAL_AD_ID.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.INTERSTITIAL_AD_ID.identifier, value)
        }

    private var bannerAdIdPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.BANNER_AD_ID.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.BANNER_AD_ID.identifier, value)
        }

    private var appOpenAdIdPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.APP_OPEN_AD_ID.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.APP_OPEN_AD_ID.identifier, value)
        }

    private var clickCountAdPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.CLICK_COUNT_ADS.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.CLICK_COUNT_ADS.identifier, value)
        }

    private var scrollCountAdPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.SCROLL_COUNT_ADS.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.SCROLL_COUNT_ADS.identifier, value)
        }

    private var homeLocationPermissionAskedPref: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.HOME_LOCATION_PERMISSION.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.HOME_LOCATION_PERMISSION.identifier, value)
        }

    private var snapAgreementPermissionAskedPref: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.SNAP_AGREEMENT.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.SNAP_AGREEMENT.identifier, value)
        }

    fun setLoggedInUserToken(token: String?) {
        userUnauthorized = false
        loggedInUserTokenLocalPref = token
        loadLoggedInUserFromLocalPrefs()
    }

    fun getLoginUserToken(): String? {
        return loggedInUserTokenLocalPref
    }

    fun setUserFirstTimeLogin(isFirstTimeLogin: Boolean) {
        userFirstTimeLoginPref = isFirstTimeLogin
    }

    fun setProfileUpdatedStatus(profileUpdatedStatus: Boolean?) {
        if (profileUpdatedStatus != null) {
            this.profileUpdatedStatus = profileUpdatedStatus
        }
    }

    fun setLoggedInUser(meetFriendUser: MeetFriendUser?) {
        localPrefs.putString(
            PreferenceKey.LOGGED_IN_USER_JSON_KEY.identifier,
            Gson().toJson(meetFriendUser)
        )
        loadLoggedInUserFromLocalPrefs()
        loggedInUserCacheUpdatesSubject.onNext(Unit)
    }

    fun getLoggedInUser(): LoggedInUser? {
        val loggedInUser = meetFriendUser ?: return null
        return LoggedInUser(loggedInUser, loggedInUserTokenLocalPref)
    }

    fun setChatUser(chatUser: ChatUser?) {
        localPrefs.putString(
            PreferenceKey.CHAT_USER_INFO.identifier,
            Gson().toJson(chatUser)
        )
        loadChatUserFromLocalPrefs()
        loggedInUserCacheUpdatesSubject.onNext(Unit)
    }

    fun getChatUser(): ChatUser? {
        loadChatUserFromLocalPrefs()
        return chatuserProfile
    }

    fun getLoggedInUserId(): Int {
        return if (getLoggedInUser()?.loggedInUser != null) {
            getLoggedInUser()?.loggedInUser?.id ?: error("User Id Not Found")
        } else {
            getLoggedInUser()?.loggedInUser?.id ?: 0
        }
    }

    fun setChatUserProfileImage(profileImage: String?) {
        chatUserProfileImagePref = profileImage
    }

    fun getChatUserProfileImage(): String? {
        return chatUserProfileImagePref
    }

    fun setIsChatUserCreated(isUserCreated: Boolean) {
        this.isChatUserCreatedPref = isUserCreated
    }

    fun setEventChannelId(channelId: String) {
        localPrefs.putStringSet(
            PreferenceKey.LOGGED_IN_USER_CHANNEL_ID.identifier,
            setOf(channelId)
        )
    }

    fun removeEventChannelId(channelId: String) {
        localPrefs.removeStringFromSet(
            channelId,
            PreferenceKey.LOGGED_IN_USER_CHANNEL_ID.identifier
        )
    }
    fun clearUserPreferences() {
        try {
            meetFriendUser = null
            for (preferenceKey in PreferenceKey.values()) {
                localPrefs.removeValue(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier)
                clearPreferenceKey(preferenceKey)
            }
        } catch (e: IOException) {
            Timber.e(e, "An error occurred $e")
        }
    }

    private fun clearPreferenceKey(preferenceKey: PreferenceKey) {
        if (preferenceKey == PreferenceKey.LOGGED_IN_USER_EMAIL_OR_PHONE ||
            preferenceKey == PreferenceKey.LOGGED_IN_USER_PASSWORD ||
            (preferenceKey == PreferenceKey.IS_CHAT_USER_CREATED)
        ) {
            return
        }

        if (!getLoggedUserRememberMe() || preferenceKey == PreferenceKey.LOGGED_IN_USER_TOKEN) {
            localPrefs.removeValue(preferenceKey.identifier)
        }
    }

    private fun loadLoggedInUserFromLocalPrefs() {
        val userJsonString =
            localPrefs.getString(PreferenceKey.LOGGED_IN_USER_JSON_KEY.identifier, null)
        var meetFriendUser: MeetFriendUser? = null

        if (userJsonString != null) {
            try {
                meetFriendUser = Gson().fromJson(userJsonString, MeetFriendUser::class.java)
            } catch (e: JsonParseException) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
        }
        this.meetFriendUser = meetFriendUser
    }

    private fun loadChatUserFromLocalPrefs() {
        val userJsonString = localPrefs.getString(PreferenceKey.CHAT_USER_INFO.identifier, null)
        var chatRoomUserProfileInfo: ChatUser? = null

        if (userJsonString != null) {
            try {
                chatRoomUserProfileInfo = Gson().fromJson(userJsonString, ChatUser::class.java)
            } catch (e: JsonParseException) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
        }
        this.chatuserProfile = chatRoomUserProfileInfo
    }

    fun userUnauthorized() {
        if (!userUnauthorized) {
            userAuthenticationFailSubject.onNext(Unit)
        }
        userUnauthorized = true
    }

    fun invitedAsCoHost(liveEventInfo: LiveEventInfo) {
        invitedAsCoHostSubject.onNext(liveEventInfo)
    }

    fun getLoggedInUserEmailOrPhone(): String? {
        return loggedInUserEmailOrPhoneLocalPref
    }

    fun setLoggedInUserEmailOrPhone(userName: String) {
        loggedInUserEmailOrPhoneLocalPref = userName
    }

    fun getLoggedUserPassword(): String? {
        return loggedInUserPasswordLocalPref
    }

    fun setLoggedInUserPassword(password: String) {
        loggedInUserPasswordLocalPref = password
    }

    fun getLoggedUserRememberMe(): Boolean {
        return loggedInUserRememberMeLocalPref
    }

    fun setLoggedInUserRememberMe(isChecked: Boolean) {
        loggedInUserRememberMeLocalPref = isChecked
    }

    fun setLocationPermissionAsked(isGranted: Boolean) {
        this.locationPermissionAskedPref = isGranted
    }

    fun getLocationPermissionAsked(): Boolean {
        return locationPermissionAskedPref
    }

    fun setIsHubRequestSent(isSent: Boolean) {
        this.hubRequestSentPref = isSent
    }

    fun getIsHubRequestSent(): Boolean {
        return hubRequestSentPref
    }

    fun setPlaceApiKey(key: String?) {
        this.placeApiKeyPref = key
    }

    fun getPlaceApiKey(): String? {
        return placeApiKeyPref
    }

    fun setNativeAdId(key: String?) {
        this.nativeAdIdPref = key
    }

    fun getNativeAdId(): String {
        return nativeAdIdPref ?: "ca-app-pub-8787848622927672/8283082766"
    }

    fun setInterstitialAdId(key: String?) {
        this.interstitialAdIdPref = key
    }

    fun getInterstitialAdId(): String {
        return interstitialAdIdPref ?: "ca-app-pub-8787848622927672/8294827026"
    }

    fun setAppOpenAdId(key: String?) {
        this.appOpenAdIdPref = key
    }

    fun getAppOpenAdId(): String {
        return appOpenAdIdPref ?: "ca-app-pub-8787848622927672/2723266996"
    }

    fun setBannerAdId(key: String?) {
        this.bannerAdIdPref = key
    }

    fun getBannerAdId(): String {
        return bannerAdIdPref ?: "ca-app-pub-8787848622927672/1939806390"
    }

    fun setScrollCountAd(key: String?) {
        this.scrollCountAdPref = key
    }

    fun getScrollCountAd(): String {
        return scrollCountAdPref ?: "5"
    }

    fun setClickCountAd(key: String?) {
        this.clickCountAdPref = key
    }

    fun getClickCountAd(): String {
        return clickCountAdPref ?: "3"
    }

    fun setHomeLocationPermissionAsked(isGranted: Boolean) {
        this.homeLocationPermissionAskedPref = isGranted
    }

    fun getHomeLocationPermissionAsked(): Boolean {
        return homeLocationPermissionAskedPref
    }

    fun setsnapAgreementPermissionAskedPref(isGranted: Boolean) {
        this.snapAgreementPermissionAskedPref = isGranted
    }

    fun getsnapAgreementPermissionAskedPref(): Boolean {
        return snapAgreementPermissionAskedPref
    }
}
