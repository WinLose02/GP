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
    reportViewModel: ReportViewModel = viewModel() // DB와 통신하기 위한 ViewModel
) {
    var tab by remember { mutableStateOf(0) } // 0: 주간, 1: 월간
    val isWeekly = tab == 0 // 현재 주간 탭이면 True

    // 히스토그램 X축 라벨
    // 주간 -> 요일, 월간 -> 주차
    val weeklyLabels = listOf("일", "월", "화", "수", "목", "금", "토")
    val monthlyWeekLabels = listOf("1주", "2주", "3주", "4주", "5주")
    
    // 주간 데이터를 실시간을 받아옴
    val weeklyStudy by reportViewModel.weeklyStudy.collectAsState()
    val weeklyExercise by reportViewModel.weeklyExercise.collectAsState()

    // 월간 데이터를 실시간으로 받아옴
    val monthlyStudyDaily by reportViewModel.monthlyStudyDaily.collectAsState()
    val monthlyExerciseDaily by reportViewModel.monthlyExerciseDaily.collectAsState()

    // (주간) 운동 및 공부 시간 합계/통계 값 계산
    val weeklyDiaryCount by reportViewModel.weeklyDiaryCount.collectAsState()
    val weeklyAvgExercise by reportViewModel.weeklyAvgExercise.collectAsState()
    val weeklyTotalExcercise by reportViewModel.weeklyTotalExercise.collectAsState()
    val weeklyTotalStudy by reportViewModel.weeklyTotalStudy.collectAsState()

    // (월간) 운동 및 공부 시간 합계/통계 값 계산
    val monthlyDiaryCount by reportViewModel.monthlyDiaryCount.collectAsState()
    val monthlyAvgExercise by reportViewModel.monthlyAvgExercise.collectAsState()
    val monthlyTotalExercise by reportViewModel.monthlyTotalExercise.collectAsState()
    val monthlyTotalStudy by reportViewModel.monthlyTotalStudy.collectAsState()

    // TODO: AI 일기 요약 파트 (추후에 연동 작업)
    val weeklyAiSummary by reportViewModel.weeklyAiSummary.collectAsState()
    val monthlyAiSummary by reportViewModel.weeklyAiSummary.collectAsState()

    // 선택된 탭에 따라 주간 또는 월간 데이터 중 하나를 선택
    val diaryCount = if (isWeekly) weeklyDiaryCount else monthlyDiaryCount
    val avgExerciseMin = if (isWeekly) weeklyAvgExercise else monthlyAvgExercise
    val totalExerciseMin = if (isWeekly) weeklyTotalExcercise else monthlyTotalExercise
    val totalStudyMin = if (isWeekly) weeklyTotalStudy else monthlyTotalStudy
    val aiSummaryText = if (isWeekly) weeklyAiSummary else monthlyAiSummary

    // 월간을 주차 단위로 묶기
    // remember -> 데이터가 변경될 때만 재계산을 위함
    val (monthlyStudyWeekly, monthlyExerciseWeekly) = remember(monthlyStudyDaily, monthlyExerciseDaily) {
        aggregateToWeeks(monthlyStudyDaily, monthlyExerciseDaily)
    }
    
    val periodTitle = if (isWeekly) "이번 주" else "이번 달"
    
    // 오늘 날짜 기준으로 자동 계산
    val today = remember { LocalDate.now() }

    // 기간 범위 텍스트 계산
    // EX> 월요일 ~ 일요일
    // EX> 3월 3일 ~ 9일
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

    
    
    // 요약 페이지 디자인 부분
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReportHeader()
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)) // 구분선
        androidx.compose.foundation.lazy.LazyColumn( // 스크롤을 가능하게
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CheckSegmentedTabs( // 주간/월간 탭
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    left = "주간",
                    right = "월간"
                )
            }
            item { PeriodCard(title = periodTitle, range = periodRange) } // 기간을 나타내는 카드
            item {
                EmotionSummaryCard( // 감정 요약 카드
                    title = "$periodTitle 나의 감정",
                    emptyText = "아직 감정 기록이 없습니다"
                )
            }
            item { // 일기 수, 평균 운동 데이터 카드
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
                        value = "${formatMinutes(avgExerciseMin)}"
                    )
                }
            }
            item { // 총 운동 시간, 총 공부 시간 카드
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.SportsGymnastics,
                        title = "총 운동시간",
                        value = "${formatMinutes(totalExerciseMin)}"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.School,
                        title = "총 공부시간",
                        value = "${formatMinutes(totalStudyMin)}"
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
// 반환 -> 공부 주차 리스트, 운동 주차 리스트를 Pair(쌍)으로 반환
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
            .heightIn(120.dp)
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
    // 전체 데이터 중 가장 큰 값을 계산
    /*
        [Example]
        study = [30, 60, 0, 45, 90, 20, 10]
        exercise = [20, 40, 30, 0, 60, 15, 5]
        study + exercise = [30, 60, 0, 45, 90, 20, 10, 20, 40, 30, 0, 60, 15, 5]
        .maxOrNull() = 90 => maxValue = 90

        이 값을 이용해 막대의 높이 비율을 계싼
        max(1,..)을 통해 0이 되는 것을 방지하기 위함
     */
    val maxValue = max(1, (study + exercise).maxOrNull() ?: 1)

    val axisColor = Color(0xFF2F343A)
    val gridColor = Color(0xFFE7EBF0)

    // 히스토그램의 디자인적인 요소들
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) } // 점선 효과를 정의

    val highlightColor = Color(0xFFBDBDBD).copy(alpha = 0.55f)
    val exerciseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val studyColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)

    var selectedIndex by remember { mutableStateOf<Int?>(null) } // 터치된 막대의 인덱스
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) } // 툴팁이 표시될 위치의 좌표
    var highlightRect by remember { mutableStateOf<Rect?>(null) } // 강조 표시할 사각형의 영역

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(labels, study, exercise) { // 터치 입력 처리를 위함
                    detectTapGestures { tap -> // 탭 터치를 감지

                        // 1. Canvas의 전체 너비 및 높이를 정의
                        val w = size.width
                        val h = size.height

                        // 2. 차트의 여백을 설정
                        val topPad = 10f
                        val bottomPad = 48f
                        val leftPad = 34f
                        val rightPad = 8f

                        // 실제 차트가 그려지는 너비와 높이를 계산
                        // EX> 전체 너비를 400px라고 하면, chartW = 400 - 34 - 8 = 358px
                        val chartW = w - leftPad - rightPad
                        val chartH = h - topPad - bottomPad

                        // X축 레이블의 개수
                        val count = labels.size

                        // 각 막대의 중심 X좌표 계산 함수
                        // step -> 각 레이블 하나가 차지하는 너비
                        // step = 차트의 전체 길이 / X축 라벨의 개수 = 차지하는 너비

                        /*
                            [Example]
                            chartW = 350px, count=7(주간)
                            step = 350 / 7 = 50px
                            --> 요일 한 칸당 50px씩 차지!
                         */
                        val step = chartW / count

                        // i번째 막대의 중심 X좌표를 계산
                        // i=0(일요일) -> center(0) = 34 + 50*0 + 25 = 59px
                        // i=1(월요일) -> center(1) = 34 + 50*1 + 25 = 109px
                        val centerX = { i: Int -> leftPad + step * i + step / 2f }

                        // 터치 위치가 어떤 막대 위에 있는지 판단
                        val highlightW = step * 0.60f // 터치를 감지하는 사각형의 너비 (step의 60%)
                        var hit: Int? = null // 터치된 막대의 인덱스를 저장

                        for (i in 0 until count) { // 터치 감지를 위한 사각형 영역을 생성
                            val cx = centerX(i)
                            val rect = Rect(
                                left = cx - highlightW / 2f,
                                top = topPad,
                                right = cx + highlightW / 2f,
                                bottom = topPad + chartH
                            )
                            if (rect.contains(tap)) { hit = i; break } // 만약 터치 위치가 사각형 안에 있으면 True
                        }

                        // 막대를 터치하면 강조 및 툴팁을 표시
                        if (hit != null) {
                            selectedIndex = hit // 터치된 해당 막대의 인덱스를 저장

                            // 툴팁을 표시할 위치를 계산
                            val cx = centerX(hit) // 터치한 막대의 중심 X좌표
                            tooltipAnchor = Offset(cx, topPad + chartH * 0.35f) // Y는 차트 높이의 35% 지점
                            highlightRect = Rect( // 강조될 사각형의 영역을 저장 -> 터치된 막대의 전체 높이만큼
                                left = cx - highlightW / 2f,
                                top = topPad,
                                right = cx + highlightW / 2f,
                                bottom = topPad + chartH
                            )
                        } else { // 빈 곳을 터치하면 해제
                            selectedIndex = null
                            highlightRect = null
                        }
                    }
                }
        ) { // 실제 Canvas에서 그리는 로직

            // Canvas 영역 설정
            val w = size.width
            val h = size.height

            // 차트 여백 및 크기를 계산
            val topPad = 10f
            val bottomPad = 48f
            val leftPad = 34f
            val rightPad = 8f
            val chartW = w - leftPad - rightPad
            val chartH = h - topPad - bottomPad

            highlightRect?.let { r -> // 선택된 막대 위에 반투명 회색 강조 사각형을 그림
                drawRect(
                    color = highlightColor,
                    topLeft = Offset(r.left, r.top),
                    size = androidx.compose.ui.geometry.Size(r.width, r.height)
                )
            }

            // 수평 격자선을 4개의 점선으로 그리기
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

            // X축, Y축 그리기
            drawLine(axisColor, Offset(leftPad, topPad), Offset(leftPad, topPad + chartH), 2f)
            drawLine(axisColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), 2f)

            val count = labels.size
            val step = chartW / count
            val centerX = { i: Int -> leftPad + step * i + step / 2f }

            // 막대 너비 및 막대 사이 간격
            val barW = step * 0.18f
            val gap = step * 0.10f

            // 각 막대의 높이를 계산
            for (i in 0 until count) {

                // 데이터가 없으면 0, 있으면 그 값을 받아옴
                val s = study.getOrElse(i) { 0 }
                val e = exercise.getOrElse(i) { 0 }

                // sH(공부 막대), eH(운동 막대)의 높이를 0~1사이로 제한하고,
                // *0.90f를 통해 차트 높이의 90%를 최대 높이로 설정
                val sH = (s.toFloat() / maxValue).coerceIn(0f, 1f) * (chartH * 0.90f)
                val eH = (e.toFloat() / maxValue).coerceIn(0f, 1f) * (chartH * 0.90f)

                val cx = centerX(i)
                val baseY = topPad + chartH

                // 운동 막대(왼쪽)
                val exLeft = cx - (barW + gap / 2f)
                drawRoundRect(
                    color = exerciseColor,
                    topLeft = Offset(exLeft, baseY - eH),
                    size = androidx.compose.ui.geometry.Size(barW, eH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )

                // 공부 막대(오른쪽)
                val stLeft = cx + (gap / 2f)
                drawRoundRect(
                    color = studyColor,
                    topLeft = Offset(stLeft, baseY - sH),
                    size = androidx.compose.ui.geometry.Size(barW, sH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
        }

        // 히스토그램 밑에 요일/주차 레이블 표시
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
            Text("운동 : ${formatMinutes(exercise)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("공부 : ${formatMinutes(study)}", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
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

// 시간 표현 포맷 함수
private fun formatMinutes (min: Int) : String {
    return if (min < 60) "${min}분"
    else "${min/60}시간 ${"%02d".format(min % 60)}분"
}