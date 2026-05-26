package com.example.diaryapplication
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.diaryapplication.notification.DiaryAlarmReceiver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DiaryApplication)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }


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
    // 안드로이드는 링크를 전달 받을 수 있는 곳은 MainActivity 밖에 안됨
    // 그래서 MainActivity.kt 파일에서 처리
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
        modifier = Modifier.fillMaxSize(),

        // 새 화면으로 이동할 때 : 오른쪽 끝(it)에서 슬라이드 인 + 페이드 인
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                    fadeIn(animationSpec = tween(300))
        },


        // 현재 화면이 뒤로 밀릴 때 : 왼쪽으로 1/3만 살짝 밀리며 페이드 아웃
        // 1/3만 밀리는 이유 : 완전히 사라지면 어색하고, 살짝 물러나는 느낌이 자연스러움
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeOut(animationSpec = tween(300))
        },

        // 뒤로 가기로 돌아올 때 : 왼쪽 1/3 위치에서 제자리로 슬라이드 인 + 페이드 인
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeIn(animationSpec = tween(300))
        },

        // 뒤로 가기로 현재 화면이 닫힐 때 : 오른쪽 끝으로 슬라이드 아웃 + 페이드 아웃
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                    fadeOut(animationSpec = tween(300))
        }
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
        composable(
            route = Route.Login.path,
            // 로그인 성공 후 Main으로 이동할 때 Login이 빠르게 사라지는 것을 막기 위해
            // exitTransition을 600ms 페이드로 오버라이드
            // NavHost 기본값(300ms 슬라이드)을 그대로 두면 Login 퇴장이 너무 빨라 Main 진입이 묻힘
            exitTransition = { fadeOut(animationSpec = tween(600)) }
        ) {
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
        composable(
            route = Route.Main.path,
            // 로그인 성공 후 메인 진입은 슬라이드 없이 페이드만 사용
            // 600ms로 여유 있게 설정해 자연스럽게 앱으로 들어오는 느낌을 줌
            enterTransition = { fadeIn(animationSpec = tween(600)) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }
        ) {
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