package com.example.diaryapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.diaryapplication.repository.ChatRepository
import java.time.LocalDate

enum class Sender { BOT, USER }

data class ChatMessage(
    val id: Long,
    val sender: Sender,
    val text: String,
    val time: String,
    val isLoading: Boolean = false // 로딩 중 말풍선 표시용
)

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    // ─────────────────────────────────────────
    // 상태 변수
    // ─────────────────────────────────────────
    // 채팅 메시지 목록
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    // 입력창 텍스트
    val inputText = MutableStateFlow("")

    // 전송 중 여부 (전송 중에는 버튼 비활성화)
    val isSending = MutableStateFlow(false)

    init {
        loadWelcomeMessage()
    }

    // ─────────────────────────────────────────
    // 환영 메시지 - Firebase에서 닉네임 가져와서 표시
    // ─────────────────────────────────────────
    private fun loadWelcomeMessage() {
        val uid = repository.currentUid ?: run {
            addBotMessage("안녕하세요! 😊\n오늘 하루는 어떠셨나요?\n무엇이든 편하게 이야기해주세요.")
            return
        }
        viewModelScope.launch {
            try {
                val nickname = repository.getNickname(uid) ?: ""
                addBotMessage(
                    "안녕하세요, ${nickname}님! 😊\n" +
                            "오늘 하루는 어떠셨나요?\n" +
                            "무엇이든 편하게 이야기해주세요!"
                )
            } catch (e: Exception) {
                addBotMessage("안녕하세요! 😊\n오늘 하루는 어떠셨나요?")
            }
        }
    }

    // ─────────────────────────────────────────
    // 메시지 전송
    // 사용자가 입력한 텍스트를 서버로 전송
    // 추천 요청도 이 함수를 통해 처리됨
    // (서버에서 입력 내용을 분석해서 추천인지 일반 대화인지 구분)
    // ─────────────────────────────────────────
    fun sendMessage() {
        val text = inputText.value.trim() // 앞뒤 공백 제거
        if (text.isEmpty() || isSending.value) return // 입력이 없으면 전송X

        inputText.value = "" // 입력창 초기화
        addUserMessage(text) // 사용자 메시지 표시

        // 로딩 말풍선 표시
        val loadingId = System.currentTimeMillis() + 1
        addLoadingBubble(loadingId)
        isSending.value = true

        viewModelScope.launch {
            try {
                val uid = repository.currentUid ?: ""
                val date = LocalDate.now().toString()
                val response = repository.sendToChatServer(
                    text = text,
                    uid = uid,
                    date = date
                )

                removeMessage(loadingId)

                addBotMessage(
                    "[${response.emotionLabel}] 감정이 느껴졌어요.\n\n" +
                    "${response.counsel}\n\n" +
                    "✨ ${response.fortune} "
                )

            } catch (e: Exception) {
                removeMessage(loadingId)
                addBotMessage("죄송합니다, 잠시 후 다시 시도해주세요")
            } finally {
                isSending.value = false
            }
        }
    }


    // ─────────────────────────────────────────
    // 메시지 헬퍼 함수
    // ─────────────────────────────────────────
    private fun now() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    private fun addBotMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(
            id = System.currentTimeMillis(),
            sender = Sender.BOT,
            text = text,
            time = now()
        )
    }
    private fun addUserMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(
            id = System.currentTimeMillis(),
            sender = Sender.USER,
            text = text,
            time = now()
        )
    }
    private fun addLoadingBubble(id: Long) {
        _messages.value = _messages.value + ChatMessage(
            id = id,
            sender = Sender.BOT,
            text = "...",
            time = now(),
            isLoading = true
        )
    }
    private fun removeMessage(id: Long) {
        _messages.value = _messages.value.filter { it.id != id }
    }
    // 입력창 텍스트 업데이트
    fun onInputChange(text: String) {
        inputText.value = text
    }
}