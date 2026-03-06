package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    onBack: () -> Unit,
    onSendCode: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("비밀번호 찾기") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("가입 시 등록한 이메일을 입력해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("이메일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "example@email.com",
                containerColor = AppFieldColor
            )

            Spacer(Modifier.height(8.dp))

            PrimaryPillButton(
                text = "인증번호 받기",
                onClick = onSendCode, // 임시 데이터
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}