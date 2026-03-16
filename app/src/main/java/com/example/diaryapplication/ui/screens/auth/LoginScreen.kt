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
import androidx.compose.ui.text.input.PasswordVisualTransformation // 비밀번호 노출 방지
import androidx.compose.ui.text.input.VisualTransformation // 텍스트를 그대로 보여주는 기능
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel : AuthViewModel = viewModel(), // DB와 통신하기 위한 ViewModel
    onLoginSuccess: () -> Unit, // 로그인 성공 시 메인 화면으로 이동
    onBack : () -> Unit,
    onGoSignUp: () -> Unit, // 회원가입 화면으로 이동
    onGoFindId: () -> Unit, // 아이디 찾기 화면으로 이동
    onGoFindPw: () -> Unit, // 비밀번호 찾기 화면으로 이동
) {

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)}

    var saveEmail by remember { mutableStateOf(prefs.getBoolean("save_email", false))}
    var autoLogin by remember { mutableStateOf(prefs.getBoolean("auto_login", false))}
    var email by remember { mutableStateOf(
        if(prefs.getBoolean("save_email", false)) prefs.getString("saved_email", "")?:"" else ""
            )}

    // 화면이 재구성돼도 값을 유지하기 위해 remeber
    // 초기 값은 빈 문자열이나, 값이 바뀌면 화면 자동 업데이트를 위해 mutableStateOf
    // var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ViewModel에서 로딩 중 여부, 에러 메시지를 실시간으로 받아옴
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // 배경 색의 그라데이션을 정의
    val backgroundGradient = remember { // 불필요하게 중복 생성을 막기 위해 remeber
        Brush.verticalGradient( // 위에서 아래 방향으로 그라데이션
            colors = listOf(
                Color(0xFFEAF3FF),
                Color(0xFFF6FAFF),
                Color(0xFFFFFFFF)
            )
        )
    }

    // Card 디자인 요소

    // 모서리 둥근 정도를 정의
    val outerShape = RoundedCornerShape(40.dp) // 전체 외각
    val cardShape = RoundedCornerShape(24.dp) // 내부 카드
    val fieldShape = RoundedCornerShape(14.dp) // 입력 필드

    // 색상 정의
    val primaryBlue = Color(0xFF2F7CF6) // 버튼/아이콘 -> 파란색
    val fieldBg = Color(0xFFF1F3F5) // 입력 필드 -> 회색
    val subtleLine = Color(0xFFE6EAF0) // 테투리 -> 연한 회색

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center // 가운데 정렬
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
            Box( // 상단 파란색 로고 박스
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(primaryBlue),
                contentAlignment = Alignment.Center
            ) {
                // TODO: 여기를 임시로 텍스트 형태로 바꿨지만, 벡터 이미지로 변경 예정
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
                        value = email, // 이메일 값
                        onValueChange = { email = it }, // 현재 입력되는 값을 email에 업데이트
                        placeholder = "example@email.com",
                        containerColor = fieldBg,
                        shape = fieldShape
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("비밀번호", fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2A3A))
                    Spacer(Modifier.height(8.dp))
                    myTextField(
                        value = password, // 비밀번호
                        onValueChange = { password = it }, // 현재 입력되는 값을 password에 업데이트
                        placeholder = "비밀번호를 입력하세요",
                        containerColor = fieldBg,
                        shape = fieldShape,
                        isPassword = true // 비밀번호 보안을 위해 가리기
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            // 아이디 저장 체크 여부에 따라 저장 / 삭제
                            prefs.edit().apply{
                                putBoolean("save_email", saveEmail)
                                putBoolean("auto_login", autoLogin)
                                if (saveEmail) putString("saved_email", email)
                                else remove("saved_email")
                                apply()
                            }

                            // onClick을 누르면 ViewModel의 Login() 호출
                            viewModel.login(
                                email = email,
                                password = password,
                                onSuccess = onLoginSuccess
                            )

                        },
                        enabled = !loading, // 중복 터치를 막기 위해 비활 성화

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        if(loading){ // 로딩 중이면, 흰색 스피너를 표시
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        }
                        else { // 아니면 로그인 텍스트를 표시
                            Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (error != null) { // 에러가 발생했을 경우
                        Text( // 에러 메시지를 출력
                            text = error!!,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveEmail,
                            onCheckedChange = { saveEmail = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryBlue)
                        )
                        Text ("아이디 저장", fontSize = 13.sp, color = Color(0xFF5A6A7A))

                        Spacer(Modifier.width(16.dp))

                        Checkbox(
                            checked = autoLogin,
                            onCheckedChange = { autoLogin = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryBlue)
                        )

                        Text("자동 로그인", fontSize = 13.sp, color  = Color(0xFF5A6A7A))
                    }

                    // 아이디 찾기 | 비밀번호 찾기 형태로 배치
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

            // 회원가입 버튼

            OutlinedButton(
                onClick = onGoSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
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
    isPassword : Boolean = false // 비밀번호 **** 형태로 처리
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
        visualTransformation = if (isPassword) { // 비밀번호 **** 형태로 처리
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
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