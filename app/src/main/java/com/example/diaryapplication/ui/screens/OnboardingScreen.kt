package com.example.diaryapplication.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diaryapplication.ui.components.PrimaryPillButton
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardPage(
    val title: String,
    val desc: String,
    val gradientStart : Color,
    val gradientEnd : Color,
    val icon : androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardPage("매일의 감정을 기록하세요", "캘린더로 쉽게 일기를 작성하고,\n날씨, 운동시간, 공부시간 등\n하루를 다양하게 기록할 수 있어요",
            Color(0xFF2B7FFF),
            Color(0xFF00D3F3),
            Icons.AutoMirrored.Rounded.MenuBook),
        OnboardPage("AI 챗봇과 대화해요", "고민이 있거나 대화가 필요할 때,\nAI 챗봇이 언제든지 당신의 이야기를\n들어드려요",
            Color(0xFFAD46FF),
            Color(0xFFFB64B6),
            Icons.AutoMirrored.Rounded.Chat),
        OnboardPage("나의 감정을 분석해요", "주간, 월간 요약으로\n나의 감정 패턴과 활동을 확인하고\n더 나은 하루를 만들어가요",
            Color(0xFFFF6900),
            Color(0xFFFDC700),
            Icons.Rounded.Insights),
        OnboardPage("안전하게 보호해요", "PIN 번호 설정으로 일기를 보호하고,\n알림 설정으로 매일 일기 작성을\n습관으로 만들어보세요",
            Color(0xFF00C950),
            Color(0xFF00D492),
            Icons.Rounded.Person),
    )

    val pager = rememberPagerState(pageCount = { pages.size }) // 현재 몇 번째 페이지인지
    val scope = rememberCoroutineScope() // 화면 전환 애니메이션
    val isLast = pager.currentPage == pages.lastIndex // 마지막 페이지인가 확인

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        TextButton(
            onClick = onFinish,
            modifier = Modifier.align(Alignment.TopEnd)
        ) { Text("건너뛰기") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 24.dp)
        ) {
            // 중앙 콘텐츠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize()
                ) { idx ->
                    val p = pages[idx]

                    /*
                    페이지[인덱스] : 1번[0]  ,  2번(현재)[1]  ,  3번[2]  , 4번[3]
                    >> papger.currentPage - idx
                       1번 페이지 : 1(현재) - 0(1번 페이지) = 1 --> 1칸 떨어짐
                       2번 페이지 : 1(현재) - 1(2번 페이지) = 0 --> 지금이 현재
                       3번 페이지 : 1(현재) - 2(3번 페이지) = -1 --> 1칸 떨어짐

                    >> paper.currentPageOffsetFraction
                       페이지가 스와이프(변경) 중일 때, 얼마나 넘겼는지 그 수치를 나타냄
                       EX) 완전히 멈춤 -> 0.0
                           반쯤 넘김 -> 0.5
                           거의 다 넘김 -> 0.9

                    >> absoluteValue : 절댓값
                    >> coerceIn(0f,1f) : 0~1 사이의 값으로 고정

                    이렇게 계산해서 pageOffset(지금 페이지에서 얼마나 멀리 있는지)를 값으로 계산
                     */
                    val pageOffset =
                        ((pager.currentPage - idx) + pager.currentPageOffsetFraction).absoluteValue
                            .coerceIn(0f, 1f)

                    /*
                        alpha : 투명도 계산
                        coerceIn(0.75f, 1f) -> 아무리 투명해도 투명도가 0.75 밑으로 내려가지 않음
                        멀리 있을 수록 투명해짐
                     */
                    val alpha = (1f - pageOffset * 0.25f).coerceIn(0.75f, 1f)

                    /*
                        translateY : 아래로 내려가는 정도
                        [EX]
                        현재 페이지 : pageOffset = 0.0 -> tranlateY = 0.0 (제자리) -> alpha = 1.0 -> 선명 + 제자리
                        다음 페이지 : pageOffset = 1.0 -> tranlateY= 18[px] (살짝 아래) -> alpha = 0.75 -> 흐릿 + 살짝 아래
                        
                        >> 아래로 내려가게 하는 이유?
                           # 3D 원근감을 주기 위함
                           # 실생활에서도 멀리 있는 물체는 가까이 있는 물체보다 아래로 보이는 것과 동일
                     */
                    val translateY = pageOffset * 18f // 멀수록 아래로 내려감

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                this.alpha = alpha
                                this.translationY = translateY
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            shadowElevation = 16.dp, // 그림자 효과 -> 높을 수록 진하게!
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(p.gradientStart, p.gradientEnd)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = p.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            p.title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            p.desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }


            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PagerDots(current = pager.currentPage, total = pages.size)

                PrimaryPillButton(
                    text = if (isLast) "시작하기" else "다음 >", // 마지막 페이지면 '다음' 버튼
                    onClick = {
                        if (isLast) onFinish() // 마지막 페이지면 로그인 페이지로 이동
                        else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } // 마지막 페이지가 아니면 다음 페이지로 이동
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// - 0 0 0
// 0 - 0 0
// 페이지 순서 막대 표현
@Composable
private fun PagerDots(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(total) { i ->
            // 현재 페이지 점은 18dp, 나머지는 6dp로 목표값을 설정
            // spring : 페이지 전환 시 도트가 통통 튀듯 확장/축소됨
            val w by animateDpAsState(
                targetValue = if (i == current) 18.dp else 6.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dotWidth"
            )
            Surface(
                color = if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(6.dp)
                    .width(w)
            ) {}
        }
    }
}