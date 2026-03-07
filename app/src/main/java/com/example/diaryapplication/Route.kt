package com.example.diaryapplication

sealed class Route(val path: String){
    data object Onboarding : Route("onboarding")

    // 로그인 화면
    data object Login : Route("login")
    data object SignUp : Route("signup")
    data object FindId : Route("find_id")
    data object FindIdResult : Route("find_id_result")
    data object FindPassword : Route("find_password")
    data object VerifyCode : Route("verify_code")
    data object ResetPassword : Route("reset_password")

    // 메인 화면
    data object Main : Route("main")

    // 하단 탭 바
    data object Diary : Route("diary")
    data object Chat : Route("chat")
    data object Home : Route("home")
    data object Report : Route("report")
    data object My : Route("my")
}