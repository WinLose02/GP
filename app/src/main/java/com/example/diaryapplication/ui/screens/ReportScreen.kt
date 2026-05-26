package com.example.diaryapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.ReportViewModel
import kotlin.math.max
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun ReportScreen(
    padding: PaddingValues,
    reportViewModel: ReportViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(0) }
    val isWeekly = tab == 0

    LaunchedEffect(Unit) {
        reportViewModel.loadWeeklyData()
        reportViewModel.loadMonthlyData()
    }

    val weeklyLabels = listOf("일", "월", "화", "수", "목", "금", "토")
    val monthlyWeekLabels = listOf("1주", "2주", "3주", "4주", "5주")

    val weeklyStudy by reportViewModel.weeklyStudy.collectAsState()
    val weeklyExercise by reportViewModel.weeklyExercise.collectAsState()
    val monthlyStudyDaily by reportViewModel.monthlyStudyDaily.collectAsState()
    val monthlyExerciseDaily by reportViewModel.monthlyExerciseDaily.collectAsState()

    val weeklyDiaryCount by reportViewModel.weeklyDiaryCount.collectAsState()
    val weeklyAvgExercise by reportViewModel.weeklyAvgExercise.collectAsState()
    val weeklyTotalExercise by reportViewModel.weeklyTotalExercise.collectAsState()
    val weeklyTotalStudy by reportViewModel.weeklyTotalStudy.collectAsState()

    val monthlyDiaryCount by reportViewModel.monthlyDiaryCount.collectAsState()
    val monthlyAvgExercise by reportViewModel.monthlyAvgExercise.collectAsState()
    val monthlyTotalExercise by reportViewModel.monthlyTotalExercise.collectAsState()
    val monthlyTotalStudy by reportViewModel.monthlyTotalStudy.collectAsState()

    val weeklyEmotions by reportViewModel.weeklyEmotions.collectAsState()
    val monthlyEmotions by reportViewModel.monthlyEmotions.collectAsState()
    val emotions = if (isWeekly) weeklyEmotions else monthlyEmotions

    val weeklySummaries by reportViewModel.weeklySummaries.collectAsState()
    val monthlySummaries by reportViewModel.monthlySummaries.collectAsState()

    val diaryCount = if (isWeekly) weeklyDiaryCount else monthlyDiaryCount
    val avgExerciseMin = if (isWeekly) weeklyAvgExercise else monthlyAvgExercise
    val totalExerciseMin = if (isWeekly) weeklyTotalExercise else monthlyTotalExercise
    val totalStudyMin = if (isWeekly) weeklyTotalStudy else monthlyTotalStudy
    val summaries = if (isWeekly) weeklySummaries else monthlySummaries

    val (monthlyStudyWeekly, monthlyExerciseWeekly) = remember(monthlyStudyDaily, monthlyExerciseDaily) {
        aggregateToWeeks(monthlyStudyDaily, monthlyExerciseDaily)
    }

    val selectedWeekDate by reportViewModel.selectedWeekDate.collectAsState()
    val selectedMonthDate by reportViewModel.selectedMonthDate.collectAsState()
    val today = remember { LocalDate.now() }

    val isCurrentWeek = remember(selectedWeekDate) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        !selectedWeekDate.isBefore(weekStart) && !selectedWeekDate.isAfter(weekEnd)
    }
    val isCurrentMonth = remember(selectedMonthDate) {
        selectedMonthDate.year == today.year && selectedMonthDate.monthValue == today.monthValue
    }

    val periodTitle = if (isWeekly) {
        if (isCurrentWeek) "이번 주" else "${selectedWeekDate.monthValue}월"
    } else {
        if (isCurrentMonth) "이번 달" else "${selectedMonthDate.monthValue}월"
    }

    val todayWeeklyIndex = remember(today, selectedWeekDate) {
        val weekStart = selectedWeekDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = selectedWeekDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        if (!today.isBefore(weekStart) && !today.isAfter(weekEnd)) today.dayOfWeek.value % 7 else -1
    }
    val todayMonthlyIndex = remember(today, selectedMonthDate) {
        if (today.year == selectedMonthDate.year && today.monthValue == selectedMonthDate.monthValue)
            minOf((today.dayOfMonth - 1) / 7, 4)
        else -1
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReportHeader()
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        LazyColumn(
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
            item {
                PeriodNavCard(
                    isWeekly = isWeekly,
                    selectedDate = if (isWeekly) selectedWeekDate else selectedMonthDate,
                    today = today,
                    onNavigate = { delta ->
                        if (isWeekly) reportViewModel.navigateWeek(delta)
                        else reportViewModel.navigateMonth(delta)
                    }
                )
            }
            item {
                EmotionSummaryCard(
                    title = "$periodTitle 나의 감정",
                    emptyText = "아직 감정 기록이 없습니다",
                    emotions = emotions
                )
            }
            item {
                StatsRow(
                    diaryCount = diaryCount,
                    avgExerciseMin = avgExerciseMin,
                    totalExerciseMin = totalExerciseMin,
                    totalStudyMin = totalStudyMin
                )
            }
            item {
                val labels = if (isWeekly) weeklyLabels else monthlyWeekLabels
                val study = if (isWeekly) weeklyStudy else monthlyStudyWeekly
                val exercise = if (isWeekly) weeklyExercise else monthlyExerciseWeekly
                val todayIndex = if (isWeekly) todayWeeklyIndex else todayMonthlyIndex
                HistogramCard(
                    title = if (isWeekly) "일별 활동" else "주간별 활동",
                    labels = labels,
                    study = study,
                    exercise = exercise,
                    isWeekly = isWeekly,
                    todayIndex = todayIndex
                )
            }
            item {
                DiaryOrSummaryCard(
                    title = "$periodTitle 일기",
                    summaries = summaries,
                    isWeekly = isWeekly
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun aggregateToWeeks(studyDaily: List<Int>, exerciseDaily: List<Int>): Pair<List<Int>, List<Int>> {
    fun chunkSum(list: List<Int>, start: Int, endInclusive: Int): Int {
        var s = 0
        val end = minOf(endInclusive, list.lastIndex)
        for (i in start..end) s += list[i]
        return s
    }
    val ranges = listOf(0 to 6, 7 to 13, 14 to 20, 21 to 27, 28 to 34)
    val study = ranges.map { (a, b) -> chunkSum(studyDaily, a, b) }
    val exercise = ranges.map { (a, b) -> chunkSum(exerciseDaily, a, b) }
    return study to exercise
}

@Composable
private fun ReportHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF3D7BF4), Color(0xFF818CF8))),
                    RoundedCornerShape(999.dp)
                )
        )
        Column {
            Text(
                "활동 요약",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "나의 기록을 확인해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
        CheckSegItem(text = left, selected = selectedIndex == 0, onClick = { onSelect(0) }, modifier = Modifier.weight(1f))
        CheckSegItem(text = right, selected = selectedIndex == 1, onClick = { onSelect(1) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CheckSegItem(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
private fun PeriodNavCard(
    isWeekly: Boolean,
    selectedDate: LocalDate,
    today: LocalDate,
    onNavigate: (Int) -> Unit
) {
    val isCurrentPeriod = if (isWeekly) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        !selectedDate.isBefore(weekStart) && !selectedDate.isAfter(weekEnd)
    } else {
        selectedDate.year == today.year && selectedDate.monthValue == today.monthValue
    }

    val title = if (isWeekly) {
        val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        "${weekStart.year}년 ${weekStart.monthValue}월"
    } else {
        "${selectedDate.year}년 ${selectedDate.monthValue}월"
    }

    val range = if (isWeekly) {
        val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        if (weekStart.monthValue == weekEnd.monthValue)
            "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 – ${weekEnd.dayOfMonth}일"
        else
            "${weekStart.monthValue}월 ${weekStart.dayOfMonth}일 – ${weekEnd.monthValue}월 ${weekEnd.dayOfMonth}일"
    } else {
        val monthEnd = selectedDate.with(TemporalAdjusters.lastDayOfMonth())
        "${selectedDate.monthValue}월 1일 – ${monthEnd.dayOfMonth}일"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                onClick = { onNavigate(-1) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronLeft, null, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(range, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isCurrentPeriod) {
                    Spacer(Modifier.height(1.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            if (isWeekly) "이번 주" else "이번 달",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = { if (!isCurrentPeriod) onNavigate(1) },
                shape = RoundedCornerShape(10.dp),
                color = if (!isCurrentPeriod) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color.Transparent,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (!isCurrentPeriod) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmotionSummaryCard(title: String, emptyText: String, emotions: List<String>) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (emotions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val sorted = emotions.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            val maxCount = sorted.first().value
            val total = emotions.size
            val top = sorted.first()

            Column {
                // 그라디언트 히어로 섹션
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF3D7BF4), Color(0xFF6366F1))))
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(top.key, fontSize = 44.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "가장 많이 느낀 감정",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                emojiToEmotionName(top.key),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "전체 기록의 ${top.value * 100 / total}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // 감정별 바 차트
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    sorted.forEach { (emoji, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                emoji,
                                fontSize = 17.sp,
                                modifier = Modifier.width(22.dp),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(count.toFloat() / maxCount)
                                        .background(
                                            Brush.horizontalGradient(listOf(Color(0xFF3D7BF4), Color(0xFF93C5FD))),
                                            RoundedCornerShape(999.dp)
                                        )
                                )
                            }
                            Text(
                                "${count}회",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(24.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    diaryCount: Int,
    avgExerciseMin: Int,
    totalExerciseMin: Int,
    totalStudyMin: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StatMiniCard("📝", "작성한\n일기", "${diaryCount}개", Modifier.weight(1f))
        StatMiniCard("🏃", "평균\n운동", formatMinutes(avgExerciseMin), Modifier.weight(1f))
        StatMiniCard("💪", "총 운동\n시간", formatMinutes(totalExerciseMin), Modifier.weight(1f))
        StatMiniCard("📚", "총 공부\n시간", formatMinutes(totalStudyMin), Modifier.weight(1f))
    }
}

@Composable
private fun StatMiniCard(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

private fun emojiToEmotionName(emoji: String): String = when (emoji) {
    "😊" -> "행복"
    "😢" -> "슬픔"
    "😡" -> "화남"
    "😰" -> "불안"
    "😐" -> "평온"
    "😄" -> "기쁨"
    "🥺" -> "속상함"
    "😴" -> "피곤함"
    "😎" -> "자신감"
    "🤩" -> "설렘"
    "😤" -> "짜증"
    "🤗" -> "감사"
    else -> emoji
}

@Composable
private fun HistogramCard(
    title: String,
    labels: List<String>,
    study: List<Int>,
    exercise: List<Int>,
    isWeekly: Boolean,
    todayIndex: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(color = Color(0xFF60A5FA), label = "운동")
                    LegendDot(color = MaterialTheme.colorScheme.primary, label = "공부")
                }
            }
            HistogramWithHighlightTooltip(
                labels = labels,
                study = study,
                exercise = exercise,
                isWeekly = isWeekly,
                todayIndex = todayIndex,
                modifier = Modifier.fillMaxWidth().height(190.dp)
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistogramWithHighlightTooltip(
    labels: List<String>,
    study: List<Int>,
    exercise: List<Int>,
    isWeekly: Boolean,
    todayIndex: Int,
    modifier: Modifier = Modifier
) {
    val maxValue = max(1, (study + exercise).maxOrNull() ?: 1)

    val gridColor = Color(0xFFF3F4F6)
    val highlightColor = Color(0xFFBDBDBD).copy(alpha = 0.40f)
    val todayBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)

    val exerciseGradientTop = Color(0xFF60A5FA)
    val exerciseGradientBottom = Color(0xFFBFDBFE)
    val studyGradientTop = MaterialTheme.colorScheme.primary
    val studyGradientBottom = Color(0xFF93C5FD)

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
                        val bottomPad = 48f
                        val leftPad = 8f
                        val rightPad = 8f
                        val chartW = w - leftPad - rightPad
                        val chartH = h - topPad - bottomPad
                        val count = labels.size
                        val step = chartW / count
                        val centerX = { i: Int -> leftPad + step * i + step / 2f }
                        val hitW = step * 0.75f
                        var hit: Int? = null
                        for (i in 0 until count) {
                            val cx = centerX(i)
                            val rect = Rect(cx - hitW / 2f, topPad, cx + hitW / 2f, topPad + chartH)
                            if (rect.contains(tap)) { hit = i; break }
                        }
                        if (hit != null) {
                            selectedIndex = hit
                            val cx = centerX(hit)
                            tooltipAnchor = Offset(cx, topPad + chartH * 0.35f)
                            highlightRect = Rect(cx - hitW / 2f, topPad, cx + hitW / 2f, topPad + chartH)
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
            val bottomPad = 48f
            val leftPad = 8f
            val rightPad = 8f
            val chartW = w - leftPad - rightPad
            val chartH = h - topPad - bottomPad

            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topPad + chartH * (i / gridLines.toFloat())
                drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + chartW, y), strokeWidth = 1.5f)
            }

            val count = labels.size
            val step = chartW / count
            val centerX = { i: Int -> leftPad + step * i + step / 2f }
            val barW = step * 0.28f
            val gap = step * 0.07f

            if (todayIndex >= 0) {
                val cx0 = centerX(todayIndex)
                val bgW = step * 0.82f
                drawRoundRect(
                    color = todayBgColor,
                    topLeft = Offset(cx0 - bgW / 2f, topPad),
                    size = androidx.compose.ui.geometry.Size(bgW, chartH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                )
            }

            highlightRect?.let { r ->
                drawRect(
                    color = highlightColor,
                    topLeft = Offset(r.left, r.top),
                    size = androidx.compose.ui.geometry.Size(r.width, r.height)
                )
            }

            for (i in 0 until count) {
                val s = study.getOrElse(i) { 0 }
                val e = exercise.getOrElse(i) { 0 }
                val sH = (s.toFloat() / maxValue).coerceIn(0f, 1f) * (chartH * 0.90f)
                val eH = (e.toFloat() / maxValue).coerceIn(0f, 1f) * (chartH * 0.90f)
                val cx = centerX(i)
                val baseY = topPad + chartH

                val exLeft = cx - (barW + gap / 2f)
                if (eH > 1f) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(exerciseGradientTop, exerciseGradientBottom),
                            start = Offset(exLeft, baseY - eH),
                            end = Offset(exLeft, baseY)
                        ),
                        topLeft = Offset(exLeft, baseY - eH),
                        size = androidx.compose.ui.geometry.Size(barW, eH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                }

                val stLeft = cx + (gap / 2f)
                if (sH > 1f) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(studyGradientTop, studyGradientBottom),
                            start = Offset(stLeft, baseY - sH),
                            end = Offset(stLeft, baseY)
                        ),
                        topLeft = Offset(stLeft, baseY - sH),
                        size = androidx.compose.ui.geometry.Size(barW, sH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { i, t ->
                val isToday = i == todayIndex
                Text(
                    text = t,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
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
private fun TooltipCard(title: String, exercise: Int, study: Int, modifier: Modifier = Modifier) {
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
            Text("운동 : ${formatMinutes(exercise)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("공부 : ${formatMinutes(study)}", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryOrSummaryCard(
    title: String,
    summaries: List<Pair<String, String>>,
    isWeekly: Boolean
) {
    val previewCount = 3
    var showAll by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasMore = summaries.size > previewCount
    val displayed = if (isWeekly && showAll) summaries else summaries.take(previewCount)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (summaries.isNotEmpty()) {
                    Text("${summaries.size}개", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (summaries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("아직 작성된 일기가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                Column {
                    displayed.forEachIndexed { index, (date, summary) ->
                        val isLast = index == displayed.lastIndex && !(hasMore && !showAll)
                        TimelineItem(date = date, summary = summary, isLast = isLast)
                    }
                }

                if (hasMore) {
                    Surface(
                        onClick = { if (isWeekly) showAll = !showAll else showSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isWeekly) (if (showAll) "접기" else "더 보기") else "전체 보기",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isWeekly) (if (showAll) "▲" else "▼") else "→",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$title 전체", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${summaries.size}개", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 36.dp)
                ) {
                    itemsIndexed(summaries) { index, (date, summary) ->
                        TimelineItem(date = date, summary = summary, isLast = index == summaries.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(date: String, summary: String, isLast: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = primary.copy(alpha = 0.18f)
    val bgColor = primary.copy(alpha = 0.05f)

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp).fillMaxHeight()
        ) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.5.dp, primary, CircleShape)
            )
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).weight(1f).background(lineColor))
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 10.dp, bottom = if (isLast) 0.dp else 14.dp)
                .weight(1f)
        ) {
            Text(
                text = formatSummaryDate(date),
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = bgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(9.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun formatSummaryDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")
        val dayName = dayNames[date.dayOfWeek.value % 7]
        "${date.monthValue}월 ${date.dayOfMonth}일 · $dayName"
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatMinutes(min: Int): String {
    return if (min < 60) "${min}분"
    else "${min / 60}시간 ${"%02d".format(min % 60)}분"
}
