package com.example.mobileapptest2.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.RoundedCard
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private enum class WeatherType(val label: String, val icon: @Composable () -> Unit) {
    SUNNY("맑음", { Icon(Icons.Outlined.LightMode, contentDescription = null) }),
    CLOUDY("흐림", { Icon(Icons.Outlined.CloudQueue, contentDescription = null) }),
    RAIN("비", { Icon(Icons.Outlined.WaterDrop, contentDescription = null) }),
    SNOW("눈", { Icon(Icons.Outlined.AcUnit, contentDescription = null) }),
}

@Composable
fun HomeScreen(padding: PaddingValues) {

    val today = remember { LocalDate.now() } // 오늘 날짜
    val yesterday = remember { LocalDate.now().minusDays(1) } // 어제 날짜

    var selectedDate by remember { mutableStateOf(today) } // 캘린더에서 선택된 날짜(디폴트: 오늘)
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) } // 현재 달

    // 날짜 별 감정 이모티콘
    // 여기에 OpenAI 연동
    val emotionEmojiByDate = remember {
        mutableStateMapOf(
            today to "😊",
            yesterday to "😢",
        )
    }

    // 입력 상태
    var diaryText by remember { mutableStateOf("") } // 일기 내용
    var routineText by remember { mutableStateOf("") } // 하루 일과
    var bestThing by remember { mutableStateOf("") } // 가장 좋았더 일
    var regretThing by remember { mutableStateOf("") } // 가장 아쉬웠던 일

    var weather by remember { mutableStateOf(WeatherType.SUNNY) } // 선택된 날씨 (디폴트: 맑음)


    var exerciseMin by remember { mutableIntStateOf(0) } // 운동 시간
    var studyMin by remember { mutableIntStateOf(0) } // 공부 시간

    // 사진 선택
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // 선택된 사진의 경로 (null이면 사진 없음)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri } // 선택 완료 시, 사진의 URI를 저장
    )

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderCard(
            title = "감정 다이어리",
            subtitle = "찬님, 안녕하세요!" // TODO: 추후에 사용자 닉네임을 반영하기
        )

        // 날짜 선택
        RoundedCard(modifier = Modifier.fillMaxWidth()) {
            Text("날짜 선택", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))

            CalendarHeader(
                yearMonth = month,
                onPrev = { month = month.minusMonths(1) }, // < 버튼 클릭 시 이전 달로
                onNext = { month = month.plusMonths(1) } // > 버튼 클릭 시 다음 달로
            )

            Spacer(Modifier.height(10.dp))

            CalendarGrid(
                yearMonth = month,
                selectedDate = selectedDate,
                today = today,
                yesterday = yesterday,
                emotionEmojiByDate = emotionEmojiByDate,
                onSelect = { picked -> // 오늘과 하루 전(어제)만 클릭 할 수 있도록
                    if (picked == today || picked == yesterday) selectedDate = picked
                }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = formatKoreanDate(selectedDate), // 2026년 3월 1일 (일요일) 형태
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center // 가운데 정렬
            )
        }

        InfoBanner(text = "💡 일기를 작성하시면 AI가 자동으로 감정을 분석하여 캘린더에 표시해드립니다")

        // 일기와 사진 추가 필드
        RoundedCard(modifier = Modifier.fillMaxWidth()) {
            Text("오늘의 일기 *", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

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

            Spacer(Modifier.height(12.dp))

            Text("사진", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(if (selectedImageUri == null) "사진 추가하기" else "사진 다시 선택하기") // 사진 선택이 되면 다시 선택하기, 아니면 사진 추가하기
            }
        }

        // 날씨 , 운동 및 공부 시간 조절, 일과 필드
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
                    modifier = Modifier.weight(1f), // Row의 절반
                    minMinutes = 0, // 최소 0분
                    maxMinutes = 1440, // 최대 24시간
                    minuteStep = 10 // 10분 단위
                )
                DurationPickerField(
                    title = "공부시간",
                    totalMinutes = studyMin,
                    onPick = { studyMin = it }, // 선택 완료 시, 값 업데이트
                    modifier = Modifier.weight(1f), // Row의 나머지 절반
                    minMinutes = 0, // 최소 0분
                    maxMinutes = 1440, // 최대 24시간
                    minuteStep = 10 // 10분 단위
                )
            }

            Spacer(Modifier.height(14.dp))

            Text("하루 일과", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            SoftOutlinedTextField(
                value = routineText, // 입력된 일과 텍스트
                onValueChange = { routineText = it }, // 입력 시, 데이터 업데이트
                placeholder = "오늘 무엇을 하셨나요?", // 아무 것도 없을 때, 힌트 메시지
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

        PrimaryPillButton(
            text = "일기 저장하기",
            onClick = {
                // 저장 API 호출
                // OpenAI 감정 분석 결과 받아서
                // emotionEmojiByDate[selectedDate] = "__" 처럼 업데이트
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPickerField(
    title: String,
    totalMinutes: Int,
    onPick: (Int) -> Unit, // 선택 완료 시, 선택한 시간(분)을 전달
    modifier: Modifier = Modifier,
    minMinutes: Int = 0, // 최소 시간 0분
    maxMinutes: Int = 1440, // 최소 시간 24시간
    minuteStep: Int = 10 // 10분 단위
) {
    var open by remember { mutableStateOf(false) }  // BottomSheet가 기본적으로 닫혀 있음 (False)

    Column(modifier = modifier) {
        Text("$title (시간/분)", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true }
        ) {
            OutlinedTextField(
                value = formatDuration(totalMinutes), // 90분 -> 1시간 30분 형태로 변환
                onValueChange = {},
                enabled = false, // 타이핑 방지를 위해 비활성화
                readOnly = true,
                singleLine = true, // 한 줄로만 표시
                trailingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) }, // 시계 모양 아이콘 표시
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors( // 색상 관련
                    disabledContainerColor = AppFieldColor,
                    disabledBorderColor = Color.Transparent,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            )
        }
    }

    if (open) { // BottomSheet가 열렸을 경우(True)
        HourMinutePickerBottomSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourMinutePickerBottomSheet(
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
    val hourValues = remember(maxHour) { (0..maxHour).toList() } // [0,1,2,3,4, . . . , 24]

    val minuteValues = remember(minuteStep) {
        // 10분 단위: 0, 10, 20, 30, 40, 50
        (0..59 step minuteStep).toList()
    }

    // 초기값을 step에 맞춰 정리
    val initClamped = initialTotalMinutes.coerceIn(minMinutes, maxMinutes) // 초기값을 0분~1440분(24시간) 내로 조정
    val initHour = (initClamped / 60).coerceIn(0, maxHour) // 조정한 값을 시간 단위로 변환
    val initMinRaw = initClamped % 60 // 조정한 값의 나머지
    val initMin = (initMinRaw / minuteStep) * minuteStep // 나머지 값을 활용해서 분 단위 값을 계산
    val initHourIndex = hourValues.indexOf(initHour).coerceAtLeast(0) // 계산한 시간 단위 값의 인덱스 반환
    val initMinIndex = minuteValues.indexOf(initMin).coerceAtLeast(0) // 계산한 분 단위의 값의 인덱스를 반환

    var hourIndex by remember { mutableIntStateOf(initHourIndex) } // 현재 선택된 시간의 인덱스
    var minuteIndex by remember { mutableIntStateOf(initMinIndex) } // 현재 선택된 분의 인덱스

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
                // 시간 휠(다이얼?)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("시간", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier.height(160.dp).width(120.dp),
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0
                                maxValue = hourValues.lastIndex // 선택 가능한 인덱스 범위 설정
                                displayedValues = hourValues.map { it.toString() }.toTypedArray() // 화면에 보여줄 텍스트
                                value = hourIndex // 초기 선택값 설정
                                wrapSelectorWheel = false // 끝에서 처음으로 돌아가지 않도록 방지 (24->0으로 가지 않게)
                                setOnValueChangedListener { _, _, newVal -> hourIndex = newVal } // 휠을 스크롤 시 값을 업데이트
                            }
                        },
                        update = { picker -> // 상태가 바뀔 때마다 실행
                            picker.minValue = 0
                            picker.maxValue = hourValues.lastIndex
                            picker.displayedValues = hourValues.map { it.toString() }.toTypedArray()
                            if (picker.value != hourIndex) picker.value = hourIndex // 현재 값과 업데이트 값이 다를 때만, 값을 업데이트
                        }
                    )
                }

                // 분 단위를 컨트롤 하는 휠(다이얼?)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("분", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier.height(160.dp).width(120.dp),
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 0
                                maxValue = minuteValues.lastIndex
                                
                                // 값을 두자리 형태로 맞추기 위함
                                // 0분 -> 00분, 5분 -> 05분, 40분 -> 40분 형태로 맞춤
                                displayedValues = minuteValues.map { it.toString().padStart(2, '0') }.toTypedArray()
                                value = minuteIndex
                                wrapSelectorWheel = false
                                setOnValueChangedListener { _, _, newVal -> minuteIndex = newVal }
                            }
                        },
                        update = { picker ->
                            picker.minValue = 0
                            picker.maxValue = minuteValues.lastIndex
                            picker.displayedValues =
                                minuteValues.map { it.toString().padStart(2, '0') }.toTypedArray()
                            if (picker.value != minuteIndex) picker.value = minuteIndex
                        }
                    )
                }
            }

            
            // 현재 선택된 시 단위의 값과 분 단위의 값을 0시간 0분 형태로 계산
            val previewTotal = hourValues[hourIndex] * 60 + minuteValues[minuteIndex]
            
            // 그 계산된 값이 최솟값과 최댓값 범위를 벗어나지 않게
            // 범위 내의 값으로 조정
            val clampedPreview = previewTotal.coerceIn(minMinutes, maxMinutes)

            Text(
                text = "선택: ${formatDuration(clampedPreview)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss, // 취소 버튼 시, BottomSheet 닫기
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp)
                ) { Text("취소") }

                Button(
                    onClick = { onConfirm(clampedPreview) }, // 완료 시, 선택된 분 값을 전달
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp)
                ) { Text("완료") }
            }
        }
    }
}

// 분 -> 시간 텍스트 변환
private fun formatDuration(totalMinutes: Int): String {
    val m = max(0, totalMinutes) // 음수 방지를 위함
    val h = m / 60 // 시간 (몫)
    val r = m % 60 // 분 (나머지)
    return if (h == 0) "${r}분" else "${h}시간 ${r}분"
}

// 디자인 부분(Header)
@Composable
private fun HeaderCard(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp, // 그림자 효과
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NoteAlt, contentDescription = null) // 노트 모양 아이콘
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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


// 디자인 부분(캘린더)
@Composable
private fun CalendarHeader(
    yearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // '<버튼' '월' '>버튼' 형태로 배치
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Outlined.ChevronLeft, contentDescription = null) } // < 버튼 클릭 시, 이전 달로
        Text(text = "${yearMonth.monthValue}월 ${yearMonth.year}", style = MaterialTheme.typography.titleSmall) // 현재 달 표시
        IconButton(onClick = onNext) { Icon(Icons.Outlined.ChevronRight, contentDescription = null) } // > 버튼 클릭 시, 다음 달로
    }
}

// 디자인 부분(캘린더 그리드)
@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    yesterday: LocalDate,
    emotionEmojiByDate: Map<LocalDate, String>,
    onSelect: (LocalDate) -> Unit
) {
    val first = yearMonth.atDay(1) // 이 달 1일의 LocalDate
    val daysInMonth = yearMonth.lengthOfMonth() // 이 달의 총 일수 - 28일/29일/30일/31일

    val firstDowIndex = dowIndexSundayStart(first.dayOfWeek) // 1일이 무슨 요일? (일요일=0, 월요일=1, 화요일=2, ... , 토요일=6)
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

        for (week in 0 until 6) { // 최대 6주
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) { // 일주일은 7일
                    val cellIndex = week * 7 + col // 6x7 칸의 현재 인덱스
                    val dayNumber = cellIndex - firstDowIndex + 1 // 실제 날짜 숫자를 계산

                    // 유효한 날짜이면 LocalDate, 아니면 null
                    val date = if (dayNumber in 1..daysInMonth) yearMonth.atDay(dayNumber) else null

                    // 어제, 오늘만 날짜를 선택 가능하도록 함
                    val enabled = date == today || date == yesterday

                    // 해당 날짜에 감정 이모티콘이 있으면 가져오고, 없으면 null
                    val emoji = if (date != null) emotionEmojiByDate[date] else null

                    CalendarCell(
                        date = date,
                        isSelected = date == selectedDate,
                        enabled = enabled,
                        emoji = emoji,
                        onClick = { if (date != null && enabled) onSelect(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// 디자인 부분(날짜 셀 하나)
@Composable
private fun CalendarCell(
    date: LocalDate?,
    isSelected: Boolean,
    enabled: Boolean,
    emoji: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 선택된 날짜는 배경을 파란 색, 아니면 투명 배경
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    val dateColor =
        when {
            date == null -> Color.Transparent // 빈 칸이면 숫자가 안 보임
            isSelected -> MaterialTheme.colorScheme.onPrimary // 선택되면 흰색 숫자
            enabled -> MaterialTheme.colorScheme.onSurface // 선택 가능하면 진한 숫자
            else -> MaterialTheme.colorScheme.outline // 선택 불가능하면 회색 숫자
        }

    val emojiColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(1f) // 비율을 1:1 형태의 정사각형으로 유지
            .padding(4.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(enabled = date != null && enabled) { onClick() }, // date가 있고, 어제/오늘 에만 선택이 가능함
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                // date가 null -> ""
                // date가 있으면, dayOfMonth -> 숫자 -> 문자열
                // 예: 2026.03.01 -> 1 -> "1"
                text = date?.dayOfMonth?.toString() ?: "",
                color = dateColor,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = emoji ?: "",
                color = emojiColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


// 디자인 부분(날씨 선택)
@Composable
private fun WeatherRow(
    selected: WeatherType,
    onSelect: (WeatherType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        WeatherType.entries.forEach { item -> // 날씨 항목 4가지를 모두 돌아가며
            val isActive = item == selected // 현재 선택된 날씨 인지의 여부
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else AppFieldColor, // 선택되면 연한 파란색, 미선택이면 연한 회색
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable { onSelect(item) } // 클릭 시, 해당 날씨를 선택
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item.icon() // 날씨 아이콘
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant // 선택되면 파란 글씨, 미선택시 회색 글씨
                    )
                }
            }
        }
    }
}


// 디자인 부분(일기 작성 가이드 및 가장 좋았던/아쉬웠던 일 부분)
@Composable
private fun HelperCard(
    bestThing: String,
    onBestChange: (String) -> Unit,
    regretThing: String,
    onRegretChange: (String) -> Unit
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
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(gradient, RoundedCornerShape(18.dp))
                .padding(16.dp),
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


// 요일 숫자 변환
private fun dowIndexSundayStart(dow: DayOfWeek): Int {
    return when (dow) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}

// 날짜를 한국어 포맷으로 변환
/// THURSDAY -> 목요일 형태로 변환
private fun formatKoreanDate(date: LocalDate): String {
    val dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREAN)
    return "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 ($dayName)"
}