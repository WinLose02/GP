package com.example.diaryapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.delay
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.ui.res.painterResource
import com.example.diaryapplication.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.ChatViewModel
import com.example.diaryapplication.viewmodel.Sender
import java.time.LocalDate

@Composable
fun ChatScreen(
    padding: PaddingValues,           // MainScaffold의 innerPadding (상태바, 하단 탭바 높이 포함)
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()   // 채팅 메시지 목록
    val input    by chatViewModel.inputText.collectAsState()  // 입력창 텍스트
    val isSending by chatViewModel.isSending.collectAsState() // 전송 중 여부 (버튼 비활성화용)
    val fortune  by chatViewModel.fortuneMessage.collectAsState() // 포춘쿠키 텍스트
    val summary  by chatViewModel.summaryMessage.collectAsState() // 요약 메시지 (예비)

    // 상담 종료 여부: true가 되면 입력창·전송버튼 비활성화, 포춘쿠키 카드 표시
    var isCounselingEnd by remember { mutableStateOf(false) }

    // LazyColumn 스크롤 위치를 제어하기 위한 상태
    val listState = rememberLazyListState()

    // 봇 답변 타이핑이 한 글자 진행될 때마다 증가하는 카운터.
    var typingTick by remember { mutableIntStateOf(0) }

    // 사용자가 대화의 맨 밑에 있는지 여부를 확인
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf true // 아직 아이템이 없으면 바닥으로 간주
            // 마지막으로 보이는 아이템이 실제 마지막 아이템이고,
            // 그 아이템 하단이 뷰포트 하단에서 200px 이내면 바닥 근처로 보도록 함
            lastVisible.index >= layoutInfo.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size <=
                    layoutInfo.viewportEndOffset + 200
        }
    }

    // 타이핑 애니메이션 중복 방지
    val typedMessageIds = remember { mutableSetOf<Long>() }

    // 말풍선 등장 애니메이션 중복 방지
    val enteredMessageIds = remember { mutableSetOf<Long>() }

    // 키보드 모드 오버라이드 (ChatScreen 전용)
    //   MainActivity에서 WindowCompat.setDecorFitsSystemWindows(window, false)로
    //   edge-to-edge 모드를 활성화했고, AndroidManifest에는 adjustResize가 설정되어 있음
    //   → 두 조정이 중첩되어 입력창이 키보드 높이의 약 2배 위치로 올라가는 버그 발생

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val originalMode = window.attributes.softInputMode
        // SOFT_INPUT_ADJUST_NOTHING: 키보드가 올라와도 앱 창을 리사이즈하지 않음
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            // ChatScreen을 떠날 때 원래 모드로 복원 (다른 화면의 키보드 동작 보장)
            window.setSoftInputMode(originalMode)
        }
    }

    // 대화 내용 자동 스크롤
    LaunchedEffect(messages.size, isSending, typingTick) {
        if (messages.isNotEmpty() && isAtBottom) {
            // 마지막 아이템 인덱스 = 전체 아이템 수 - 1 (DateChip, 메시지들, 조건부 포춘카드 모두 포함)
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) listState.scrollToItem(lastIndex)
        }
    }

    // 키보드 올라오면 대화 내역도 같이 위로 올라가게 하기 위함
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottom) {
        if (messages.isNotEmpty() && isAtBottom) {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) listState.scrollToItem(lastIndex)
        }
    }

    // 포춘쿠키 등장 시 해당 아이템으로 부드럽게 스크롤
    // isCounselingEnd 또는 fortune 값이 바뀔 때마다 실행되며,
    // 두 조건이 모두 충족된 경우에만 스크롤 수행
    LaunchedEffect(isCounselingEnd, fortune) {
        if (isCounselingEnd && fortune.isNotEmpty()) {
            // 포춘쿠키 아이템이 컴포지션에 추가될 시간을 잠시 기다린 후 스크롤
            delay(150)
            // DateChip(0) + messages(1..n) 다음 인덱스 = messages.size + 1
            listState.animateScrollToItem(messages.size + 1)
        }
    }

    // 챗봇 화면 진입 시 대화 초기화
    val today = remember { LocalDate.now() }
    val dateLabel = remember(today) { "오늘 · ${today.monthValue}월 ${today.dayOfMonth}일" }

    LaunchedEffect(Unit) {
        chatViewModel.resetChat()
    }

    // 레이아웃
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEEF0FF), // 상단: 연한 라벤더 블루
                        Color(0xFFE8EEFF), // 중간: 약간 더 짙은 블루
                        Color(0xFFEEF3FF)  // 하단: 다시 연해지는 톤
                    )
                )
            )
    ) {
        // 실제 UI 컨텐츠 배치
        // consumeWindowInsets(padding): 하위 컴포저블이 동일 인셋을 중복 적용하지 않도록 방지
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 상단 헤더 (아이콘 + 제목)
            ChatHeaderCard(title = "감정 챗봇", subtitle = "당신의 이야기를 들려주세요")

            Spacer(Modifier.height(8.dp))

            // 채팅 메시지 목록
            // weight(1f): 입력창이 항상 하단에 고정되도록 남은 공간을 전부 차지
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // 말풍선 간 간격
            ) {
                // 날짜 구분선 (index 0)
                item {
                    DateChip(label = dateLabel)
                }

                // 메시지 목록 (index: 1 ~ messages.size)
                // key = { it.id }: 메시지 ID를 key로 사용해 LazyColumn이 아이템을 효율적으로 재사용
                items(messages, key = { it.id }) { msg ->
                    // 아이템 등장 애니메이션용 상태
                    // 이미 등장했던 메시지면 visible을 true로 반복 재생 X
                    // 처음 보는 메시지면 false로 시작 → LaunchedEffect에서 true로 바꿔 등장 애니메이션 재생.
                    var visible by remember { mutableStateOf(enteredMessageIds.contains(msg.id)) }
                    LaunchedEffect(Unit) {
                        visible = true
                        enteredMessageIds.add(msg.id) // 한 번 등장하면 등록
                    }

                    // 로딩 말풍선
                    if (msg.isLoading) {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) { -it / 5 } // 왼쪽에서 1/5 만큼 슬라이드 인
                        ) {
                            LoadingBubble()
                        }
                        return@items // 로딩 말풍선이면 아래 when 블록 실행 안 함
                    }

                    // 발신자에 따라 봇/유저 말풍선 구분
                    when (msg.sender) {
                        Sender.BOT -> AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(350)) + slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) { -it / 5 } // 왼쪽에서 슬라이드 인
                        ) {
                            BotBubble(
                                text = msg.text,
                                time = msg.time,

                                // 타이핑 애니메이션 중복 방지
                                isFirstDisplay = !typedMessageIds.contains(msg.id),
                                onTypingDone = { typedMessageIds.add(msg.id) },

                                // 글자가 하나 타이핑될 때마다 호출 → 자동 스크롤이 자라나는 말풍선을 따라가게 하기 위함
                                onCharTyped = { typingTick++ }
                            )
                        }
                        Sender.USER -> AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(350)) + slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) { it / 5 } // 오른쪽에서 슬라이드 인
                        ) {
                            UserBubble(text = msg.text, time = msg.time)
                        }
                    }
                }

                // 포춘쿠키 카드
                // 상담 마치기 버튼을 눌러 isCounselingEnd가 true가 되고,
                // 서버에서 fortune 텍스트가 도착한 후에만 표시
                if (isCounselingEnd && fortune.isNotEmpty()) {
                    item {
                        FortuneMessage(
                            text = fortune,
                            // 포춘 텍스트가 타이핑으로 길어질 때마다 자동 스크롤
                            onCharTyped = { typingTick++ }
                        )
                    }
                }
            }

            // 상담 마치기 버튼
            // isCounselingEnd가 false일 때만 표시 (상담 종료 후 버튼 숨김)
            if (!isCounselingEnd) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 반투명 유리 스타일 pill 버튼
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                        modifier = Modifier.clickable { isCounselingEnd = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "상담 마치기",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // 메시지 입력창
            // isCounselingEnd가 true면 value = "", enabled = false → 입력 완전 비활성화
            ChatInputBar(
                value = if (isCounselingEnd) "" else input,
                onValueChange = { if (!isCounselingEnd) chatViewModel.onInputChange(it) },
                onSend      = { if (!isCounselingEnd) chatViewModel.sendMessage() },
                enabled     = !isSending && !isCounselingEnd
            )
        }
    }
}

// ChatHeaderCard
// 화면 상단 헤더 영역
@Composable
private fun ChatHeaderCard(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 그라디언트 아이콘 박스
        // shadow → clip → background 순서: 그림자가 clip 전에 적용되어 둥근 그림자 생성
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1D4ED8), Color(0xFF3D7BF4)) // 네이비 → 블루
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sparkles),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        // 텍스트 영역: weight(1f)로 아이콘 제외 나머지 공간 전부 차지
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Date 날짜 구분선. LazyColumn 최상단에 배치.
@Composable
private fun DateChip(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
    }
}

// BotBubble : 봇 말풍선
@Composable
private fun BotBubble(
    text: String,
    time: String,
    isFirstDisplay: Boolean = true,
    onTypingDone: () -> Unit = {},
    onCharTyped: () -> Unit = {}
) {
    // isFirstDisplay가 false면 재진입이므로 타이핑 X
    var displayedText by remember(text) {
        mutableStateOf(if (isFirstDisplay) "" else text)
    }

    // 새 메시지가 도착했을 때 실행
    LaunchedEffect(text) {
        if (isFirstDisplay) {
            // 최초 표시: 35ms마다 글자 하나씩 추가 (타이핑 효과)
            displayedText = ""
            text.forEachIndexed { index, _ ->
                delay(35) // 타이핑 속도
                displayedText = text.substring(0, index + 1)
                onCharTyped() // 말풍선 높이가 늘어날 때마다 자동 스크롤
            }
            // 타이핑 완료 → ChatScreen에 알려 typedMessageIds에 이 메시지 ID 등록
            onTypingDone()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        BotMiniAvatar()
        Spacer(Modifier.width(7.dp))
        // 흰 카드 말풍선
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp, topEnd = 18.dp,
                bottomEnd = 18.dp, bottomStart = 18.dp
            ),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(max = 270.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(displayedText, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(5.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// UserBubble
@Composable
private fun UserBubble(text: String, time: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // shadow → clip → background 순서: 그라디언트에 둥근 그림자 적용
        Box(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .shadow(
                    6.dp,
                    RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                )
                .clip(
                    RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1D4ED8), Color(0xFF3D7BF4)) // 네이비 → 블루
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(Modifier.height(5.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// 메시지 입력창 + 전송 버튼 행.
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("메시지를 입력하세요...") },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                disabledContainerColor  = Color.White.copy(alpha = 0.5f),
                // 테두리 색
                focusedIndicatorColor   = Color.White.copy(alpha = 0.95f),
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.95f)
            )
        )
        Spacer(Modifier.width(8.dp))
        // 전송 버튼: 원형, 그라디언트 배경
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1D4ED8), Color(0xFF3D7BF4))
                    )
                )
                .clickable(enabled = enabled) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Send,
                contentDescription = "send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// 봇 응답 대기 중에 표시되는 말풍선.
@Composable
private fun LoadingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    // 각 점의 애니메이션 시작 딜레이 (ms): 0 → 160 → 320
    val delays = listOf(0, 160, 320)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        BotMiniAvatar()
        Spacer(Modifier.width(7.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp, topEnd = 18.dp,
                bottomEnd = 18.dp, bottomStart = 18.dp
            ),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                delays.forEach { delayMs ->
                    // 각 점의 Y축 오프셋 (0f → -5dp → 0f 반복)
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 450,
                                delayMillis = delayMs,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse // 올라갔다 내려오는 반복
                        ),
                        label = "dot_$delayMs"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .offset(y = offsetY.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9AA3AE)) // 회색 점
                    )
                }
            }
        }
    }
}

// FortuneMessage
// 상담 마치기 후 표시되는 포춘쿠키 카드.
@Composable
private fun FortuneMessage(text: String, onCharTyped: () -> Unit = {}) {
    // 카드가 처음 등장한 시각을 기억 -> 이후 리컴포지션이 일어나도 시각이 바뀌지 않음
    val time = remember {
        val t = java.time.LocalTime.now()
        String.format("%02d:%02d", t.hour, t.minute)
    }

    // 애니메이션 제어 상태
    var animStarted   by remember { mutableStateOf(false) } // 블러인 시작 여부
    var displayedText by remember { mutableStateOf("") }    // 현재까지 타이핑된 텍스트
    var showCursor    by remember { mutableStateOf(false) } // 커서 표시 여부
    var showTime      by remember { mutableStateOf(false) } // 시각 표시 여부

    // 블러인
    val alpha by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "fortune_alpha"
    )
    val blurRadius by animateFloatAsState(
        targetValue = if (animStarted) 0f else 12f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "fortune_blur"
    )

    // 커서 깜빡임: alpha 1 → 0 → 1 무한 반복 (350ms 주기)
    val cursorAlpha by rememberInfiniteTransition(label = "fortune_cursor").animateFloat(
        initialValue = 1f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(350),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    // 등장 애니메이션 시퀀스
    LaunchedEffect(Unit) {
        animStarted = true                                  // 1) 블러인 시작
        delay(700)                                // 2) 블러인 완료 대기
        showCursor = true                                  // 3) 커서 표시 시작
        text.forEachIndexed { index, _ ->
            delay(38)                            // 4) 38ms마다 글자 하나씩 타이핑
            displayedText = text.substring(0, index + 1)
            onCharTyped()                                   // 카드 높이가 자랄 때마다 자동 스크롤
        }
        delay(800)                                // 5) 타이핑 완료 후 커서 800ms 깜빡임
        showCursor = false                                 // 6) 커서 숨김
        showTime   = true                                  // 7) 시각 표시
    }

    val shape         = RoundedCornerShape(18.dp)
    val gradientBrush = Brush.linearGradient(
        listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
    )

    // 외부 Box: 블러 + 알파 적용
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .then(
                if (blurRadius > 0.5f) Modifier.blur(blurRadius.dp) else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(gradientBrush)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("🍪", fontSize = 26.sp)
                    Column {
                        Text(
                            "오늘의 포춘쿠키",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "당신을 위한 오늘의 한 마디",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                // 헤더와 본문 구분선
                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                // 포춘 텍스트 + 커서
                // buildAnnotatedString으로 일반 텍스트 + 알파가 변하는 커서 문자를 하나의 Text에 표시
                Text(
                    text = buildAnnotatedString {
                        append(displayedText) // 현재까지 타이핑된 텍스트
                        if (showCursor) {
                            // 커서: 알파값이 cursorAlpha(0~1)에 따라 깜빡이는 | 문자
                            withStyle(SpanStyle(color = Color.White.copy(alpha = cursorAlpha))) {
                                append("|")
                            }
                        }
                    },
                    fontSize   = 16.sp,
                    color      = Color.White.copy(alpha = 0.9f),
                    lineHeight = 25.sp
                )

                // 시각: 타이핑 완료 후 우측 하단에 페이드 인
                if (showTime) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            time,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f) // 40% 흰색으로 은은하게
                        )
                    }
                }
            }
        }
    }
}

// BotMiniAvatar
// 봇 말풍선 왼쪽에 표시되는 작은 원형 아바타.
@Composable
private fun BotMiniAvatar() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1D4ED8), Color(0xFF3D7BF4))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkles),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}
