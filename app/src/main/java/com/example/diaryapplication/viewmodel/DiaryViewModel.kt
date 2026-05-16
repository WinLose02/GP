package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.net.Uri
import com.example.diaryapplication.repository.DiaryRepository
import com.example.diaryapplication.repository.ChatRepository
import com.google.firebase.Timestamp
import java.io.File
import com.example.diaryapplication.model.DiaryEntry

class DiaryViewModel : ViewModel() { // ViewModel을 상속받아 DiaryViewModel을 생성

    // DiaryRepository 선언
    private val diaryrepository = DiaryRepository()

    // ChatRepository 선언
    private val chatRepository = ChatRepository()


    val isLoading = MutableStateFlow(false) // 로딩 중 여부
    val errorMessage = MutableStateFlow<String?>(null) // 에러 메시지
    val isSaveSuccess = MutableStateFlow(false) // 일기 저장 성공 여부

    // 현재 선택된 날짜의 일기
    // null 이면 해당 날짜의 일기가 없음
    val currentDiary = MutableStateFlow<DiaryEntry?>(null)

    // 달력에 표시할 날짜별 이모지 Map
    // EX> {2026-03-09: "이모지"}
    val emotionEmojiMap = MutableStateFlow<Map<LocalDate, String>>(emptyMap())

    // 이번달에 일기를 작성한 날짜의 목록 -> 중복이 있으면 안되므로 Set 형태
    // EX> {"2026-03-03", "2026-03-04", "2026-03-05"}
    val writtenDates = MutableStateFlow<Set<String>>(emptySet())

    // 연속 일기 작성 일수
    val streakCount = MutableStateFlow(0)

    // 이번 달 일기 이모지를 가져오는 함수 -> 캘린더에 표시를 위함
    fun loadMonthEmojis(year: Int, month: Int) {
        val uid = diaryrepository.currentUid ?: return // 현재 로그인한 유저의 UID 가져옴. 없으면 함수가 종료

        viewModelScope.launch { // 비동기로 처리
            try {
                emotionEmojiMap.value = diaryrepository.getMonthEmojis(uid, year, month) // 완성된 Map을 저장
                //val result = diaryrepository.getMonthEmojis(uid, year, month)
                //android.util.Log.d("DiaryVM", "이모지 로드 결과: $result")
                //emotionEmojiMap.value = result
            } catch (e: Exception) {
                // 이모지 로드 실패 시 무시
                android.util.Log.e("DiaryVM", "이모지 로드 실패 : ${e.message}")
            }
        }
    }

    // 특정 날짜 일기 불러오기
    fun loadDiary(date: LocalDate) {
        val uid = diaryrepository.currentUid ?: return // UID가 없으면 종료

        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true // 로딩 시작
            currentDiary.value = null // 이전 일기 데이터를 초기화

            try {
                currentDiary.value = diaryrepository.getDiary(uid, date.toString())
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    // 일기 저장 (신규 or 수정)
    fun saveDiary(
        date : LocalDate,
        content : String,
        weather : String,
        exerciseMin : Int,
        studyMin : Int,
        routine : String,
        bestThing : String,
        regretThing : String,
        imageUri : Uri?,
        videoFile : File?,
        onSuccess : () -> Unit
    ) {
        val uid = diaryrepository.currentUid ?: return
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            isSaveSuccess.value = false

            try {
                val dateStr = date.toString()

                val imageUrl = if (imageUri != null) {
                    diaryrepository.uploadImage(uid, dateStr, imageUri)
                } else {
                    currentDiary.value?.imageUrl?:""
                }

                val emoji = currentDiary.value?.emotionEmoji ?: ""

                val data = hashMapOf<String, Any>(
                    "user_id" to uid,
                    "diary_date" to dateStr,
                    "content" to content,
                    "weather" to weather,
                    "exercise_min" to exerciseMin,
                    "study_min" to studyMin,
                    "routine" to routine,
                    "best_thing" to bestThing,
                    "regret_thing" to regretThing,
                    "image_url" to imageUrl,
                    "emotion_emoji" to emoji ,
                    "updated_at" to Timestamp.now()
                )

                val existing = currentDiary.value // 현재 선택된 날짜에 기존의 일기가 있는지를 확인

                val diaryId = if (existing != null && existing.id.isNotEmpty()) { // 기존에 일기가 있다면, 일기 내용을 수정
                    // 기존 일기 수정
                    diaryrepository.updateDiary(existing.id, data)
                    existing.id // 기존의 일기 ID를 반환

                } else { // 기존에 일기가 없다면, 일기를 새로 작성
                    data["created_at"] = Timestamp.now()
                    diaryrepository.addDiary(data)

                }

                isSaveSuccess.value = true // 저장 성공 시, 성공 상태로 변경

                loadDiary(date) // 저장 후 다시 불러오기
                loadMonthEmojis(date.year, date.monthValue) // 달력 이모지 업데이트
                loadMonthStreak() // 일기를 저장 후에 스트릿 데이터도 업데이트
                onSuccess()

                viewModelScope.launch {
                    // 챗봇 서버 호출 -> Summary, Keyword 데이터를 DB에 저장
                    try {
                        val chatResponse = chatRepository.sendToChatServer(
                            text = content,
                            uid = uid,
                            date = dateStr
                        )

                        diaryrepository.saveSummary(
                            diaryId = diaryId,
                            summary = chatResponse.summary,
                            keywords = chatResponse.keywords
                        )
                    } catch (e: Exception) {
                        // 실패해도 무시 -> 앱 동작 계속 되어야 함
                    }


                    // 영상 파일이 있으면 서버로 전송
                    if (videoFile != null) {
                        val emotion = diaryrepository.sendVideoToServer(
                            videoFile = videoFile,
                            diaryText = content,
                            uid = uid,
                            diaryDate = dateStr
                        )

                        if (emotion != null) {
                            val diary = diaryrepository.getDiary(uid, dateStr)
                            diary?.let {
                                val emotionEmoji = getEmotionEmoji(emotion)
                                if (emotionEmoji.isNotEmpty()) {
                                    diaryrepository.updateEmotionEmoji(it.id, emotionEmoji)
                                    loadMonthEmojis(date.year, date.monthValue)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "저장에 실패했습니다: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadMonthStreak() {
        val uid = diaryrepository.currentUid ?: return
        val today = LocalDate.now() // 오늘 날짜를 가져옴
        viewModelScope.launch {
            try{
                val prefix = "%04d-%02d".format(today.year, today.monthValue) // 0000-00 형태의 문자열 -> 이번달 일기만 조회하기 위함
                val dates = diaryrepository.getMonthWrittenDates(uid, prefix)
                writtenDates.value = dates // 스트릿 그리드에 사용할 날짜 Set을 업데이트

                // 연속 작성 횟수를 계산
                var streak = 0
                var checkDate = today // 오늘부터 거꾸로 확인을 시작 -> Why? -> 오늘까지 연속으로 몇일을 썼냐라는 것이 기준이기 때문

                // 거꾸로 가면서 일기가 있으면 streak 값을 1 증가
                while (dates.contains(checkDate.toString())) {
                    streak++
                    checkDate = checkDate.minusDays(1) // 날짜에서 1일을 뺌 (EX. 2026-03-03 -> 2026-03-02)
                }
                streakCount.value = streak
            } catch (e: Exception) { }
        }
    }
    // 에러 메시지 초기화
    fun clearError() {
        errorMessage.value = null
    }

    private fun getEmotionEmoji(emotion: String) : String {
        return when(emotion) {
            "기쁨" -> "😊"
            "슬픔" -> "😢"
            "분노" -> "😠"
            "불안" -> "😰"
            "당황" -> "😳"
            else   -> "❎"
        }
    }

    /*
    fun loadEmotionAndUpdate(date: LocalDate) {
        val uid = diaryrepository.currentUid?: return

        viewModelScope.launch{

            try {
                // 1. 해당 날짜의 일기 문서 ID를 가져옴
                val diary = diaryrepository.getDiary(uid, date.toString()) ?: return@launch
                val diaryId = diary.id

                // 2. 서버에서 저장한 final_emotion 읽기
                val finalEmotion = diaryrepository.getEmotionResult(diaryId) ?: return@launch

                // 3. 감정을 앱에 표시할 이모지로 변환
                val emotionEmoji = getEmotionEmoji(finalEmotion)

                // 4. diary 컬렉션에 emotion_emoji 업데이트
                if (emotionEmoji.isNotEmpty()) {
                    diaryrepository.updateEmotionEmoji(diaryId, emotionEmoji)
                }

                // 5. 캘린더에 이모지를 업데이트
                loadMonthEmojis(date.year, date.monthValue)

            } catch(e: Exception) {
                // 분석 결과가 없어도 앱은 실행해야 하므로 무시
            }
        }
    }
    */
}