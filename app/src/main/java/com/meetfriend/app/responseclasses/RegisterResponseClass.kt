package com.meetfriend.app.responseclasses

data class RegisterResponseClass(
    val baseUrl: String,
    val mediaUrl: String,
    val message: String,
    val profileUpdatedStatus: Boolean,
    val result: Result,
    val status: Boolean,
    val accessToken: String
)
