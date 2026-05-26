package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    viewModel : AuthViewModel = viewModel(), // DB와 통신하기 위한 ViewModel
    onBack: () -> Unit, // 뒤로가기 선택 시 실행할 함수
    onSendCode: (String) -> Unit // 인증번호 전송 성공 시, 다음 화면으로 이동 -> String으로 이메일을 전달
) {

    // 화면이 재구성돼도 값을 유지하기 위해 remeber
    // 초기 값은 빈 문자열이나, 값이 바뀌면 화면 자동 업데이트를 위해 mutableStateOf
    var email by remember { mutableStateOf("") }

    // ViewModel에서 로딩 중 여부, 에러 메시지를 실시간으로 받아옴
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // 화면 기본 디자인 틀 만들기
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("비밀번호 찾기") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("가입 시 등록한 이메일을 입력해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("이메일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = email, // 현재 입력 값
                onValueChange = { email = it }, // 값을 입력할 때 마다 email 값을 업데이트
                placeholder = "example@email.com", // 힌트메시지
                containerColor = AppFieldColor
            )

            Spacer(Modifier.height(8.dp))

            // 에러가 발생하면 에러 메시지를 표시
            if (error != null) { // 입력한 이메일이 등록되지 않은 이메일이면 에러 메시지 표시
                Text(
                    text = "등록되지 않은 이메일 입니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            PrimaryPillButton(
                text = "인증번호 받기",
                onClick = { // 버튼 클릭 시, sendPasswordResetEmail()을 호출
                    viewModel.sendPasswordResetEmail(
                        email = email,
                        onSuccess = { onSendCode(email) }, // 이메일 전송을 성공하면, 다음 화면으로 이동함과 동시에 이메일 값을 전달
                        onFail = {} // 실패시에는 별도 처리는 없음
                    )
                },
                enabled = !loading, // 중복 클릭을 방지하기 위해, 버튼을 비활성화
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}