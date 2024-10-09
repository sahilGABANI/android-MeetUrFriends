package com.meetfriend.app.api.story.model

data class AddStoryRequest(
    val type: String? = null,
    val image: String? = null,
    val uid: String? = null,
    val duration: String? = null,
    val position: String? = null,
    val rotation: Float? = null,
    val webUrl: String? = null,
    val musicTitle: String? = null,
    val artists: String? = null,
    val height: Int? = null,
    val width: Int? = null
)