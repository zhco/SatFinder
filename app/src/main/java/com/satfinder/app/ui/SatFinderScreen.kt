package com.satfinder.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satfinder.app.model.*
import com.satfinder.app.satellite.SatelliteCalculator
import com.satfinder.app.satellite.SatelliteDatabase
import com.satfinder.app.location.LocationProvider
import com.satfinder.app.sensor.OrientationProvider
import com.satfinder.app.ui.theme.*

/**
 * 主界面 - AR寻星指示器
 */
@Composable
fun SatFinderScreen(
    location: GpsLocation?,
    orientation: DeviceOrientation,
    selectedSatellite: Satellite,
    satelliteList: List<Satellite>,
    onSatelliteSelected: (Satellite) -> Unit,
    locationProvider: LocationProvider,
    orientationProvider: OrientationProvider
) {
    var showSatelliteList by remember { mutableStateOf(false) }
    var showManualLocation by remember { mutableStateOf(false) }

    // 计算选中卫星的位置
    val satPosition = remember(location, selectedSatellite) {
        if (location != null) {
            SatelliteCalculator.calculateSatellitePosition(
                selectedSatellite, location.latitude, location.longitude
            )
        } else null
    }

    // 计算所有可见卫星位置
    val visibleSatellites = remember(location) {
        if (location != null) {
            SatelliteDatabase.getVisibleSatellites(location.latitude, location.longitude)
                .map { sat ->
                    SatelliteCalculator.calculateSatellitePosition(
                        sat, location.latitude, location.longitude
                    )
                }
                .filter { it.isVisible }
                .sortedByDescending { it.elevation }
        } else emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // AR相机预览层
        ARCameraOverlay(
            modifier = Modifier.fillMaxSize(),
            orientation = orientation,
            satPosition = satPosition,
            visibleSatellites = visibleSatellites,
            location = location
        )

        // 顶部状态栏
        TopStatusBar(
            modifier = Modifier.align(Alignment.TopCenter),
            location = location,
            orientation = orientation,
            selectedSatellite = selectedSatellite,
            satPosition = satPosition
        )

        // 底部信息面板
        BottomInfoPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            satPosition = satPosition,
            selectedSatellite = selectedSatellite,
            location = location,
            visibleCount = visibleSatellites.size,
            onToggleList = { showSatelliteList = !showSatelliteList },
            onManualLocation = { showManualLocation = true }
        )

        // 卫星选择列表弹窗
        if (showSatelliteList) {
            SatelliteListDialog(
                satelliteList = satelliteList,
                currentLocation = location,
                selectedSatellite = selectedSatellite,
                onSatelliteSelected = {
                    onSatelliteSelected(it)
                    showSatelliteList = false
                },
                onDismiss = { showSatelliteList = false }
            )
        }

        // 手动输入坐标弹窗
        if (showManualLocation) {
            ManualLocationDialog(
                locationProvider = locationProvider,
                onDismiss = { showManualLocation = false }
            )
        }
    }
}

/**
 * AR相机预览叠加层
 */
@Composable
fun ARCameraOverlay(
    modifier: Modifier = Modifier,
    orientation: DeviceOrientation,
    satPosition: SatellitePosition?,
    visibleSatellites: List<SatellitePosition>,
    location: GpsLocation?
) {
    Canvas(modifier = modifier.background(Color.Black)) {
        val w = size.width
        val h = size.height

        // 绘制半透明背景（模拟相机预览）
        drawRect(color = Color(0xFF0D1117))

        // 绘制地平线参考
        val horizonY = h / 2f - (orientation.pitch / 45f) * (h / 2f)
        if (horizonY in 0f..h) {
            drawLine(
                color = Color(0x40FFFFFF),
                start = Offset(0f, horizonY),
                end = Offset(w, horizonY),
                strokeWidth = 1f
            )
        }

        // 绘制指南针刻度
        val directions = listOf(
            "N" to 0f, "NE" to 45f, "E" to 90f, "SE" to 135f,
            "S" to 180f, "SW" to 225f, "W" to 270f, "NW" to 315f
        )
        directions.forEach { (label, angle) ->
            var diff = angle - orientation.azimuth
            if (diff > 180) diff -= 360
            if (diff < -180) diff += 360
            val x = w / 2f + (diff / 90f) * (w / 2f)
            if (x in 0f..w) {
                val tickLen = if (label.length == 1) 20f else 10f
                drawLine(
                    color = if (label == "N") AccentRed else Color(0x60FFFFFF),
                    start = Offset(x, 0f),
                    end = Offset(x, tickLen),
                    strokeWidth = if (label == "N") 3f else 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x - 10f,
                    38f,
                    android.graphics.Paint().apply {
                        color = if (label == "N") android.graphics.Color.RED else android.graphics.Color.parseColor("#80FFFFFF")
                        textSize = if (label.length == 1) 40f else 24f
                        isAntiAlias = true
                    }
                )
            }
        }

        // 绘制十字准心
        val cx = w / 2f
        val cy = h / 2f
        drawLine(
            color = Color(0x40FFFFFF),
            start = Offset(cx - 30f, cy),
            end = Offset(cx + 30f, cy),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0x40FFFFFF),
            start = Offset(cx, cy - 30f),
            end = Offset(cx, cy + 30f),
            strokeWidth = 1f
        )
        drawCircle(
            color = Color(0x40FFFFFF),
            radius = 5f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )

        // 绘制仰角刻度（左侧）
        for (el in listOf(0, 15, 30, 45, 60, 75)) {
            val y = h / 2f - (el / 45f) * (h / 2f)
            if (y in 20f..(h - 20f)) {
                drawLine(
                    color = Color(0x30FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(15f, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${el}°",
                    18f,
                    y - 5f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#60FFFFFF")
                        textSize = 20f
                        isAntiAlias = true
                    }
                )
            }
        }

        // ===== 雷达图：显示所有可见卫星的方位角和仰角 =====
        drawRadarView(visibleSatellites, satPosition, w, h)

        // ===== 绘制选中卫星标记或方向箭头 =====
        satPosition?.let { pos ->
            drawSatelliteIndicator(pos, orientation, w, h, true)
        }

        // 绘制其他可见卫星标记
        visibleSatellites.forEach { pos ->
            if (pos.satellite.isDefault) return@forEach
            drawSatelliteIndicator(pos, orientation, w, h, false)
        }
    }
}

/**
 * 绘制卫星指示器 - 在视场内显示标记，不在视场内显示方向箭头
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSatelliteIndicator(
    pos: SatellitePosition,
    orientation: DeviceOrientation,
    w: Float,
    h: Float,
    isSelected: Boolean
) {
    val hFOV = 68.0
    val vFOV = 42.0

    val screenPos = SatelliteCalculator.satelliteToScreenPosition(
        satAzimuth = pos.azimuth,
        satElevation = pos.elevation,
        deviceAzimuth = orientation.azimuth.toDouble(),
        devicePitch = orientation.pitch.toDouble(),
        screenWidth = w.toInt(),
        screenHeight = h.toInt(),
        horizontalFOV = hFOV,
        verticalFOV = vFOV
    )

    val markerColor = if (isSelected) AccentYellow else AccentCyan

    if (screenPos != null) {
        // ===== 卫星在视场内 - 绘制标记 =====
        val (sx, sy) = screenPos
        val markerRadius = if (isSelected) 24f else 14f

        // 引导线（从中心到卫星）
        if (isSelected) {
            drawLine(
                color = GuideLineColor,
                start = Offset(w / 2f, h / 2f),
                end = Offset(sx, sy),
                strokeWidth = 2f
            )
        }

        // 外圈发光效果
        drawCircle(
            color = markerColor.copy(alpha = 0.3f),
            radius = markerRadius + 8f,
            center = Offset(sx, sy)
        )

        // 外圈
        drawCircle(
            color = markerColor,
            radius = markerRadius,
            center = Offset(sx, sy),
            style = Stroke(width = if (isSelected) 3f else 2f)
        )
        // 内圈实心
        drawCircle(
            color = markerColor,
            radius = 6f,
            center = Offset(sx, sy)
        )

        // 标签
        if (isSelected) {
            val label = "${pos.satellite.name}  ${"%.1f".format(pos.azimuth)}° / ${"%.1f".format(pos.elevation)}°"
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = 32f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(label, sx + markerRadius + 12f, sy - 5f, paint)
        }
    } else {
        // ===== 卫星不在视场内 - 绘制方向箭头 =====
        drawDirectionArrow(pos, orientation, w, h, isSelected)
    }
}

/**
 * 绘制方向箭头 - 始终指向卫星方向
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDirectionArrow(
    pos: SatellitePosition,
    orientation: DeviceOrientation,
    w: Float,
    h: Float,
    isSelected: Boolean
) {
    // 计算卫星相对于设备的方向
    var dAz = pos.azimuth - orientation.azimuth
    if (dAz > 180) dAz -= 360
    if (dAz < -180) dAz += 360
    val dEl = pos.elevation - orientation.pitch

    val cx = w / 2f
    val cy = h / 2f

    // 箭头距离中心的距离
    val arrowDist = kotlin.math.min(w, h) * 0.35f

    // 将角度差转换为屏幕坐标偏移
    val dx = dAz / 90f * arrowDist
    val dy = -dEl / 45f * arrowDist

    // 归一化到边缘
    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
    val maxDist = arrowDist
    val scale = if (dist > maxDist) maxDist / dist else 1f

    val arrowX = cx + dx * scale
    val arrowY = cy + dy * scale

    val arrowColor = if (isSelected) AccentYellow else AccentCyan

    // 绘制从中心到箭头的虚线
    if (isSelected) {
        drawLine(
            color = arrowColor.copy(alpha = 0.3f),
            start = Offset(cx, cy),
            end = Offset(arrowX, arrowY),
            strokeWidth = 2f
        )
    }

    // 绘制大箭头
    val arrowSize = if (isSelected) 30f else 18f
    val angleRad = kotlin.math.atan2(arrowY - cy, arrowX - cx)
    val cosA = kotlin.math.cos(angleRad).toFloat()
    val sinA = kotlin.math.sin(angleRad).toFloat()

    // 箭头主体
    val tipX = arrowX + arrowSize * cosA
    val tipY = arrowY + arrowSize * sinA
    val baseX = arrowX - arrowSize * cosA
    val baseY = arrowY - arrowSize * sinA

    // 绘制三角形箭头
    val perpX = -sinA * arrowSize * 0.5f
    val perpY = cosA * arrowSize * 0.5f

    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseX + perpX, baseY + perpY)
            lineTo(baseX - perpX, baseY - perpY)
            close()
        },
        color = arrowColor
    )

    // 选中卫星显示名称和距离
    if (isSelected) {
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = 28f
            isAntiAlias = true
        }
        val label = "${pos.satellite.name} ${"%.0f".format(pos.azimuth)}° ${"%.0f".format(pos.elevation)}°"
        drawContext.canvas.nativeCanvas.drawText(
            label,
            arrowX - 60f,
            arrowY + (if (arrowY > cy) 35f else -15f),
            textPaint
        )

        // 显示"向左转/向右转"提示
        val turnText = when {
            dAz < -15 -> "<< 向左转 ${"%.0f".format(-dAz)}°"
            dAz > 15 -> "向右转 ${"%.0f".format(dAz)}° >>"
            dEl < -10 -> "向上抬 ${"%.0f".format(-dEl)}°"
            dEl > 10 -> "向下压 ${"%.0f".format(dEl)}°"
            else -> "对准了!"
        }
        val turnPaint = android.graphics.Paint().apply {
            color = if (turnText == "对准了!") android.graphics.Color.GREEN else android.graphics.Color.parseColor("#FFEA00")
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            turnText,
            cx - 80f,
            cy + 80f,
            turnPaint
        )
    }
}

/**
 * 顶部状态栏
 */
@Composable
fun TopStatusBar(
    modifier: Modifier = Modifier,
    location: GpsLocation?,
    orientation: DeviceOrientation,
    selectedSatellite: Satellite,
    satPosition: SatellitePosition?
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC000000)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SatelliteAlt,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedSatellite.name,
                        color = AccentYellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (satPosition?.isVisible == true) "可见" else "不可见",
                    color = if (satPosition?.isVisible == true) AccentGreen else AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (location != null) {
                Text(
                    text = "GPS: %.4f, %.4f | 精度: %.0fm".format(location.latitude, location.longitude, location.accuracy),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "正在获取GPS位置...",
                    color = AccentOrange,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 雷达图：在AR视图右上角显示所有可见卫星的方位角分布
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadarView(
    visibleSatellites: List<SatellitePosition>,
    satPosition: SatellitePosition?,
    w: Float,
    h: Float
) {
    val radarRadius = 70f
    val cx = w - radarRadius - 20f
    val cy = radarRadius + 80f

    // 半透明背景圆
    drawCircle(
        color = Color(0x30FFFFFF),
        radius = radarRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0x20FFFFFF),
        radius = radarRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )

    // 同心圆（仰角参考线）
    for (r in listOf(radarRadius * 0.33f, radarRadius * 0.66f)) {
        drawCircle(
            color = Color(0x15FFFFFF),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 0.5f)
        )
    }

    // 十字线
    drawLine(
        color = Color(0x20FFFFFF),
        start = Offset(cx - radarRadius, cy),
        end = Offset(cx + radarRadius, cy),
        strokeWidth = 0.5f
    )
    drawLine(
        color = Color(0x20FFFFFF),
        start = Offset(cx, cy - radarRadius),
        end = Offset(cx, cy + radarRadius),
        strokeWidth = 0.5f
    )

    // 方向标签
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#80FFFFFF")
        textSize = 18f
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("N", cx - 5f, cy - radarRadius - 4f, paint)
    drawContext.canvas.nativeCanvas.drawText("S", cx - 5f, cy + radarRadius + 14f, paint)
    drawContext.canvas.nativeCanvas.drawText("E", cx + radarRadius + 4f, cy + 5f, paint)
    drawContext.canvas.nativeCanvas.drawText("W", cx - radarRadius - 12f, cy + 5f, paint)

    // 标题
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#B0BEC5")
        textSize = 20f
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("卫星雷达", cx - 30f, cy - radarRadius - 22f, titlePaint)

    // 绘制卫星点
    visibleSatellites.forEach { pos ->
        // 将方位角映射到雷达图坐标
        // 方位角0°=北(上), 90°=东(右), 180°=南(下), 270°=西(左)
        val azRad = Math.toRadians(pos.azimuth - 90) // 调整使0°(北)在上方
        // 仰角越高，点越靠近中心
        val dist = radarRadius * (1.0f - (pos.elevation.toFloat() / 90.0f))

        val sx = cx + dist * Math.cos(azRad).toFloat()
        val sy = cy + dist * Math.sin(azRad).toFloat()

        val isSelected = satPosition != null && pos.satellite.name == satPosition.satellite.name
        val dotColor = if (isSelected) AccentYellow else AccentCyan
        val dotRadius = if (isSelected) 6f else 3.5f

        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(sx, sy)
        )

        // 选中卫星显示名称
        if (isSelected) {
            val namePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = 18f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f°".format(pos.azimuth),
                sx + 8f,
                sy - 4f,
                namePaint
            )
        }
    }
}

/**
 * 手动输入经纬度对话框
 */
@Composable
fun ManualLocationDialog(
    locationProvider: LocationProvider,
    onDismiss: () -> Unit
) {
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    // 预填一些常用城市坐标
    val cityPresets = listOf(
        "北京" to "39.9042,116.4074",
        "上海" to "31.2304,121.4737",
        "广州" to "23.1291,113.2644",
        "成都" to "30.5728,104.0668",
        "乌鲁木齐" to "43.8256,87.6168"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "手动输入位置",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "输入您的经纬度坐标（北纬为正，东经为正）",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it; errorText = "" },
                    label = { Text("纬度", color = TextSecondary) },
                    placeholder = { Text("例: 39.9042", color = TextDim) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextDim
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it; errorText = "" },
                    label = { Text("经度", color = TextSecondary) },
                    placeholder = { Text("例: 116.4074", color = TextDim) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextDim
                    )
                )

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = errorText, color = AccentRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("快速选择城市:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                cityPresets.forEach { (city, coords) ->
                    TextButton(
                        onClick = {
                            val parts = coords.split(",")
                            latText = parts[0]
                            lonText = parts[1]
                            errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$city ($coords)",
                            color = AccentCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val lat = latText.toDoubleOrNull()
                val lon = lonText.toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    locationProvider.setManualLocation(lat, lon)
                    onDismiss()
                } else {
                    errorText = "请输入有效的经纬度（纬度-90~90，经度-180~180）"
                }
            }) {
                Text("确定", color = AccentGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

/**
 * 底部信息面板
 */
@Composable
fun BottomInfoPanel(
    modifier: Modifier = Modifier,
    satPosition: SatellitePosition?,
    selectedSatellite: Satellite,
    location: GpsLocation?,
    visibleCount: Int,
    onToggleList: () -> Unit,
    onManualLocation: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xCC000000)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (satPosition != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DataColumn(
                            label = "方位角",
                            value = "%.1f°".format(satPosition.azimuth),
                            icon = Icons.Default.Explore,
                            color = AccentGreen
                        )
                        DataColumn(
                            label = "仰角",
                            value = "%.1f°".format(satPosition.elevation),
                            icon = Icons.Default.Height,
                            color = AccentBlue
                        )
                        DataColumn(
                            label = "极化角",
                            value = "%.1f°".format(satPosition.polarizationAngle),
                            icon = Icons.Default.RotateRight,
                            color = AccentOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "轨道: ${selectedSatellite.orbitLongitude}°${if (selectedSatellite.orbitLongitude >= 0) "E" else "W"}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "频段: ${selectedSatellite.band}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "极化: ${selectedSatellite.polarization}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "覆盖: ${selectedSatellite.region}",
                        color = TextDim,
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        text = "等待GPS定位以计算卫星参数...",
                        color = AccentOrange,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onManualLocation,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("手动输入经纬度", color = AccentCyan, fontSize = 13.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onToggleList,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentCyan
                )
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("卫星列表 (${visibleCount}颗可见)", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onManualLocation,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentOrange
                )
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("手动定位", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DataColumn(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

/**
 * 卫星选择列表弹窗
 */
@Composable
fun SatelliteListDialog(
    satelliteList: List<Satellite>,
    currentLocation: GpsLocation?,
    selectedSatellite: Satellite,
    onSatelliteSelected: (Satellite) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择卫星",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                satelliteList.forEach { satellite ->
                    val elevation = if (currentLocation != null) {
                        SatelliteCalculator.calculateElevation(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            satellite.orbitLongitude
                        )
                    } else null

                    val azimuth = if (currentLocation != null) {
                        SatelliteCalculator.calculateAzimuth(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            satellite.orbitLongitude
                        )
                    } else null

                    SatelliteListItem(
                        satellite = satellite,
                        elevation = elevation,
                        azimuth = azimuth,
                        isSelected = satellite.name == selectedSatellite.name,
                        onClick = { onSatelliteSelected(satellite) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = AccentCyan)
            }
        }
    )
}

@Composable
private fun SatelliteListItem(
    satellite: Satellite,
    elevation: Double?,
    azimuth: Double?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0x40FF9100) else DarkCard
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = satellite.name,
                    color = if (isSelected) AccentOrange else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Row {
                    Text(
                        text = "${satellite.orbitLongitude}°${if (satellite.orbitLongitude >= 0) "E" else "W"}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = satellite.band,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = satellite.polarization,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            if (elevation != null && azimuth != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "仰角 %.1f°".format(elevation),
                        color = if (elevation > 0) AccentGreen else AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "方位 %.1f°".format(azimuth),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
