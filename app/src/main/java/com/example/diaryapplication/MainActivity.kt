package com.example.diaryapplication
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.*
import com.example.diaryapplication.ui.screens.MainScaffold
import com.example.diaryapplication.ui.screens.OnboardingScreen
import com.example.diaryapplication.ui.screens.auth.LoginScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.diaryapplication.ui.theme.DiaryApplicationTheme
import com.example.diaryapplication.ui.screens.auth.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiaryApplicationTheme {
                RootNav(intent?.data?.toString())
            }
        }
    }
}

@Composable
fun RootNav(deepLink: String? = null) {
    val nav = rememberNavController()

    val oobCode = remember(deepLink) {
        Uri.parse(deepLink ?: "").getQueryParameter("oobCode") ?: ""
    }

    // 딥 링크가 있으면 ResetPassword 화면으로 이동하게끔!
    LaunchedEffect(deepLink){
        if(deepLink != null && deepLink.startsWith("diaryapp://reset-password")) {
            nav.navigate(Route.ResetPassword.path) {
                popUpTo(Route.Login.path) { inclusive = false }
            }
        }
    }
    NavHost(
        navController = nav,
        /*
            FireBase가 로그인 정보를 폰에 저장해둔다
            1. 처음 로그인 성공
            2. FireBase가 폰에 로그인 정보를 저장
            3. 앱을 껏다 켜도 그 정보가 남아 있고
            4. currentUser로 확인이 가능
         */

        startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
            // currentUser가 null이 아님 -> 로그인 된 상태 -> 메인 페이지로 바로 이동
            Route.Main.path
        } else {
            // currentUser가 null이면 -> 로그인이 안된 상태 -> 온보딩으로 이동
            Route.Onboarding.path
        },
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onFinish = {
                    nav.navigate(Route.Login.path) {
                        popUpTo(Route.Onboarding.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Login.path) {
            LoginScreen(
                onBack = { /* 필요하면 종료 처리 */ },
                onLoginSuccess = {
                    nav.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onGoSignUp = { nav.navigate(Route.SignUp.path) },
                onGoFindId = { nav.navigate(Route.FindId.path) },
                onGoFindPw = { nav.navigate(Route.FindPassword.path) }
            )
        }
        composable(Route.SignUp.path) {
            SignUpScreen(
                onBack = { nav.popBackStack() },
                onComplete = { nav.popBackStack() }
            )
        }
        composable(Route.FindId.path) {
            FindIdScreen(
                onBack = { nav.popBackStack() },
                onFound = { foundEmail ->
                    nav.navigate("find_id_result/$foundEmail")
                }
            )
        }
        composable(Route.FindIdResult.path) {
            val email = it.arguments?.getString("email")?: ""
            FindIdResultScreen(
                foundEmail = email,
                onClose = { nav.popBackStack(Route.Login.path, inclusive = false) },
                onGoLogin = { nav.popBackStack(Route.Login.path, inclusive = false) },
                onGoFindPw = { nav.navigate(Route.FindPassword.path) }
            )
        }
        composable(Route.FindPassword.path) {
            FindPasswordScreen(
                onBack = { nav.popBackStack() },
                onSendCode = { email ->
                    nav.navigate("verify_code/$email") }
            )
        }
        composable(Route.VerifyCode.path) {
            VerifyCodeScreen(
                email = it.arguments?.getString("email")?:"",
                onBack = { nav.popBackStack() },
                onReEnterEmail = {
                    nav.popBackStack(route = Route.FindPassword.path, inclusive = false)
                }
            )
        }
        composable(Route.ResetPassword.path) {
            ResetPasswordScreen(
                oobCode = oobCode,
                onBack = { nav.popBackStack() },
                onComplete = {
                    nav.navigate(Route.Login.path) {
                        popUpTo(0) {inclusive=true}
                    }
                }
            )
        }
        composable(Route.Main.path) {
            // RootNav의 nav를 사용하는 onLogout 콜백을 MainScaffold에 전달
            // MainScaffold 내부 nav는 "login"을 모르지만, 여기 nav는 알고 있음
            MainScaffold(
                onLogout = {
                    nav.navigate(Route.Login.path) {
                        // Main을 포함한 백스택을 전부 제거 → 뒤로가기 눌러도 메인으로 못 돌아오게
                        popUpTo(Route.Main.path) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}