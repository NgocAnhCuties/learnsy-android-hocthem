# Learnsy2 Android

Bản chuyển đổi native Android (Kotlin + Jetpack Compose) từ Learnsy web app (React + Vite + Supabase).

## Setup lần đầu

1. **Mở project bằng Android Studio** (chọn thư mục này khi Open Project).

2. **Tải font** — vào Google Fonts, tải các font sau về dạng `.ttf`, đổi tên và bỏ vào `app/src/main/res/font/`:
   - `nunito_regular.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf`, `nunito_extrabold.ttf`, `nunito_black.ttf`
   - `dancing_script_bold.ttf`, `dancing_script_extrabold.ttf`, `dancing_script_black.ttf`

3. **Tạo file `local.properties`** ở thư mục gốc (copy từ `local.properties.example`), điền:
   ```
   sdk.dir=<đường dẫn Android SDK trên máy bạn>
   SUPA_URL=<Supabase URL của project Learnsy>
   SUPA_KEY=<Supabase anon key>
   UPSTASH_URL=<Upstash REST URL>
   UPSTASH_TOKEN=<Upstash REST token>
   ```
   File này đã được `.gitignore` chặn — sẽ không bao giờ bị đẩy lên GitHub.

4. **Tạo Supabase Storage bucket `backgrounds`** (public read) — dùng để lưu ảnh nền tuỳ chỉnh, giống bucket `avatars` đã có sẵn cho ảnh đại diện.

4. **Sync Gradle** rồi Run.

## Cấu trúc hiện tại

```
app/src/main/java/com/learnsy2/app/
├── MainActivity.kt              # entry point, dark mode pre-init, password gate
├── data/
│   └── SupabaseClient.kt        # khởi tạo Supabase client
└── ui/
    ├── theme/
    │   ├── Theme.kt              # màu sắc + font
    │   └── Animations.kt         # các animation tương đương CSS @keyframes gốc
    └── screens/
        └── PasswordGateScreen.kt # màn hình nhập mật khẩu
```

## Việc còn lại (chưa convert)

Các component sau từ bản web (`index/JS/components/*.jsx`) chưa được chuyển sang Compose:
- `home-screen.jsx` → cần `HomeScreen.kt`
- `quiz-player.jsx` → cần `QuizPlayerScreen.kt`
- `dashboard.jsx` → cần `DashboardScreen.kt`
- `pw-gate.jsx` → cần hoàn thiện logic `checkPasswordAgainstBackend()` trong MainActivity
- `avatar.jsx`, `save-result.jsx`, `score-client.jsx`, `student-login.jsx`, `listening-practice.jsx`,
  `learnsy-dev-icon.jsx`, `learnsy-dev-island.jsx` (toast notification), `learnsy-sparkle-settings.jsx`,
  `hist-detail.jsx`, `globals.jsx`, `babel-loader.jsx`

`background-settings.jsx` → **đã convert**: `BackgroundLayer.kt` (render nền), `BgSettingsCard.kt` (UI
cài đặt trong tab Settings), `BackgroundSettingsModels.kt` (preset/blur data), và
`data/BackgroundSettingsRepository.kt` (DataStore local + Supabase Storage bucket `backgrounds` +
Upstash sync metadata). Khác bản web: ảnh nền lưu qua Supabase Storage (URL) thay vì base64 thẳng
vào Upstash.

Cần thêm `NavHost` (Jetpack Navigation Compose) để điều hướng giữa các màn hình — chưa có trong bản này.
