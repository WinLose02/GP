package com.example.diaryapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun ReportScreen(padding: PaddingValues) {
    var tab by remember { mutableStateOf(0) } // 0: 주간, 1: 월간
    val isWeekly = tab == 0
    val weeklyLabels = listOf("일", "월", "화", "수", "목", "금", "토")
    val monthlyWeekLabels = listOf("1주", "2주", "3주", "4주", "5주")
    
    // 주간 임시 데이터 -> 0 (기록 없음)
    val weeklyStudy = List(7) { 0 }
    val weeklyExercise = List(7) { 0 }

    // 월간 임시 데이터 -> 0 (기록 없음)
    val monthlyStudyDaily = List(30) { 0 }
    val monthlyExerciseDaily = List(30) { 0 }
    
    // 월간을 주차 단위로 묶기
    val (monthlyStudyWeekly, monthlyExerciseWeekly) = remember(monthlyStudyDaily, monthlyExerciseDaily) {
        aggregateToWeeks(monthlyStudyDaily, monthlyExerciseDaily)
    }
    
    val periodTitle = if (isWeekly) "이번 주" else "이번 달"
    
    // 오늘 날짜 기준으로 자동 계산
    val today = remember { LocalDate.now() }
    val periodRange = remember(isWeekly, today) {
        if (isWeekly) {
            
            // 이번 주 월요일 ~ 일요일
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            
            // 월이 같으면 "3월 3일 - 9일", 다르면 "3월 31일 - 4월 6일"
            if (weekStart.monthValue == weekEnd.monthValue) {
                "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 - ${weekEnd.dayOfMonth}일"
            } else {
                "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 - ${weekEnd.monthValue}월 ${weekEnd.dayOfMonth}일"
            }
        } else {
            // 이번 달 1일 ~ 말일
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
            "${monthStart.monthValue}월 ${monthStart.dayOfMonth}일 - ${monthEnd.dayOfMonth}일"
        }
    }
    
    val diaryCount = 0 // 일기 개수
    val avgExerciseMin = 0 // 평균 운동 시간
    val totalExerciseMin = 0 // 총 운동 시간
    val totalStudyMin = 0 // 총 공부 시간
    
    // OpenAI 요약 내용이 이곳에 들어가기
    val aiSummaryText: String? = null
    
    
    // 요약 페이지 디자인 부분
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReportHeader()
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CheckSegmentedTabs(
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    left = "주간",
                    right = "월간"
                )
            }
            item { PeriodCard(title = periodTitle, range = periodRange) }
            item {
                EmotionSummaryCard(
                    title = "$periodTitle 나의 감정",
                    emptyText = "아직 감정 기록이 없습니다"
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.MenuBook,
                        title = "작성한 일기",
                        value = "${diaryCount}개"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.SportsGymnastics,
                        title = "평균 운동",
                        value = "${avgExerciseMin}분"
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.SportsGymnastics,
                        title = "총 운동시간",
                        value = "${totalExerciseMin}분"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.School,
                        title = "총 공부시간",
                        value = "${totalStudyMin}분"
                    )
                }
            }
            item {
                val labels = if (isWeekly) weeklyLabels else monthlyWeekLabels
                val study = if (isWeekly) weeklyStudy else monthlyStudyWeekly
                val exercise = if (isWeekly) weeklyExercise else monthlyExerciseWeekly
                HistogramCard(
                    title = if (isWeekly) "일별 활동" else "주간별 활동",
                    labels = labels,
                    study = study,
                    exercise = exercise,
                    isWeekly = isWeekly
                )
            }
            item {
                DiaryOrSummaryCard(
                    title = if (isWeekly) "이번 주 일기" else "이번 달 일기",
                    summary = aiSummaryText
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// 월간을 주간 단위로 묶기 함수
private fun aggregateToWeeks(studyDaily: List<Int>, exerciseDaily: List<Int>): Pair<List<Int>, List<Int>> {
    fun chunkSum(list: List<Int>, start: Int, endInclusive: Int): Int {
        var s = 0
        val end = minOf(endInclusive, list.lastIndex)
        for (i in start..end) s += list[i]
        return s
    }
    val ranges = listOf(
        0 to 6, // 1주
        7 to 13, // 2주
        14 to 20, // 3주
        21 to 27, // 4주
        28 to 34 // 5주 (29~31)
    )
    val study = ranges.map { (a, b) -> chunkSum(studyDaily, a, b) }
    val exercise = ranges.map { (a, b) -> chunkSum(exerciseDaily, a, b) }
    return study to exercise
}

@Composable
private fun ReportHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.BarChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text("활동 요약", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "나의 기록을 확인해보세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 주간, 월간 탭
@Composable
private fun CheckSegmentedTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    left: String,
    right: String
) {
    val outer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(outer, RoundedCornerShape(16.dp))
            .border(1.dp, outline, RoundedCornerShape(16.dp))
            .padding(5.dp)
    ) {
        CheckSegItem(
            text = left,
            selected = selectedIndex == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        CheckSegItem(
            text = right,
            selected = selectedIndex == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CheckSegItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = if (selected) 1.dp else 0.dp,
        modifier = modifier.fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Text("✓", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
            }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
@Composable
private fun PeriodCard(title: String, range: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(range, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
@Composable
private fun EmotionSummaryCard(title: String, emptyText: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// 일기 개수, 운도 시간 등 표현해주는 네모칸(?) 디자인
@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    val start = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val end = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(118.dp)
            .background(
                brush = Brush.linearGradient(listOf(start, end)),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// 통계 요약 히스토그램
@Composable
private fun HistogramCard(
    title: String,
    labels: List<String>,
    study: List<Int>,
    exercise: List<Int>,
    isWeekly: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HistogramWithHighlightTooltip(
                labels = labels,
                study = study,
                exercise = exercise,
                isWeekly = isWeekly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
        }
    }
}

// 히스토그램
@Composable
private fun HistogramWithHighlightTooltip(
    labels: List<String>,
    study: List<Int>,
    exercise: List<Int>,
    isWeekly: Boolean,
    modifier: Modifier = Modifier
) {
    val maxValue = max(1, (study + exercise).maxOrNull() ?: 1)
    val axisColor = Color(0xFF2F343A)
    val gridColor = Color(0xFFE7EBF0)
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
    val highlightColor = Color(0xFFBDBDBD).copy(alpha = 0.55f)
    val exerciseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val studyColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }
    var highlightRect by remember { mutableStateOf<Rect?>(null) }
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(labels, study, exercise) {
                    detectTapGestures { tap ->
                        val w = size.width
                        val h = size.height
                        val topPad = 10f
                        val bottomPad = 34f
                        val leftPad = 34f
                        val rightPad = 8f
                        val chartW = w - leftPad - rightPad
                        val chartH = h - topPad - bottomPad
                        val count = labels.size
                        val step = chartW / count
                        val centerX = { i: Int -> leftPad + step * i + step / 2f }
                        val highlightW = step * 0.60f
                        var hit: Int? = null
                        for (i in 0 until count) {
                            val cx = centerX(i)
                            val rect = Rect(
                                left = cx - highlightW / 2f,
                                top = topPad,
                                right = cx + highlightW / 2f,
                                bottom = topPad + chartH
                            )
                            if (rect.contains(tap)) { hit = i; break }
                        }
                        if (hit != null) {
                            selectedIndex = hit
                            val cx = centerX(hit)
                            tooltipAnchor = Offset(cx, topPad + chartH * 0.35f)
                            highlightRect = Rect(
                                left = cx - highlightW / 2f,
                                top = topPad,
                                right = cx + highlightW / 2f,
                                bottom = topPad + chartH
                            )
                        } else {
                            selectedIndex = null
                            highlightRect = null
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val topPad = 10f
            val bottomPad = 34f
            val leftPad = 34f
            val rightPad = 8f
            val chartW = w - leftPad - rightPad
            val chartH = h - topPad - bottomPad
            highlightRect?.let { r ->
                drawRect(
                    color = highlightColor,
                    topLeft = Offset(r.left, r.top),
                    size = androidx.compose.ui.geometry.Size(r.width, r.height)
                )
            }
            // grid
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topPad + chartH * (i / gridLines.toFloat())
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartW, y),
                    strokeWidth = 2f,
                    pathEffect = dash
                )
            }
            drawLine(axisColor, Offset(leftPad, topPad), Offset(leftPad, topPad + chartH), 2f)
            drawLine(axisColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), 2f)
            val count = labels.size
            val step = chartW / count
            val centerX = { i: Int -> leftPad + step * i + step / 2f }
            val barW = step * 0.18f
            val gap = step * 0.10f
            for (i in 0 until count) {
                val s = study.getOrElse(i) { 0 }
                val e = exercise.getOrElse(i) { 0 }
                val sH = (s.toFloat() / maxValue).coerceIn(0f, 1f) * (chartH * 0.90f)
                val eH = (e.toFloat() / maxValue).coerceIn(0f, 1f) * (chartH * 0.90f)
                val cx = centerX(i)
                val baseY = topPad + chartH
                val exLeft = cx - (barW + gap / 2f)
                drawRoundRect(
                    color = exerciseColor,
                    topLeft = Offset(exLeft, baseY - eH),
                    size = androidx.compose.ui.geometry.Size(barW, eH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                val stLeft = cx + (gap / 2f)
                drawRoundRect(
                    color = studyColor,
                    topLeft = Offset(stLeft, baseY - sH),
                    size = androidx.compose.ui.geometry.Size(barW, sH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 34.dp, end = 8.dp, bottom = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { t ->
                Text(
                    text = t,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        selectedIndex?.let { idx ->
            val label = labels.getOrElse(idx) { "" }
            val e = exercise.getOrElse(idx) { 0 }
            val s = study.getOrElse(idx) { 0 }
            TooltipCard(
                title = label,
                exercise = e,
                study = s,
                modifier = Modifier.offset {
                    IntOffset(tooltipAnchor.x.roundToInt() - 90, tooltipAnchor.y.roundToInt() - 120)
                }
            )
        }
    }
}
@Composable
private fun TooltipCard(
    title: String,
    exercise: Int,
    study: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        modifier = modifier
            .width(180.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text("운동 : $exercise", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("공부 : $study", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
        }
    }
}

// OpenAI로 요약한 내용 카드
@Composable
private fun DiaryOrSummaryCard(
    title: String,
    summary: String?
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val text = summary?.takeIf { it.isNotBlank() } ?: "아직 작성된 일기가 없습니다"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}