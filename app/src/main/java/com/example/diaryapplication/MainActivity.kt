package com.example.diaryapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
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
            DiaryApplicationTheme() {
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
                onBack = { },
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
                foundEmail = "kim@naver.com", // 임시데이터
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
                email = "kim@naver.com", // 임시 데이터
                demoCode = "123456", // 임시 인증 코드
                onBack = { nav.popBackStack() },
                onVerified = { nav.navigate(Route.ResetPassword.path) },
                onReEnterEmail = {
                    nav.popBackStack(Route.FindPassword.path, inclusive = false)
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
            MainScaffold()
        }
    }
}