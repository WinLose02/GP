package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.RoundedCard
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var pw1 by remember { mutableStateOf("") } // 새 비밀번호 값
    var pw2 by remember { mutableStateOf("") } // 비밀번호 확인 입력값

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("비밀번호 변경") },
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RoundedCard(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CheckCircle, null)
                Spacer(Modifier.height(10.dp))
                Text("인증이 완료되었습니다", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("새로운 비밀번호를 설정해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("새 비밀번호", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = pw1,
                onValueChange = { pw1 = it },
                placeholder = "새 비밀번호를 입력하세요",
                containerColor = AppFieldColor
            )

            Text("비밀번호 확인", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = pw2,
                onValueChange = { pw2 = it },
                placeholder = "비밀번호를 다시 입력하세요",
                containerColor = AppFieldColor
            )

            PrimaryPillButton(
                text = "비밀번호 변경 완료",
                onClick = onComplete, // 더미 완료
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}