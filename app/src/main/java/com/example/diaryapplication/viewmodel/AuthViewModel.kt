package com.example.diaryapplication.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapplication.Route
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() { // ViewModel을 상속받아 AuthViewModel을 선언
    private val auth = FirebaseAuth.getInstance() // Firebase Auth 인스턴스를 생성
    private val db = FirebaseFirestore.getInstance() // Firestore DB 인스턴스를 생성


    val isLoading = MutableStateFlow(false) // 로딩 중 여부 -> Firebase와 통신 중일 때 true -> 버튼 비활성화
    val errorMessage = MutableStateFlow<String?>(null) // 에러 메시지
    val isLoggedIn = MutableStateFlow(auth.currentUser != null) // 로그인 여부
    val currentNickname = MutableStateFlow<String?>(null) // 현재 로그인한 사용자의 닉네임 -> 초기는 null

    init { // ViewModel이 생성될때 자동으로 실행
        // ?.uid -> 로그인이 안 됐으면 Null ==> 실행X
        // .let{uid -> } -> uid가 null이 아니면, 로그인된 상태라면 자동으로 닉네임을 불러옴
        auth.currentUser?.uid?.let { uid -> loadNickname(uid) }
    }

    private fun loadNickname(uid: String) {
        viewModelScope.launch { // 비동기로 실행 -> UI가 멈추면 안됨
            try {
                // users 컬렉션에서 uid에 해당되는 문서를 선택 후
                // .get().await()로 문서 가져오기를 완료될 때 까지 기다린 후에
                val doc = db.collection("users").document(uid).get().await()
                currentNickname.value = doc.getString("nickname") // doc.getString()을 통해 닉네임 필드값 가져옴
            } catch (e: Exception) { // 만약 못가져와도, 앱은 실행이 되어야 하므로 따로 처리는 X }
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
                    // Firebase Auth로 계정 생성
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val userId = result.user?.uid ?: return@launch // 생성된 사용자 UID를 가져오고, null이면 함수를 종료 (코틀린에서는 람다함수의 종료는 return@ 형태)
                    // Firestore에 사용자 정보 저장
                    val user = hashMapOf(
                        "email" to email,
                        "name" to name,
                        "nickname" to nickname,
                        "birth_date" to birthDate,
                        "gender" to gender,
                        "created_at" to com.google.firebase.Timestamp.now()
                    )
                    db.collection("users").document(userId).set(user).await() // 입력된 정보를 DB에 저장
                    isLoggedIn.value = true
                    currentNickname.value = nickname
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
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    isLoggedIn.value = true
                    val uid = result.user?.uid
                    if (uid != null) {
                        val doc = db.collection("users").document(uid).get().await()
                        currentNickname.value = doc.getString("nickname")
                    }
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
            auth.signOut()
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
                    val result = db.collection("users")
                        .whereEqualTo("name", name) // 필터링 -> name이 같은 것 찾기
                        .whereEqualTo("birth_date", birthDate) // 필터링 -> 생일이 같은 것 찾기
                        .get().await()
                    val email = result.documents.firstOrNull()?.getString("email") // 찾아서 첫번째 문서를 가져옴
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
                    auth.sendPasswordResetEmail(email).await()
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
                    auth.confirmPasswordReset(oobCode, newPassword).await()
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
}