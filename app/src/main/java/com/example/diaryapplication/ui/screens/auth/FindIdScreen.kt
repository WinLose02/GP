package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindIdScreen(
    onBack: () -> Unit,
    onFound: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var birth by remember { mutableStateOf("연도-월-일") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("아이디 찾기") },
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
            Text("가입 시 등록한 정보를 입력해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("이름", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "홍길동",
                containerColor = AppFieldColor
            )

            Text("생년월일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = birth,
                onValueChange = {},
                placeholder = "연도-월-일",
                containerColor = AppFieldColor,
                readOnly = true,
                trailing = { Icon(Icons.Outlined.DateRange, null) }
            )

            Spacer(Modifier.height(8.dp))

            PrimaryPillButton(
                text = "아이디 찾기",
                onClick = onFound, // 더미: 찾았다고 가정
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}