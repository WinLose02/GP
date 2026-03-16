package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.RoundedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    email: String, // 인증 번호를 보낼 이메일
    onBack: () -> Unit, // 뒤로 가기 버튼을 눌렀을 때 실행할 동작
    onReEnterEmail: () -> Unit // '이메일 다시 입력' 버튼 클릭 시 실행
) {

    // 화면 디자인 기본 틀 생성
    Scaffold(
        topBar = { // 상단 앱 바
            TopAppBar(
                title = {}, // 제목은 X
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // <- 뒤로가기 화살표 아이콘
                        // 클릭 시, onBack() 실행
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface( // 알림 배너 부분
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row( // 그 배너 안 부분의 내용
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), // 좌우, 위아래 여백
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("인증 링크가 이메일로 전송되었습니다.")
                }
            }
            
            // 이메일 발송 확인 파트 디자인
            RoundedCard(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Email, null) // 아이콘 표시
                Spacer(Modifier.height(10.dp))
                Text("비밀번호 재설정 링크가 발송되었습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(email, style = MaterialTheme.typography.titleMedium) // 실제 발송 된 이메일 주소를 굵게 표시
                Spacer(Modifier.height(6.dp))

            }
            
            // 이메일 다시 입력 버튼
            OutlinedButton(
                onClick = onReEnterEmail, // 버튼 클릭 시, 이메일 입력 화면으로 다시 이동
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("이메일 다시 입력") }
        }
    }
}