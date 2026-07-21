package com.learnsy2.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.learnsy2.app.ui.theme.LearnsyTheme
import com.learnsy2.app.ui.toast.ToastHost
import com.learnsy2.app.ui.nav.LearnsyNavHost
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore tương đương localStorage.getItem('learnsy_dark') trong index.html gốc
val android.content.Context.darkModeDataStore by preferencesDataStore(name = "learnsy_prefs")
private val DARK_MODE_KEY = booleanPreferencesKey("learnsy_dark")

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            val scope = rememberCoroutineScope()

            // ═══ Dark mode pre-init ═══
            // Tương đương IIFE ở đầu index.html gốc: đọc giá trị lưu (nếu có),
            // nếu chưa có thì fallback theo prefers-color-scheme của hệ thống.
            // null = đang đọc DataStore, chưa quyết định theme -> tránh flash sai màu.
            var isDarkTheme by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                context.darkModeDataStore.data
                    .map { prefs -> prefs[DARK_MODE_KEY] }
                    .collect { storedValue ->
                        isDarkTheme = storedValue ?: systemDark
                    }
            }

            // Chưa xác định theme xong thì không vẽ UI (tránh flash sai giao diện,
            // đúng tinh thần của đoạn script chạy trước mọi thứ khác trong bản web)
            val resolvedDark = isDarkTheme
            if (resolvedDark == null) return@setContent

            fun toggleDarkMode(newValue: Boolean) {
                isDarkTheme = newValue
                scope.launch {
                    context.darkModeDataStore.edit { prefs ->
                        prefs[DARK_MODE_KEY] = newValue
                    }
                }
            }

            LearnsyTheme(isDarkTheme = resolvedDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        LearnsyNavHost(isDarkTheme = resolvedDark, onToggleDarkMode = ::toggleDarkMode)

                        // ToastHost phủ lên trên cùng — hiển thị toast bất kể màn hình nào đang mở,
                        // tương đương #toastContainer cố định trong <body> của bản web.
                        ToastHost(dark = resolvedDark, modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter))
                    }
                }
            }
        }
    }
}
