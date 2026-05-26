package com.example.diaryapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapplication.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReportViewModel : ViewModel() {

    private val repository = ReportRepository()

    val weeklyStudy = MutableStateFlow(List(7) { 0 })
    val weeklyExercise = MutableStateFlow(List(7) { 0 })

    val monthlyStudyDaily = MutableStateFlow(List(31) { 0 })
    val monthlyExerciseDaily = MutableStateFlow(List(31) { 0 })

    val weeklyDiaryCount = MutableStateFlow(0)
    val weeklyAvgExercise = MutableStateFlow(0)
    val weeklyTotalExercise = MutableStateFlow(0)
    val weeklyTotalStudy = MutableStateFlow(0)

    val monthlyDiaryCount = MutableStateFlow(0)
    val monthlyAvgExercise = MutableStateFlow(0)
    val monthlyTotalExercise = MutableStateFlow(0)
    val monthlyTotalStudy = MutableStateFlow(0)

    val weeklyEmotions = MutableStateFlow<List<String>>(emptyList())
    val monthlyEmotions = MutableStateFlow<List<String>>(emptyList())

    val weeklySummaries = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val monthlySummaries = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    val isLoading = MutableStateFlow(false)

    val selectedWeekDate = MutableStateFlow(LocalDate.now())
    val selectedMonthDate = MutableStateFlow(LocalDate.now())

    init {
        loadWeeklyData()
        loadMonthlyData()
    }

    fun loadWeeklyData(date: LocalDate = selectedWeekDate.value) {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            isLoading.value = true
            try {
                val data = repository.getWeeklyData(uid, date)
                weeklyStudy.value = data.studyArr
                weeklyExercise.value = data.exerciseArr
                weeklyDiaryCount.value = data.count
                weeklyTotalStudy.value = data.totalStudy
                weeklyTotalExercise.value = data.totalExercise
                weeklyAvgExercise.value = if (data.count > 0) data.totalExercise / data.count else 0
                weeklyEmotions.value = repository.getWeeklyEmotions(uid, date)
                weeklySummaries.value = repository.getWeeklySummaries(uid, date)
            } catch (e: Exception) {
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadMonthlyData(date: LocalDate = selectedMonthDate.value) {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            isLoading.value = true
            try {
                val data = repository.getMonthlyData(uid, date)
                monthlyStudyDaily.value = data.studyArr
                monthlyExerciseDaily.value = data.exerciseArr
                monthlyDiaryCount.value = data.count
                monthlyTotalStudy.value = data.totalStudy
                monthlyTotalExercise.value = data.totalExercise
                monthlyAvgExercise.value = if (data.count > 0) data.totalExercise / data.count else 0
                monthlyEmotions.value = repository.getMonthlyEmotions(uid, date)
                monthlySummaries.value = repository.getMonthlySummaries(uid, date)
            } catch (e: Exception) {
            } finally {
                isLoading.value = false
            }
        }
    }

    fun navigateWeek(delta: Int) {
        val newDate = selectedWeekDate.value.plusWeeks(delta.toLong())
        selectedWeekDate.value = newDate
        loadWeeklyData(newDate)
    }

    fun navigateMonth(delta: Int) {
        val newDate = selectedMonthDate.value.plusMonths(delta.toLong())
        selectedMonthDate.value = newDate
        loadMonthlyData(newDate)
    }
}
