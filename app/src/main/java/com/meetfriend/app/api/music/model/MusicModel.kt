package com.meetfriend.app.api.music.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


data class MusicResponse(

    @field:SerializedName("data")
    val data: Data? = null,

    @field:SerializedName("success")
    val success: Boolean? = null
)

data class Data(

    @field:SerializedName("total")
    val total: Int? = null,

    @field:SerializedName("start")
    val start: Int? = null,

    @field:SerializedName("results")
    val results: List<MusicInfo>? = null
)

@Parcelize
data class MusicInfo(

    @field:SerializedName("lyricsId")
    val lyricsId: String? = null,

    @field:SerializedName("image")
    val image: List<ImageItem>? = null,

    @field:SerializedName("copyright")
    val copyright: String? = null,

    @field:SerializedName("year")
    val year: String? = null,

    @field:SerializedName("releaseDate")
    val releaseDate: String? = null,

    @field:SerializedName("album")
    val album: Album? = null,

    @field:SerializedName("downloadUrl")
    val downloadUrl: List<DownloadUrlItem?>? = null,

    @field:SerializedName("language")
    val language: String? = null,

    @field:SerializedName("label")
    val label: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("explicitContent")
    val explicitContent: Boolean? = null,

    @field:SerializedName("url")
    val url: String? = null,

    @field:SerializedName("duration")
    val duration: Int? = null,

    @field:SerializedName("playCount")
    val playCount: Int? = null,

    @field:SerializedName("hasLyrics")
    val hasLyrics: Boolean? = null,

    @field:SerializedName("artists")
    val artists: Artists? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    var isPlaying: Boolean = false
) : Parcelable

@Parcelize
data class ImageItem(

    @field:SerializedName("url")
    val url: String? = null,

    @field:SerializedName("quality")
    val quality: String? = null
) : Parcelable

@Parcelize
data class DownloadUrlItem(

    @field:SerializedName("url")
    val url: String? = null,

    @field:SerializedName("quality")
    val quality: String? = null
) : Parcelable

@Parcelize
data class PrimaryItem(

    @field:SerializedName("image")
    val image: List<ImageItem?>? = null,

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("url")
    val url: String? = null
) : Parcelable

@Parcelize
data class Artists(

    @field:SerializedName("all")
    val all: List<AllItem?>? = null,

    @field:SerializedName("featured")
    val featured: List<FeaturedResponse>? = null,

    @field:SerializedName("primary")
    val primary: List<PrimaryItem?>? = null
) : Parcelable

@Parcelize
data class AllItem(

    @field:SerializedName("image")
    val image: List<ImageItem?>? = null,

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("url")
    val url: String? = null
) : Parcelable

@Parcelize
data class Album(

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("url")
    val url: String? = null
) : Parcelable

@Parcelize
data class FeaturedResponse(

    @field:SerializedName("image")
    val image: List<ImageItem?>? = null,

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("url")
    val url: String? = null
) : Parcelable
