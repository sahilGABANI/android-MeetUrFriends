package com.meetfriend.app.api.userprofile.model

import com.google.gson.annotations.SerializedName

data class ReportUser(

	@field:SerializedName("report_for")
	val reportFor: String? = null,

	@field:SerializedName("report_by")
	val reportBy: Int? = null,

	@field:SerializedName("reason")
	val reason: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

)

data class ReportUserRequest(

	@field:SerializedName("report_for")
	val reportFor: Int,

	@field:SerializedName("reason")
	val reason: String? = null,

	)


