package com.example.diaryapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUid get() = auth.currentUser?.uid // 현재 사용자의 User ID
    val currentUser get() = auth.currentUser

    // 사용자의 닉네임을 불러오는 함수
    suspend fun getNickname(uid: String): String? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("nickname")
    }

    // 회원가입 함수
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        nickname: String,
        birthDate: String,
        gender: Int
    ): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()

        val uid = result.user?.uid ?: throw Exception("UID를 가져올 수 없습니다")

        val user = hashMapOf(
            "email" to email,
            "name" to name,
            "nickname" to nickname,
            "birth_date" to birthDate,
            "gender" to gender,
            "created_at" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(user).await()

        return nickname // 닉네임 반환 (ViewModel에서 상태 업데이트용)
    }

    // 로그인 함수
    suspend fun login(email: String, password: String): String? {
        val result = auth.signInWithEmailAndPassword(email, password).await()

        val uid = result.user?.uid ?: return null

        val doc = db.collection("users").document(uid).get().await()

        return doc.getString("nickname")
    }

    // 로그아웃 함수
    fun logout() {
        auth.signOut()
    }

    // 아이디(이메일) 찾기 함수
    suspend fun findEmail(name: String, birthDate: String): String? {
        val result = db.collection("users")
            .whereEqualTo("name", name)
            .whereEqualTo("birth_date", birthDate)
            .get().await()

        return result.documents.firstOrNull()?.getString("email")
    }

    // 비밀번호 재설정 인증 메일 보내는 함수
    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    // 비밀번호 재설정 함수
    suspend fun resetPassword(oobCode: String, newPassword: String) {
        auth.confirmPasswordReset(oobCode, newPassword).await()
    }
}