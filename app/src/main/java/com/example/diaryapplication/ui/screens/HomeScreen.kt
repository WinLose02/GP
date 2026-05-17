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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.AuthViewModel
import com.example.diaryapplication.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    padding: PaddingValues, // 패딩 값
    onWriteDiary: () -> Unit, // 일기로 답하기 버튼 클릭 시 일기 작성 화면으로 이동
    authViewModel: AuthViewModel, // DB에서 닉네임을 읽어오기 위한 ViewModel
    diaryViewModel: DiaryViewModel // 스트릿 데이터를 가져오기 위한 DiaryViewModel
) {

    // 실시간으로 ViewModel에서 사용자의 닉네임을 받아옴
    val nickname by authViewModel.currentNickname.collectAsState()
    val userName = nickname ?: "사용자" // 닉네임이 null이면 "사용자"로 표시

    val emotionEmojiMap by diaryViewModel.emotionEmojiMap.collectAsState()

    val today = remember { LocalDate.now() } // 오늘 날짜 가져오기 (재계산 방지를 위해 remember)
    val greeting = when (today.hour) { // 현재 시간의 시간(Hour)를 가져와서

        // 시간대 별로 인사 멘트 다르게 하기
        in 5..11 -> "좋은 아침이에요"
        in 12..16 -> "좋은 오후에요"
        in 17..20 -> "좋은 저녁이에요"
        in 21 .. 24 -> "좋은 밤이에요"
        else -> "안녕하세요"
    }

    // 화면이 처음 표시될 때, 이번 달 스트릿 데이터를 불러옴
    LaunchedEffect(Unit) {
        diaryViewModel.loadMonthStreak()
        diaryViewModel.loadMonthEmojis(today.year, today.monthValue)
    }

    // DiaryViewModel에서 실시간으로 정보들을 받아옴
    val writtenDates by diaryViewModel.writtenDates.collectAsState() // 일기를 작성한 날짜 목록
    val streakCount by diaryViewModel.streakCount.collectAsState() // 연속으로 작성한 일수
    val streakGrid = remember(writtenDates, today) { // 스트릿 그리드를 생성
        // writtenDates와 today가 바뀔때만 계산을 함

        val firstDay = today.withDayOfMonth(1) // 이번 달 1일(첫날)의 날짜
        val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth // 이번 달 마지막 날짜의 숫자(3월 -> 31일)
        val startOffset = firstDay.dayOfWeek.value % 7 // 1일이 무슨 요일인지 계산 -> 월(1), 화(2), ... , 일(0)

        /*
            >> 이번 달을 표시하는 데 필요한 주의 수를 계산
            >> 3월의 경우 5주이므로 5개의 주가 필요
            >> [startOffset(1일이 무슨 요일인지) + lastDayOfMonth(이번달 마지막 일 수) + 6] / 7 로 계산

            [EX] 2026년 3월을 기준으로 하면,
                 1. 3월 1일은 일요일(startOffset = 0)
                 2. 3월은 31일까지 있음(lastDayOfMonth=31)
                 3. (0 + 31 + 6) / 7 = 5.xxx
                 4. 총 3월의 스트릿 그리드는 5줄이 필요
        */

        val weeks = (startOffset + lastDayOfMonth + 6) / 7
        List(weeks) { weekIdx -> // 주(week) X 7일의 2차원 리스트 생성
            List(7) { dayIdx ->
                val dayOfMonth = weekIdx * 7 + dayIdx - startOffset + 1 // 현재 칸의 날짜 숫자를 계산
                when {
                    dayOfMonth < 1 || dayOfMonth > lastDayOfMonth -> null // 이번 달 범위 밖이면 null -> 투명칸
                    else -> {
                        val date = today.withDayOfMonth(dayOfMonth) // 날짜를 실제 LocalDate로 변환
                        if (date.isAfter(today)) null // 오늘 이후의 날짜이면 null을 반환(투명)
                        else writtenDates.contains(date.toString()) // 아니면 일기 작성여부를 판단해서 회색(false)/파란색(true)를 결정
                    }
                }
            }
        }
    }

    // 이번 주 나의 마음 날시 부분

    // 이번 주 일요일 날짜를 계산
    // previousOrSame -> 오늘이 일요일이면 오늘, 아니면 가장 최근 일요일
    // EX> 3월 12일(목)이면, 가장 최근 일요일인 3월 8일(일) 반환
    val weekStart = remember(today) { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }

    // 이번 주 일요일 ~ 오늘까지의 날씨 이모지 목록을 생성
    // plusDays -> 일요일에서 i일을 더한 날짜
    // takeIf -> 오늘 이후의 날짜는 제외
    // .let{} -> 해당 날짜가 있으면 이모지를 추가하되, 현재는 하드 코딩으로 임시로 대체한 상태
    val weekWeatherEmojis = remember(today, emotionEmojiMap) {
        (0..6).mapNotNull { i ->
            val date = weekStart.plusDays(i.toLong())
            if (date.isAfter(today)) null
            else emotionToWeatherEmoji(emotionEmojiMap[date])
        }
    }

    // 이번주 일요일~오늘까지의 요일 라벨 목록을 생성
    val weekDayLabels = remember(today) {
        val names = listOf("일","월","화","수","목","금","토")
        // d -> names[d.dayOfWeek.value%7] ==> names[0]="일", names[1]="월" 형태로 계산해서
        // 요일 라벨을 만들기 위함
        (0..6).mapNotNull { i -> weekStart.plusDays(i.toLong()).takeIf { !it.isAfter(today) }?.let { d -> names[d.dayOfWeek.value % 7] } }
    }

    // 3월 8일 ~ 14일 형태의 주간 범위 텍스트를 생성
    val weekRangeLabel = remember(today) {
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        if (weekStart.monthValue == weekEnd.monthValue) "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 ~ ${weekEnd.dayOfMonth}일"
        else "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 ~ ${weekEnd.monthValue}월 ${weekEnd.dayOfMonth}일"
    }

    // TODO: 오늘의 질문 -> 이 부분은 질문을 여러개 만들어서 랜덤으로 출력해도 괜찮을것 같음
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
            streakGrid = streakGrid,
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
    onWriteDiary: () -> Unit // 일기로 답하기 버튼을 클릭 시, 일기 작성 화면으로 전환
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
    streakGrid: List<List<Boolean?>>, // 4주 x 7일
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
                streakGrid.forEach { week -> // 주(1주,2주,..)만큼 반복
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
                                        when (recorded) {
                                            true -> MaterialTheme.colorScheme.primary
                                            false -> MaterialTheme.colorScheme.surfaceVariant
                                            null -> Color.Transparent
                                        }
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

private fun emotionToWeatherEmoji(emotionEmoji:String?) : String {
    return when(emotionEmoji) {
        "😊" -> "☀️"   // 기쁨  → 맑음
        "😢" -> "🌧️"  // 슬픔  → 비
        "😠" -> "⛈️"  // 분노  → 폭풍
        "😰" -> "🌬️"  // 불안  → 바람
        "😳" -> "🌦️"  // 당황  → 소나기
        "❎" -> "❎"   // 분석 실패 → 구름 조금
        else -> "🌫️"  // 기록 없음 → 안개 (회색)
    }
}