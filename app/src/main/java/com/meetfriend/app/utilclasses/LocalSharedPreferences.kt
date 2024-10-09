package com.meetfriend.app.utilclasses

import android.content.Context
import android.content.SharedPreferences
import com.meetfriend.app.application.MeetFriendApplication


class LocalSharedPreferences {


    private var sharedPreferences: SharedPreferences =
        MeetFriendApplication.context.getSharedPreferences("meet", Context.MODE_PRIVATE)
    var editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun removeAllData() {
        editor.clear().commit()
    }
}


