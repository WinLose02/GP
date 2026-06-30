package com.example.diaryapplication.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.AuthViewModel

val greetingMessage = listOf(
    "안녕하세요,\n다시 만났네요 👋",    "오늘도\n좋은 하루 보내세요 🌤️",
    "반가워요,\n오늘 하루는 어떠셨나요? ☺️",  "어서 오세요,\n오늘의 이야기를 들려주세요 📖",
    "오늘 하루도\n수고 많으셨어요 🌿", "다시 만나서\n반가워요 ✨",
    "오늘은 어떤 감정으로\n하루를 채우셨나요? 💭",   "잠깐 쉬어가도\n괜찮아요 🍃",
    "당신의 하루가\n궁금해요 🌙",  "오늘도 와주셔서\n고마워요 😊",
    "마음 편히\n이야기해주세요 🤍", "오늘 하루도\n잘 마무리해봐요 🌙"
)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    onGoSignUp: () -> Unit,
    onGoFindId: () -> Unit,
    onGoFindPw: () -> Unit,
) {
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 상단 문구 진입 애니메이션 트리거
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val primaryBlue = Color(0xFF2F7CF6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryBlue)
    ) {
        // 상단 문구 : 상태바 아래 40dp부터 시작
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 32.dp, end = 32.dp, top = 40.dp)
        ) {
            // 타이틀 : 딜레이 없이 가장 먼저 등장
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 600)) +
                        slideInVertically(animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )) { 40 }
            ) {
                Text(
                    text = remember { greetingMessage.random() },
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 48.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            // 서브타이틀 : 타이틀보다 200ms 늦게 등장 (fade에만 delay 적용)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200)) +
                        slideInVertically(animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )) { 40 }
            ) {
                Text(
                    text = "오늘 하루를 기록해보세요",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }

        // 하단 흰색 카드 : 화면의 72%를 차지하며 하단에 고정
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.BottomCenter)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            // 폼 영역 : 스크롤 가능, 하단 회원가입 영역과 겹치지 않도록 bottom padding 확보
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 36.dp, bottom = 100.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("이메일", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF444444))
                Spacer(Modifier.height(8.dp))
                LoginTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "example@email.com"
                )

                Spacer(Modifier.height(18.dp))

                Text("비밀번호", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF444444))
                Spacer(Modifier.height(8.dp))
                LoginTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "비밀번호를 입력하세요",
                    isPassword = true
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.login(
                            email = email,
                            password = password,
                            onSuccess = onLoginSuccess
                        )
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 아이디 찾기 | 비밀번호 찾기
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onGoFindId, contentPadding = PaddingValues(0.dp)) {
                        Text("아이디 찾기", fontSize = 13.sp, color = Color(0xFF8C98A8))
                    }
                    Text("  |  ", fontSize = 13.sp, color = Color(0xFFCFD6DF))
                    TextButton(onClick = onGoFindPw, contentPadding = PaddingValues(0.dp)) {
                        Text("비밀번호 찾기", fontSize = 13.sp, color = Color(0xFF8C98A8))
                    }
                }
            }

            // 회원가입 안내 : 카드 맨 하단에 고정
            // 흰 배경을 깔아 스크롤 내용과 겹쳐 보이지 않도록 함
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = Color(0xFFF0F3F6))
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("아직 계정이 없으신가요? ", fontSize = 14.sp, color = Color(0xFF8C98A8))
                    TextButton(onClick = onGoSignUp, contentPadding = PaddingValues(0.dp)) {
                        Text("회원가입", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                    }
                }
            }
        }
    }
}

// 로그인 화면 전용 입력 필드
@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(placeholder, color = Color(0xFF98A5B5), fontSize = 14.sp) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF4F6F8),
            unfocusedContainerColor = Color(0xFFF4F6F8),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color(0xFF2F7CF6)
        )
    )
}
