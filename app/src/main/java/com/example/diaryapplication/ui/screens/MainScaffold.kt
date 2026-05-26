package com.example.diaryapplication.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.diaryapplication.R
import com.example.diaryapplication.viewmodel.AuthViewModel
import com.example.diaryapplication.viewmodel.DiaryViewModel
import com.example.diaryapplication.viewmodel.ReportViewModel
import com.example.diaryapplication.viewmodel.MyPageViewModel
import com.example.diaryapplication.Route
import com.example.diaryapplication.ui.theme.BluePrimary

@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val diaryViewModel: DiaryViewModel = viewModel()
    val reportViewModel: ReportViewModel = viewModel()
    val context = LocalContext.current
    val myPageViewModel: MyPageViewModel = viewModel()

    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

    // 탭 이동 공통 로직
    fun navigate(route: String) {
        nav.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(nav.graph.startDestinationId) { saveState = true }
        }
    }

    Scaffold(
        bottomBar = {
            LucideFabBottomBar(
                currentRoute = currentRoute,
                onNavigate = ::navigate
            )
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Route.Home.path,
            enterTransition = { fadeIn(animationSpec = tween(600)) },
            exitTransition = { fadeOut(animationSpec = tween(600)) },
            popEnterTransition = { fadeIn(animationSpec = tween(600)) },
            popExitTransition = { fadeOut(animationSpec = tween(600)) }
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    padding = inner,
                    authViewModel = authViewModel,
                    diaryViewModel = diaryViewModel,
                    onWriteDiary = {
                        nav.navigate(Route.Diary.path) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                        }
                    }
                )
            }
            composable(Route.Diary.path) {
                DiaryScreen(
                    padding = inner,
                    diaryViewModel = diaryViewModel,
                    myPageViewModel = myPageViewModel
                )
            }
            composable(Route.Chat.path) {
                ChatScreen(padding = inner)
            }
            composable(Route.Report.path) {
                ReportScreen(
                    padding = inner,
                    reportViewModel = reportViewModel
                )
            }
            composable(Route.My.path) {
                MyPageScreen(
                    padding = inner,
                    onLogout = onLogout,
                    authViewModel = authViewModel,
                    myPageViewModel = myPageViewModel
                )
            }
        }
    }
}

@Composable
private fun LucideFabBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // 좌 2개 / 우 2개 탭 항목 (홈 FAB 제외)
    val leftItems = listOf(
        Triple(Route.Diary.path, "일기", R.drawable.ic_book_open),
        Triple(Route.Chat.path, "챗봇", R.drawable.ic_message_circle),
    )
    val rightItems = listOf(
        Triple(Route.Report.path, "요약", R.drawable.ic_trending_up),
        Triple(Route.My.path, "프로필", R.drawable.ic_user_round),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // 흰색 탭 바 (하단에 고정)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach { (route, label, icon) ->
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        selected = currentRoute == route,
                        onClick = { onNavigate(route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 중앙 홈 탭
                Spacer(Modifier.weight(1f))
                rightItems.forEach { (route, label, icon) ->
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        selected = currentRoute == route,
                        onClick = { onNavigate(route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 살짝 떠 있는 홈 버튼
        Box(
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.TopCenter)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF3D7BF4), Color(0xFF6FA3FF))
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNavigate(Route.Home.path) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_house),
                contentDescription = "홈",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (selected) BluePrimary else Color(0xFFB0BAC6),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) BluePrimary else Color(0xFFB0BAC6)
        )
    }
}
