package com.example.diaryapplication.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import com.example.diaryapplication.ui.components.SoftOutlinedTextField
import com.example.diaryapplication.ui.theme.AppFieldColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }

    var gender by remember { mutableIntStateOf(0) } // 0=남성 1=여성 2=기타
    var birthText by remember { mutableStateOf("연도-월-일") }
    var showPicker by remember { mutableStateOf(false) } // 날짜 선택 달력 다이얼로그 열기/닫기 상태
    val pickerState = rememberDatePickerState() // 날짜 선택기 상태 -> 선택된 날짜를 기억

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // 뒤로가기 아이콘 클릭 시, onBack() 실행
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
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
                containerColor = AppFieldColor
            )

            Text("닉네임", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it }, // 입력 시 nickname 값 업데이트
                placeholder = "별명을 입력하세요", // 힌트 메시지
                containerColor = AppFieldColor
            )

            Text("생년월일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = birthText, // "연도-월-일"
                onValueChange = {}, // 달력 다이얼로그로 선택
                placeholder = "연도-월-일",
                containerColor = AppFieldColor,
                readOnly = true,
                trailing = { Icon(Icons.Outlined.DateRange, contentDescription = null) }, // 달력 아이콘 표시
                onClick = { showPicker = true } // 클릭 시, 날짜 선택 달력 다이얼로그 열기
            )

            Text("이메일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "example@email.com",
                containerColor = AppFieldColor
            )

            Text("비밀번호", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = pw,
                onValueChange = { pw = it },
                placeholder = "비밀번호를 입력하세요",
                containerColor = AppFieldColor
            )

            Text("비밀번호 확인", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = pw2,
                onValueChange = { pw2 = it },
                placeholder = "비밀번호를 다시 입력하세요",
                containerColor = AppFieldColor
            )

            Text("성별", style = MaterialTheme.typography.labelMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) { // 성별은 하나만 선택할 수 있음
                SegmentedButton(
                    selected = gender == 0, // gender가 0이면 이 버튼이 선택됨
                    onClick = { gender = 0 }, // 클릭 시, gender를 0으로
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) { Text("남성") }
                SegmentedButton(
                    selected = gender == 1, // gender가 1이면 이 버튼이 선택됨
                    onClick = { gender = 1 }, // 클릭 시, gender를 1로
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) { Text("여성") }
                SegmentedButton(
                    selected = gender == 2, // gender가 2이면 이 버튼이 선택됨
                    onClick = { gender = 2 }, // 클릭 시, gender를 2로
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text("기타") }
            }

            Spacer(Modifier.height(8.dp))

            PrimaryPillButton(
                text = "회원가입 완료",

                // 버튼 클릭 시, onCllick() 실행
                // TODO: 추후 여러 검증을 통해 통과시켜야 함
                // 1) 항목들이 빠지지 않았는지?
                // 2) 이메일 형식이 올바른지?
                // 3) 비밀번호 pw1==pw2 인지?
                onClick = { onComplete() },
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
                    if (millis != null) { // 선택되었으면
                        // 간단 표시(정확 포맷은 나중에 util로)
                        birthText = "선택됨"
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