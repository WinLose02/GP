package com.example.diaryapplication.ui.screens

import android.net.Uri
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.RoundedCard
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.DiaryViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.max

// 날씨 타입
private enum class WeatherType(val label: String, val icon: @Composable () -> Unit) {
    SUNNY("맑음", { Icon(Icons.Outlined.LightMode, contentDescription = null) }),
    CLOUDY("흐림", { Icon(Icons.Outlined.CloudQueue, contentDescription = null) }),
    RAIN("비", { Icon(Icons.Outlined.WaterDrop, contentDescription = null) }),
    SNOW("눈", { Icon(Icons.Outlined.AcUnit, contentDescription = null) }),
}
@Composable
fun DiaryScreen(
    padding: PaddingValues, // 여백값
    diaryViewModel: DiaryViewModel = viewModel() // DB 통신을 위한 ViewModel
) {
    val today = remember { LocalDate.now() } // 오늘 날짜
    val yesterday = remember { LocalDate.now().minusDays(1) } // 어제 날짜
    var selectedDate by remember { mutableStateOf(today) } // 캘린더에서 선택된 날짜(디폴트: 오늘)
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) } // 현재 달

    val isLoading by diaryViewModel.isLoading.collectAsState()
    val currentDiary by diaryViewModel.currentDiary.collectAsState()
    val emotionEmojiMap by diaryViewModel.emotionEmojiMap.collectAsState()

    // 입력 상태
    var diaryText by remember { mutableStateOf("") } // 일기 내용
    var routineText by remember { mutableStateOf("") } // 하루 일과
    var bestThing by remember { mutableStateOf("") } // 가장 좋았던 일
    var regretThing by remember { mutableStateOf("") } // 가장 아쉬웠던 일
    var weather by remember { mutableStateOf(WeatherType.SUNNY) } // 선택된 날씨 (디폴트: 맑음)
    var exerciseMin by remember { mutableIntStateOf(0) } // 운동 시간
    var studyMin by remember { mutableIntStateOf(0) } // 공부 시간

    LaunchedEffect(month) { // 달이 바뀔 때마다 해당 달의 감정 이모지를 DB에서 가져옴
        diaryViewModel.loadMonthEmojis(month.year, month.monthValue)
    }
    LaunchedEffect(selectedDate) { // selectedDate가 바뀔때마다 해당 날짜의 일기를 불러옴
        diaryViewModel.loadDiary(selectedDate)
    }
    LaunchedEffect(currentDiary) { // currentDiary가 바뀔 때마다 값을 업데이트
        val diary = currentDiary
        if (diary != null) { // 일기가 있으면 기존 내용으로 필드를 채우기
            diaryText = diary.content
            routineText = diary.routine
            bestThing = diary.bestThing
            regretThing = diary.regretThing
            exerciseMin = diary.exerciseMin
            studyMin = diary.studyMin
            weather = WeatherType.entries.find { it.name == diary.weather } ?: WeatherType.SUNNY
        } else { // 일기가 없으면 모든 필드를 공백으로 초기화
            diaryText = ""
            routineText = ""
            bestThing = ""
            regretThing = ""
            exerciseMin = 0
            studyMin = 0
            weather = WeatherType.SUNNY
        }
    }

    // 일기 저장 시에, 스낵바 메시지를 표시하기 위함
    val snackbarHostState = remember { SnackbarHostState() }
    val isSaveSuccess by diaryViewModel.isSaveSuccess.collectAsState() // 일기가 저장되었는지 여부 -> True, False
    LaunchedEffect(isSaveSuccess) { // 일기가 저장 되었으면
        if (isSaveSuccess) snackbarHostState.showSnackbar("일기가 저장되었습니다 ✅") // 스낵바 메시지 출력
    }

    // 사진 선택
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // 선택된 사진의 경로 (null이면 사진 X)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri } // 사진 선택 완료 시, 사진의 URI 저장
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 페이지 타이틀
            Text(
                text = "📔 일기",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            // 날짜 선택
            RoundedCard(modifier = Modifier.fillMaxWidth()) {
                CalendarHeader(
                    yearMonth = month,
                    onPrev = { month = month.minusMonths(1) }, // < 버튼 클릭 시, 이전 달로
                    onNext = { month = month.plusMonths(1) } // > 버튼 클릭 시, 다음 달로
                )
                Spacer(Modifier.height(10.dp))
                CalendarGrid(
                    yearMonth = month,
                    selectedDate = selectedDate,
                    today = today,
                    yesterday = yesterday,
                    emotionEmojiByDate = emotionEmojiMap,
                    onSelect = { picked -> selectedDate = picked }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = formatKoreanDate(selectedDate), // 2026년 3월 1일 (일요일) 형태
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center // 가운데 정렬
                )
            }
            // 안내 배너
            InfoBanner(text = "💡 날짜를 선택하면 해당 날의 일기를 작성하거나 확인할 수 있어요")

            // 입력 폼
            RoundedCard(modifier = Modifier.fillMaxWidth()) {

                // 선택된 날짜 표시
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatKoreanDate(selectedDate),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))

                // 일기 텍스트
                Text("오늘의 일기 *", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = diaryText,
                    onValueChange = { diaryText = it },
                    placeholder = { Text("오늘 하루 어떠셨나요?") },
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AppFieldColor,
                        focusedContainerColor = AppFieldColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 사진 추가
                Spacer(Modifier.height(14.dp))
                Text("사진", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                if (selectedImageUri != null) { // 사진이 선택 되었다면
                    AsyncImage( // 미리 보기로 표시
                        model = selectedImageUri, // 사진
                        contentDescription = "선택한 사진",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(AppFieldColor, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(10.dp))

                    // 사진이 선택이 되면 다시 선택하기, 아니면 사진 추가하기
                    Text(if (selectedImageUri == null) "사진 추가하기" else "사진 다시 선택하기")
                }
            }

            // 날씨, 운동 및 공부 시간 컨트롤, 일과 필드
            RoundedCard(modifier = Modifier.fillMaxWidth()) {
                Text("날씨", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))

                // 날씨 선택 버튼 및 선택된 날씨로 변수에 저장
                WeatherRow(selected = weather, onSelect = { weather = it })
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DurationPickerField(
                        title = "운동시간",
                        totalMinutes = exerciseMin,
                        onPick = { exerciseMin = it }, // 선택 완료 시, 값 업데이트
                        modifier = Modifier.weight(1f), // ROW의 절반을 차지
                        minMinutes = 0, // 최소: 0분
                        maxMinutes = 1440, // 최대: 1440분(24시간)
                        minuteStep = 10 // 10분 단위로 컨트롤
                    )
                    DurationPickerField(
                        title = "공부시간",
                        totalMinutes = studyMin,
                        onPick = { studyMin = it }, // 선택 완료 시, 값 업데이트
                        modifier = Modifier.weight(1f), // ROW의 절반을 차지
                        minMinutes = 0, // 최소 : 0분
                        maxMinutes = 1440, // 최대 : 1440분(24시간)
                        minuteStep = 10 // 10분 단위로 컨트롤
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("하루 일과", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SoftOutlinedTextField(
                    value = routineText, // 입력된 일과 텍스트
                    onValueChange = { routineText = it }, // 입력 시, 데이터 업데이트
                    placeholder = "오늘 무엇을 하셨나요?", // 힌트 메시지
                    containerColor = AppFieldColor // 배경 색 - 연한 회색
                )
            }

            // 일기 작성 도우미 필드
            HelperCard(
                bestThing = bestThing, // 가장 좋았던 일 텍스트
                onBestChange = { bestThing = it }, // 입력 시, 데이터 업데이트
                regretThing = regretThing, // 가장 아쉬웠던 일 텍스트
                onRegretChange = { regretThing = it } // 입력 시, 데이터 업데이트
            )

            // 저장하기
            PrimaryPillButton(
                text = if (isLoading) "저장 중 . . . " else "일기 저장하기",
                onClick = { // TODO:  AI 감정 분석 + DB 저장
                    diaryViewModel.saveDiary(
                        date = selectedDate,
                        content = diaryText,
                        weather = weather.name,
                        exerciseMin = exerciseMin,
                        studyMin = studyMin,
                        routine = routineText,
                        bestThing = bestThing,
                        regretThing = regretThing,
                        onSuccess = {}
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }
    }
}
// 안내 배너
@Composable
private fun InfoBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}
// 달력 배너
@Composable
private fun CalendarHeader(yearMonth: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Outlined.ChevronLeft, contentDescription = null) }
        Text("${yearMonth.monthValue}월 ${yearMonth.year}", style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onNext) { Icon(Icons.Outlined.ChevronRight, contentDescription = null) }
    }
}
// 달력 그리드
@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    yesterday: LocalDate,
    emotionEmojiByDate: Map<LocalDate, String>,
    onSelect: (LocalDate) -> Unit
) {
    val first = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDowIndex = dowIndexSundayStart(first.dayOfWeek)
    val labels = listOf("일", "월", "화", "수", "목", "금", "토")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNumber = week * 7 + col - firstDowIndex + 1
                    val date = if (dayNumber in 1..daysInMonth) yearMonth.atDay(dayNumber) else null
                    val enabled = date != null && (date == today || date == yesterday)
                    CalendarCell(
                        date = date,
                        isSelected = date == selectedDate,
                        enabled = enabled,
                        emoji = if (date != null) emotionEmojiByDate[date] else null,
                        onClick = { if (date != null && enabled) onSelect(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
// 달력 셀
@Composable
private fun CalendarCell(
    date: LocalDate?,
    isSelected: Boolean,
    enabled: Boolean,
    emoji: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val dateColor = when {
        date == null -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(enabled = date != null && enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date?.dayOfMonth?.toString() ?: "", color = dateColor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(2.dp))
            Text(emoji ?: "", style = MaterialTheme.typography.bodySmall)
        }
    }
}
// 날씨 선택
@Composable
private fun WeatherRow(selected: WeatherType, onSelect: (WeatherType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        WeatherType.entries.forEach { item ->
            val isActive = item == selected
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else AppFieldColor,
                modifier = Modifier.weight(1f).height(64.dp).clickable { onSelect(item) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item.icon()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
// 일기 작성 도우미
@Composable
private fun HelperCard(
    bestThing: String, onBestChange: (String) -> Unit,
    regretThing: String, onRegretChange: (String) -> Unit
) {
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
        )
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.background(gradient, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("일기 작성 도우미", style = MaterialTheme.typography.titleSmall)
            }
            Text("오늘 하루 중 가장 좋았던 일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = bestThing,
                onValueChange = onBestChange,
                placeholder = "기억하고 싶은 순간을 적어보세요",
                containerColor = AppFieldColor
            )
            Text("오늘 하루 가장 아쉬웠던 일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = regretThing,
                onValueChange = onRegretChange,
                placeholder = "아쉬운 점을 적어보세요",
                containerColor = AppFieldColor
            )
        }
    }
}
// 시간 선택 필드
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPickerField(
    title: String,
    totalMinutes: Int,
    onPick: (Int) -> Unit, // 선택 완료 시, 선택한 시간(분)을 전달
    modifier: Modifier = Modifier,
    minMinutes: Int = 0, // 최소 : 0분
    maxMinutes: Int = 1440, // 최대 : 1440분
    minuteStep: Int = 10 // 10분 단위
) {
    var open by remember { mutableStateOf(false) } // BottomSheet가 기본적으로 닫혀 있음 (False)
    Column(modifier = modifier) {
        Text("$title (시간/분)", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().clickable { open = true }) {
            OutlinedTextField(
                value = formatDuration(totalMinutes), // 90분 -> 1시간 30분 형태로 변환
                onValueChange = {},
                enabled = false, // 타이핑 방지를 위해 비활성화
                readOnly = true,
                singleLine = true, // 한 줄로만 표시
                trailingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) }, // 시계 아이콘 표시
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors( // 색상 관련 파라미터
                    disabledContainerColor = AppFieldColor,
                    disabledBorderColor = Color.Transparent,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            )
        }
    }
    if (open) { // BottomSheet가 열렸을 경우(True)
        HourMinutePickerSheet(
            title = title,
            initialTotalMinutes = totalMinutes, // 현재 데이터를 초기 값으로 설정
            minMinutes = minMinutes,
            maxMinutes = maxMinutes,
            minuteStep = minuteStep,
            onDismiss = { open = false }, // 취소 혹은 바깥 부분을 터치하면 창만 닫기
            onConfirm = { picked ->
                onPick(picked.coerceIn(minMinutes, maxMinutes))
                open = false
            }
        )
    }
}
// 시간 선택 BottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourMinutePickerSheet(
    title: String,
    initialTotalMinutes: Int,
    minMinutes: Int,
    maxMinutes: Int,
    minuteStep: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHour = maxMinutes / 60 // 최대 시간 계산
    val hourValues = remember(maxHour) { (0..maxHour).toList() } // [0,1,2,3,..,24]시간
    val minuteValues = remember(minuteStep) {
        // 10분 단위: 0, 10, 20, 30, 40, 50
        (0..59 step minuteStep).toList()
    }
    // 초기값을 Step에 맞춰 정리
    val initClamped = initialTotalMinutes.coerceIn(minMinutes, maxMinutes) // 초기 값을 0분 ~ 1440분(24시간) 내로 조정
    val initHour = (initClamped / 60).coerceIn(0, maxHour) // 조정한 값을 시간 단위로 변환
    val initMin = (initClamped % 60 / minuteStep) * minuteStep // 나머지 값을 활용해서 분 단위 값을 계산

    // 현재 선택된 시간의 인덱스 ({} 안에 있는 부분이 계산한 시간 단위 값의 인덱스를 반환)
    var hourIndex by remember { mutableIntStateOf(hourValues.indexOf(initHour).coerceAtLeast(0)) }

    // 현재 선택된 시간(분)의 인덱스 ({} 안에 있는 부분이 계산한 분 단위 값의 인덱스를 반환)
    var minuteIndex by remember { mutableIntStateOf(minuteValues.indexOf(initMin).coerceAtLeast(0)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss, // 바깥 터치 시, onDismiss 호출
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 시간 휠(다이얼)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("시간", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier
                            .height(160.dp)
                            .width(120.dp),
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0
                                maxValue = hourValues.lastIndex // 선택 가능한 인덱스 범위를 설정
                                displayedValues = hourValues.map { it.toString() }.toTypedArray() // 화면에 보여줄 텍스트
                                value = hourIndex // 초기 선택값 설정
                                wrapSelectorWheel = false // 끝에서 처음으로 돌아가지 않도록 방지 (24->0으로 가지 않게)
                                setOnValueChangedListener { _, _, v -> hourIndex = v } // 휠(다이얼)을 스크롤 시에 값을 업데이트
                            } },
                        update = { p -> // 상태가 바뀔 때 마다 실행
                            p.minValue = 0; p.maxValue = hourValues.lastIndex
                            p.displayedValues = hourValues.map { it.toString() }.toTypedArray()
                            if (p.value != hourIndex) p.value = hourIndex // 현재 값과 업데이트 값이 다를 때만, 값을 업데이트 함
                        }
                    )
                }
                // 분 단위 휠(다이얼)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("분", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier
                            .height(160.dp)
                            .width(120.dp),
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0
                                maxValue = minuteValues.lastIndex

                                // 분의 값을 두자리 형태로 고정
                                // 0분 -> 00분, 5분 -> 05분, 30분 -> 30분 형태로 맞춤
                                displayedValues = minuteValues.map { it.toString().padStart(2, '0') }.toTypedArray()
                                value = minuteIndex
                                wrapSelectorWheel = false
                                setOnValueChangedListener { _, _, v -> minuteIndex = v }
                            }},
                        update = { p ->
                            p.minValue = 0
                            p.maxValue = minuteValues.lastIndex
                            p.displayedValues =
                                minuteValues.map { it.toString().padStart(2, '0') }.toTypedArray()
                            if (p.value != minuteIndex) p.value = minuteIndex
                        }
                    )
                }
            }
            // 현재 선택된 시 단위의 값과 분 단위의 값을 0시간 0분 형태로 계산
            // 그 후에, 범위 내의 값으로 조정(맞춰줌)
            val clampedPreview = (hourValues[hourIndex] * 60 + minuteValues[minuteIndex])
                .coerceIn(minMinutes, maxMinutes)
            Text(
                "선택: ${formatDuration(clampedPreview)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss, // 취소 버튼 시, BottomSheet 닫기
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp)
                ) { Text("취소") }
                Button(
                    onClick = { onConfirm(clampedPreview) }, // 선택 완료 시, 선택된 분 값을 전달
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp)
                ) { Text("완료") }
            }
        }
    }
}

// 그 외의 유틸리티 함수들
private fun formatDuration(totalMinutes: Int): String {
    val m = max(0, totalMinutes)
    return if (m / 60 == 0) "${m % 60}분" else "${m / 60}시간 ${m % 60}분"
}
// 이번 달 1일을 무슨 요일인지에 따라 달력에 표시할 위치를 정하는 함수 -> 인덱스 반환
// 이게 없으면 매달 1일이 일요일로 시작하는 문제 발생
private fun dowIndexSundayStart(dow: DayOfWeek): Int = when (dow) {
    DayOfWeek.SUNDAY -> 0; DayOfWeek.MONDAY -> 1; DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3; DayOfWeek.THURSDAY -> 4; DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
}
private fun formatKoreanDate(date: LocalDate): String {
    val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREAN)
    return "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 ($dayName)"
}