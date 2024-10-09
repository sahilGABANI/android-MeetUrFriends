package com.meetfriend.app.newbase.network

import com.meetfriend.app.api.authentication.LoggedInUserCache
import com.meetfriend.app.newbase.extension.getAPIBaseUrl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class MeetFriendInterceptorHeaders(
    private val loggedInUserCache: LoggedInUserCache
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val requestBuilder = original.newBuilder()
        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Accept", "application/json")

        // authenticated user
        val token = loggedInUserCache.getLoginUserToken() ?: ""
        if (token.isNotEmpty()) {
            if (getAPIBaseUrl().contains(original.url.host)) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        val response: Response
        try {
            response = chain.proceed(requestBuilder.build())
            if (response.code == 401 && loggedInUserCache.getLoggedInUser()?.loggedInUser != null) {
                loggedInUserCache.userUnauthorized()
            }
        } catch (t: Throwable) {
            Timber.e("error in InterceptorHeaders:\n${t.message}")
            throw IOException(t.message)
        }
        return response
    }
}