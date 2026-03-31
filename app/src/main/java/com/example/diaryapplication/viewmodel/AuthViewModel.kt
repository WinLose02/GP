package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapplication.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() { // ViewModel을 상속받아 AuthViewModel을 선언

    private val repository = AuthRepository()

    val isLoading = MutableStateFlow(false) // 로딩 중 여부 -> Firebase와 통신 중일 때 true -> 버튼 비활성화
    val errorMessage = MutableStateFlow<String?>(null) // 에러 메시지
    val isLoggedIn = MutableStateFlow(repository.currentUser != null) // 로그인 여부
    val currentNickname = MutableStateFlow<String?>(null) // 현재 로그인한 사용자의 닉네임 -> 초기는 null

    init { // ViewModel이 생성될때 자동으로 실행
        // ?.uid -> 로그인이 안 됐으면 Null ==> 실행X
        // .let{uid -> } -> uid가 null이 아니면, 로그인된 상태라면 자동으로 닉네임을 불러옴
        repository.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                currentNickname.value = repository.getNickname(uid) }
        }
    }

    // 회원가입
    fun signUp(
            email: String,
            password: String,
            name: String,
            nickname: String,
            birthDate: String,
            gender: Int,
            onSuccess: () -> Unit
        ) {
            viewModelScope.launch { // 비동기로 처리
                isLoading.value = true
                errorMessage.value = null
                try {
                    val result_nick = repository.signUp(email, password, name, nickname, birthDate, gender)
                    isLoggedIn.value = true
                    currentNickname.value = result_nick
                    onSuccess()
                } catch (e: Exception) { // 에러 발생 시, 에러 메시지를 저장
                    errorMessage.value = e.message
                } finally { // 에러가 발생해도 로딩이 멈추지 않는 버그를 방지하기 위해 finally 설정
                    isLoading.value = false
                }
            }
        }

    // 로그인
    fun login(
            email: String,
            password: String,
            onSuccess: () -> Unit
        ) {
            viewModelScope.launch { // 비동기로 처리
                isLoading.value = true
                errorMessage.value = null
                try {
                    val result = repository.login(email, password)
                    isLoggedIn.value = true
                    currentNickname.value = result
                    onSuccess()
                } catch (e: Exception) {
                    errorMessage.value = "이메일 또는 비밀번호가 올바르지 않습니다"
                } finally {
                    isLoading.value = false
                }
            }
        }

    // 로그아웃
    fun logout(onComplete: () -> Unit) {
            repository.logout()
            isLoggedIn.value = false
            currentNickname.value = null
            onComplete()
        }


    // 아이디(이메일) 찾기
    fun findEmail(
            name: String,
            birthDate: String,
            onResult: (String?) -> Unit
        ) {
            viewModelScope.launch {
                isLoading.value = true
                try {
                    val email = repository.findEmail(name, birthDate)
                    onResult(email)
                } catch (e: Exception) {
                    errorMessage.value = e.message
                    onResult(null)
                } finally {
                    isLoading.value = false
                }
            }
        }

        // 비밀번호 재설정 이메일 발송

    fun sendPasswordResetEmail(
            email: String,
            onSuccess: () -> Unit,
            onFail: () -> Unit
        ) {
            viewModelScope.launch {
                isLoading.value = true
                try {
                    repository.sendPasswordResetEmail(email)
                    onSuccess()
                } catch (e: Exception) {
                    errorMessage.value = e.message
                    onFail()
                } finally {
                    isLoading.value = false
                }
            }
        }

        // 비밀번호 재설정

    fun resetPassword(
            oobCode: String,
            newPassword: String,
            onSuccess: () -> Unit,
            onFail: () -> Unit
        ) {
            viewModelScope.launch {
                isLoading.value = true
                errorMessage.value = null
                try {
                    repository.resetPassword(oobCode, newPassword)
                    onSuccess()
                } catch (e: Exception) {
                    errorMessage.value = e.message
                    onFail()
                } finally {
                    isLoading.value = false
                }
            }
        }
    }