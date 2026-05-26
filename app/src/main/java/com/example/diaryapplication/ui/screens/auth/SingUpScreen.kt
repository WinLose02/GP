package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor
import com.example.diaryapplication.viewmodel.AuthViewModel
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel : AuthViewModel = viewModel(), // DB 연동을 위한 ViewModel
    onBack: () -> Unit, // 뒤로가기 버튼 클릭 시 호출
    onComplete: () -> Unit // 회원가입 완료 시, 다음 화면으로 이동
) {
    // 이름, 닉네임, 이메일, 비밀번호, 비밀번호 확인 값 상태 변수
    // 값이 바뀔때마다 화면에서 자동으로 업데이트
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }

    var gender by remember { mutableIntStateOf(0) } // 0=남성 1=여성 2=기타
    var birthText by remember { mutableStateOf("연도-월-일") }
    var showPicker by remember { mutableStateOf(false) } // 날짜 선택 달력 다이얼로그 열기/닫기 상태
    val pickerState = rememberDatePickerState() // 날짜 선택기 상태 -> 선택된 날짜를 기억

    // ViewModel에서 실시간으로 로딩 중 여부와 에러 메시지를 받아옴
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // 화면 기본 디자인 틀 생성
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // 뒤로가기 아이콘 클릭 시, onBack() 실행
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // 스크롤 가능할 수 있게
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("이름", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = name,
                onValueChange = { name = it }, // 입력 시 name 값 업데이트
                placeholder = "홍길동", // 힌트 메시지
                containerColor = AppFieldColor // 색상
            )

            Text("닉네임", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it }, // 입력 시 nickname 값 업데이트
                placeholder = "별명을 입력하세요", // 힌트 메시지
                containerColor = AppFieldColor // 색상
            )

            Text("생년월일", style = MaterialTheme.typography.labelMedium)

            SoftOutlinedTextField(
                value = birthText, // "연도-월-일"
                onValueChange = {}, // 달력 다이얼로그로 선택
                placeholder = "연도-월-일",
                containerColor = AppFieldColor, // 색상
                readOnly = true, // readOnly=false이면 키보드로 생년월일을 입력하게 됨 -> 그걸 방지
                trailing = {
                    IconButton(onClick = { showPicker = true }) { // 아이콘을 클릭 시, showPicker=true로 설정해서
                        // 캘린더 다이얼로그를 띄어줄 수 있게 해줌
                        Icon(Icons.Rounded.DateRange, contentDescription = null)
                    } // 오른쪽 끝 부분에 달력 아이콘 표시
                }
            )


            Text("이메일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = email,
                onValueChange = { email = it }, // 입력 시 email 값 업데이트
                placeholder = "example@email.com", // 힌트 메시지
                containerColor = AppFieldColor // 색상
            )

            Text("비밀번호", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = pw,
                onValueChange = { pw = it }, // 입력 시 pw값 업데이트
                placeholder = "비밀번호를 입력하세요", // 힌트 메시지
                containerColor = AppFieldColor, // 색상
                isPassword = true // 비밀번호 이므로, **** 형태로 출력
            )

            Text("비밀번호 확인", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = pw2,
                onValueChange = { pw2 = it }, // 입력 시 pw2값 업데이트
                placeholder = "비밀번호를 다시 입력하세요", // 힌트 메시지
                containerColor = AppFieldColor, // 색상
                isPassword = true // 비밀번호 이므로, **** 형태로 출력
            )

            Text("성별", style = MaterialTheme.typography.labelMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) { // 성별은 하나만 선택할 수 있음
                SegmentedButton(
                    selected = gender == 0, // gender가 0이면 이 버튼이 선택됨
                    onClick = { gender = 0 }, // 클릭 시, gender를 0으로
                    shape = SegmentedButtonDefaults.itemShape(0, 3) // 3개 버튼 중 인덱스 0 (1번째)
                ) { Text("남성") }
                SegmentedButton(
                    selected = gender == 1, // gender가 1이면 이 버튼이 선택됨
                    onClick = { gender = 1 }, // 클릭 시, gender를 1로
                    shape = SegmentedButtonDefaults.itemShape(1, 3) // 3개 버튼 중 인덱스 1 (2번째)
                ) { Text("여성") }
                SegmentedButton(
                    selected = gender == 2, // gender가 2이면 이 버튼이 선택됨
                    onClick = { gender = 2 }, // 클릭 시, gender를 2로
                    shape = SegmentedButtonDefaults.itemShape(2, 3) // 3개 버튼 중 인덱스 2 (3번째)
                ) { Text("기타") }
            }

            Spacer(Modifier.height(8.dp))

            // DB로 부터 에러 메시지를 받아오면
            if(error != null) {
                Text(
                    text = error!!, // 에러 메시지를 출력
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            PrimaryPillButton(
                text = "회원가입 완료",
                onClick = { // 회원가입 완료 버튼을 클릭 시, 실행
                    viewModel.signUp( // ViewModel의 SignUp()을 호출
                        // 입력 값들을 넘겨 DB에 저장
                        email = email,
                        password =pw,
                        name = name,
                        nickname = nickname,
                        birthDate = birthText,
                        gender = gender,
                        onSuccess = onComplete
                    )
                },
                enabled = !loading, // 로딩 중일 때 중복 클릭을 방지하기 위함
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 날짜 선택 다이얼로그
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false }, // 바깥 터치, 뒤로가기 -> 달력 다이얼로그 종료
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis // 선택된 날짜를 밀리초 단위로 가져옴
                    if (millis != null) { // 선택되었으면 날짜 포맷으로 변경
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        birthText = sdf.format(java.util.Date(millis))
                    }
                    showPicker = false // 달력 다이얼로그 닫기
                }) { Text("완료") }
            },
            dismissButton = { // 취소 클릭하면 달력 다이얼로그 닫기
                TextButton(onClick = { showPicker = false }) { Text("취소") }
            }
        ) { DatePicker(state = pickerState) }
    }
}