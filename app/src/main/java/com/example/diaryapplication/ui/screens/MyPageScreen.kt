package com.example.diaryapplication.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import com.example.diaryapplication.viewmodel.MyPageViewModel
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.notification.NotificationHelper
import com.example.diaryapplication.viewmodel.AuthViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
@Composable
fun MyPageScreen(
    padding: PaddingValues,
    onLogout: () -> Unit = {}, // 로그아웃 시 Login으로 보내는 nav는 여기서 처리
    myPageViewModel : MyPageViewModel = viewModel(),
    authViewModel : AuthViewModel = viewModel()
) {

    val context = LocalContext.current

    // 사용자 프로필 정보
    val userProfile by myPageViewModel.userProfile.collectAsState()
    val notificationEnabled by myPageViewModel.notificationEnabled.collectAsState()
    val notifyTime by myPageViewModel.notifyTime.collectAsState()
    val pinEnabled by myPageViewModel.pinEnabled.collectAsState()
    val totalDiary by myPageViewModel.totalDiaryCount.collectAsState()
    val thisMonthDiary by myPageViewModel.thisMonthDiaryCount.collectAsState()

    val userName = userProfile.name
    val nickname = userProfile.nickname
    val email = userProfile.email
    val birthDate = if (userProfile.birthDate.isNotEmpty()) {
        LocalDate.parse(userProfile.birthDate)
    } else {
        LocalDate.now()
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    val cardGap = 14.dp
    val sectionGap = 18.dp

    Box(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(cardGap)
        ) {
            item { MyHeader() }
            item {
                ProfileHeaderCard(
                    name = nickname,
                    email = email
                )
            }
            item {
                SectionCard(
                    title = "계정 관리"
                ) {
                    SettingRow(
                        icon = Icons.Outlined.PersonOutline,
                        title = "프로필 정보 변경",
                        subtitle = null,
                        showChevron = true,
                        onClick = { showProfileDialog = true }
                    )
                }
            }
            item {
                SectionCard(
                    title = "앱 설정"
                ) {
                    ToggleRow(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "알림 설정",
                        subtitle = "매일 일기 작성 알림",
                        checked = notificationEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showTimeDialog = true
                            } else {
                                myPageViewModel.disableNotification()
                                NotificationHelper.cancelAlarm(context)

                            }
                        }
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                    ToggleRow(
                        icon = Icons.Outlined.Lock,
                        title = "PIN 번호 설정",
                        subtitle = "일기 보호 PIN 번호",
                        checked = pinEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                myPageViewModel.deletePin()
                            }
                        }
                    )
                }
            }
            item {
                SectionCard(title = "나의 활동") {
                    ActivityTwoCards(
                        leftTitle = "총 일기",
                        leftValue = totalDiary.toString(),
                        rightTitle = "이번 달",
                        rightValue = thisMonthDiary.toString()
                    )
                }
            }
            item { Spacer(Modifier.height(sectionGap)) }
            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFFF3B30)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF3B30))
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("로그아웃", fontWeight = FontWeight.SemiBold)
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
    // 프로필 정보 변경 다이얼로그
    if (showProfileDialog) {
        ProfileEditDialog(
            name = userName, // 현재 이름
            nickname = nickname, // 현재 닉네임
            email = email, // 현재 이메일
            birthDate = birthDate, // 현재 생년월일
            onDismiss = { showProfileDialog = false }, // 닫기
            onSave = { newName, newNick, newBirth ->
                myPageViewModel.saveProfile(newName, newNick, newBirth) {
                    authViewModel.currentNickname.value = newNick // 홈/챗봇에 반영될 수 있게 하기 위함
                    showProfileDialog = false
                }
            } // 저장하기
        )
    }

    // 알림 설정 다이얼로그
    if (showTimeDialog) {
        NotificationTimeDialog(
            current = notifyTime, // 현재 설정된 시간
            onDismiss = {
                // 시간이 선택되지 않고 닫으면 알림 설정 OFF
                showTimeDialog = false

            },
            onSave = { time ->
                myPageViewModel.saveNotificationSetting(true, time)
                showTimeDialog = false // 창 닫기
            } // 새로운 시간 설정
        )
    }

    LaunchedEffect(notificationEnabled, notifyTime) {
        if (notificationEnabled) {
            NotificationHelper.scheduleDailyAlarm(
                context = context,
                hour = notifyTime.hour,
                minute = notifyTime.minute
            )
        } else {
            NotificationHelper.cancelAlarm(context)
        }
    }

    // PIN 설정 다이얼로그
    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = {
                showPinDialog = false
            },
            onSave = { pin ->
                myPageViewModel.savePin(pin)
                showPinDialog = false
            }
        )
    }
}
@Composable
private fun MyHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Outlined.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "마이페이지",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
@Composable
private fun ProfileHeaderCard(name: String, email: String) {
    val shape = RoundedCornerShape(18.dp)
    val start = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val end = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Surface(
        shape = shape,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(start, end)))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        email,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}
@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    showChevron: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showChevron) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}
@Composable
private fun ActivityTwoCards(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MiniStatCard(title = leftTitle, value = leftValue, modifier = Modifier.weight(1f))
        MiniStatCard(title = rightTitle, value = rightValue, modifier = Modifier.weight(1f))
    }
}
@Composable
private fun MiniStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.height(88.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
// 다이얼로그 기본 틀 생성
@Composable
private fun AppDialogContainer(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) { // 다이얼로그 외부를 터치하면 닫힘
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    }
                }
                content()
            }
        }
    }
}

// 기본 다이얼로그을 기반으로 프로필 변경 다이얼로그 생성
@Composable
private fun ProfileEditDialog(
    name: String,
    nickname: String,
    email: String,
    birthDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate) -> Unit
) {
    var nameState by remember { mutableStateOf(name) }
    var nickState by remember { mutableStateOf(nickname) }
    var birthState by remember { mutableStateOf(birthDate) }
    AppDialogContainer(
        title = "프로필 정보 변경",
        subtitle = "프로필 정보를 수정할 수 있습니다",
        onDismiss = onDismiss
    ) {
        Text("이름", fontWeight = FontWeight.SemiBold)
        SoftField(value = nameState, onValueChange = { nameState = it })
        Text("닉네임", fontWeight = FontWeight.SemiBold)
        SoftField(value = nickState, onValueChange = { nickState = it })
        Text("이메일", fontWeight = FontWeight.SemiBold)
        SoftField(value = email, onValueChange = {}, enabled = false)
        Text("생년월일", fontWeight = FontWeight.SemiBold)
        BirthDateField(
            value = birthState,
            onChange = { birthState = it }
        )
        Spacer(Modifier.height(6.dp))
        PrimaryActionButton(text = "저장하기") {
            onSave(nameState.trim(), nickState.trim(), birthState)
        }
    }
}

// 알림 시간 설정
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimeDialog(
    current: LocalTime,
    onDismiss: () -> Unit,
    onSave: (LocalTime) -> Unit
) {
    var time by remember { mutableStateOf(current) }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "알림 시간 설정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "일기 작성 알림을 받을 시간을 설정하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            Text("알림 시간", fontWeight = FontWeight.SemiBold)
            TimeField(
                time = time,
                onPick = {
                    showAndroidTimePicker(
                        context = context,
                        initial = time,
                        onSelected = { time = it }
                    )
                }
            )
            Spacer(Modifier.height(4.dp))
            PrimaryActionButton(text = "저장하기") {
                onSave(time)
            }
        }
    }
}

// PIN 다이얼로그
@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun validate(): Boolean {
        val p = pin.trim()
        val c = confirm.trim()
        if (p.length !in 4..6) { error = "PIN은 4~6자리여야 합니다."; return false } // PIN 번호가 4~6자리 아니면 에러
        if (!p.all { it.isDigit() }) { error = "PIN은 숫자만 입력 가능합니다."; return false } // 모든 글자가 문자이면 에러
        if (p != c) { error = "PIN 번호가 일치하지 않습니다."; return false } // 설정할 PIN 번호와 확인용 PIN 번호 둘이 다르면 에러
        error = null
        return true
    }
    AppDialogContainer(
        title = "PIN 번호 설정",
        subtitle = "일기 보호를 위한 PIN 번호를 설정하세요",
        onDismiss = onDismiss
    ) {
        Text("PIN 번호 (4~6자리)", fontWeight = FontWeight.SemiBold)
        SoftField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6) },
            placeholder = "PIN 번호 입력",
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
        Text("PIN 번호 확인", fontWeight = FontWeight.SemiBold)
        SoftField(
            value = confirm,
            onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
            placeholder = "PIN 번호 재입력",
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true
        )
        if (error != null) {
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(6.dp))
        PrimaryActionButton(text = "PIN 설정하기") {
            if (validate()) onSave(pin.trim())
        }
    }
}

// 입력 필드
@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false // true면 입력값을 ●●● 으로 가림
) {
    val shape = RoundedCornerShape(14.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        shape = shape,
        placeholder = { if (placeholder != null) Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            // 포커스 상태(클릭해서 커서가 있을때), 언포커스(그냥 있을 때), 비활성(enabled=false일때)
            // .copy(alpha=___)를 이용해서 색상을 그대로 쓰되 투명도를 조정 (alpha: 투명도)
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(
    value: LocalDate,
    onChange: (LocalDate) -> Unit
) {
    val formatted = remember(value) { value.toString() }
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(formatted)
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?. let {millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.of("UTC"))
                            .toLocalDate()
                        onChange(picked)
                    }
                    showPicker = false
                }) { Text ("확인")}
            },
            dismissButton =  {
                TextButton(onClick = { showPicker = false}) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
@Composable
private fun TimeField(
    time: LocalTime,
    onPick: () -> Unit
) {
    val label = remember(time) {
        val formatter = DateTimeFormatter.ofPattern("a hh:mm", Locale.KOREA)
        time.format(formatter)
            .replace("AM", "오전")
            .replace("PM", "오후")
    }
    Surface(
        onClick = onPick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
private fun showAndroidTimePicker(
    context: Context,
    initial: LocalTime,
    onSelected: (LocalTime) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) }, // 선택한 시간을 Local Time으로 변환
        initial.hour,
        initial.minute,
        false
    ).show()
}