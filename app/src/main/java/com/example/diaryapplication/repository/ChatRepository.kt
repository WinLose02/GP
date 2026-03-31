package com.example.diaryapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit


class ChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val serverUrl = "" // 챗봇 서버 IP 및 Port 번호
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val currentUid get() = auth.currentUser?.uid

    // 닉네임 불러오기 함수
    suspend fun getNickname(uid: String): String? {
        val doc = db.collection("users").document(uid).get().await()

        return doc.getString("nickname")
    }

    // 챗봇 서버에게 메시지를 전송하고 챗봇의 응답을 받는 함수
    suspend fun sendToChatServer(text: String): String {
        return withContext(Dispatchers.IO) {

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("content", text)
                .build()

            val request = Request.Builder()
                .url("$serverUrl/chat")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = org.json.JSONObject(body)
                json.getString("response")
            } else {
                throw Exception("서버 오류: ${response.code}")
            }
        }
    }
}