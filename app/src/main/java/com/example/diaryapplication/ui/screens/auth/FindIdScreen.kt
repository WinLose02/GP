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
import com.example.diaryapplication.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindIdScreen(
    viewModel : AuthViewModel = viewModel(), // DB와 통신하기 위한 ViewModel
    onBack: () -> Unit, // 뒤로 가기 눌렀을때 실행할 동작
    onFound: (String) -> Unit // 이메일을 찾았을때 결과화면으로 이동하기 위한 동작
) {
    // 값이 바뀌면 화면이 자동으로 업데이트 되어야 하기 때문에 mutableStateOf
    // 화면이 재구성돼도 값은 유지해야 하므로 remeber
    var name by remember { mutableStateOf("") }
    var birth by remember { mutableStateOf("연도-월-일") }

    // DB와 통신하는 과정 중에서 로딩 중 및 에러 메시지를 실시간으로 받아오기 위함
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // 캘린더 다이얼로그를 위한 변수
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState()


    // 화면 기본 틀 디자인
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
                onValueChange = { name = it }, // 입력 될때마다 name값을 업데이트
                placeholder = "홍길동", // 힌트 메시지
                containerColor = AppFieldColor
            )

            Text("생년월일", style = MaterialTheme.typography.labelMedium)
            SoftOutlinedTextField(
                value = birth,
                onValueChange = {}, // 입력이 불가 -> 생년월일은 캘린더에서 체크
                placeholder = "연도-월-일",
                containerColor = AppFieldColor,
                readOnly = true,
                trailing = {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Outlined.DateRange, null)
                    }
                } // 오른쪽 끝에 달력 아이콘을 표시
            )

            Spacer(Modifier.height(8.dp))

            // 에러가 발생하면 에러 메시지를 표시
            if (error != null) { // 이 페이지에서는 입력한 아이디가 없을 경우
                Text(
                    text = "일치하는 정보를 찾을 수 없습니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom=4.dp)
                )
            }
            PrimaryPillButton(
                text = "아이디 찾기",
                onClick = { // 버튼 클릭 시 findEmail()을 호출
                    viewModel.findEmail(
                        name = name,
                        birthDate = birth,
                        onResult = { foundEmail -> // firebase에서 찾은 이메일값을 받아
                            if(foundEmail != null) { // 그 이메일값이 있으면 -> 아이디 찾기 결과가 있으면
                                onFound(foundEmail) // 결과 화면으로 이동
                            }
                        }
                    )
                },
                enabled = !loading, // 중복 클릭 방지를 위해 버튼을 비활성화
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if(showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val dateform = java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            java.util.Locale.getDefault()
                        )
                        birth = dateform.format(java.util.Date(millis))
                    }
                    showPicker = false
                }) { Text("완료")}
            },
            dismissButton =  {
                TextButton(onClick = { showPicker = false}) { Text("취소")}
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}