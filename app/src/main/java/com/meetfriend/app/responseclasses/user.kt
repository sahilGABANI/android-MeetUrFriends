package com.meetfriend.app.responseclasses

data class user(val id:String,
                val firstName:String,
                val lastName:String,
                val userName:String?=null,
                val profile_photo:String
)