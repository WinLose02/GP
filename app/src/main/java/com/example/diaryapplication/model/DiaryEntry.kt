package com.example.diaryapplication.model

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