package com.example.diaryapplication.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,

    secondary = BluePrimaryDark,
    onSecondary = Color.White,

    tertiary = BluePrimary,
    onTertiary = Color.White,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = AppCardColor,
    onSurface = TextPrimary,

    surfaceVariant = AppFieldColor,
    onSurfaceVariant = TextSecondary,

    outline = Color(0xFFD1D6DB),
    outlineVariant = Color(0xFFE5E8EB)
)

val ChatBubbleGray = Color(0xFFE9EBEF)
val ChatInputGray = Color(0xFFF1F3F6)

@Composable
fun DiaryApplicationTheme(
    darkTheme: Boolean = false, // 다크 모드 여부
    content: @Composable () -> Unit
) {
    val colors = LightColors // 현재는 라이트 모드만

    CompositionLocalProvider(
        LocalSpacing provides AppSpacing() // Dimens.kt 파일에서 staticCompositionLocalOf 사용
    ) {
        MaterialTheme(
            colorScheme = colors, // 색상
            typography = AppTypography, // 폰트
            shapes = AppShapes, // 모양
            content = content // 앱 전체 화면
        )
    }
}