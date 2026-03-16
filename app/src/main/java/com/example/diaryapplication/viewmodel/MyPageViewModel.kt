package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
data class UserProfile(
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val birthDate: String = "" // "2000-01-01" 형태로
)
class MyPageViewModel : ViewModel() { // ViewModel을 상속받아 MyPageViewModel을 생성

    // FireBase AUth, store 인스턴스 생성
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 사용자의 UID를 가져옴
    private val uid get() = auth.currentUser?.uid


    val isLoading = MutableStateFlow(false) // 로딩 중 여부
    val errorMessage = MutableStateFlow<String?>(null) // 에러 메시지
    val isSaveSuccess = MutableStateFlow(false) // 저장 성공 여부

    // 프로필 데이터
    val userProfile = MutableStateFlow(UserProfile())

    // 알림 설정
    val notificationEnabled = MutableStateFlow(false)
    val notifyTime = MutableStateFlow(LocalTime.of(21, 0)) // 기본 오후 9시

    // PIN 설정
    val pinEnabled = MutableStateFlow(false)
    val savedPin = MutableStateFlow<String?>(null)

    // 활동 요약
    val totalDiaryCount = MutableStateFlow(0)
    val thisMonthDiaryCount = MutableStateFlow(0)

    init { // ViewModel시 이 부분이 자동으로 실행되는데, 실행됨으로써 프로필 정보 데이터를 불러오기
        loadProfile()
        loadNotificationSetting()
        loadPinSetting()
        loadDiaryCounts()
    }

    // 프로필 불러오기
    fun loadProfile() {
        val uid = uid ?: return // UID가 null이면 종료
        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true
            try {
                val doc = db.collection("users").document(uid).get().await()
                userProfile.value = UserProfile(
                    // FireBase에서 내 사용자의 정보(문서/객체)를 가져옴
                    name = doc.getString("name") ?: "",
                    nickname = doc.getString("nickname") ?: "",
                    email = doc.getString("email") ?: auth.currentUser?.email ?: "",
                    birthDate = doc.getString("birth_date") ?: ""
                )
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    // 프로필 저장
    fun saveProfile(
        name: String,
        nickname: String,
        birthDate: LocalDate,
        onSuccess: () -> Unit
    ) {
        val uid = uid ?: return
        viewModelScope.launch {
            isLoading.value = true
            isSaveSuccess.value = false
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "name" to name,
                        "nickname" to nickname,
                        "birth_date" to birthDate.toString()
                    )
                ).await()
                userProfile.value = userProfile.value.copy(
                    // copy 사용은 이메일은 변경하지 않으므로, 기존값을 유지하기 위함 -> 변경된 필드만 교체하겠다
                    name = name,
                    nickname = nickname,
                    birthDate = birthDate.toString()
                )
                isSaveSuccess.value = true
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = "저장에 실패했습니다: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // 알림 설정 불러오기
    private fun loadNotificationSetting() {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid)
                    .collection("notification_setting").document("setting")
                    .get().await()
                notificationEnabled.value = doc.getBoolean("enabled") ?: false
                val hour = (doc.getLong("hour") ?: 21).toInt()
                val minute = (doc.getLong("minute") ?: 0).toInt()
                notifyTime.value = LocalTime.of(hour, minute)
            } catch (e: Exception) {
                // 설정 없으면 기본값 유지
            }
        }
    }

    // 알림 설정 저장
    fun saveNotificationSetting(enabled: Boolean, time: LocalTime) {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("notification_setting").document("setting")
                    .set(
                        mapOf(
                            "enabled" to enabled,
                            "hour" to time.hour,
                            "minute" to time.minute
                        )
                    ).await()
                notificationEnabled.value = enabled
                notifyTime.value = time
            } catch (e: Exception) {
                errorMessage.value = "알림 설정 저장에 실패했습니다"
            }
        }
    }

    // 알림 끄기
    fun disableNotification() {
        saveNotificationSetting(false, notifyTime.value)
    }

    // PIN 설정 불러오기
    private fun loadPinSetting() {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid)
                    .collection("pin_setting").document("pin")
                    .get().await()
                val pin = doc.getString("pin")
                if (pin != null) {
                    savedPin.value = pin
                    pinEnabled.value = true
                }
            } catch (e: Exception) {
                // 설정 없으면 기본값 유지
            }
        }
    }

    // PIN 저장
    fun savePin(pin: String) {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("pin_setting").document("pin")
                    .set(mapOf("pin" to pin)).await()
                savedPin.value = pin
                pinEnabled.value = true
            } catch (e: Exception) {
                errorMessage.value = "PIN 저장에 실패했습니다"
            }
        }
    }

    // PIN 삭제
    fun deletePin() {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("pin_setting").document("pin")
                    .delete().await()
                savedPin.value = null
                pinEnabled.value = false
            } catch (e: Exception) {
                errorMessage.value = "PIN 삭제에 실패했습니다"
            }
        }
    }

    // 일기 개수 불러오기
    private fun loadDiaryCounts() {
        val uid = uid ?: return
        viewModelScope.launch {
            try {
                // 전체 일기 개수
                val total = db.collection("diaries")
                    .whereEqualTo("user_id", uid)
                    .get().await()
                totalDiaryCount.value = total.size()

                // 이번 달 일기 개수
                val now = LocalDate.now()
                val prefix = "%04d-%02d".format(now.year, now.monthValue) // 2026-03 형태로 맞춤
                val thisMonth = db.collection("diaries")  // 이번달 작성한 일기를 조회
                    .whereEqualTo("user_id", uid)
                    .whereGreaterThanOrEqualTo("diary_date", "${prefix}-01")
                    .whereLessThanOrEqualTo("diary_date", "${prefix}-31")
                    .get().await()
                thisMonthDiaryCount.value = thisMonth.size()
            } catch (e: Exception) {
                // 개수 로드 실패 시 0 유지
            }
        }
    }
    fun clearError() {
        errorMessage.value = null
    }
}