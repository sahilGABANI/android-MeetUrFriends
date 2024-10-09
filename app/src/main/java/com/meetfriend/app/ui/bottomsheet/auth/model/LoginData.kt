package com.meetfriend.app.ui.bottomsheet.auth.model

data class LoginData(
    val email: String,
    val password: String,
    val loginType: String,
    val googleId: String,
    val firstName: String,
    val lastName: String
)
