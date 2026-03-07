package com.example.diaryapplication
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
import com.example.diaryapplication.ui.theme.DiaryApplicationTheme
import com.example.diaryapplication.ui.screens.auth.*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiaryApplicationTheme {
                RootNav()
            }
        }
    }
}
@Composable
fun RootNav() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Route.Onboarding.path,
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
                onFound = { nav.navigate(Route.FindIdResult.path) }
            )
        }
        composable(Route.FindIdResult.path) {
            FindIdResultScreen(
                foundEmail = "kim@naver.com",
                onClose = { nav.popBackStack(Route.Login.path, inclusive = false) },
                onGoLogin = { nav.popBackStack(Route.Login.path, inclusive = false) },
                onGoFindPw = { nav.navigate(Route.FindPassword.path) }
            )
        }
        composable(Route.FindPassword.path) {
            FindPasswordScreen(
                onBack = { nav.popBackStack() },
                onSendCode = { nav.navigate(Route.VerifyCode.path) }
            )
        }
        composable(Route.VerifyCode.path) {
            VerifyCodeScreen(
                email = "kim@naver.com",
                onBack = { nav.popBackStack() },
                onVerified = { nav.navigate(Route.ResetPassword.path) },
                onReEnterEmail = {
                    nav.popBackStack(route = Route.FindPassword.path, inclusive = false)
                }
            )
        }
        composable(Route.ResetPassword.path) {
            ResetPasswordScreen(
                onBack = { nav.popBackStack() },
                onComplete = {
                    nav.popBackStack(Route.Login.path, inclusive = false)
                }
            )
        }
        composable(Route.Main.path) {
// ✅ 핵심 수정: RootNav의 nav를 사용하는 onLogout 콜백을 MainScaffold에 전달
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