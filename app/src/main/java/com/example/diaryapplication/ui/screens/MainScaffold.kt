package com.example.diaryapplication.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.diaryapplication.Route
import com.example.diaryapplication.ui.theme.BlueLight
import com.example.diaryapplication.ui.theme.BluePrimary
import com.example.mobileapptest2.ui.screens.HomeScreen

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun MainScaffold() {
    val nav = rememberNavController() // 화면 이동 역할 네비게이션 컨트롤러

    // 하단 탭 바 항목 정의
    val items = listOf(
        BottomItem(Route.Home.path, "홈") { Icon(Icons.Outlined.Home, null) },
        BottomItem(Route.Chat.path, "챗봇") { Icon(Icons.Outlined.ChatBubbleOutline, null) },
        BottomItem(Route.Report.path, "요약") { Icon(Icons.Outlined.BarChart, null) },
        BottomItem(Route.My.path, "마이") { Icon(Icons.Outlined.PersonOutline, null) },
    )

    // 현재 어떤 화면 있는지 경로를 실시간을 가져옴
    // null 이면 null 반환
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 2.dp // 그림자 효과
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route // 현재 화면 경로와 선택된 탭의 경로가 같으면 true

                    NavigationBarItem(
                        selected = selected, // 선택 여부
                        onClick = {
                            nav.navigate(item.route) { // 선택된 탭으로 이동
                                launchSingleTop = true // 이미 그 화면이면 중복 생성 되지 않게 함
                                restoreState = true // 현재 탭 상태에서 다른 탭에 갔다 다시 왔을 때, 그 이전의 상태를 유지
                                popUpTo(nav.graph.startDestinationId) { saveState = true } // 백스택을 정리
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary, // 선택된 탭 아이콘 색상
                            selectedTextColor = BluePrimary, // 선택된 탭 텍스트 색상
                            indicatorColor = BlueLight, // 선택된 탭 배경 색상
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // 미선택 탭 아이콘 색상
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant // 미선택 탭 텍스트 색상
                        )
                    )
                }
            }
        }
    ) { inner -> // Scaffold가 계산한 padding 값을 넘겨줌
        NavHost(
            navController = nav, // 위에서 만든 네비게이션 컨트롤러를 연결
            startDestination = Route.Home.path, // 앱 시작 시 첫 화면
            modifier = Modifier
        ) {
            composable(Route.Home.path) { HomeScreen(padding = inner) } // 홈 -> HomeScreen 화면 표시
            composable(Route.Chat.path) { ChatScreen(padding = inner) } // Chat -> ChatScreen 화면 표시
            composable(Route.Report.path) { ReportScreen(padding = inner) } // 요약 -> ReportScreen 화면 표시
            composable(Route.My.path) { MyPageScreen( // 마이페이지 -> MyPageScreen 표시
                padding = inner,
                onLogout = { // 로그아웃 버튼 클릭 시, 처리
                    nav.navigate(Route.Login.path) { // 로그인 화면으로 이동
                        popUpTo(nav.graph.findStartDestination().id) { inclusive = true } // 백스택을 전부 삭제
                        launchSingleTop = true // 로그인 화면 중복 생성을 방지
                    }
                }
            )
            }
        }
    }
}