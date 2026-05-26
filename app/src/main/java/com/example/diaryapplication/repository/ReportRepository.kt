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

    suspend fun getWeeklyData(uid: String, referenceDate: LocalDate = LocalDate.now()): WeeklyReportData {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

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
            val idx = date.dayOfWeek.value % 7

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

    suspend fun getMonthlyData(uid: String, referenceDate: LocalDate = LocalDate.now()): MonthlyReportData {
        val monthStart = referenceDate.withDayOfMonth(1)
        val monthEnd = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
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
            val idx = date.dayOfMonth - 1

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

    suspend fun getWeeklyEmotions(uid: String, referenceDate: LocalDate = LocalDate.now()): List<String> {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", weekStart.toString())
            .whereLessThanOrEqualTo("diary_date", weekEnd.toString())
            .get().await()

        return result.documents.mapNotNull { doc ->
            doc.getString("emotion_emoji")?.takeIf { it.isNotEmpty() }
        }
    }

    suspend fun getMonthlyEmotions(uid: String, referenceDate: LocalDate = LocalDate.now()): List<String> {
        val monthStart = referenceDate.withDayOfMonth(1)
        val monthEnd = referenceDate.with(TemporalAdjusters.lastDayOfMonth())

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", monthStart.toString())
            .whereLessThanOrEqualTo("diary_date", monthEnd.toString())
            .get().await()

        return result.documents.mapNotNull { doc ->
            doc.getString("emotion_emoji")?.takeIf { it.isNotEmpty() }
        }
    }

    suspend fun getWeeklySummaries(uid: String, referenceDate: LocalDate = LocalDate.now()): List<Pair<String, String>> {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekEnd = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", weekStart.toString())
            .whereLessThanOrEqualTo("diary_date", weekEnd.toString())
            .get().await()

        return result.documents.mapNotNull { doc ->
            val date = doc.getString("diary_date") ?: return@mapNotNull null
            val summary = doc.getString("summary") ?: return@mapNotNull null
            if (summary.isNotEmpty()) Pair(date, summary) else null
        }.sortedBy { it.first }
    }

    suspend fun getMonthlySummaries(uid: String, referenceDate: LocalDate = LocalDate.now()): List<Pair<String, String>> {
        val monthStart = referenceDate.withDayOfMonth(1)
        val monthEnd = referenceDate.with(TemporalAdjusters.lastDayOfMonth())

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", monthStart.toString())
            .whereLessThanOrEqualTo("diary_date", monthEnd.toString())
            .get().await()

        return result.documents.mapNotNull { doc ->
            val date = doc.getString("diary_date") ?: return@mapNotNull null
            val summary = doc.getString("summary") ?: return@mapNotNull null
            if (summary.isNotEmpty()) Pair(date, summary) else null
        }.sortedBy { it.first }
    }
}
