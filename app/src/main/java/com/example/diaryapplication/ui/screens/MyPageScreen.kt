package com.example.diaryapplication.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.diaryapplication.R
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
    onLogout: () -> Unit = {},
    myPageViewModel: MyPageViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 14.dp,
                bottom = padding.calculateBottomPadding() + 20.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MyHeader() }
            item {
                ProfileHeaderCard(
                    name = nickname,
                    email = email,
                    onEdit = { showProfileDialog = true }
                )
            }
            item {
                StatsRow(totalDiary = totalDiary, thisMonthDiary = thisMonthDiary)
            }
            item {
                GlassSectionCard(title = "앱 설정") {
                    ToggleRow(
                        icon = painterResource(R.drawable.ic_bell),
                        iconTint = Color(0xFFFF9800),
                        iconBg = Color(0xFFFFF3E0),
                        title = "알림 설정",
                        subtitle = if (notificationEnabled) {
                            notifyTime.format(DateTimeFormatter.ofPattern("a hh:mm", Locale.KOREA))
                                .replace("AM", "오전").replace("PM", "오후")
                        } else {
                            "매일 일기 작성 알림"
                        },
                        checked = notificationEnabled,
                        onCheckedChange = { checked ->
                            if (checked) showTimeDialog = true
                            else {
                                myPageViewModel.disableNotification()
                                NotificationHelper.cancelAlarm(context)
                            }
                        }
                    )
                    GlassDivider()
                    ToggleRow(
                        icon = painterResource(R.drawable.ic_lock_keyhole),
                        iconTint = Color(0xFF9C27B0),
                        iconBg = Color(0xFFF3E5F5),
                        title = "PIN 번호 설정",
                        subtitle = "일기 보호 PIN 번호",
                        checked = pinEnabled,
                        onCheckedChange = { checked ->
                            if (checked) showPinDialog = true
                            else myPageViewModel.deletePin()
                        }
                    )
                }
            }
            item {
                GlassSectionCard(title = "계정") {
                    SettingRow(
                        icon = painterResource(R.drawable.ic_user_round),
                        iconTint = Color(0xFF3D7BF4),
                        iconBg = Color(0xFFEEF3FF),
                        title = "프로필 정보 변경",
                        subtitle = null,
                        showChevron = true,
                        onClick = { showProfileDialog = true }
                    )
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
            item {
                Surface(
                    onClick = { authViewModel.logout { onLogout() } },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFF3B30).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_log_out),
                            contentDescription = null,
                            tint = Color(0xFFFF3B30)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "로그아웃",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            name = userName,
            nickname = nickname,
            email = email,
            birthDate = birthDate,
            onDismiss = { showProfileDialog = false },
            onSave = { newName, newNick, newBirth ->
                myPageViewModel.saveProfile(newName, newNick, newBirth) {
                    authViewModel.currentNickname.value = newNick
                    showProfileDialog = false
                }
            }
        )
    }

    if (showTimeDialog) {
        NotificationTimeDialog(
            current = notifyTime,
            onDismiss = { showTimeDialog = false },
            onSave = { time ->
                myPageViewModel.saveNotificationSetting(true, time)
                showTimeDialog = false
            }
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

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
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
            painterResource(R.drawable.ic_user_round),
            contentDescription = null,
            tint = Color(0xFF3D7BF4)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "마이페이지",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1a1a2e)
        )
    }
}

@Composable
private fun ProfileHeaderCard(name: String, email: String, onEdit: () -> Unit) {
    val gradientBrush = Brush.linearGradient(
        listOf(Color(0xFF3D7BF4), Color(0xFF8B5CF6))
    )
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(gradientBrush)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name.ifEmpty { "닉네임" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Surface(
                onClick = onEdit,
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text(
                    "편집 ›",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsRow(totalDiary: Int, thisMonthDiary: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassStatCard(
            icon = "📓",
            value = totalDiary.toString(),
            label = "총 일기",
            modifier = Modifier.weight(1f)
        )
        GlassStatCard(
            icon = "📅",
            value = thisMonthDiary.toString(),
            label = "이번 달",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlassStatCard(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1a1a2e)
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9ca3af))
        }
    }
}

@Composable
private fun GlassSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White.copy(alpha = 0.60f))
                .border(1.dp, Color.White.copy(alpha = 0.9f), shape)
                .padding(14.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun GlassDivider() {
    Divider(
        modifier = Modifier.padding(vertical = 6.dp),
        color = Color.White.copy(alpha = 0.7f),
        thickness = 1.dp
    )
}

@Composable
private fun SettingRow(
    icon: Painter,
    iconTint: Color,
    iconBg: Color,
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
            IconBox(icon, iconTint, iconBg)
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
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: Painter,
    iconTint: Color,
    iconBg: Color,
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
        IconBox(icon, iconTint, iconBg)
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
private fun IconBox(icon: Painter, iconTint: Color, iconBg: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(iconBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AppDialogContainer(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                        Icon(Icons.Rounded.Close, contentDescription = null)
                    }
                }
                content()
            }
        }
    }
}

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
        BirthDateField(value = birthState, onChange = { birthState = it })
        Spacer(Modifier.height(6.dp))
        PrimaryActionButton(text = "저장하기") {
            onSave(nameState.trim(), nickState.trim(), birthState)
        }
    }
}

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
                    Icon(Icons.Rounded.Close, contentDescription = null)
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
            PrimaryActionButton(text = "저장하기") { onSave(time) }
        }
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun validate(): Boolean {
        val p = pin.trim(); val c = confirm.trim()
        if (p.length !in 4..6) { error = "PIN은 4~6자리여야 합니다."; return false }
        if (!p.all { it.isDigit() }) { error = "PIN은 숫자만 입력 가능합니다."; return false }
        if (p != c) { error = "PIN 번호가 일치하지 않습니다."; return false }
        error = null; return true
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
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(6.dp))
        PrimaryActionButton(text = "PIN 설정하기") { if (validate()) onSave(pin.trim()) }
    }
}

@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = { if (placeholder != null) Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
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
private fun BirthDateField(value: LocalDate, onChange: (LocalDate) -> Unit) {
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
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                        onChange(picked)
                    }
                    showPicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun TimeField(time: LocalTime, onPick: () -> Unit) {
    val label = remember(time) {
        time.format(DateTimeFormatter.ofPattern("a hh:mm", Locale.KOREA))
            .replace("AM", "오전").replace("PM", "오후")
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrimaryActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
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
        { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        false
    ).show()
}
