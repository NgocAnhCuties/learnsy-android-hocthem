package com.learnsy2.app.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy2.app.ui.theme.Baloo2FontFamily
import com.learnsy2.app.ui.theme.NunitoFontFamily

/**
 * ── GlitchText ──
 * Tương đương function GlitchText({children,color,liteMode}) trong dashboard.jsx.
 * Bản web dùng 2 layer text lệch màu đỏ/lam chạy clip-path animation để tạo
 * hiệu ứng "hư hình" kiểu HUD sci-fi. Compose không có clip-path animation
 * built-in dễ dùng như CSS, nên ta mô phỏng bằng cách dịch chuyển 2 layer
 * text nhẹ theo trục X với BlendMode.Screen — hiệu ứng thị giác tương đương.
 *
 * liteMode=true → chỉ render text thường (giữ hiệu năng máy yếu, đúng logic gốc).
 */
@Composable
fun GlitchText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    liteMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (liteMode) {
        Text(
            text = text,
            color = color,
            fontFamily = Baloo2FontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            modifier = modifier
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "glitch")

    // Layer 1 — lệch đỏ, dao động ngang nhẹ theo chu kỳ 4s (tương đương hud-glitch-1)
    // Giữ dạng State<Float>, KHÔNG "by" ở đây — nếu unwrap sớm thì GlitchText
    // (dùng ở tiêu đề, hiện liên tục) sẽ bị recompose lại 60 lần/giây.
    val offsetX1 = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                -2f at 800
                2f at 1600
                -1f at 2400
                1f at 3200
                0f at 4000
            }
        ),
        label = "glitchOffset1"
    )
    val alpha1 = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                1f at 0
                1f at 720
                0f at 800
                1f at 880
                1f at 4000
            }
        ),
        label = "glitchAlpha1"
    )

    // Layer 2 — lệch lam, lệch pha 150ms so với layer 1 (tương đương hud-glitch-2)
    val offsetX2 = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                2f at 800
                -2f at 1600
                1f at 2400
                -1f at 3200
                0f at 4000
            }
        ),
        label = "glitchOffset2"
    )

    Box(modifier = modifier) {
        // Layer nền — text gốc luôn hiển thị rõ
        Text(
            text = text,
            color = color,
            fontFamily = Baloo2FontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize
        )
        // Layer glitch đỏ — alpha đọc trong cùng graphicsLayer{} (draw phase),
        // KHÔNG dùng Modifier.alpha() rời vì đó là đọc eager ở composition phase.
        Text(
            text = text,
            color = Color(0xFFFF5078).copy(alpha = 0.55f),
            fontFamily = Baloo2FontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX1.value
                    alpha = alpha1.value
                    // Không dùng CompositingStrategy.Offscreen — không có blend mode
                    // đặc biệt nào cần render target riêng ở đây, mà offscreen lại
                    // tốn thêm 1 pass GPU hẳn hoi, đặc biệt nặng trên GPU Mali
                    // (MediaTek/Exynos tầm trung) so với Adreno.
                }
        )
        // Layer glitch lam
        Text(
            text = text,
            color = Color(0xFF78A0FF).copy(alpha = 0.45f),
            fontFamily = Baloo2FontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX2.value
                }
        )
    }
}

/**
 * ── HudSearchInput ──
 * Tương đương function HudSearchInput({value,onChange,dark,liteMode}) trong dashboard.jsx.
 * Ô tìm kiếm với placeholder giả "Tìm bài học cute..." có con trỏ nhấp nháy
 * (chỉ hiện khi rỗng và chưa focus — giống hệt logic gốc).
 */
@Composable
fun HudSearchInput(
    value: String,
    onChange: (String) -> Unit,
    dark: Boolean,
    liteMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val C = dashboardColors(dark)
    val showCursor = !focused && value.isEmpty()

    val borderColor = if (dark) Color(0x38F472B6) else Color(0x47F472B6)
    val iconColor = if (dark) Color(0x73F472B6) else Color(0x73C86490)

    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(
                color = C.fg,
                fontSize = 13.sp,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(C.accent),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .background(C.inputBg, RoundedCornerShape(14.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(start = 36.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            singleLine = true
        )

        // Icon kính lúp
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 13.dp)
        ) {
            DashboardIcon(name = "search", size = 15.dp, color = iconColor)
        }

        // Placeholder giả + con trỏ nhấp nháy, chỉ hiện khi rỗng & chưa focus
        if (showCursor) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tìm bài học cute",
                    color = Color(0x6BF472B6),
                    fontSize = 13.sp,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
                if (!liteMode) {
                    BlinkingCursor()
                }
                Text(
                    text = "...",
                    color = Color(0x6BF472B6),
                    fontSize = 13.sp,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }
}

/**
 * ── BlinkingCursor ──
 * Tương đương animation hud-cursor-blink: step-start 1s infinite (nhấp nháy
 * dạng bật/tắt tức thời, không mờ dần).
 *
 * State<Float> giữ nguyên (không "by"), đọc .value trong graphicsLayer{} —
 * tránh recompose lại BlinkingCursor + cha của nó mỗi frame.
 */
@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursorBlink")
    val visible = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 490
                0f at 500
                0f at 990
                1f at 1000
            }
        ),
        label = "cursorBlinkAlpha"
    )
    Box(
        modifier = Modifier
            .padding(start = 1.dp)
            .size(width = 1.5.dp, height = 13.dp)
            .graphicsLayer { alpha = visible.value }
            .background(Color(0xB3F472B6), RoundedCornerShape(1.dp))
    )
}
