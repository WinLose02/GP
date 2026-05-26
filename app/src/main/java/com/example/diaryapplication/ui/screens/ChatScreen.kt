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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.ChatViewModel
import com.example.diaryapplication.viewmodel.Sender
import java.time.LocalDate

// ═══════════════════════════════════════════════════════════════
// ChatScreen
// 감정 챗봇 대화 화면. 사용자 ↔ 봇 말풍선, 포춘쿠키 카드로 구성.
//
// [디자인 테마]
//   - 배경: 연한 블루 계열 수직 그라디언트 (#EEF0FF → #E8EEFF → #EEF3FF)
//   - 봇 말풍선: 흰 카드 + 그림자 (왼쪽 정렬)
//   - 유저 말풍선: 네이비→블루 그라디언트 (#1D4ED8 → #3D7BF4, 오른쪽 정렬)
//   - 아이콘/전송버튼: 동일한 네이비→블루 그라디언트
//   - 포춘쿠키 카드: 다크 인디고 (#1E1B4B → #312E81)
//
// [주요 기능]
//   1. 탭 재진입 시 대화 초기화 (resetChat)
//   2. 새 메시지/응답 도착 시 자동 스크롤
//   3. 봇 답변 타이핑 애니메이션 (재진입 시 반복 방지)
//   4. 상담 마치기 → 포춘쿠키 카드 등장 (블러인 + 타이핑)
//   5. 키보드 표시 시 입력창이 키보드 바로 위에 위치 (adjustNothing + imePadding)
// ═══════════════════════════════════════════════════════════════
@Composable
fun ChatScreen(
    padding: PaddingValues,           // MainScaffold의 innerPadding (상태바·하단 탭바 높이 포함)
    chatViewModel: ChatViewModel = viewModel()
) {
    // ─── 상태 수집 ───────────────────────────────────────────
    val messages by chatViewModel.messages.collectAsState()   // 채팅 메시지 목록
    val input    by chatViewModel.inputText.collectAsState()  // 입력창 텍스트
    val isSending by chatViewModel.isSending.collectAsState() // 전송 중 여부 (버튼 비활성화용)
    val fortune  by chatViewModel.fortuneMessage.collectAsState() // 포춘쿠키 텍스트
    val summary  by chatViewModel.summaryMessage.collectAsState() // 요약 메시지 (예비)

    // 상담 종료 여부: true가 되면 입력창·전송버튼 비활성화, 포춘쿠키 카드 표시
    var isCounselingEnd by remember { mutableStateOf(false) }

    // LazyColumn 스크롤 위치를 제어하기 위한 상태
    val listState = rememberLazyListState()

    // ─────────────────────────────────────────────────────────
    // [1] 타이핑 애니메이션 중복 방지
    // ─────────────────────────────────────────────────────────
    // 문제:
    //   LazyColumn은 메모리 절약을 위해 화면 밖으로 나간 아이템을 컴포지션에서
    //   제거하고, 다시 들어올 때 새로 생성한다.
    //   재생성 시 BotBubble 내부의 remember 상태가 초기화되고
    //   LaunchedEffect(text)가 재실행 → 이미 본 메시지에 타이핑 애니메이션이 반복됨.
    //   (키보드 올라갔다 내려올 때 가장 흔하게 발생)
    //
    // 해결:
    //   ChatScreen 레벨(LazyColumn 바깥)에서 "타이핑 완료된 메시지 ID" 집합을 유지.
    //   BotBubble에 isFirstDisplay 플래그를 전달:
    //     - true  → 타이핑 애니메이션 실행 후 ID 집합에 등록
    //     - false → 전체 텍스트를 즉시 표시 (재진입이므로 애니메이션 생략)
    //
    // 생명주기:
    //   - 탭 전환으로 ChatScreen이 컴포지션에서 제거되면 typedMessageIds도 함께 소멸
    //   - 재진입 시 resetChat()으로 메시지 목록도 초기화되므로 ID 집합이 빈 상태에서 재시작
    val typedMessageIds = remember { mutableSetOf<Long>() }

    // ─────────────────────────────────────────────────────────
    // [2] 키보드 모드 오버라이드 (ChatScreen 전용)
    // ─────────────────────────────────────────────────────────
    // 문제:
    //   MainActivity에서 WindowCompat.setDecorFitsSystemWindows(window, false)로
    //   edge-to-edge 모드를 활성화했고, AndroidManifest에는 adjustResize가 설정되어 있다.
    //
    //   edge-to-edge + adjustResize 조합에서:
    //     ① adjustResize  : 키보드 등장 시 시스템이 앱 창 전체를 리사이즈(줄임)
    //     ② imePadding()  : Compose가 키보드 높이만큼 추가 패딩을 삽입
    //   → 두 조정이 중첩되어 입력창이 키보드 높이의 약 2배 위치로 올라가는 버그 발생.
    //
    // 해결:
    //   ChatScreen 진입 시에만 adjustNothing으로 전환 → 시스템 리사이즈 OFF.
    //   Compose의 imePadding()만 단독으로 동작하므로 입력창이 키보드 바로 위에 위치.
    //   화면을 벗어날 때 onDispose에서 원래 모드(adjustResize)로 복원 → 다른 화면에 영향 없음.
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

    // ─────────────────────────────────────────────────────────
    // [3] 자동 스크롤
    // ─────────────────────────────────────────────────────────
    // LazyColumn 아이템 인덱스 구조:
    //   0          : DateChip (날짜 구분선)
    //   1 ~ n      : messages[0] ~ messages[n-1]
    //   n+1 (조건부): FortuneMessage (상담 종료 후)
    //
    // 트리거 조건을 messages.size AND isSending 두 가지로 설정한 이유:
    //   - messages.size 만으로는 새 메시지 추가 시에만 스크롤됨
    //   - BotBubble 내부의 타이핑 애니메이션이 진행되는 동안 말풍선 높이가 증가해도
    //     messages.size는 변하지 않으므로 스크롤이 트리거되지 않음
    //   - isSending이 true→false로 바뀌는 시점(봇 응답 도착)에도 스크롤하여
    //     새 말풍선이 화면에 진입하는 순간 사용자가 볼 수 있도록 함
    //
    // scrollToItem(즉시) vs animateScrollToItem(부드럽게):
    //   - 타이핑 애니메이션과 animateScrollToItem이 동시에 실행되면 스크롤이 끊기는 현상 발생
    //   - scrollToItem으로 즉시 이동하여 충돌 방지
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            // 마지막 메시지 인덱스 = messages.size (DateChip이 index 0이므로)
            listState.scrollToItem(messages.size)
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

    // ─────────────────────────────────────────────────────────
    // [4] 화면 진입 시 대화 초기화
    // ─────────────────────────────────────────────────────────
    // LaunchedEffect(Unit): key가 Unit이므로 이 컴포저블이 컴포지션에 처음 추가될 때 단 한 번 실행.
    //
    // ViewModel은 Navigation으로 탭을 이동해도 소멸되지 않고 살아있으므로
    // 이전 대화 내용이 그대로 남아있다. 탭으로 돌아올 때마다 ChatScreen 컴포저블이
    // 재생성되므로 LaunchedEffect(Unit)이 재실행 → resetChat() 호출로 항상 초기 상태로 시작.
    val today = remember { LocalDate.now() }
    val dateLabel = remember(today) { "오늘 · ${today.monthValue}월 ${today.dayOfMonth}일" }

    LaunchedEffect(Unit) {
        chatViewModel.resetChat()
    }

    // ─────────────────────────────────────────────────────────
    // 레이아웃
    // ─────────────────────────────────────────────────────────
    // Box: 배경 그라디언트를 전체 화면에 표시
    //   - imePadding()을 Box가 아닌 내부 Column에 적용해야 함
    //   - Box에 imePadding()을 두면 Box 자체가 위로 올라가 배경 공백 영역이 생김
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
        // Column: 실제 UI 컨텐츠 배치
        //   - padding(padding)          : Scaffold innerPadding 적용 (상태바·하단 탭바 여백)
        //   - consumeWindowInsets(padding): 위에서 적용한 인셋을 "소비됨"으로 표시
        //                                  → 하위 컴포저블이 동일 인셋을 중복 적용하지 않도록 방지
        //   - imePadding()              : [2]에서 adjustNothing으로 전환했으므로 시스템 리사이즈 없음
        //                                  이 패딩 하나만 키보드 높이를 처리 → 입력창이 키보드 바로 위에 위치
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

                // 메시지 목록 (index 1 ~ messages.size)
                // key = { it.id }: 메시지 ID를 key로 사용해 LazyColumn이 아이템을 효율적으로 재사용
                items(messages, key = { it.id }) { msg ->
                    // 아이템 등장 애니메이션용 상태
                    // false로 시작하다가 LaunchedEffect에서 true로 바꿔 AnimatedVisibility 트리거
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }

                    // 로딩 말풍선 (봇 응답 대기 중 ". . ." 표시)
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

                    // 발신자에 따라 봇/유저 말풍선 분기
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
                                // typedMessageIds에 이 메시지 ID가 없으면 첫 표시 → 타이핑 애니메이션 실행
                                // 있으면 재진입 → 전체 텍스트 즉시 표시 (반복 방지)
                                isFirstDisplay = !typedMessageIds.contains(msg.id),
                                onTypingDone = { typedMessageIds.add(msg.id) }
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

                // 포춘쿠키 카드 (index messages.size + 1)
                // 상담 마치기 버튼을 눌러 isCounselingEnd가 true가 되고,
                // 서버에서 fortune 텍스트가 도착한 후에만 표시
                if (isCounselingEnd && fortune.isNotEmpty()) {
                    item {
                        FortuneMessage(text = fortune)
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

// ═══════════════════════════════════════════════════════════════
// ChatHeaderCard
// 화면 상단 헤더 영역.
// 네이비→블루 그라디언트 아이콘 박스(18dp 둥근 모서리) + 제목 + 부제목으로 구성.
// ═══════════════════════════════════════════════════════════════
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

// ═══════════════════════════════════════════════════════════════
// DateChip
// 날짜 구분선. LazyColumn 최상단(index 0)에 배치.
// 반투명 흰색 pill 형태로 "오늘 · N월 N일"을 가운데 표시.
// ═══════════════════════════════════════════════════════════════
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

// ═══════════════════════════════════════════════════════════════
// BotBubble
// 봇 말풍선. 왼쪽 정렬, 흰 카드 + 4dp 그림자.
// 말풍선 모양: 왼쪽 상단만 각지고 나머지 세 모서리는 둥글게 (카카오톡 스타일).
//
// [타이핑 애니메이션]
// isFirstDisplay = true : 35ms 간격으로 글자를 한 자씩 추가 (최초 표시)
// isFirstDisplay = false: displayedText를 처음부터 전체 텍스트로 초기화 (재진입, 애니메이션 없음)
//
// [파라미터]
//   text           : 표시할 전체 텍스트
//   time           : 메시지 전송 시각 (HH:mm)
//   isFirstDisplay : 이 메시지를 처음 보여주는지 여부
//                    false면 타이핑 없이 전체 텍스트 즉시 표시
//   onTypingDone   : 타이핑 완료 시 호출되는 콜백
//                    ChatScreen에서 typedMessageIds에 id를 추가하는 데 사용
// ═══════════════════════════════════════════════════════════════
@Composable
private fun BotBubble(
    text: String,
    time: String,
    isFirstDisplay: Boolean = true,
    onTypingDone: () -> Unit = {}
) {
    // isFirstDisplay가 false면 재진입이므로 처음부터 전체 텍스트로 초기화
    // → LazyColumn이 아이템을 재생성해도 빈 문자열에서 다시 타이핑하지 않음
    var displayedText by remember(text) {
        mutableStateOf(if (isFirstDisplay) "" else text)
    }

    // text가 바뀔 때마다 실행 (새 메시지가 도착한 경우)
    LaunchedEffect(text) {
        if (isFirstDisplay) {
            // 최초 표시: 35ms마다 글자 하나씩 추가 (타이핑 효과)
            displayedText = ""
            text.forEachIndexed { index, _ ->
                delay(35) // 타이핑 속도 (값이 클수록 느림)
                displayedText = text.substring(0, index + 1)
            }
            // 타이핑 완료 → ChatScreen에 알려 typedMessageIds에 이 메시지 ID 등록
            // 이후 이 아이템이 재생성되면 isFirstDisplay = false로 전달되어 애니메이션 생략
            onTypingDone()
        }
        // isFirstDisplay가 false면 아무것도 하지 않음
        // (displayedText는 이미 전체 텍스트로 초기화되어 있음)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        BotMiniAvatar()
        Spacer(Modifier.width(7.dp))
        // 흰 카드 말풍선: 최대 너비 270dp, 왼쪽 상단 모서리만 각지게
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
            singleLine = true,         // 엔터 대신 전송 버튼 사용 유도
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                disabledContainerColor  = Color.White.copy(alpha = 0.5f),
                // 테두리 색도 흰색에 가깝게 → 배경과 자연스럽게 어우러짐
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

// LoadingBubble
// 봇 응답 대기 중에 표시되는 말풍선.
// 3개의 점이 순서대로 위아래로 튀어오르는 애니메이션 (채팅앱 타이핑 인디케이터).
//
// 구현:
//   rememberInfiniteTransition으로 3개의 점 각각 delayMillis를 다르게 설정
//   → 160ms 간격으로 물결치듯 움직이는 효과
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
//
// [등장 애니메이션 순서]
//   1. 블러인 (650ms): 흐릿하게 나타났다가 선명해짐 (blurRadius 12dp → 0dp, alpha 0 → 1)
//   2. 타이핑 (38ms/글자): 블러인 완료 후 텍스트가 한 글자씩 나타남
//   3. 커서 깜빡임: 타이핑 중 | 커서가 0.7초 주기로 깜빡임
//   4. 커서 숨김 + 시각 표시: 타이핑 완료 800ms 후 커서 사라지고 HH:mm 시각 등장
//
// [구현 방식]
//   - 블러: Modifier.blur(blurRadius.dp) — 값이 0.5f 이하면 적용 생략 (성능 최적화)
//   - 페이드: Modifier.alpha(alpha)
//   - 두 값 모두 animateFloatAsState로 animStarted 플래그에 따라 0↔목표값 전환
//   - 커서: buildAnnotatedString + SpanStyle(color = alpha가 변하는 흰색)
//   - 커서 알파: rememberInfiniteTransition으로 1f → 0f 반복 (RepeatMode.Reverse)
@Composable
private fun FortuneMessage(text: String) {
    // 카드가 처음 등장한 시각을 기억 (이후 리컴포지션이 일어나도 시각이 바뀌지 않음)
    val time = remember {
        val t = java.time.LocalTime.now()
        String.format("%02d:%02d", t.hour, t.minute)
    }

    // 애니메이션 제어 상태
    var animStarted   by remember { mutableStateOf(false) } // 블러인 시작 여부
    var displayedText by remember { mutableStateOf("") }    // 현재까지 타이핑된 텍스트
    var showCursor    by remember { mutableStateOf(false) } // 커서 표시 여부
    var showTime      by remember { mutableStateOf(false) } // 시각 표시 여부

    // 블러인: animStarted가 true가 되면 alpha 0→1, blurRadius 12→0 으로 전환 (650ms)
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

    // 등장 애니메이션 시퀀스 (컴포지션에 추가될 때 단 한 번 실행)
    LaunchedEffect(Unit) {
        animStarted = true                         // 1) 블러인 시작
        delay(700)                      // 2) 블러인 완료 대기 (650ms + 여유 50ms)
        showCursor = true                         // 3) 커서 표시 시작
        text.forEachIndexed { index, _ ->
            delay(38)                   // 4) 38ms마다 글자 하나씩 타이핑 (일반 봇 35ms보다 살짝 느리게)
            displayedText = text.substring(0, index + 1)
        }
        delay(800)                      // 5) 타이핑 완료 후 커서 800ms 깜빡임
        showCursor = false                        // 6) 커서 숨김
        showTime   = true                         // 7) 시각 표시
    }

    val shape         = RoundedCornerShape(18.dp)
    val gradientBrush = Brush.linearGradient(
        listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
    )

    // 외부 Box: 블러 + 알파 적용 (전체 카드에 동시 적용)
    // blurRadius > 0.5f 일 때만 blur 모디파이어 적용 (0일 때 적용 시 성능 낭비)
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
                // 헤더와 본문 구분선 (10% 흰색 → 배경에 자연스럽게 스며듦)
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
