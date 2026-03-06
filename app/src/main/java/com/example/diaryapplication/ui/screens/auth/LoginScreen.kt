package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onGoSignUp: () -> Unit,
    onGoFindId: () -> Unit,
    onGoFindPw: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val backgroundGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEAF3FF),
                Color(0xFFF6FAFF),
                Color(0xFFFFFFFF)
            )
        )
    }

    // Card 디자인 요소
    val outerShape = RoundedCornerShape(40.dp)
    val cardShape = RoundedCornerShape(24.dp)
    val fieldShape = RoundedCornerShape(14.dp)

    val primaryBlue = Color(0xFF2F7CF6)
    val fieldBg = Color(0xFFF1F3F5)
    val subtleLine = Color(0xFFE6EAF0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(outerShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, subtleLine, outerShape)
                .padding(horizontal = 22.dp, vertical = 26.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(primaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "감정 다이어리",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1320)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "오늘 하루를 기록해보세요",
                fontSize = 14.sp,
                color = Color(0xFF7A8798)
            )

            Spacer(Modifier.height(18.dp))

            ElevatedCard( // 그림자가 있는 카드
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("이메일", fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2A3A))
                    Spacer(Modifier.height(8.dp))
                    myTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "example@email.com",
                        containerColor = fieldBg,
                        shape = fieldShape
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("비밀번호", fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2A3A))
                    Spacer(Modifier.height(8.dp))
                    myTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "비밀번호를 입력하세요",
                        containerColor = fieldBg,
                        shape = fieldShape,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            // TODO: 여기에 실제 로그인 검증 부분 넣기
                            onLoginSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onGoFindId, contentPadding = PaddingValues(0.dp)) {
                            Text("아이디 찾기", color = Color(0xFF8C98A8))
                        }
                        Text("  |  ", color = Color(0xFFCFD6DF))
                        TextButton(onClick = onGoFindPw, contentPadding = PaddingValues(0.dp)) {
                            Text("비밀번호 찾기", color = Color(0xFF8C98A8))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onGoSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true) // ✅ deprecated 경고 방지
            ) {
                Text("회원가입", color = Color(0xFF0B1320), fontWeight = FontWeight.SemiBold)
            }

        }
    }
}

// 로그인 입력 전용 창
@Composable
private fun myTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    containerColor: Color,
    shape: RoundedCornerShape,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = shape,
        placeholder = { Text(placeholder, color = Color(0xFF98A5B5)) },
        singleLine = true,
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color(0xFF2F7CF6)
        )
    )
}