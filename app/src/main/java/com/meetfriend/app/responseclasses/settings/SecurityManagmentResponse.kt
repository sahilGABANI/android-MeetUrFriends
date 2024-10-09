package com.meetfriend.app.responseclasses.settings

data class SecurityManagmentResponse(
    val status: Boolean,
    val message: String,
    val media_url: String?,
    val base_url: String,
    val securityManagementResult: SecurityManagementResult
)

