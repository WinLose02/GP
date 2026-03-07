package com.example.diaryapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
@Composable
fun HomeScreen(
    padding: PaddingValues,
    onWriteDiary: () -> Unit,
) {
// 임시 데이터
    val userName = "찬영"
    val today = remember { LocalDate.now() } // 오늘 날짜 가져오기
    val greeting = when (today.hour) { // 현재 시간의 시간(Hour)를 가져와서
// 시간대 별로 인사 멘트 다르게 하기
        in 5..11 -> "좋은 아침이에요"
        in 12..16 -> "좋은 오후에요"
        in 17..20 -> "좋은 저녁이에요"
        in 21 .. 24 -> "좋은 밤이에요"
        else -> "안녕하세요"
    }
// 임시 마음 날씨 데이터 - 이번 주 요일별
// 이번 주 월~오늘까지만 표시
// TODO: DB 연동 후에 코드 수정
    val weekWeatherEmojis = listOf("🌧", "⛅", "☁", "🌤", "🌥", "☀")
    val weekDayLabels = listOf("월", "화", "수", "목", "금", "토")
// TODO: 이번 주 날짜 범위 텍스트 (임시 데이터)
    val weekRangeLabel = "3월 1일 ~ 3월 7일"
// TODO: 임시 스트릭 데이터 (DB 연동 전)
// true = 기록함, false = 기록 안 함
    val streakData = listOf(
        listOf(true, true, true, true, true, true, true), // 1주차
        listOf(true, true, true, true, true, true, true), // 2주차
        listOf(true, true, true, true, true, true, true), // 3주차
        listOf(false, false, false, false, false, false, true) // 4주차 (오늘만 기록)
    )
    val streakCount = 22 // TODO: 연속 기록 일수 -> 임시 데이터
// 오늘의 질문 -> 이 부분은 질문을 여러개 만들어서 랜덤으로 출력해도 괜찮을 부분
    val todayQuestion = "오늘 가장 마음이 편했던 순간은 언제였나요?"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
// 헤더 인사말
        HomeGreetingHeader(
            greeting = greeting,
            userName = userName,
            today = today
        )
        Spacer(Modifier.height(8.dp))
// 오늘의 질문
        TodayQuestionCard(
            question = todayQuestion,
            onWriteDiary = onWriteDiary
        )
// 이번달 나의 기록 (스트릿 + 마음 일기 데이터)
        MonthlyRecordCard(
            streakData = streakData,
            streakCount = streakCount,
            weekWeatherEmojis = weekWeatherEmojis,
            weekDayLabels = weekDayLabels,
            weekRangeLabel = weekRangeLabel
        )
        Spacer(Modifier.height(8.dp))
    }
}
// 인사말 헤더
@Composable
private fun HomeGreetingHeader(
    greeting: String,
    userName: String,
    today: LocalDate
) {
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    val dateStr = "${today.year}년 ${today.monthValue}월 ${today.dayOfMonth}일 ($dayName)"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text( // 인사말
            text = greeting,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text( // 닉네임
                text = "${userName}님 ",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(text = "✨", fontSize = 28.sp)
        }
        Text(
            text = dateStr, // 날짜
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
// 오늘의 질문
@Composable
private fun TodayQuestionCard(
    question: String,
    onWriteDiary: () -> Unit
) {
    val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3D7BF4),
            Color(0xFF6FA3FF)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✦ 오늘의 질문",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 26.sp
            )
            Button(
                onClick = onWriteDiary,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "일기로 답하기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
// 이번 달 나의 기록
// TODO: 추후 여기도 수정 가능성
@Composable
private fun MonthlyRecordCard(
    streakData: List<List<Boolean>>, // 4주 x 7일
    streakCount: Int,
    weekWeatherEmojis: List<String>, // 이번 주 요일별 날씨 이모지
    weekDayLabels: List<String>, // ["월", "화", "수", "목", "금", ...]
    weekRangeLabel: String // "3월 1일 ~ 3월 7일"
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
// 카드 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "이번 달 나의 기록",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFEEF3FF))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${streakCount}일 연속",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
// 스트릿 데이터 그리드
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                streakData.forEach { week -> // 주(1주,2주,..)만큼 반복
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        week.forEach { recorded -> // 일(월,화,수,...) 만큼 반복
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (recorded) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }
            }
// 구분선
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )
// 마음 날씨 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "☁ 마음 날씨",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "($weekRangeLabel)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Normal
                )
            }
// 요일별 마음 날씨
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekWeatherEmojis.forEachIndexed { index, emoji ->
                    val isToday = index == weekWeatherEmojis.lastIndex // 마지막 인덱스가 '오늘'인지 여부
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isToday) { // 마지막 인덱스가 오늘이면
                            Box(
                                modifier = Modifier // 오늘을 강조
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEEF3FF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        } else {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                        Text(
                            text = weekDayLabels[index], // 요일 텍스트
                            fontSize = 11.sp,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
// LocalDate에서 hour를 가져오는 확장
// java.time.LocalTime 활용
private val LocalDate.hour: Int get() = java.time.LocalTime.now().hour