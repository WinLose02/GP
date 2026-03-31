package com.example.diaryapplication.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.diaryapplication.viewmodel.AuthViewModel
import com.example.diaryapplication.viewmodel.DiaryViewModel
import com.example.diaryapplication.viewmodel.ReportViewModel
import com.example.diaryapplication.viewmodel.MyPageViewModel
import com.example.diaryapplication.Route
import com.example.diaryapplication.ui.theme.BlueLight
import com.example.diaryapplication.ui.theme.BluePrimary
private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isCenter: Boolean = false
)
@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val nav = rememberNavController() // 화면 이동 역할 네비게이션 컨트롤러
    val authViewModel : AuthViewModel = viewModel()
    val diaryViewModel : DiaryViewModel = viewModel()
    val reportViewModel : ReportViewModel = viewModel()

    val context = LocalContext.current
    val myPageViewModel : MyPageViewModel = viewModel()

    // 현재 어떤 화면 있는지 경로를 실시간으로 가져옴
    // null이면 null 반환
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

    // 하단 탭 바 항목들을 정의
    val items = listOf(
        BottomItem(Route.Diary.path, "일기", Icons.Outlined.Edit),
        BottomItem(Route.Chat.path, "챗봇", Icons.Outlined.ChatBubbleOutline),
        BottomItem(Route.Home.path, "", Icons.Filled.Home, isCenter = true),
        BottomItem(Route.Report.path, "요약", Icons.Outlined.BarChart),
        BottomItem(Route.My.path, "프로필", Icons.Outlined.PersonOutline),
    )

    // 하단 탭 바는 항상 표시
    val showBottomBar = true
    Scaffold(
        bottomBar = {
            if(showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 4.dp // 그림자 효과
                ) {
                    items.forEach { item ->
                        val selected = currentRoute == item.route // 현재 화면 경로와 선택된 탭의 경로가 같으면 true
                        if (item.isCenter) {
                            // 중앙 메인 홈 버튼
                            NavigationBarItem(
                                selected = selected, // 선택 여부
                                onClick = {
                                    nav.navigate(item.route) { // 선택된 탭으로 이동
                                        launchSingleTop = true // 이미 그 화면이라면, 중복 생성 방지
                                        restoreState =
                                            true // 현재 탭 상태에서 다른 탭에 갔다가 다시 왔을 때, 그 이전의 상태를 유지
                                        popUpTo(nav.graph.startDestinationId) {
                                            saveState = true
                                        } // 백스택 정리
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shadow(6.dp, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = BluePrimary,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    item.icon,
                                                    contentDescription = item.label,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedTextColor = BluePrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else { // 중앙 홈 버튼이 아닌 나머지 버튼들
                            NavigationBarItem(
                                selected = selected, // 선택 여부
                                onClick = {
                                    nav.navigate(item.route) { // 선택된 탭으로 이동
                                        launchSingleTop = true // 이미 그 화면이면, 중복 생성을 방지
                                        restoreState = true // 현재 탭에서 다른 탭에 갔다가 다시 왔을 때, 이전의 상태를 유지
                                        popUpTo(nav.graph.startDestinationId) {
                                            saveState = true
                                        } // 백스택 정리
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BluePrimary, // 선택된 탭 아이콘 색상
                                    selectedTextColor = BluePrimary, // 선택된 탭 텍스트 색상
                                    indicatorColor = BlueLight, // 선택된 탭의 배경 색상
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // 미선택 탭 아이콘 색상
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant // 미선택 탭 텍스트 색상
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { inner -> // Scaffold가 계산한 padding 값을 넘겨줌
        NavHost(
            navController = nav, // 위에서 만든 네비게이션 컨트롤러를 연결
            startDestination = Route.Home.path // 앱 시작 시, 첫 화면
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    padding = inner,
                    authViewModel = authViewModel,
                    diaryViewModel = diaryViewModel,
                    onWriteDiary = { // 일기 작성 버튼 클릭시
                        nav.navigate(Route.Diary.path) { // 해당 탭으로 이동
                            launchSingleTop = true // 이미 해당 탭이면, 중복 생성 방지
                            restoreState = true // 다른 탭에 갔다 다시 와도 이전의 상태를 유지
                            popUpTo(nav.graph.startDestinationId) { saveState = true } // 백스택 정리
                        }
                    }
                )
            }
            composable(Route.Diary.path) {
                DiaryScreen(
                    padding = inner,
                    diaryViewModel = diaryViewModel
                    )
            }
            composable(Route.Chat.path) {
                ChatScreen(
                    padding = inner
                )
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