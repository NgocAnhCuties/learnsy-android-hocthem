package com.learnsy2.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.learnsy2.app.data.AuthRepository
import com.learnsy2.app.data.LessonRepository
import com.learnsy2.app.data.OfflineCacheStore
import com.learnsy2.app.data.QuizResultRepository
import com.learnsy2.app.ui.auth.StudentLoginScreen
import com.learnsy2.app.ui.dashboard.DashboardScreen
import com.learnsy2.app.ui.dashboard.HistDetailModal
import com.learnsy2.app.ui.dashboard.HistoryEntry
import com.learnsy2.app.ui.listening.ListeningPracticeScreen
import com.learnsy2.app.ui.loading.GameLoadingScreen
import com.learnsy2.app.ui.quiz.Lesson
import com.learnsy2.app.ui.quiz.PerQuestionResult
import com.learnsy2.app.ui.quiz.PwGateModal
import com.learnsy2.app.ui.quiz.QuizPlayerScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val android.content.Context.sessionDataStore by preferencesDataStore(name = "learnsy_session")
private val USERNAME_KEY = stringPreferencesKey("session_username")
private val STUDENT_ID_KEY = stringPreferencesKey("session_student_id")

private sealed class AppRoute {
    object Login : AppRoute()
    object Dashboard : AppRoute()
    data class Quiz(val lesson: Lesson) : AppRoute()
    object Listening : AppRoute()
    // Route trung gian: hiện GameLoadingScreen trước khi vào Quiz/Listening
    // thật, thay vì chuyển màn ngay lập tức không hiệu ứng gì.
    data class Transitioning(val target: AppRoute, val label: String) : AppRoute()
}

/**
 * ── LearnsyNavHost ──
 * Tương đương function App() trong app.jsx. Điều hướng Login → Dashboard →
 * Quiz/Listening bằng state đơn giản.
 *
 * CHƯA convert: lưu quiz result thật lên Supabase bảng quiz_results —
 * hiện chỉ lưu tạm in-memory, mất khi app bị kill. Cần schema cột thật
 * của bảng quiz_results để nối đúng.
 */
@Composable
fun LearnsyNavHost(
    isDarkTheme: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository() }
    val offlineCache = remember { OfflineCacheStore(context.applicationContext as android.app.Application) }
    val lessonRepo = remember { LessonRepository(offlineCache) }
    val quizResultRepo = remember { QuizResultRepository() }

    var lessonsIsOffline by remember { mutableStateOf(false) }
    var downloadedLessonIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var route by remember { mutableStateOf<AppRoute>(AppRoute.Login) }
    var username by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf<String?>(null) }
    var checkingSession by remember { mutableStateOf(true) }

    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var perQuestionByLesson by remember { mutableStateOf<Map<String, List<PerQuestionResult>>>(emptyMap()) }
    var histDetailEntry by remember { mutableStateOf<HistoryEntry?>(null) }

    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var lessonsLoading by remember { mutableStateOf(true) }
    var lessonsError by remember { mutableStateOf(false) }

    // Bài học đang chờ mở khóa bằng mật khẩu (hiển thị PwGateModal trước khi vào Quiz)
    var pendingUnlockLesson by remember { mutableStateOf<Lesson?>(null) }

    // Trạng thái nút refresh trên header Dashboard — tải lại lessons + lịch
    // sử làm bài từ Supabase mà không cần thoát app hay đăng nhập lại.
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.sessionDataStore.data.first()
        val savedUsername = prefs[USERNAME_KEY]
        if (!savedUsername.isNullOrBlank()) {
            username = savedUsername
            studentId = prefs[STUDENT_ID_KEY]
            route = AppRoute.Dashboard
        }
        checkingSession = false
    }

    var historyLoaded by remember { mutableStateOf(false) }

    // Tải danh sách bài học thật từ Supabase khi vào Dashboard lần đầu
    LaunchedEffect(route) {
        if (route == AppRoute.Dashboard && lessons.isEmpty() && lessonsLoading) {
            downloadedLessonIds = offlineCache.downloadedLessonIds()
            try {
                lessons = lessonRepo.fetchLessons()
                lessonsIsOffline = lessonRepo.lastFetchWasFromCache
                lessonsError = false
            } catch (e: Exception) {
                android.util.Log.e("LearnsyLessons", "Failed to fetch lessons", e)
                lessonsError = true
            }
            lessonsLoading = false
        }

        // Tải lịch sử làm bài thật từ Supabase (trước đây chỉ giữ in-memory
        // nên bị mất/ghi đè mỗi khi app khởi động lại).
        if (route == AppRoute.Dashboard && !historyLoaded && !studentId.isNullOrBlank()) {
            historyLoaded = true
            try {
                val (h, perQ) = quizResultRepo.fetchHistory(studentId!!)
                history = h
                perQuestionByLesson = perQuestionByLesson + perQ
            } catch (e: Exception) {
                android.util.Log.e("LearnsyHistory", "Failed to fetch history", e)
            }
        }
    }

    if (checkingSession) {
        GameLoadingScreen(dark = isDarkTheme, label = "Đang khởi động...")
        return
    }

    when (val r = route) {
        is AppRoute.Login -> {
            StudentLoginScreen(
                dark = isDarkTheme,
                onLoginSuccess = { u, sid ->
                    username = u
                    studentId = sid
                    scope.launch {
                        context.sessionDataStore.edit { prefs ->
                            prefs[USERNAME_KEY] = u
                            sid?.let { prefs[STUDENT_ID_KEY] = it }
                        }
                    }
                    route = AppRoute.Dashboard
                },
                onCheckLogin = { u, p -> authRepo.checkLogin(u, p) }
            )
        }

        is AppRoute.Dashboard -> {
            DashboardScreen(
                username = username,
                studentId = studentId,
                lessons = lessons,
                loading = lessonsLoading,
                fetchError = lessonsError,
                history = history,
                dark = isDarkTheme,
                onDarkChange = onToggleDarkMode,
                onPlay = { lesson ->
                    if (!lesson.password.isNullOrBlank()) {
                        pendingUnlockLesson = lesson
                    } else {
                        route = AppRoute.Transitioning(AppRoute.Quiz(lesson), "Đang vào bài học...")
                    }
                },
                onClearHistory = { history = emptyList() },
                onHistDetail = { entry -> histDetailEntry = entry },
                onOpenListening = { route = AppRoute.Transitioning(AppRoute.Listening, "Đang tải bài Listening...") },
                isOffline = lessonsIsOffline,
                downloadedLessonIds = downloadedLessonIds,
                onDownloadLesson = { lesson ->
                    scope.launch {
                        offlineCache.markLessonDownloaded(lesson.id)
                        downloadedLessonIds = offlineCache.downloadedLessonIds()
                    }
                },
                onRefresh = {
                    if (!isRefreshing) {
                        isRefreshing = true
                        scope.launch {
                            try {
                                lessons = lessonRepo.fetchLessons()
                                lessonsIsOffline = lessonRepo.lastFetchWasFromCache
                                lessonsError = false
                            } catch (e: Exception) {
                                android.util.Log.e("LearnsyLessons", "Refresh failed", e)
                                lessonsError = true
                            }
                            if (!studentId.isNullOrBlank()) {
                                try {
                                    val (h, perQ) = quizResultRepo.fetchHistory(studentId!!)
                                    history = h
                                    perQuestionByLesson = perQuestionByLesson + perQ
                                } catch (e: Exception) {
                                    android.util.Log.e("LearnsyHistory", "Refresh failed", e)
                                }
                            }
                            downloadedLessonIds = offlineCache.downloadedLessonIds()
                            isRefreshing = false
                        }
                    }
                },
                isRefreshing = isRefreshing,
                onLogout = {
                    scope.launch { context.sessionDataStore.edit { it.clear() } }
                    username = ""
                    studentId = null
                    history = emptyList()
                    historyLoaded = false
                    route = AppRoute.Login
                }
            )

            histDetailEntry?.let { entry ->
                HistDetailModal(
                    entry = entry,
                    perQuestion = perQuestionByLesson[entry.lessonTitle] ?: emptyList(),
                    dark = isDarkTheme,
                    onClose = { histDetailEntry = null }
                )
            }

            pendingUnlockLesson?.let { lesson ->
                PwGateModal(
                    lessonTitle = lesson.title,
                    lessonPassword = lesson.password.orEmpty(),
                    dark = isDarkTheme,
                    onUnlock = {
                        pendingUnlockLesson = null
                        route = AppRoute.Transitioning(AppRoute.Quiz(lesson), "Đang vào bài học...")
                    },
                    onCancel = { pendingUnlockLesson = null }
                )
            }
        }

        is AppRoute.Quiz -> {
            QuizPlayerScreen(
                lesson = r.lesson,
                dark = isDarkTheme,
                onToggleDark = { onToggleDarkMode(!isDarkTheme) },
                onBack = { route = AppRoute.Dashboard },
                onSaveHistory = { record, perQ ->
                    // Merge theo lessonTitle (thay thế dòng cùng tên, không cộng dồn vô
                    // điều kiện) — giữ được cả 2 mục tiêu cùng lúc: (1) không tái tạo bug
                    // trùng lặp trước đó (optimistic cộng dồn + fetchHistory cộng thêm =
                    // 2 dòng giống hệt), và (2) không để "Lịch sử" trống trơn nếu
                    // saveResult()/fetchHistory() gặp lỗi mạng/API — trải nghiệm "trống
                    // hoàn toàn dù vừa làm 10/10" còn tệ hơn nhiều so với trùng lặp.
                    fun mergeOptimistic() {
                        val optimistic = HistoryEntry(
                            lessonTitle = record.lessonTitle,
                            pct = record.pct.toDouble(),
                            timestampMillis = System.currentTimeMillis(),
                            subject = r.lesson.subject,
                            total = record.total,
                            score = record.score
                        )
                        history = listOf(optimistic) + history.filter { it.lessonTitle != record.lessonTitle }
                    }

                    perQuestionByLesson = perQuestionByLesson + (record.lessonTitle to perQ)
                    mergeOptimistic()

                    scope.launch {
                        val sid = studentId
                        if (sid.isNullOrBlank()) {
                            // Không lưu lên Supabase với student_id rỗng — bài làm khi đó
                            // vẫn "thành công" theo mắt học sinh (điểm hiện ra bình thường,
                            // đã có optimistic entry ở trên) nhưng bị gắn với một học sinh
                            // rỗng nên KHÔNG bao giờ hiện lên được ở admin (admin lọc/group
                            // theo student_id thật). Đây là nguyên nhân hay gặp nhất của
                            // việc "điểm không gửi về admin" dù học sinh không thấy lỗi gì.
                            android.util.Log.e(
                                "LearnsySaveResult",
                                "Bỏ qua lưu kết quả lên Supabase vì thiếu studentId (phiên đăng nhập không có student_id) — lessonId=${r.lesson.id}"
                            )
                            return@launch
                        }
                        try {
                            quizResultRepo.saveResult(
                                studentId = sid,
                                studentName = username,
                                lessonId = r.lesson.id,
                                lessonTitle = record.lessonTitle,
                                score = record.score,
                                total = record.total,
                                perQuestion = perQ
                            )
                            // Lưu thành công — đồng bộ lại toàn bộ history từ Supabase
                            // (nguồn sự thật đầy đủ nhất, có mọi lần làm bài khác cũng vừa
                            // lưu) để thay thế optimistic entry tạm ở trên bằng dữ liệu
                            // thật, đồng thời gộp per_q của các lần làm bài cũ.
                            val (h, perQFromServer) = quizResultRepo.fetchHistory(sid)
                            history = h
                            perQuestionByLesson = perQuestionByLesson + perQFromServer
                        } catch (e: Exception) {
                            // saveResult() hoặc fetchHistory() thất bại (mất mạng, RLS
                            // chặn, lỗi API...) — GIỮ NGUYÊN optimistic entry đã thêm ở
                            // trên thay vì xoá nó đi, để học sinh vẫn thấy kết quả bài vừa
                            // làm trong Lịch sử. Bài này sẽ KHÔNG có trên admin cho tới khi
                            // lưu lại thành công — log rõ để chẩn đoán khi cần.
                            android.util.Log.e("LearnsySaveResult", "Lưu kết quả lên Supabase thất bại", e)
                        }
                    }
                }
            )
        }

        is AppRoute.Listening -> {
            ListeningPracticeScreen(
                dark = isDarkTheme,
                onBack = { route = AppRoute.Dashboard }
            )
        }

        is AppRoute.Transitioning -> {
            GameLoadingScreen(
                dark = isDarkTheme,
                durationMillis = 900,
                label = r.label,
                onFinished = { route = r.target }
            )
        }
    }
}
