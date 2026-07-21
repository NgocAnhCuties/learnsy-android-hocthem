package com.learnsy2.app.ui.listening

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy2.app.ui.dashboard.DashboardIcon
import com.learnsy2.app.ui.quiz.quizColors
import com.learnsy2.app.ui.theme.NunitoFontFamily

@Composable
fun ListeningListScreen(
    items: List<ListeningItem>,
    loading: Boolean,
    loadError: Boolean,
    dark: Boolean,
    onBack: () -> Unit,
    onOpenItem: (ListeningItem) -> Unit,
    isOffline: Boolean = false,
    downloadedIds: Set<String> = emptySet(),
    onDownloadItem: (String) -> Unit = {}
) {
    val C = quizColors(dark)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.headerBg)
                .padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(C.navBtn, RoundedCornerShape(50))
                    .border(1.5.dp, C.navBtnBorder, RoundedCornerShape(50))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DashboardIcon(name = "chevronLeft", size = 11.dp, color = C.navBtnText)
                Text(text = "Quay lại", fontSize = 12.sp, fontWeight = FontWeight.Black, color = C.navBtnText, fontFamily = NunitoFontFamily)
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardIcon(name = "book", size = 15.dp, color = C.text)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Listening", fontSize = 14.sp, fontWeight = FontWeight.Black, color = C.text, fontFamily = NunitoFontFamily)
            }

            Spacer(modifier = Modifier.width(70.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isOffline) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1AF59E0B), RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0x40F59E0B), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardIcon(name = "wifiOff", size = 14.dp, color = Color(0xFFB45309))
                        Text(text = "Không có mạng — đang xem bài đã lưu offline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), fontFamily = NunitoFontFamily)
                    }
                }
            }

            if (loadError) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x14EF4444), RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0x40EF4444), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(text = "Không tải được danh sách Listening. Thử lại sau nhé!", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontFamily = NunitoFontFamily)
                    }
                }
            }

            if (loading) {
                items(4) { SkeletonCard(dark) }
            } else if (items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardIcon(name = "book", size = 20.dp, color = C.textMid)
                        Text(text = "Chưa có bài Listening nào. Quay lại sau nhé!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.textMid, fontFamily = NunitoFontFamily)
                    }
                }
            } else {
                items(items.size) { idx ->
                    ListeningItemCard(
                        item = items[idx],
                        index = idx,
                        dark = dark,
                        downloaded = downloadedIds.contains(items[idx].id),
                        onClick = { onOpenItem(items[idx]) },
                        onDownload = { onDownloadItem(items[idx].id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningItemCard(
    item: ListeningItem,
    index: Int,
    dark: Boolean,
    downloaded: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val C = quizColors(dark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.surfaceQ, RoundedCornerShape(18.dp))
            .border(1.5.dp, C.borderQ, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0x2EB07CF0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = (index + 1).toString(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFB07CF0), fontFamily = NunitoFontFamily)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stripHtml(item.text),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = C.text,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontFamily = NunitoFontFamily
            )
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (item.answers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x1A10B981), RoundedCornerShape(50))
                            .border(1.dp, Color(0x4D10B981), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(text = "${item.answers.size} chỗ trống", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669), fontFamily = NunitoFontFamily)
                    }
                }
                if (item.statements.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x14DC2626), RoundedCornerShape(50))
                            .border(1.dp, Color(0x47DC2626), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(text = "${item.statements.size} nhận định", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626), fontFamily = NunitoFontFamily)
                    }
                }
            }
        }

        // Nút "Tải về" thủ công — đánh dấu bài chắc chắn dùng offline được.
        // Nội dung thực tế đã tự cache sẵn khi mở danh sách (auto-cache),
        // nút này chủ yếu là tín hiệu UI + tránh học sinh lo lắng mất bài.
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (downloaded) Color(0x1A10B981) else if (dark) Color(0x14FFFFFF) else Color(0x14000000))
                .clickable(enabled = !downloaded, onClick = onDownload),
            contentAlignment = Alignment.Center
        ) {
            DashboardIcon(
                name = if (downloaded) "check" else "download",
                size = 14.dp,
                color = if (downloaded) Color(0xFF10B981) else C.textMid
            )
        }
    }
}

@Composable
private fun SkeletonCard(dark: Boolean) {
    val bg = if (dark) Color(0x0AFFFFFF) else Color(0x0A000000)
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "shimmerX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(bg, RoundedCornerShape(18.dp))
            .border(1.5.dp, if (dark) Color(0x0DFFFFFF) else Color(0x0D000000), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(30.dp).graphicsLayer { alpha = 0.5f + 0.3f * (shimmerX + 1f) / 2f }.background(bg, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.75f).height(14.dp).background(bg, RoundedCornerShape(6.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp).background(bg, RoundedCornerShape(6.dp)))
            }
        }
    }
}
