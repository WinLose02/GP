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
import com.example.diaryapplication.ui.theme.ChatBubbleGray
import com.example.diaryapplication.ui.theme.ChatInputGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Sender { BOT, USER } // 메시지 발신자를 구분

private data class ChatMessage(
    val id: Long,
    val sender: Sender,
    val text: String,
    val time: String
)

private fun nowTime() : String {
    // 현재 시간을 HH:mm 형태로 변환
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

val now = nowTime()

@Composable
fun ChatScreen(padding: PaddingValues) {
    var input by remember { mutableStateOf("") } // 텍스트 필드에 입력된 텍스트 상태 (입력시 값이 업데이트)

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = 1,
                sender = Sender.BOT, // 발신자 -> 챗봇
                text = "안녕하세요, 님! 😊\n오늘 하루는 어떠셨나요? 무엇이든 편하게 이야기해주세요.",
                time = now
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
        ChatHeaderCard(
            title = "감정 챗봇",
            subtitle = "당신의 이야기를 들려주세요"
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn( // 메시지가 많을 경우, 보이는 것만 렌더링 -> 쉽게 말해 스크롤 할 수 있게
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
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
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