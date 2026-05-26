package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.RoundedCard
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor
import com.example.diaryapplication.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel : AuthViewModel = viewModel (), // DB 통신을 위한 ViewModel
    oobCode : String = "", // DB에서 발급한 비밀번호 재설정 인증 코드
    onBack: () -> Unit, // 뒤로가기 클릭 시 호출
    onComplete: () -> Unit // 비밀번호 변경 완료 시, 다음 화면으로 전환
) {
    var pw1 by remember { mutableStateOf("") } // 새 비밀번호 값
    var pw2 by remember { mutableStateOf("") } // 비밀번호 확인 입력값

    // ViewModel에서 실시간으로 로딩 중 여부와 에러 메세지를 받아옴
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // 화면 기본 디자인 틀 생성
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // 상단 앱 바 + 제목이 가운데 정렬 형태
                title = { Text("비밀번호 변경") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->
        Column( // 세로 배치 레이아웃
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RoundedCard(modifier = Modifier.fillMaxWidth()) { // 인증 완료 안내
                Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.height(10.dp))
                Text("인증이 완료되었습니다", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("새로운 비밀번호를 설정해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("새 비밀번호", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField( // 새 비밀번호 입력 필드
                value = pw1, // 현재 입력한 값 -> pw1
                onValueChange = { pw1 = it }, // 현재 입력되는 값을 pw1에 업데이트
                placeholder = "새 비밀번호를 입력하세요",
                containerColor = AppFieldColor,
                isPassword = true // 비밀번호이므로, **** 형태로 표시
            )

            Text("비밀번호 확인", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField( // 비밀번호 확인 필드
                value = pw2, // 현재 입력한 값 -> pw2
                onValueChange = { pw2 = it }, // 현재 입력되는 값을 pw2에 업데이트
                placeholder = "비밀번호를 다시 입력하세요",
                containerColor = AppFieldColor,
                isPassword = true // 비밀번호 이므로, **** 형태로 표시
            )

            if (pw1 != pw2) { // 입력 필드에 입력한 두 password가 일치하지 않으면
                Text(
                    text = "비밀번호가 일치하지 않습니다", // 에러 메시지 출력
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (error != null) { // DB에서 에러가 발생했으면
                Text(
                    text = error!!, // 에러 메시지를 출력
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            PrimaryPillButton( // 비밀번호 변경 완료 버튼
                text = "비밀번호 변경 완료",
                onClick = {
                    if (pw1 != pw2) { // 두 비밀번호가 일치하지 않으면
                        return@PrimaryPillButton // 함수를 종료 -> DB 호출 X
                        // 코틀린에서의 람다 함수 종료는
                        // return 이 아니라 return@레이블명 형태
                        // 그래서 return@PrimaryPillButton인 것임

                    }
                    viewModel.resetPassword( // 비밀번호 변경 DB 호출
                        oobCode = oobCode, // 이메일 링크에서 받은 인증 코드를 전달
                        newPassword = pw1, // 새 비밀번호를 전달
                        onSuccess = onComplete, // 비밀번호 변경 성공 시, 완료 화면으로 이동
                        onFail = {} // 비밀번호 변경 실패 시, 별도 처리는 없음
                    )
                },
                enabled = !loading, // 로딩 중일 때 중복 클릭 방지를 위해 비활성화
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}