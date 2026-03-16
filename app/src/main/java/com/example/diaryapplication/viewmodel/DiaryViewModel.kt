package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// 일기 데이터 모델
data class DiaryEntry(
    val id: String = "",
    val diaryDate: String = "", // "2026-03-09" 형식
    val content: String = "", // 일기 내용
    val weather: String = "SUNNY", // 날씨
    val exerciseMin: Int = 0, // 운동 시간 (분)
    val studyMin: Int = 0, // 공부 시간 (분)
    val routine: String = "", // 하루 일과
    val bestThing: String = "", // 가장 좋았던 일
    val regretThing: String = "", // 가장 아쉬웠던 일
    val imageUrl: String = "", // TODO: 사진 URL (Firebase Storage와 연동 예정)
    val emotionEmoji: String = "" // TODO: 감정 이모지 (AI 감정 분석 연동 예정)
)

class DiaryViewModel : ViewModel() { // ViewModel을 상속받아 DiaryViewModel을 생성

    // Firebase Auth, Firestore 인스턴스를 생성
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


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

    // 현재 로그인한 유저의 UID를 가져옴
    private val uid get() = auth.currentUser?.uid

    // 이번 달 일기 이모지를 가져오는 함수 -> 캘린더에 표시를 위함
    fun loadMonthEmojis(year: Int, month: Int) {
        val uid = uid ?: return // 현재 로그인한 유저의 UID 가져옴. 없으면 함수가 종료

        viewModelScope.launch { // 비동기로 처리
            try {
                // "2026-03" 형식으로 해당 월의 일기들을 가져옴
                val prefix = "%04d-%02d".format(year, month)

                val result = db.collection("diaries") // diaries 컬렉션에서 일기를 가져옴
                    .whereEqualTo("user_id", uid) // 필터링(1) -> User_ID
                    .whereGreaterThanOrEqualTo("diary_date", "${prefix}-01") // 필터링(2) -> 1일 이후 날짜의 일기
                    .whereLessThanOrEqualTo("diary_date", "${prefix}-31") // 필터링(3) -> 31일 이전 날짜의 일기
                    .get().await()

                val map = mutableMapOf<LocalDate, String>() // 날짜-이모지를 담을 Map 형태의 컨테이너 생성

                result.documents.forEach { doc -> // 가져온 일기를 탐색하면서
                    val dateStr = doc.getString("diary_date") ?: return@forEach // 일기 날짜와
                    val emoji = doc.getString("emotion_emoji") ?: return@forEach // 일기 이모지를 가져옴

                    if (emoji.isNotEmpty()) { // 만약 이모지가 있으면
                        val date = LocalDate.parse(dateStr) // 날짜(문자열 형태)를 LocalDate로 변환
                        map[date] = emoji // 이모지도 입력
                    }
                }
                emotionEmojiMap.value = map // 완성된 Map을 저장
            } catch (e: Exception) {
                // 이모지 로드 실패 시 무시
            }
        }
    }

    // 특정 날짜 일기 불러오기
    fun loadDiary(date: LocalDate) {
        val uid = uid ?: return // UID가 없으면 종료

        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true // 로딩 시작
            currentDiary.value = null // 이전 일기 데이터를 초기화

            try {
                val dateStr = date.toString() // "2026-03-09" 형태로 가져옴
                val result = db.collection("diaries") // DB에서 해당 날짜의 내 일기를 가져옴
                    .whereEqualTo("user_id", uid)
                    .whereEqualTo("diary_date", dateStr)
                    .limit(1)
                    .get().await()
                val doc = result.documents.firstOrNull() // 결과 중 첫번째 문서(객체)를 가져옴

                if (doc != null) { // 해당 날짜의 일기가 있으면
                    currentDiary.value = DiaryEntry(
                        // 일기에 작성된 각 값들을 다 불러옴
                        id = doc.id,
                        diaryDate = doc.getString("diary_date") ?: "",
                        content = doc.getString("content") ?: "",
                        weather = doc.getString("weather") ?: "SUNNY",
                        exerciseMin = (doc.getLong("exercise_min") ?: 0).toInt(),
                        studyMin = (doc.getLong("study_min") ?: 0).toInt(),
                        routine = doc.getString("routine") ?: "",
                        bestThing = doc.getString("best_thing") ?: "",
                        regretThing = doc.getString("regret_thing") ?: "",
                        imageUrl = doc.getString("image_url") ?: "",
                        emotionEmoji = doc.getString("emotion_emoji") ?: ""
                    )
                }
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    // 일기 저장 (신규 or 수정)
    fun saveDiary(
        date: LocalDate,
        content: String,
        weather: String,
        exerciseMin: Int,
        studyMin: Int,
        routine: String,
        bestThing: String,
        regretThing: String,
        onSuccess: () -> Unit
    ) {
        val uid = uid ?: return
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            isSaveSuccess.value = false

            try {
                val dateStr = date.toString()
                val data = hashMapOf(
                    "user_id" to uid,
                    "diary_date" to dateStr,
                    "content" to content,
                    "weather" to weather,
                    "exercise_min" to exerciseMin,
                    "study_min" to studyMin,
                    "routine" to routine,
                    "best_thing" to bestThing,
                    "regret_thing" to regretThing,
                    "image_url" to "", // TODO: firestorage 작업
                    "emotion_emoji" to "", // TODO: 감정 분석 모델의 값을 받아와 여기에 들어가기
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                val existing = currentDiary.value // 현재 선택된 날짜에 기존의 일기가 있는지를 확인

                if (existing != null && existing.id.isNotEmpty()) { // 기존에 일기가 있다면, 일기 내용을 수정
                    // 기존 일기 수정
                    db.collection("diaries").document(existing.id).update(data as Map<String, Any>).await()
                } else { // 기존에 일기가 없다면, 일기를 새로 작성
                    data["created_at"] = com.google.firebase.Timestamp.now()
                    db.collection("diaries").add(data).await()
                }

                isSaveSuccess.value = true // 저장 성공 시, 성공 상태로 변경

                loadDiary(date) // 저장 후 다시 불러오기
                loadMonthEmojis(date.year, date.monthValue) // 달력 이모지 업데이트
                loadMonthStreak() // 일기를 저장 후에 스트릿 데이터도 업데이트
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = "저장에 실패했습니다: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadMonthStreak() {
        val uid = uid ?: return
        val today = LocalDate.now() // 오늘 날짜를 가져옴
        viewModelScope.launch {
            try{
                val prefix = "%04d-%02d".format(today.year, today.monthValue) // 0000-00 형태의 문자열 -> 이번달 일기만 조회하기 위함

                // 조건에 맞는 일기를 firebase에서 조회
                // 내 일기만
                // 이번달 1일~31일
                val result = db.collection("diaries")
                    .whereEqualTo("user_id", uid)
                    .whereGreaterThanOrEqualTo("diary_date", "${prefix}-01")
                    .whereLessThanOrEqualTo("diary_date", "${prefix}-31")
                    .get().await()

                // 조회된 값들 중에서 diary_date 필드만 뽑아서 Set 형태로 변환
                // mapNotNull -> null인 날짜는 제외하고 뽑아냄
                val dates = result.documents
                    .mapNotNull { it.getString("diary_date") }
                    .toSet()

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
}