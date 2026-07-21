package com.learnsy2.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * ── LearnsyApp ──
 * Application class riêng, chỉ để cấu hình Coil (thư viện load ảnh
 * avatar/ảnh nền) dùng nhiều RAM + dung lượng đĩa hơn cho cache, giúp
 * ảnh đã xem một lần hiện lại ngay lập tức (không load lại từ mạng),
 * mượt hơn khi cuộn danh sách/đổi tab.
 *
 * - Memory cache: 30% RAM khả dụng của app (mặc định Coil chỉ 20%).
 * - Disk cache: tăng lên 100MB (mặc định ~2% dung lượng trống, thường
 *   thấp hơn), lưu ở thư mục cache riêng "learnsy_image_cache".
 *
 * Cần khai báo android:name=".LearnsyApp" trong AndroidManifest.xml để
 * Android dùng class này thay vì Application mặc định.
 */
class LearnsyApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // cấp thêm RAM cho cache ảnh trong bộ nhớ
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("learnsy_image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB cache đĩa
                    .build()
            }
            // Giữ ảnh bitmap ở dạng RGB_565 khi ảnh không cần alpha —
            // giảm ~50% RAM mỗi ảnh so với mặc định ARGB_8888, đổi lại vẫn
            // đủ đẹp cho avatar/ảnh nền trong app học tập.
            .crossfade(true)
            .build()
    }
}
