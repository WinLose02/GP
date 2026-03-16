package com.example.diaryapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.AuthViewModel
import com.example.diaryapplication.ui.theme.ChatBubbleGray
import com.example.diaryapplication.ui.theme.ChatInputGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Sender { BOT, USER } // 메시지 발신자를 구분

// 채팅 메시지 하나의 데이터 클래스
private data class ChatMessage(
    val id: Long, // 메시지 고유 ID
    val sender: Sender, // 발신자 (Bot 또는 사용자)
    val text: String, // 메시지 내용
    val time: String // 전송 시간
)

// 현재 시간을 HH:mm 형태로 변환하는 함수
private fun nowTime() : String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

// 앱 시작시에 현재 시간을 한번 저장
val now = nowTime()

// 채팅 화면 함수 선언
@Composable
fun ChatScreen(
    padding: PaddingValues, // 여백값
    authViewModel: AuthViewModel // DB에서 닉네임을 가져오기 위한 ViewModel
) {

    // DB에서 닉네임을 실시간으로 가져옴
    val nickname by authViewModel.currentNickname.collectAsState()

    // 텍스트 필드에 입력된 텍스트 상태 (입력시 값이 업데이트)
    var input by remember { mutableStateOf("") }

    // 채팅 메시지를 모아놓는 List
    val messages = remember(nickname) {
        mutableStateListOf( // 리스트에 메시지를 추가 및 삭제 시 화면에서 자동 업데이트를 위함
            ChatMessage(
                id = 1, // 맨 처음은 봇의 환영 메시지
                sender = Sender.BOT, // 발신자 -> 챗봇
                text = "안녕하세요, ${nickname ?: ""}님! 😊\n오늘 하루는 어떠셨나요?\n무엇이든 편하게 이야기해주세요.",
                time = now // 메시지 전송 시간은 현재 시간
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ChatHeaderCard( // 상단에 챗봇 프로필 표시 부분
            title = "감정 챗봇",
            subtitle = "당신의 이야기를 들려주세요"
        )

        Spacer(Modifier.height(12.dp))

        Surface( // 메시지 목록 부분 -> 상단 프로필과 입력창을 제외한 나머지 모든 부분
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            color = Color.Transparent, // 투명색 배경
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 입력창을 제외한 나머지 모든 부분
        ) {
            LazyColumn( // 메시지가 많을 경우, 보이는 것만 렌더링 -> 쉽게 말해 스크롤 할 수 있게 -> 성능 개선을 위함도 있다고 함
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp) // 메시지와 메시지 사이의 간격을 지정
            ) {
                items(messages, key = { it.id }) { msg -> // 각 메시지를 id로 구분
                    when (msg.sender) {
                        Sender.BOT -> BotBubble(text = msg.text, time = msg.time) // 챗봇 메시지이면 왼쪽 회색 말풍선
                        Sender.USER -> UserBubble(text = msg.text, time = msg.time) // 사용자 메시지이면 오른쪽 파란 말풍선
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ChatInputBar(
            value = input, // 현재 입력된 텍스트
            onValueChange = { input = it }, // 텍스트를 입력할 때마다 위의 input 값을 업데이트
            onSend = { // 전송 버튼을 누르면
                val trimmed = input.trim() // 앞 뒤 공백을 제거
                if (trimmed.isEmpty()) return@ChatInputBar // 공백만 있으면 전송 X

                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) // 전송 시점의 현재 시간을 가져오기
                
                // 사용자 메시지를 목록에 추가
                // 메시지 id를 현재의 밀리초로 구별
                messages.add(ChatMessage(System.currentTimeMillis(), Sender.USER, trimmed, now)) 
                
                // 그러고 입력창 빈칸으로 만들기
                input = ""

                // OpenAI 응답 연결 시 여기에서 BOT 메시지 추가
                messages.add(
                    ChatMessage(
                        System.currentTimeMillis() + 1, // 사용자 메시지 id와 구분하기 위해 +1을 해줌
                        Sender.BOT, // 발신자는 챗봇
                        "좋아요. 조금 더 자세히 이야기해줄래요?", // 지금은 임시 데이터
                        now
                    )
                )
            }
        )
    }
}

// 디자인 부분(상단 챗봇 프로필 카드)
@Composable
private fun ChatHeaderCard(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// 디자인 부분(챗봇 말풍선/왼쪽)
@Composable
private fun BotBubble(text: String, time: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start // 말풍선 왼쪽
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ChatBubbleGray,
            modifier = Modifier.widthIn(max = 320.dp) // 말풍선의 최대 너비
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 디자인 부분(사용자 말풍선/오른쪽)
@Composable
private fun UserBubble(text: String, time: String) {
    val bubbleColor = MaterialTheme.colorScheme.primary
    val onBubble = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
        // 챗봇과 반대로 오른쪽
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 6.dp,     
                bottomEnd = 18.dp,
                bottomStart = 18.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBubble
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    time,
                    style = MaterialTheme.typography.labelSmall,
                    color = onBubble.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// 디자인 부분(입력창 및 전송 버튼)
@Composable
private fun ChatInputBar(
    value: String, // 현재 입력된 메시지
    onValueChange: (String) -> Unit, // 타이핑할때마다 호출
    onSend: () -> Unit // 전송 버튼 클릭 시 호출
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("메시지를 입력하세요...") },
            singleLine = true,                                   // 한 줄 입력
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f), // 입력창을 전송 버튼 제외하고 모두 차지
            colors = TextFieldDefaults.colors( // 포커스 및 언포커스 시 색상
                focusedContainerColor = ChatInputGray,
                unfocusedContainerColor = ChatInputGray,
                disabledContainerColor = ChatInputGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )

        )

        Spacer(Modifier.width(10.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), // 파란색 25% 불투명
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(onClick = onSend) { // 버튼 클릭 시, onSend 실행 -> 메시지 전송
                Icon(
                    imageVector = Icons.Outlined.Send, // 아이콘
                    contentDescription = "send",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}