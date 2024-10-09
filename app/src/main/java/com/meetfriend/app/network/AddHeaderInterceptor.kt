package com.meetfriend.app.network

import com.meetfriend.app.application.MeetFriendApplication
import contractorssmart.app.utilsclasses.PreferenceHandler
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.*

/**
 */

class AddHeaderInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = PreferenceHandler.readString(
            MeetFriendApplication.context,
            PreferenceHandler.AUTHORIZATION_TOKEN,
            ""
        )
        val builder = chain.request().newBuilder()
        builder.addHeader("Authorization", "Bearer " + token)
        builder.addHeader("Accept", "application/json")
        builder.addHeader("Content-Type", "application/x-www-form-urlencoded")
        return chain.proceed(builder.build())
    }

}