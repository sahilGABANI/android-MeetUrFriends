package com.meetfriend.app.utilclasses

import com.meetfriend.app.BuildConfig

object AppConstants {
    val APIS_BASE_URL = if (BuildConfig.DEBUG) {
        // dev
        "https://dev.meeturfriends.com/"
    } else {
        //production
        "https://api.meeturfriends.com/"

    }
    val MEDIA_BASE_URL = "https://d1zf9vbnxobyry.cloudfront.net"
    val POST_PIC_QUALITY=23
    val POST_PIC_LOADING=2500
    internal interface httpcodes {
        companion object {
            val STATUS_API_VALIDATION_ERROR = 401
        }
    }

    const val DOB_FORMAT = "MM/dd/yyyy"
    const val CHALLENGE_DATE_FORMAT = "yyyy-MM-dd"

    const val SOMETHING_WENT_WRONG = "Something went wrong please try again later!"
}