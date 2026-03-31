package com.example.diaryapplication.repository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import com.example.diaryapplication.model.UserProfile

class MyPageRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    val currentUid get() = auth.currentUser?.uid
    val currentEmail get() = auth.currentUser?.email

    // 프로필을 불러오는 함수
    suspend fun getProfile(uid: String): UserProfile {
        val doc = db.collection("users").document(uid).get().await()

        return UserProfile(
            name = doc.getString("name") ?: "",
            nickname = doc.getString("nickname") ?: "",
            email = doc.getString("email") ?: auth.currentUser?.email ?: "",
            birthDate = doc.getString("birth_date") ?: ""
        )
    }

    // 프로필 저장 함수
    suspend fun saveProfile(uid: String, name: String, nickname: String, birthDate: String) {
        db.collection("users").document(uid).update(
            mapOf(
                "name" to name,
                "nickname" to nickname,
                "birth_date" to birthDate
            )
        ).await()
    }

    // 알림 설정을 불러오는 함수
    suspend fun getNotificationSetting(uid: String): Pair<Boolean, String> {
        val doc = db.collection("users").document(uid)
            .collection("notification_setting").document("setting")
            .get().await()

        val enabled = doc.getBoolean("is_enabled") ?: false
        val time = doc.getString("notify_time") ?: "21:00"

        return Pair(enabled, time) // (활성화 여부, "HH:mm" 형태 시간)
    }

    // 알림 설정을 저장하는 함수
    suspend fun saveNotificationSetting(uid: String, enabled: Boolean, alertTime: String) {
        db.collection("users").document(uid)
            .collection("notification_setting").document("setting")
            .set(
                mapOf(
                    "is_enabled" to enabled,
                    "notify_time" to alertTime
                )
            ).await()
    }

    // PIN 설정을 불러오는 함수
    suspend fun getPinSetting(uid: String): Pair<String?, Boolean> {
        val doc = db.collection("users").document(uid)
            .collection("pin_setting").document("pin")
            .get().await()

        val pin = doc.getString("pin")
        val isEnabled = doc.getBoolean("is_enabled") ?: false

        return Pair(pin, isEnabled) // (PIN 값, 활성화 여부)
    }

    // PIN을 저장하는 함수
    suspend fun savePin(uid: String, pin: String) {
        db.collection("users").document(uid)
            .collection("pin_setting").document("pin")
            .set(
                mapOf(
                    "pin" to pin,
                    "is_enabled" to true
                )
            ).await()
    }

    // PIN을 삭제하는 함수
    suspend fun deletePin(uid: String) {
        db.collection("users").document(uid)
            .collection("pin_setting").document("pin")
            .set(
                mapOf(
                    "pin" to "",
                    "is_enabled" to false
                )
            ).await()
    }

    // 전체 일기 개수를 불러오는 함수
    suspend fun getTotalDiaryCount(uid: String): Int {
        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .get().await()
        return result.size()
    }

    // 이번 달 일기 개수를 불러오는 함수
    suspend fun getThisMonthDiaryCount(uid: String): Int {
        val now = LocalDate.now()

        val prefix = "%04d-%02d".format(now.year, now.monthValue)

        val result = db.collection("diaries")
            .whereEqualTo("user_id", uid)
            .whereGreaterThanOrEqualTo("diary_date", "${prefix}-01")
            .whereLessThanOrEqualTo("diary_date", "${prefix}-31")
            .get().await()

        return result.size()
    }
}