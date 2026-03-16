package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
class ReportViewModel : ViewModel() { // ViewModel을 상속받아 ReportViewModel을 생성

    // Firebase Auth, Store 인스턴스 생성
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 사용자 UID를 불러옴
    private val uid get() = auth.currentUser?.uid

    // 주간 데이터 (인덱스 0=일 ~ 6=토)
    val weeklyStudy = MutableStateFlow(List(7) { 0 })
    val weeklyExercise = MutableStateFlow(List(7) { 0 })

    // 월간 데이터 (일별, 최대 31일)
    val monthlyStudyDaily = MutableStateFlow(List(31) { 0 })
    val monthlyExerciseDaily = MutableStateFlow(List(31) { 0 })

    // KPI 카드를 위한 변수들
    val weeklyDiaryCount = MutableStateFlow(0)
    val weeklyAvgExercise = MutableStateFlow(0)
    val weeklyTotalExercise = MutableStateFlow(0)
    val weeklyTotalStudy = MutableStateFlow(0)
    val monthlyDiaryCount = MutableStateFlow(0)
    val monthlyAvgExercise = MutableStateFlow(0)
    val monthlyTotalExercise = MutableStateFlow(0)
    val monthlyTotalStudy = MutableStateFlow(0)

    // AI 요약 (추후 OpenAI 연동)
    val weeklyAiSummary = MutableStateFlow<String?>(null)
    val monthlyAiSummary = MutableStateFlow<String?>(null)

    val isLoading = MutableStateFlow(false)

    init { // ViewModel 생성 시에 자동으로 주간/월간 데이터를 불러오기
        loadWeeklyData()
        loadMonthlyData()
    }

    // 주간 데이터 로드 (이번 주 월~일)
    fun loadWeeklyData() {
        val uid = uid ?: return // UID가 null이면 종료
        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true
            try {
                val today = LocalDate.now() // 오늘 날짜를 가져옴

                // 이번 주 일요일 ~ 토요일 (일요일 시작)
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

                val result = db.collection("diaries") // 주간 데이터를 물러옴
                    .whereEqualTo("user_id", uid)
                    .whereGreaterThanOrEqualTo("diary_date", weekStart.toString())
                    .whereLessThanOrEqualTo("diary_date", weekEnd.toString())
                    .get().await()

                // 주간 공부/운동 시간을 계산
                val studyArr = MutableList(7) { 0 }
                val exerciseArr = MutableList(7) { 0 }

                var totalStudy = 0
                var totalExercise = 0

                result.documents.forEach { doc -> // 각 일기를 돌면서...

                    val dateStr = doc.getString("diary_date") ?: return@forEach // 날짜를 가져오며, null(일기가 없으면) 건너뜀
                    val date = LocalDate.parse(dateStr) // LocalDate로 변환

                    // 일요일=0, 월요일=1, ... 토요일=6
                    val idx = date.dayOfWeek.value % 7 // 무슨 요일인지를 계산하기 위한 인덱스 계산
                    val study = (doc.getLong("study_min") ?: 0).toInt() // 공부 시간 값 가져오기
                    val exercise = (doc.getLong("exercise_min") ?: 0).toInt() // 운동 시간 값 가져오기

                    // 그 해당 날짜의 공부/운동 시간을 저장
                    studyArr[idx] = study
                    exerciseArr[idx] = exercise

                    // 총 공부/운동시간 값을 업데이트
                    totalStudy += study
                    totalExercise += exercise
                }

                val count = result.size() // 이번주 일기의 개수

                // 계산된 값들을 각 상태 변수에 저장
                weeklyStudy.value = studyArr
                weeklyExercise.value = exerciseArr
                weeklyDiaryCount.value = count
                weeklyTotalStudy.value = totalStudy
                weeklyTotalExercise.value = totalExercise
                weeklyAvgExercise.value = if (count > 0) totalExercise / count else 0 // 일기가 없으면 0으로 나눌 수 없으므로 값은 0
            } catch (e: Exception) {
                        // 로드 실패 시 기본값 유지
            } finally {
                isLoading.value = false
            }
        }
    }

    // 월간 데이터 로드 (이번 달)
    fun loadMonthlyData() {
        val uid = uid ?: return // UID가 null이면 종료

        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true

            try {
                val today = LocalDate.now()

                val monthStart = today.withDayOfMonth(1)
                val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
                val daysInMonth = monthEnd.dayOfMonth

                val result = db.collection("diaries")
                    .whereEqualTo("user_id", uid)
                    .whereGreaterThanOrEqualTo("diary_date", monthStart.toString()) // 1일 날짜 이후의 일기
                    .whereLessThanOrEqualTo("diary_date", monthEnd.toString()) // 이번 달 마지막 날짜 이전의 일기
                    .get().await()

                val studyArr = MutableList(daysInMonth) { 0 }
                val exerciseArr = MutableList(daysInMonth) { 0 }

                var totalStudy = 0
                var totalExercise = 0

                result.documents.forEach { doc ->

                    val dateStr = doc.getString("diary_date") ?: return@forEach
                    val date = LocalDate.parse(dateStr)

                    val idx = date.dayOfMonth - 1 // 1일은 인덱스가 0이므로, 전체적으로 -1을 해야 함

                    val study = (doc.getLong("study_min") ?: 0).toInt()
                    val exercise = (doc.getLong("exercise_min") ?: 0).toInt()

                    if (idx in studyArr.indices) { // 인덱스가 리스트 범위 안에 있을 때만 값을 저장
                        studyArr[idx] = study
                        exerciseArr[idx] = exercise
                    }

                    totalStudy += study
                    totalExercise += exercise
                }
                val count = result.size()

                monthlyStudyDaily.value = studyArr
                monthlyExerciseDaily.value = exerciseArr
                monthlyDiaryCount.value = count
                monthlyTotalStudy.value = totalStudy
                monthlyTotalExercise.value = totalExercise
                monthlyAvgExercise.value = if (count > 0) totalExercise / count else 0
            } catch (e: Exception) {
                    // 로드 실패 시 기본값 유지
            } finally {
                isLoading.value = false
            }
        }
    }
}