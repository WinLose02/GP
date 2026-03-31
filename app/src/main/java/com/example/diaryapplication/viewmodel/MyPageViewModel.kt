package com.example.diaryapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import com.example.diaryapplication.repository.MyPageRepository
import com.example.diaryapplication.model.UserProfile

class MyPageViewModel() : ViewModel() { // ViewModel을 상속받아 MyPageViewModel을 생성

    private val repository = MyPageRepository()


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
        val uid = repository.currentUid ?: return // UID가 null이면 종료
        viewModelScope.launch { // 비동기로 처리
            isLoading.value = true
            try {
                userProfile.value = repository.getProfile(uid)
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
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            isLoading.value = true
            isSaveSuccess.value = false
            try {
                repository.saveProfile(uid, name, nickname, birthDate.toString())
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
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {
                val (enabled, timeStr) = repository.getNotificationSetting(uid)
                notificationEnabled.value = enabled

                // Time을 HH:mm 형태로 LocalTime으로 변환
                val alertTime = timeStr.split(":")
                val hour = alertTime[0].toIntOrNull()?:21
                val minute = alertTime[1].toIntOrNull()?:0
                notifyTime.value = LocalTime.of(hour, minute)
            } catch (e: Exception) {
                // 설정 없으면 기본값 유지
            }
        }
    }

    // 알림 설정 저장
    fun saveNotificationSetting(enabled: Boolean, time: LocalTime) {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {

                // time을 HH:mm 형태로 변환해서 DB에 저장
                val alertTime = "%02d:%02d".format(time.hour, time.minute)
                repository.saveNotificationSetting(uid, enabled, alertTime)
                notificationEnabled.value = enabled
                notifyTime.value = time

            } catch (e: Exception) {
                errorMessage.value = "알림 설정 저장에 실패했습니다"
                return@launch
            }

        }
    }

    // 알림 끄기
    fun disableNotification() {
        saveNotificationSetting(false, notifyTime.value)
    }

    // PIN 설정 불러오기
    private fun loadPinSetting() {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {
                val (pin, isEnabled) = repository.getPinSetting(uid)
                if (pin != null && isEnabled) {
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
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {
                repository.savePin(uid,pin)
                savedPin.value = pin
                pinEnabled.value = true
            } catch (e: Exception) {
                errorMessage.value = "PIN 저장에 실패했습니다"
            }
        }
    }

    // PIN 삭제
    fun deletePin() {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {
                repository.deletePin(uid)
                savedPin.value = null
                pinEnabled.value = false
            } catch (e: Exception) {
                errorMessage.value = "PIN 삭제에 실패했습니다"
            }
        }
    }

    // 일기 개수 불러오기
    private fun loadDiaryCounts() {
        val uid = repository.currentUid ?: return
        viewModelScope.launch {
            try {
                // 전체 일기 개수
                totalDiaryCount.value = repository.getTotalDiaryCount(uid)

                // 이번 달 일기 개수
                thisMonthDiaryCount.value = repository.getThisMonthDiaryCount(uid)

            } catch (e: Exception) {
                // 개수 로드 실패 시 0 유지
            }
        }
    }
    fun clearError() {
        errorMessage.value = null
    }
}