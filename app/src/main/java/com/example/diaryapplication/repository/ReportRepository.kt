package com.example.diaryapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import com.example.diaryapplication.model.WeeklyReportData
import com.example.diaryapplication.model.MonthlyReportData

class ReportRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    val currentUid get() = auth.currentUser?.uid

    // 주간 데이터를 불러오는 함수 (이번 주 : 일요일 ~ 토요일)
    suspend fun getWeeklyData(uid: String): WeeklyReportData {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", weekStart.toString())
            .whereLessThanOrEqualTo("diary_date", weekEnd.toString())
            .get().await()

        val studyArr = MutableList(7) { 0 }
        val exerciseArr = MutableList(7) { 0 }

        var totalStudy = 0
        var totalExercise = 0

        result.documents.forEach { doc ->
            val dateStr = doc.getString("diary_date") ?: return@forEach
            val date = LocalDate.parse(dateStr)
            val idx = date.dayOfWeek.value % 7 // 일=0, 월=1 ... 토=6

            val study = (doc.getLong("study_min") ?: 0).toInt()
            val exercise = (doc.getLong("exercise_min") ?: 0).toInt()

            studyArr[idx] = study
            exerciseArr[idx] = exercise

            totalStudy += study
            totalExercise += exercise
        }
        return WeeklyReportData(
            studyArr = studyArr,
            exerciseArr = exerciseArr,
            totalStudy = totalStudy,
            totalExercise = totalExercise,
            count = result.size()
        )
    }

    // 이번 달 데이터 불러오는 함수
    suspend fun getMonthlyData(uid: String): MonthlyReportData {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
        val daysInMonth = monthEnd.dayOfMonth

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", monthStart.toString())
            .whereLessThanOrEqualTo("diary_date", monthEnd.toString())
            .get().await()

        val studyArr = MutableList(daysInMonth) { 0 }
        val exerciseArr = MutableList(daysInMonth) { 0 }

        var totalStudy = 0
        var totalExercise = 0

        result.documents.forEach { doc ->
            val dateStr = doc.getString("diary_date") ?: return@forEach
            val date = LocalDate.parse(dateStr)
            val idx = date.dayOfMonth - 1 // 1일 = 인덱스 0

            val study = (doc.getLong("study_min") ?: 0).toInt()
            val exercise = (doc.getLong("exercise_min") ?: 0).toInt()

            if (idx in studyArr.indices) {
                studyArr[idx] = study
                exerciseArr[idx] = exercise
            }

            totalStudy += study
            totalExercise += exercise
        }
        return MonthlyReportData(
            studyArr = studyArr,
            exerciseArr = exerciseArr,
            totalStudy = totalStudy,
            totalExercise = totalExercise,
            count = result.size()
        )
    }
}