package com.example.diaryapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.diaryapplication.repository.ChatRepository
import java.time.LocalDate

// ═══════════════════════════════════════════════════════════════
// Sender
// 메시지 발신자를 구분하는 열거형.
//   BOT  → 왼쪽 흰 카드 말풍선 (BotBubble)
//   USER → 오른쪽 네이비·블루 그라디언트 말풍선 (UserBubble)
// ═══════════════════════════════════════════════════════════════
enum class Sender { BOT, USER }

// ═══════════════════════════════════════════════════════════════
// ChatMessage
// 채팅 목록 한 줄에 대응하는 데이터 클래스.
//
// [필드 설명]
//   id        : 메시지 고유 식별자.
//               System.currentTimeMillis() 기반으로 생성 → 밀리초 단위이므로 실질적 중복 없음.
//               LazyColumn의 key 파라미터 및 ChatScreen의 typedMessageIds 집합에서 사용.
//   sender    : 발신자 (BOT / USER)
//   text      : 메시지 본문
//   time      : 전송 시각 문자열 (HH:mm 형식, now() 헬퍼로 생성)
//   isLoading : 봇 응답 대기 중 ". . ." 점 애니메이션 말풍선 여부.
//               true면 LoadingBubble을 표시하고, 실제 응답이 오면 removeMessage로 제거.
// ═══════════════════════════════════════════════════════════════
data class ChatMessage(
    val id: Long,
    val sender: Sender,
    val text: String,
    val time: String,
    val isLoading: Boolean = false
)

// ═══════════════════════════════════════════════════════════════
// ChatViewModel
// ChatScreen의 상태와 비즈니스 로직을 담당하는 ViewModel.
//
// [ViewModel을 사용하는 이유]
//   Compose 컴포저블은 화면 회전·언어 변경 등 구성 변경(Configuration Change) 시 재생성된다.
//   ViewModel은 이 생명주기와 독립적으로 유지되므로 메시지 목록 등 UI 상태를 보존할 수 있다.
//
// [Navigation과 ViewModel 생명주기 관계]
//   Jetpack Navigation에서 탭을 전환해도 ViewModel은 NavBackStackEntry가 살아있는 동안 유지된다.
//   즉, 챗봇 탭에서 다른 탭으로 이동했다가 돌아와도 ChatViewModel 인스턴스가 그대로 남아
//   이전 대화가 _messages에 그대로 보존된다.
//   → ChatScreen 진입 시 LaunchedEffect(Unit)에서 resetChat()을 호출하여 항상 초기 상태로 시작.
//
// [상태 노출 방식 — 단방향 데이터 흐름]
//   MutableStateFlow는 private으로 보유하고, 외부에는 읽기 전용 StateFlow(asStateFlow())만 노출.
//   ChatScreen은 상태를 읽기만 하고, 수정은 반드시 ViewModel의 함수를 통해서만 이루어짐.
// ═══════════════════════════════════════════════════════════════
class ChatViewModel : ViewModel() {

    // Firebase 및 채팅 서버 통신을 담당하는 Repository
    private val repository = ChatRepository()

    // ─────────────────────────────────────────────────────────
    // 상태 변수 (StateFlow)
    // ─────────────────────────────────────────────────────────

    // 채팅 메시지 목록 (봇 말풍선 + 유저 말풍선 + 로딩 말풍선 모두 포함)
    // private: 외부에서 직접 수정 불가 → messages(읽기 전용 StateFlow)로만 접근
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    // 최근 감정 로그: 서버 전송 시 relatedMemories로 함께 전달 → 챗봇이 맥락 있는 답변 생성
    // ChatScreen에 노출할 필요가 없으므로 private 일반 변수로 보관 (StateFlow 불필요)
    private var recentEmotionLogs: List<Map<String, String>> = emptyList()

    // 입력창 텍스트: ChatScreen의 OutlinedTextField와 양방향으로 연결
    // (public MutableStateFlow — 화면에서 직접 읽고 쓰므로)
    val inputText = MutableStateFlow("")

    // 전송 중 여부: true일 때 전송 버튼·입력창 비활성화 (사용자 중복 전송 방지)
    val isSending = MutableStateFlow(false)

    // 포춘쿠키 메시지: 서버 응답의 fortune 필드 값이 저장됨
    // ChatScreen에서 isCounselingEnd && fortune.isNotEmpty() 조건을 모두 만족할 때 카드 표시
    val fortuneMessage = MutableStateFlow("")

    // 요약 메시지: 현재 UI에서 미사용, 추후 대화 요약 기능 확장 시 활용 예정
    val summaryMessage = MutableStateFlow("")

    init {
        // [의도적으로 비워둠]
        // 환영 메시지 로드는 ChatScreen 진입 시 resetChat()에서 처리하므로 여기서는 생략.
        //
        // init에서 loadWelcomeMessage()를 호출하면:
        //   앱 첫 실행 → ViewModel 생성(init 실행) → ChatScreen 컴포지션 → LaunchedEffect(Unit)
        //   → resetChat() → loadWelcomeMessage() 순서로 환영 메시지가 두 번 추가된다.
        // 이를 방지하기 위해 init에서는 호출하지 않음.
    }

    // ─────────────────────────────────────────────────────────
    // 환영 메시지 로드
    // ─────────────────────────────────────────────────────────
    // Firebase에서 현재 로그인한 사용자의 닉네임을 가져와 개인화된 환영 메시지를 표시.
    // currentUid가 null이거나 닉네임 조회 실패 시 기본 환영 메시지를 표시.
    // resetChat() 내부에서 호출되어 화면 진입마다 실행됨.
    private fun loadWelcomeMessage() {
        val uid = repository.currentUid ?: run {
            // 로그인 정보 없음 → 닉네임 없는 기본 메시지
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
                // Firebase 통신 실패 → 기본 메시지로 대체
                addBotMessage("안녕하세요! 😊\n오늘 하루는 어떠셨나요?")
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 최근 감정 로그 로드
    // ─────────────────────────────────────────────────────────
    // 사용자의 최근 감정 기록을 미리 로드하여 sendMessage() 호출 시 서버에 함께 전달.
    // 이를 통해 챗봇이 사용자의 최근 감정 흐름을 참고해 더 맥락 있는 상담을 제공.
    // 조회 실패 시 recentEmotionLogs는 빈 리스트로 유지 → 서버 전송 시 빈 배열로 전달되어
    // 감정 로그 없이 일반 상담으로 진행되므로 채팅 기능 자체에는 영향 없음.
    private fun loadRecentEmotionLogs() {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {
                recentEmotionLogs = repository.getRecentEmotionLogs(uid)
            } catch (e: Exception) {
                // 실패해도 빈 리스트로 동작 (채팅 기능에 영향 없음)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 메시지 전송
    // ─────────────────────────────────────────────────────────
    // 사용자 입력을 서버로 전송하고 봇 응답을 받아 말풍선으로 표시.
    //
    // [전송 흐름]
    //   1. 입력값 trim → 공백만 있거나 전송 중이면 무시
    //   2. 입력창 초기화 & 유저 말풍선 즉시 표시
    //   3. 500ms 후 로딩 말풍선(". . .") 추가 (사용자 말풍선이 렌더링될 시간 확보)
    //   4. 서버에 {text, uid, date, recentEmotionLogs} 전송
    //   5. 성공: 로딩 말풍선 제거 → 봇 말풍선 + 포춘쿠키 텍스트 저장
    //   6. 실패: 로딩 말풍선 제거 → 에러 안내 메시지
    //   7. finally: 성공·실패 무관하게 isSending = false 해제
    fun sendMessage() {
        val text = inputText.value.trim()
        if (text.isEmpty() || isSending.value) return // 빈 입력 또는 중복 전송 방지

        inputText.value = ""      // 입력창 즉시 비움
        addUserMessage(text)      // 유저 말풍선 바로 표시
        isSending.value = true    // 전송 중 상태 → 버튼 비활성화

        viewModelScope.launch {
            delay(500) // 유저 말풍선이 화면에 그려진 뒤 로딩 말풍선 추가
            val loadingId = System.currentTimeMillis() + 1 // 직전 메시지 ID와 겹치지 않도록 +1
            addLoadingBubble(loadingId)

            try {
                val uid  = repository.currentUid ?: ""
                val date = LocalDate.now().toString() // "YYYY-MM-DD"

                val response = repository.sendToChatServer(
                    text            = text,
                    uid             = uid,
                    date            = date,
                    relatedMemories = recentEmotionLogs // 이전에 로드해 둔 감정 로그
                )

                removeMessage(loadingId)           // 로딩 말풍선 제거
                addBotMessage("${response.counsel}") // 봇 응답 말풍선 추가
                fortuneMessage.value = response.fortune // 포춘쿠키 텍스트 저장

            } catch (e: Exception) {
                removeMessage(loadingId)
                addBotMessage("죄송합니다, 잠시 후 다시 시도해주세요")
            } finally {
                isSending.value = false // 성공·실패 무관하게 전송 상태 해제
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 메시지 헬퍼 함수
    // ─────────────────────────────────────────────────────────

    // 현재 시각을 "HH:mm" 형식으로 반환 (말풍선 하단 시각 표시용)
    private fun now() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // 봇 말풍선을 목록 끝에 추가
    private fun addBotMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(
            id     = System.currentTimeMillis(),
            sender = Sender.BOT,
            text   = text,
            time   = now()
        )
    }

    // 유저 말풍선을 목록 끝에 추가
    private fun addUserMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(
            id     = System.currentTimeMillis(),
            sender = Sender.USER,
            text   = text,
            time   = now()
        )
    }

    // 로딩 말풍선(". . ." 점 애니메이션)을 목록 끝에 추가
    // id를 직접 받아 나중에 removeMessage(loadingId)로 정확히 제거할 수 있도록 함
    private fun addLoadingBubble(id: Long) {
        _messages.value = _messages.value + ChatMessage(
            id        = id,
            sender    = Sender.BOT,
            text      = ". . .",
            time      = now(),
            isLoading = true // LoadingBubble 컴포저블로 렌더링됨
        )
    }

    // 지정 id의 메시지를 목록에서 제거 (주로 로딩 말풍선 제거에 사용)
    private fun removeMessage(id: Long) {
        _messages.value = _messages.value.filter { it.id != id }
    }

    // 입력창 텍스트 업데이트 (ChatScreen OutlinedTextField의 onValueChange 콜백에서 호출)
    fun onInputChange(text: String) {
        inputText.value = text
    }

    // ─────────────────────────────────────────────────────────
    // 채팅 초기화 (resetChat)
    // ─────────────────────────────────────────────────────────
    // ChatScreen의 LaunchedEffect(Unit)에서 화면 진입마다 호출된다.
    //
    // [초기화 이유]
    //   ViewModel은 Navigation 탭 전환 후 돌아와도 소멸되지 않고 이전 대화가 그대로 남아있다.
    //   실제 앱에서는 챗봇 탭 재진입 시 항상 새로운 대화를 시작해야 하므로
    //   화면 진입마다 이 함수를 통해 완전 초기화한다.
    //
    // [초기화 항목]
    //   - 메시지 목록 비우기 (_messages → emptyList)
    //   - 입력창 텍스트 초기화
    //   - 전송 중 상태 해제 (비정상 종료 후 재진입 대비)
    //   - 포춘쿠키 / 요약 메시지 초기화
    //   - 환영 메시지 새로 로드 (닉네임 포함)
    //   - 최근 감정 로그 새로 로드 (서버 전송 맥락 정보 갱신)
    fun resetChat() {
        _messages.value      = emptyList()
        inputText.value      = ""
        isSending.value      = false
        fortuneMessage.value = ""
        summaryMessage.value = ""
        loadWelcomeMessage()    // Firebase에서 닉네임을 가져와 개인화 환영 메시지 표시
        loadRecentEmotionLogs() // 서버 전송 시 첨부할 최근 감정 로그 미리 로드
    }
}
