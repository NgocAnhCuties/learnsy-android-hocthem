package com.learnsy2.app.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learnsy2.app.ui.quiz.Lesson
import com.learnsy2.app.ui.quiz.shuffled
import com.learnsy2.app.ui.theme.Baloo2FontFamily
import kotlinx.coroutines.launch

/**
 * ── DashboardScreen ──
 * Tương đương function DashboardEnhanced({...}) trong dashboard.jsx — điểm
 * ghép nối chính của toàn bộ Dashboard, thay cho return(...) JSX gốc.
 *
 * Props đến từ app.jsx trong bản web (student, lessons, loading, fetchError,
 * history, dark, onPlay, onLogout...) — ở đây nhận qua tham số hàm tương ứng,
 * vì Compose không có prop-drilling qua React Context giống hệt cách JSX
 * làm; NavHost cấp trên (app.jsx tương lai) sẽ truyền các callback này vào.
 *
 * Đã thêm LessonPreviewSheet (intercept onPlay để hiện bottom sheet xác
 * nhận trước khi vào quiz thật) — không tìm thấy file JSX nguồn tương ứng
 * trong repo tại thời điểm convert, UI được tái tạo lại theo ảnh chụp
 * thực tế từ bản deploy (xem ghi chú chi tiết trong LessonPreviewSheet.kt).
 * ExportSheet (xuất HTML offline) đã CHÍNH THỨC BỎ theo yêu cầu — không
 * cần thay thế bằng tính năng nào khác.
 */
@Composable
fun DashboardScreen(
    username: String,
    studentId: String?,
    lessons: List<Lesson>,
    loading: Boolean,
    fetchError: Boolean,
    history: List<HistoryEntry>,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onPlay: (Lesson) -> Unit,
    onClearHistory: () -> Unit,
    onHistDetail: (HistoryEntry) -> Unit,
    onLogout: () -> Unit,
    onOpenListening: () -> Unit = {},
    isOffline: Boolean = false,
    downloadedLessonIds: Set<String> = emptySet(),
    onDownloadLesson: (Lesson) -> Unit = {},
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    viewModel: DashboardViewModel = viewModel()
) {
    val C = dashboardColors(dark)
    val scope = rememberCoroutineScope()
    val tab by viewModel.tab.collectAsState()
    val liteMode by viewModel.liteMode.collectAsState()
    val flickerFx by viewModel.flickerFx.collectAsState()
    val shuffleQ by viewModel.shuffleQ.collectAsState()
    val shuffleA by viewModel.shuffleA.collectAsState()
    val student by viewModel.student.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val avatarLoading by viewModel.avatarLoading.collectAsState()
    val achievementQueue by viewModel.achievementQueue.collectAsState()
    val bgSettings by viewModel.bgSettings.collectAsState()
    val bgSyncState by viewModel.bgSyncState.collectAsState()
    val bgUploading by viewModel.bgUploading.collectAsState()
    val bgUploadError by viewModel.bgUploadError.collectAsState()

    // Hiển thị Toast khi upload ảnh nền thất bại (bao gồm thông báo chặn tính
    // năng "chỉ dùng được ở website") — trước đây result.msg khi ok=false
    // không có đường nào tới UI, người dùng bấm xong không thấy phản hồi gì.
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(bgUploadError) {
        bgUploadError?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearBgUploadError()
        }
    }

    // State cho LessonPreviewSheet — bấm "Học!" mở sheet xác nhận trước,
    // chỉ gọi onPlay(lesson) thật khi bấm "Bắt đầu học!" trong sheet.
    // "starting" chặn double-tap: AnimatedVisibility giữ nội dung sheet
    // (kể cả nút) trong composition suốt animation exit (~180ms) sau khi
    // previewLesson đã về null, nên nút vẫn bấm được thêm 1 lần trong lúc
    // đó nếu không có cờ này — dẫn tới onPlay() (và do đó lưu kết quả bài
    // học) có thể bị kích hoạt 2 lần cho cùng một lượt vào bài.
    var previewLesson by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Lesson?>(null) }
    var startingLesson by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(username, studentId) {
        viewModel.initForStudent(username, studentId)
    }

    LaunchedEffect(history) {
        viewModel.checkAchievements(history)
    }

    // Tương đương MutationObserver theo dõi class 'dark' trong background-settings.js
    LaunchedEffect(dark) {
        viewModel.onBgDarkModeChanged(dark)
    }

    // Tab "Trang chủ" đã được tô sáng ở thanh điều hướng dưới, nên tiêu đề
    // "Trang chủ" ở giữa topbar là thừa — thay bằng lời chào theo tên học
    // sinh để topbar có ích hơn; các tab khác vẫn giữ tiêu đề rõ ràng.
    // Tab "Trang chủ" đã tô sáng ở thanh điều hướng dưới, và hero banner
    // trong TabHome đã có lời chào theo giờ trong ngày riêng (icon động +
    // sub-text như "Ngày mới tươi đẹp nè!") — nên để trống ở đây tránh lặp
    // thông điệp 2 lần trên cùng một màn hình. Các tab khác vẫn giữ tiêu đề.
    val tabTitle = when (tab) {
        DashboardTab.HOME -> ""
        DashboardTab.STATS -> "Thống kê"
        DashboardTab.HISTORY -> "Lịch sử"
        DashboardTab.SETTINGS -> "Cài đặt"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Nền tuỳ chỉnh (gradient/ảnh + blur) — tương đương #learnsy-bg-overlay ──
        BackgroundLayer(settings = bgSettings, dark = dark, liteMode = liteMode, modifier = Modifier.fillMaxSize())

        // Nền trang trí bay lượn — tắt khi liteMode (đúng logic gốc !liteMode&&<FloatingDecos>)
        if (!liteMode) {
            FloatingDecos(dark = dark, modifier = Modifier.fillMaxSize())
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        // Gradient rất nhẹ trên→dưới thay vì nền trắng phẳng,
                        // để mềm hơn và bớt tách biệt với phần nội dung.
                        // Ở dark mode giữ nguyên C.navBg phẳng như cũ.
                        if (dark) androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(C.navBg, C.navBg)
                        ) else androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFF8FC), Color(0xFFFFF2FA))
                        )
                    )
                    // Shadow mờ phía dưới header — tách header khỏi nội dung
                    // thay vì để hai vùng nối liền không có ranh giới.
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = Color.Black.copy(alpha = 0.05f),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height - strokeWidth / 2),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                // Logo — bên trái
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    val logoFloatState = rememberTopBarFloat()
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = logoFloatState.value }
                            .size(30.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF9A8D4), Color(0xFFC084FC))
                                ),
                                androidx.compose.foundation.shape.RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        com.learnsy2.app.ui.branding.AtomBadge(
                            size = 17.dp,
                            badgeColor = Color.White,
                            backgroundColor = Color.Transparent
                        )
                    }
                    Text(
                        text = "Learnsy Plus",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = C.fg,
                        fontFamily = Baloo2FontFamily
                    )
                }

                // Tiêu đề tab — giữa
                Text(
                    text = tabTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.sub,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Refresh + Dark/Light toggle — bên phải
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    // Nút refresh — làm mới dữ liệu app (bài học, lịch sử, hồ sơ)
                    // không cần thoát app hay kéo-để-làm-mới; xoay 360° liên tục
                    // trong lúc isRefreshing=true để phản hồi trực quan.
                    val refreshRotation = if (isRefreshing) {
                        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "refreshSpin")
                        transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.LinearEasing)
                            ),
                            label = "refreshSpinValue"
                        ).value
                    } else 0f

                    val refreshInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val refreshPressed by refreshInteractionSource.collectIsPressedAsState()
                    val refreshScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (refreshPressed) 0.90f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(120),
                        label = "refreshBtnScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer { scaleX = refreshScale; scaleY = refreshScale }
                            .background(
                                if (dark) Color(0x26F59E0B) else Color(0x1AA855F7),
                                androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
                            )
                            .border(
                                1.5.dp,
                                if (dark) Color(0x4DF59E0B) else Color(0x40A855F7),
                                androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
                            )
                            .clickable(
                                enabled = !isRefreshing,
                                interactionSource = refreshInteractionSource,
                                indication = null
                            ) { onRefresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        DashboardIcon(
                            name = "refresh",
                            size = 18.dp,
                            color = if (dark) Color(0xFFF59E0B) else Color(0xFFA855F7),
                            modifier = Modifier.graphicsLayer { rotationZ = refreshRotation }
                        )
                    }

                    // Dark/Light toggle
                    val darkToggleInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val darkTogglePressed by darkToggleInteractionSource.collectIsPressedAsState()
                    val darkToggleScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (darkTogglePressed) 0.90f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(120),
                        label = "darkToggleBtnScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer { scaleX = darkToggleScale; scaleY = darkToggleScale }
                            .background(
                                if (dark) Color(0x26F59E0B) else Color(0x1AA855F7),
                                androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
                            )
                            .border(
                                1.5.dp,
                                if (dark) Color(0x4DF59E0B) else Color(0x40A855F7),
                                androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
                            )
                            .clickable(
                                interactionSource = darkToggleInteractionSource,
                                indication = null
                            ) { onDarkChange(!dark) },
                        contentAlignment = Alignment.Center
                    ) {
                        DashboardIcon(
                            name = if (dark) "sun" else "moon",
                            size = 18.dp,
                            color = if (dark) Color(0xFFF59E0B) else Color(0xFFA855F7)
                        )
                    }
                }
            }

            // ── Tab Content ──
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "dashboardTabContent"
                ) { currentTab ->
                    when (currentTab) {
                        DashboardTab.HOME -> TabHome(
                            student = student,
                            lessons = lessons,
                            loading = loading,
                            fetchError = fetchError,
                            history = history,
                            dark = dark,
                            liteMode = liteMode,
                            flickerFx = flickerFx,
                            avatarUrl = avatarUrl,
                            shuffleQ = shuffleQ,
                            shuffleA = shuffleA,
                            onShuffleQChange = viewModel::setShuffleQ,
                            onShuffleAChange = viewModel::setShuffleA,
                            onPlay = { lesson -> previewLesson = lesson; startingLesson = false },
                            onGoToStatsTab = { viewModel.setTab(DashboardTab.STATS) },
                            onOpenListening = onOpenListening,
                            isOffline = isOffline,
                            downloadedLessonIds = downloadedLessonIds,
                            onDownloadLesson = onDownloadLesson
                        )
                        DashboardTab.STATS -> TabStats(history = history, dark = dark)
                        DashboardTab.HISTORY -> TabHistory(
                            history = history,
                            dark = dark,
                            onHistDetail = onHistDetail,
                            onClearHistory = onClearHistory
                        )
                        DashboardTab.SETTINGS -> TabSettings(
                            student = student,
                            avatarUrl = avatarUrl,
                            avatarLoading = avatarLoading,
                            onAvatarUpload = { uri -> viewModel.uploadAvatarNow(uri) },
                            onAvatarRemove = { viewModel.removeAvatarNow() },
                            history = history,
                            dark = dark,
                            onDarkChange = onDarkChange,
                            shuffleQ = shuffleQ,
                            onShuffleQChange = viewModel::setShuffleQ,
                            shuffleA = shuffleA,
                            onShuffleAChange = viewModel::setShuffleA,
                            liteMode = liteMode,
                            onLiteModeChange = viewModel::setLiteMode,
                            flickerFx = flickerFx,
                            onFlickerFxChange = viewModel::setFlickerFx,
                            bgSettings = bgSettings,
                            bgSyncState = bgSyncState,
                            bgUploading = bgUploading,
                            onBgPickPreset = viewModel::pickBgPreset,
                            onBgPickBlurMode = { mode -> viewModel.pickBgBlurMode(dark, mode) },
                            onBgPickBlurPercent = { percent -> viewModel.pickBgBlurPercent(dark, percent) },
                            onBgPickImage = { uri -> scope.launch { viewModel.uploadBgImage(uri) } },
                            onBgRemoveImage = viewModel::removeBgImage,
                            onLogout = onLogout
                        )
                    }
                }
            }

            // ── Tab Bar ──
            TabBar(
                tab = tab,
                onTabChange = viewModel::setTab,
                dark = dark,
                liteMode = liteMode,
                flickerFx = flickerFx
            )
        }

        // ── Achievement Toast (bản đơn giản — xem ghi chú TODO ở đầu file) ──
        achievementQueue.firstOrNull()?.let { achievement ->
            SimpleAchievementBanner(
                achievement = achievement,
                dark = dark,
                onDismiss = viewModel::dismissTopAchievement
            )
        }

        // ── Lesson Preview Sheet ──
        LessonPreviewSheet(
            lesson = previewLesson,
            dark = dark,
            onDismiss = { previewLesson = null },
            onStart = {
                if (!startingLesson) {
                    startingLesson = true
                    val lesson = previewLesson
                    previewLesson = null
                    if (lesson != null) onPlay(lesson.shuffled(shuffleQ, shuffleA))
                }
            }
        )
    }
}

@Composable
private fun rememberTopBarFloat(): androidx.compose.runtime.State<Float> {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "topBarFloat")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2800, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "topBarFloatY"
    )
}

/**
 * ── SimpleAchievementBanner ──
 * Bản TẠM THỜI thay cho AchievementToast component thật của bản web (chưa
 * đọc được source — chỉ thấy nơi gọi). Hiện confetti + animation phức tạp
 * hơn trong bản gốc; đây là banner đơn giản đủ dùng, sẽ nâng cấp khi có
 * thêm thông tin về AchievementToast thật.
 */
@Composable
private fun SimpleAchievementBanner(
    achievement: AchievementUnlock,
    dark: Boolean,
    onDismiss: () -> Unit
) {
    val C = dashboardColors(dark)
    LaunchedEffect(achievement) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(achievement.color, achievement.color.copy(alpha = 0.8f))
                    ),
                    androidx.compose.foundation.shape.RoundedCornerShape(50)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                DashboardIcon(name = achievement.icon, size = 18.dp, color = androidx.compose.ui.graphics.Color.White)
                Text(
                    text = achievement.label,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}
