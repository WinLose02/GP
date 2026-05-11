package com.example.diaryapplication.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatResponse (
    val refinedDiary : String,
    val summary : String,
    val emotionLabel : String,
    val emotionReason : String,
    val fortune : String,
    val keywords : List<String>,
    val counsel : String
)


class ChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val serverUrl = "http://192.168.125.1:8000" // 챗봇 서버 IP 및 Port 번호
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
    suspend fun sendToChatServer(
        text: String,
        uid : String,
        date:String
    ): ChatResponse {
        return withContext(Dispatchers.IO) {

            val jsonBody = org.json.JSONObject().apply {
                put("user_id", uid)
                put("content", text)
                put("date", date)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$serverUrl/diary/analyze")
                .post(jsonBody)
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = org.json.JSONObject(body)

                val emotionObj = json.getJSONObject("emotion")
                val keywords = json.getJSONArray("keywords")
                    .let { arr -> List(arr.length()) { arr.getString(it)} }

                ChatResponse (
                    refinedDiary = json.getString("refined_diary"),
                    summary = json.getString("summary"),
                    emotionLabel = emotionObj.getString("label"),
                    emotionReason = emotionObj.getString("reason"),
                    fortune = emotionObj.getString("fortune"),
                    keywords = keywords,
                    counsel = json.getString("counsel")
                )

            } else {
                throw Exception("서버 오류: ${response.code}")
            }
        }
    }
}