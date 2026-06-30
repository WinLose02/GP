package com.example.diaryapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diaryapplication.viewmodel.AuthViewModel
import com.example.diaryapplication.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

val emotionQuestion = listOf(
    "오늘 가장 마음이 편했던 순간은\n언제였나요?",    "오늘 나를 웃게 만든\n일이 있었나요?",
    "오늘 하루 중 가장 기억에 남는\n장면은 무엇인가요?", "오늘 스스로를 칭찬하고 싶은\n순간이 있었나요?",
    "오늘 조금 힘들었던 일이 있었다면\n무엇인가요?",   "오늘 가장 많이 떠올랐던 생각은\n무엇인가요?",
    "오늘 나에게 가장 필요했던 위로는\n무엇이었나요?",  "오늘 하루를 한 단어로 표현한다면\n무엇일까요?",
    "오늘 가장 평온했던 시간은\n언제였나요?",   "오늘 작은 행복을 느낀 순간이\n있었나요?",
    "오늘 스스로에게 해주고 싶은 말은\n무엇인가요?", "오늘 가장 긴장되거나 떨렸던 순간은\n언제였나요?",
    "오늘 하루 중 가장 몰입했던 일은\n무엇인가요?",   "오늘 가장 뿌듯했던 일은 무엇인가요?",
    "오늘 나를 가장 지치게 만든 일은\n무엇이었나요?",  "오늘 의외로 기분 좋았던 순간이\n있었나요?",
    "오늘 나만의 작은 성취가 있었다면\n무엇인가요?",   "오늘 가장 솔직한 감정은\n무엇이었나요?",
    "오늘 마음이 복잡했던\n순간이 있었나요?",    "오늘 하루 중 가장 여유로웠던\n시간은 언제였나요?",
    "오늘 나를 미소 짓게 한 작은 일이\n있었나요?",   "오늘 하루를 돌아봤을 때 가장 잘한 선택은\n무엇인가요?",
    "오늘 마음 한구석에 남아있는\n걱정이 있나요?",     "오늘 나에게 일어난 가장 의미 있는 일은\n무엇인가요?",
    "오늘 내일의 나에게 하고 싶은 말이 있다면\n무엇인가요?",  "오늘 가장 나답다고 느낀 순간은\n언제였나요?",
    "오늘 스스로에게 가장 솔직해진 순간이\n있었나요?",  "오늘 무심코 지나친 감정이 있다면\n무엇인가요?",
    "오늘 나에게 휴식이 필요했던 순간은\n언제였나요?",  "오늘 가장 나를 힘 나게 한 생각은\n무엇이었나요?",
)


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

    // 카드 진입 애니메이션 트리거
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // DiaryViewModel에서 실시간으로 정보들을 받아옴
    val writtenDates by diaryViewModel.writtenDates.collectAsState() // 일기를 작성한 날짜 목록
    val streakCount by diaryViewModel.streakCount.collectAsState() // 연속으로 작성한 일수
    val monthStart = remember(today) { today.withDayOfMonth(1) }
    val topEmotion = remember(emotionEmojiMap, today) {
        emotionEmojiMap.entries
            .filter { it.key.year == today.year && it.key.monthValue == today.monthValue }
            .groupingBy { it.value }.eachCount()
            .maxByOrNull { it.value }?.key ?: ""
    }
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
    val weekWeatherEmojis = remember(today, emotionEmojiMap) {
        (0..6).map { i ->
            val date = weekStart.plusDays(i.toLong())
            when {
                date.isAfter(today) -> ""
                emotionEmojiMap[date] != null ->
                    emotionToWeatherEmoji(emotionEmojiMap[date])
                else -> ""

            }
        }

    }

    // 이번주 일요일~오늘까지의 요일 라벨 목록을 생성
    val weekDayLabels = remember(today) {
        val names = listOf("일","월","화","수","목","금","토")
        (0..6).map { i ->
            val date = weekStart.plusDays(i.toLong())
            names[date.dayOfWeek.value%7]
        }
    }

    // 3월 8일 ~ 14일 형태의 주간 범위 텍스트를 생성
    val weekRangeLabel = remember(today) {
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        if (weekStart.monthValue == weekEnd.monthValue) "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 ~ ${weekEnd.dayOfMonth}일"
        else "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 ~ ${weekEnd.monthValue}월 ${weekEnd.dayOfMonth}일"
    }

    val todayQuestion = emotionQuestion.random()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 홈 화면 카드 등장 애니메이션
        fun <T> cardEnterSpec(delay: Int): FiniteAnimationSpec<T> =
            tween(durationMillis = 650, delayMillis = delay, easing = FastOutSlowInEasing)

        // 헤더 인사말
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = cardEnterSpec(0)) +
                    slideInVertically(animationSpec = cardEnterSpec(0)) { 40 }
        ) {
            HomeGreetingHeader(
                greeting = greeting,
                userName = userName,
                today = today
            )
        }
        // 빠른 통계 카드
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = cardEnterSpec(150)) +
                    slideInVertically(animationSpec = cardEnterSpec(150)) { 40 }
        ) {
            QuickStatsRow(
                streakCount = streakCount,
                monthlyWriteCount = writtenDates.size,
                topEmotion = topEmotion
            )
        }
        // 오늘의 질문 카드
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = cardEnterSpec(300)) +
                    slideInVertically(animationSpec = cardEnterSpec(300)) { 40 }
        ) {
            TodayQuestionCard(
                question = todayQuestion,
                onWriteDiary = onWriteDiary
            )
        }
        // 이번달 나의 기록 카드
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = cardEnterSpec(450)) +
                    slideInVertically(animationSpec = cardEnterSpec(450)) { 40 }
        ) {
            MonthlyRecordCard(
                streakGrid = streakGrid,
                monthlyWriteCount = writtenDates.size,
                weekWeatherEmojis = weekWeatherEmojis,
                weekDayLabels = weekDayLabels,
                weekRangeLabel = weekRangeLabel,
                today = today,
                weekStart = weekStart,
                emotionEmojiMap = emotionEmojiMap,
                monthStart = monthStart
            )
        }
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
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF3D7BF4), Color(0xFF6366F1))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-40).dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = 70.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
        )
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✦ 오늘의 질문",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 0.06.sp
            )
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 26.sp
            )
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "buttonScale"
            )
            Button(
                onClick = onWriteDiary,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .height(46.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("일기로 답하기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
// 이번 달 나의 기록
@Composable
private fun MonthlyRecordCard(
    streakGrid: List<List<Boolean?>>,
    monthlyWriteCount: Int,
    weekWeatherEmojis: List<String>,
    weekDayLabels: List<String>,
    weekRangeLabel: String,
    today: LocalDate,
    weekStart: LocalDate,
    emotionEmojiMap: Map<LocalDate, String>,
    monthStart: LocalDate
) {
    val startOffset = remember(monthStart) { monthStart.dayOfWeek.value % 7 }
    val lastDayOfMonth = remember(monthStart) { monthStart.plusMonths(1).minusDays(1).dayOfMonth }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 카드 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF9F1C)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 17.sp)
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
                        text = "${monthlyWriteCount}회 작성",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 이모지 히트맵 그리드
            var waveVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { waveVisible = true }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                streakGrid.forEachIndexed { weekIdx, week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        week.forEachIndexed { dayIdx, recorded ->
                            val cellIndex = weekIdx * 7 + dayIdx
                            val dayOfMonth = weekIdx * 7 + dayIdx - startOffset + 1
                            val cellDate = if (dayOfMonth in 1..lastDayOfMonth)
                                monthStart.plusDays((dayOfMonth - 1).toLong()) else null
                            val isToday = cellDate == today


                            val cellAlpha by animateFloatAsState(
                                targetValue = if (waveVisible) 1f else 0f,
                                animationSpec = tween(
                                    durationMillis = 450,
                                    delayMillis = cellIndex * 16,
                                    easing = FastOutSlowInEasing
                                ),
                                label = "cellAlpha_$cellIndex"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .graphicsLayer { alpha = cellAlpha }
                                    .then(
                                        if (isToday && recorded != null)
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
                                        else Modifier
                                    )
                                    .clip(RoundedCornerShape(7.dp))
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

            // 범례
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.width(4.dp))
                Text("미작성", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(4.dp))
                Text("작성", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

            // 마음 날씨 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "☁ 마음 날씨",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = weekRangeLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // 요일별 마음 날씨 버블
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..6).forEach { index ->
                    val date = weekStart.plusDays(index.toLong())
                    val isToday = date == today
                    val isFuture = date.isAfter(today)
                    val emoji = weekWeatherEmojis[index]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .then(
                                    if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(11.dp))
                                    else Modifier
                                )
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    when {
                                        isFuture -> Color.Transparent
                                        emoji.isNotEmpty() -> Color(0xFFEEF3FF)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (emoji.isNotEmpty()) Text(text = emoji, fontSize = 20.sp)
                        }
                        Text(
                            text = weekDayLabels[index],
                            fontSize = 10.sp,
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
// 통계 카드 3개(연속 작성 일수, 이번 달 작성 일수, 주요 감정)
@Composable
private fun QuickStatsRow(
    streakCount: Int,
    monthlyWriteCount: Int,
    topEmotion: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatMiniCard(
            icon = "🔥",
            label = "연속 작성",
            value = "${streakCount}일째",
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = "📝",
            label = "이번 달",
            value = "${monthlyWriteCount}회 작성",
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = if (topEmotion.isNotEmpty()) topEmotion else "😊",
            label = "주요 감정",
            value = emojiToEmotionLabel(topEmotion),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatMiniCard(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun emojiToEmotionLabel(emoji: String): String = when (emoji) {
    "😊" -> "행복"
    "😢" -> "슬픔"
    "😠" -> "분노"
    "😰" -> "불안"
    "😳" -> "당황"
    else -> if (emoji.isNotEmpty()) emoji else "-"
}

// LocalDate에서 hour를 가져오는 확장
// java.time.LocalTime 활용
private val LocalDate.hour: Int get() = java.time.LocalTime.now().hour

private fun emotionToWeatherEmoji(emotionEmoji:String?) : String {
    return when(emotionEmoji) {
        "😊" -> "☀️"   // 기쁨  → 맑음
        "😢" -> "🌧️"  // 슬픔  → 비
        "😠" -> "⛈️"  // 분노  → 폭풍
        "😰" -> "🌀️"  // 불안  → 바람
        "😳" -> "🌦️"  // 당황  → 소나기
        "❎" -> "❎"   // 분석 실패 → 구름 조금
        else -> ""  // 기록 없음
    }
}