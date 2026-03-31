package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapplication.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() { // ViewModel을 상속받아 ReportViewModel을 생성

    private val repository = ReportRepository()

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
        val uid = repository.currentUid ?: return // UID가 null이면 종료
        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true
            try {
                val data = repository.getWeeklyData(uid)

                // 계산된 값들을 각 상태 변수에 저장
                weeklyStudy.value = data.studyArr
                weeklyExercise.value = data.exerciseArr
                weeklyDiaryCount.value = data.count
                weeklyTotalStudy.value = data.totalStudy
                weeklyTotalExercise.value = data.totalExercise
                weeklyAvgExercise.value = if (data.count > 0) data.totalExercise / data.count else 0 // 일기가 없으면 0으로 나눌 수 없으므로 값은 0

            } catch (e: Exception) {
                        // 로드 실패 시 기본값 유지
            } finally {
                isLoading.value = false
            }
        }
    }

    // 월간 데이터 로드 (이번 달)
    fun loadMonthlyData() {
        val uid = repository.currentUid ?: return // UID가 null이면 종료

        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true

            try {
                val data = repository.getMonthlyData(uid)

                monthlyStudyDaily.value = data.studyArr
                monthlyExerciseDaily.value = data.exerciseArr
                monthlyDiaryCount.value = data.count
                monthlyTotalStudy.value = data.totalStudy
                monthlyTotalExercise.value = data.totalExercise
                monthlyAvgExercise.value = if (data.count > 0) data.totalExercise / data.count else 0
            } catch (e: Exception) {
                    // 로드 실패 시 기본값 유지
            } finally {
                isLoading.value = false
            }
        }
    }
}