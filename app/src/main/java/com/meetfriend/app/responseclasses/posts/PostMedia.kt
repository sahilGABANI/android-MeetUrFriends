package com.meetfriend.app.responseclasses.posts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostMedia(
    val created_at: String,
    val extension: String,
    val file_name: String,
    val file_path: String,
    val id: Int,
    val posts_id: Int,
    val size: String,
    val thumbnail: String,
    val width: Int,
    val height: Int,
    val original_video_url: String,
):Parcelable