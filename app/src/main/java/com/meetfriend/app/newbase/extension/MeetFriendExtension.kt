package com.meetfriend.app.newbase.extension

import android.os.Bundle
import com.meetfriend.app.BuildConfig
import com.meetfriend.app.api.authentication.model.MeetFriendUser
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow


fun getAPIBaseUrl(): String {
    return if (BuildConfig.DEBUG) {
        // dev
        "https://dev.meeturfriends.com/api/"

    } else {
        //production
       "https://api.meeturfriends.com/api/"

    }
}

fun getSocketBaseUrl(): String {
    return if (BuildConfig.DEBUG) {

        // dev
        "http://44.212.163.52:5000"

        //"http://3.219.129.251:5000"

    } else {
        "http://3.219.129.251:5000"
    }
}

const val cloudFlareImageUploadBaseUrl =
    "https://api.cloudflare.com/client/v4/accounts/%s/images/v1"
const val cloudFlareVideoUploadBaseUrl = "https://api.cloudflare.com/client/v4/accounts/%s/stream"
const val cloudFlareGetVideoUploadBaseUrl = "https://api.cloudflare.com/client/v4/accounts/%s/stream/%s"


fun getCommonPhotoFileName(userId: Int): String {
    return "${userId}_img_android_${System.currentTimeMillis()}"
}

fun getCommonVideoFileName(userId: Int): String {
    return "${userId}_video_android_${System.currentTimeMillis()}"
}

fun Bundle.putEnum(key:String, enum: Enum<*>){
    putString(key, enum.name)
}

inline fun <reified T: Enum<T>> Bundle.getEnum(key: String, default: T): T {
    val found = getString(key)
    return if (found == null) { default } else enumValueOf(found)
}

fun Int.prettyCount(): String? {
    val suffix = charArrayOf(' ', 'k', 'M', 'B', 'T', 'P', 'E')
    val numValue: Long = this.toLong()
    val value = floor(log10(numValue.toDouble())).toInt()
    val base = value / 3
    return if (value >= 3 && base < suffix.size) {
        DecimalFormat("#0.00").format(numValue / 10.0.pow(base * 3.toDouble())) + suffix[base]
    } else {
        DecimalFormat().format(numValue)
    }
}

fun getSelectedTagUserIds(
    selectedTagUserInfo: MutableList<MeetFriendUser>,
    commentString: String
): String? {
    var selectedTagsUserIds: String? = null
    var selectedTags = commentString.split(" ")
    val tempTagsList = ArrayList<String>()
    for (element in selectedTags) {
        if (element.contains("@") && element.length > 1) {
            tempTagsList.addAll(element.split("@"))
        }
    }
    tempTagsList.removeAll {
        it.isEmpty() || it == "@"
    }
    selectedTags = tempTagsList
    if (!selectedTags.isNullOrEmpty()) {
        selectedTags = selectedTags.toMutableList()
        for (i in selectedTags.indices) {
            selectedTags[i] = selectedTags[i].trim()
        }
        val selectedSearchTagUserId: MutableList<Int> = mutableListOf()
        selectedTagUserInfo.forEach {
            val username = if (it.chatUserName.isNullOrEmpty()) it.userName else it.chatUserName
            if (username in selectedTags) {
                selectedSearchTagUserId.add(it.id)
            }
        }
        selectedTagsUserIds = selectedSearchTagUserId.joinToString(",")
    }
    Timber.tag("<><><><>").e(commentString.plus("\t").plus(selectedTagsUserIds))
    return selectedTagsUserIds
}


fun getSelectedHashTags(
    commentString: String
): String? {
    var selectedTagsUserIds: String? = null
    var selectedTags = commentString.split(" ")
    val tempTagsList = ArrayList<String>()
    val urlRegex = Regex("""(?:https?://)?(?:www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b(?:[-a-zA-Z0-9@:%_\+.~#?&//=]*)""")
    val specialCharRegex = Regex("""[.?/]""")

    for (element in selectedTags) {
        if (element.contains("#") && element.length > 1) {
            val tags = element.split("#")
            tempTagsList.addAll(tags.map { tag ->
                tag.replace(urlRegex, "").replace(specialCharRegex, "")
            })
        }
    }
    tempTagsList.removeAll {
        it.isEmpty() || it == "#"
    }
    selectedTagsUserIds = tempTagsList.joinToString(",")

    Timber.tag("<><><><>").e(commentString.plus("\t").plus(selectedTagsUserIds))

    return selectedTagsUserIds
}