# ═══ Learnsy — R8 / ProGuard rules ═══
# Bật isMinifyEnabled=true giúp giảm kích thước APK + tải class nhanh hơn khi
# cold-start, đáng chú ý trên CPU yếu / flash storage chậm (máy MediaTek,
# Exynos tầm trung phổ biến). Các rule dưới đây giữ lại phần dễ vỡ do R8 xoá
# nhầm ở các thư viện dùng reflection/codegen: kotlinx.serialization, Ktor,
# Supabase-kt, Coil.
#
# ⚠️ QUAN TRỌNG: hãy build + chạy thử bản release thật trên máy/emulator
# trước khi phát hành — R8 không compile được ở môi trường tạo file này nên
# chưa verify được 100%. Nếu gặp crash liên quan network/serialize sau khi
# bật minify, thường là do thiếu 1 rule keep cho model class cụ thể — thêm
# rule "-keep class <package>.<ClassName> { *; }" cho class đó.

# ── kotlinx.serialization ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.learnsy2.app.**$$serializer { *; }
-keepclassmembers class com.learnsy2.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.learnsy2.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Ktor (client engine dùng reflection để chọn implementation) ──
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.android.** { *; }

# ── slf4j — Ktor/Supabase kéo theo slf4j-api làm facade logging, nhưng
# không có implementation binder cụ thể (slf4j-simple/logback) nên
# StaticLoggerBinder không tồn tại lúc runtime — bình thường, slf4j tự
# fallback NOPLogger, không phải bug. R8 mặc định chặn cứng khi thấy class
# bị thiếu, cần khai rõ để bỏ qua cảnh báo này.
-dontwarn org.slf4j.**

# ── Supabase-kt (postgrest/gotrue/realtime/storage — models qua @Serializable) ──
-keep class io.github.jantennert.supabase.** { *; }
-dontwarn io.github.jantennert.supabase.**

# ── Coil ──
-dontwarn coil.**

# ── Model / data classes của app dùng để (de)serialize JSON — giữ nguyên field ──
-keepclassmembers class com.learnsy2.app.data.** {
    <fields>;
}
-keepclassmembers class com.learnsy2.app.ui.**.* {
    <fields>;
}

# ── Coroutines ──
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
