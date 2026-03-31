package com.example.diaryapplication.model

// 주간 데이터 결과를 담는 데이터 클래스
data class WeeklyReportData(
    val studyArr: List<Int>, // 요일별 공부 시간 (인덱스 0=일 ~ 6=토)
    val exerciseArr: List<Int>, // 요일별 운동 시간
    val totalStudy: Int, // 총 공부 시간
    val totalExercise: Int, // 총 운동 시간
    val count: Int // 이번 주 일기 개수
)
// 월간 데이터 결과를 담는 데이터 클래스
data class MonthlyReportData(
    val studyArr: List<Int>, // 일별 공부 시간 (최대 31일)
    val exerciseArr: List<Int>, // 일별 운동 시간
    val totalStudy: Int, // 총 공부 시간
    val totalExercise: Int, // 총 운동 시간
    val count: Int // 이번 달 일기 개수
)