package com.example.diaryapplication.model

data class UserProfile(
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val birthDate: String = "" // "2000-01-01" 형태로
)