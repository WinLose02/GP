package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.RoundedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindIdResultScreen(
    foundEmail: String,
    onClose: () -> Unit,
    onGoLogin: () -> Unit,
    onGoFindPw: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, null)
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
                    Text("이메일을 찾았습니다.")
                }
            }

            RoundedCard(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Email, null)
                Spacer(Modifier.height(10.dp))
                Text("회원님의 이메일은", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(foundEmail, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text("입니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            PrimaryPillButton(
                text = "로그인하러 가기",
                onClick = onGoLogin,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onGoFindPw,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("비밀번호 찾기") }
        }
    }
}