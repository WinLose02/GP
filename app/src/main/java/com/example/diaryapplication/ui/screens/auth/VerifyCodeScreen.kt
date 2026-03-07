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
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.RoundedCard
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    email: String, // 인증 번호를 보낼 이메일
    // 데모용 인증번호 -> 임시
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onReEnterEmail: () -> Unit // '이메일 다시 입력' 버튼 클릭 시 실행
) {
    var code by remember { mutableStateOf("") } // 사용자가 입력한 인증번호 텍스트

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // <- 뒤로가기 화살표 아이콘
                        // 클릭 시, onBack() 싫애
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
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("인증번호가 이메일로 전송되었습니다.")
                }
            }

            RoundedCard(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Email, null)
                Spacer(Modifier.height(10.dp))
                Text("인증번호가 발송되었습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(email, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

            }

            Text("인증번호", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = code, // 현재 입력된 인증번호
                onValueChange = { code = it }, // 입력할 때마다 code 값을 업데이트
                placeholder = "6자리 인증번호를 입력하세요", // 힌트 텍스트
                containerColor = AppFieldColor
            )

            PrimaryPillButton(
                text = "인증하기",

                // 클릭 시, onVerified() 실행 -> 다음 화면으로 이동
                // 지금은 임시 데이터라 바로 통과
                // TODO: 실제 입력한 code값과 democode 값을 비교 후 통과 여부를 결정
                onClick = { onVerified() },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onReEnterEmail,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("이메일 다시 입력") }
        }
    }
}