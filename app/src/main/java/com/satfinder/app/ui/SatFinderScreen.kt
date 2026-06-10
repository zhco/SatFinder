package com.satfinder.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    var showInfoPanel by remember { mutableStateOf(true) }

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
            onToggleInfo = { showInfoPanel = !showInfoPanel }
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
    }
}

/**
 * AR相机预览叠加层
 * 在相机预览上绘制卫星标记、指南针和方向指示
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
        drawHorizonLine(w, h, orientation.pitch)

        // 绘制指南针刻度
        drawCompassScale(w, h, orientation.azimuth)

        // 绘制十字准心
        drawCrosshair(w, h)

        // 绘制选中卫星标记
        satPosition?.let { pos ->
            if (pos.isVisible) {
                drawSatelliteMarker(
                    pos, orientation, w, h,
                    isSelected = true
                )
            }
        }

        // 绘制所有可见卫星标记
        visibleSatellites.forEach { pos ->
            if (pos.satellite.isDefault) return@forEach  // 跳过已绘制的默认卫星
            drawSatelliteMarker(
                pos, orientation, w, h,
                isSelected = false
            )
        }

        // 绘制方向标签
        drawDirectionLabels(w, h, orientation.azimuth)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizonLine(
    w: Float, h: Float, pitch: Float
) {
    // 地平线位置
    val horizonY = h / 2f - (pitch / 45f) * (h / 2f)

    if (horizonY in 0f..h) {
        drawLine(
            color = Color(0x40FFFFFF),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompassScale(
    w: Float, h: Float, azimuth: Float
) {
    val directions = listOf(
        "N" to 0f, "NE" to 45f, "E" to 90f, "SE" to 135f,
        "S" to 180f, "SW" to 225f, "W" to 270f, "NW" to 315f
    )

    directions.forEach { (label, angle) ->
        var diff = angle - azimuth
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        val x = w / 2f + (diff / 90f) * (w / 2f)

        if (x in 0f..w) {
            // 刻度线
            val tickLen = if (label.length == 1) 20f else 10f
            drawLine(
                color = if (label == "N") AccentRed else Color(0x60FFFFFF),
                start = Offset(x, 0f),
                end = Offset(x, tickLen),
                strokeWidth = if (label == "N") 3f else 1f
            )

            // 标签
            drawText(
                textMeasurer = androidx.compose.ui.text.TextMeasurer(
                    androidx.compose.ui.text.TextStyle(
                        fontSize = if (label.length == 1) 16.sp else 10.sp,
                        color = if (label == "N") AccentRed else Color(0x80FFFFFF)
                    )
                ),
                text = label,
                topLeft = Offset(x - 10f, 22f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrosshair(
    w: Float, h: Float
) {
    val cx = w / 2f
    val cy = h / 2f
    val len = 30f

    drawLine(
        color = Color(0x40FFFFFF),
        start = Offset(cx - len, cy),
        end = Offset(cx + len, cy),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0x40FFFFFF),
        start = Offset(cx, cy - len),
        end = Offset(cx, cy + len),
        strokeWidth = 1f
    )

    // 中心圆
    drawCircle(
        color = Color(0x40FFFFFF),
        radius = 5f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSatelliteMarker(
    pos: SatellitePosition,
    orientation: DeviceOrientation,
    w: Float,
    h: Float,
    isSelected: Boolean
) {
    // 典型手机相机视场角
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

    if (screenPos != null) {
        val (sx, sy) = screenPos
        val markerColor = if (isSelected) AccentYellow else AccentCyan
        val markerRadius = if (isSelected) 18f else 12f

        // 绘制引导线（从中心到卫星）
        if (isSelected) {
            drawLine(
                color = GuideLineColor,
                start = Offset(w / 2f, h / 2f),
                end = Offset(sx, sy),
                strokeWidth = 2f
            )
        }

        // 外圈
        drawCircle(
            color = markerColor,
            radius = markerRadius,
            center = Offset(sx, sy),
            style = Stroke(width = if (isSelected) 3f else 2f)
        )

        // 内圈
        drawCircle(
            color = markerColor,
            radius = 4f,
            center = Offset(sx, sy)
        )

        // 卫星名称标签
        val label = if (isSelected) {
            "${pos.satellite.name}\n${"%.1f".format(pos.azimuth)}° / ${"%.1f".format(pos.elevation)}°"
        } else {
            pos.satellite.name
        }

        drawText(
            textMeasurer = androidx.compose.ui.text.TextMeasurer(
                androidx.compose.ui.text.TextStyle(
                    fontSize = if (isSelected) 14.sp else 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = SatelliteLabelColor
                )
            ),
            text = label,
            topLeft = Offset(sx + markerRadius + 8f, sy - 10f)
        )
    } else {
        // 卫星不在视场内，绘制边缘箭头
        val arrow = SatelliteCalculator.getEdgeArrowPosition(
            satAzimuth = pos.azimuth,
            satElevation = pos.elevation,
            deviceAzimuth = orientation.azimuth.toDouble(),
            devicePitch = orientation.pitch.toDouble(),
            screenWidth = w.toInt(),
            screenHeight = h.toInt()
        )

        val (ax, ay, angle) = arrow
        val arrowColor = if (isSelected) AccentYellow else Color(0x80FFFFFF)

        rotate(angle) {
            drawLine(
                color = arrowColor,
                start = Offset(ax, ay),
                end = Offset(ax + 20f, ay),
                strokeWidth = 3f
            )
            // 箭头头部
            drawLine(
                color = arrowColor,
                start = Offset(ax + 20f, ay),
                end = Offset(ax + 14f, ay - 6f),
                strokeWidth = 3f
            )
            drawLine(
                color = arrowColor,
                start = Offset(ax + 20f, ay),
                end = Offset(ax + 14f, ay + 6f),
                strokeWidth = 3f
            )
        }

        // 边缘标签
        if (isSelected) {
            drawText(
                textMeasurer = androidx.compose.ui.text.TextMeasurer(
                    androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = AccentYellow,
                        fontWeight = FontWeight.Bold
                    )
                ),
                text = "${pos.satellite.name} ${"%.1f".format(pos.azimuth)}°",
                topLeft = Offset(ax - 30f, ay + 15f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDirectionLabels(
    w: Float, h: Float, azimuth: Float
) {
    // 仰角刻度（左侧）
    for (el in listOf(0, 15, 30, 45, 60, 75)) {
        val y = h / 2f - (el / 45f) * (h / 2f)
        if (y in 20f..(h - 20f)) {
            drawLine(
                color = Color(0x30FFFFFF),
                start = Offset(0f, y),
                end = Offset(15f, y),
                strokeWidth = 1f
            )
            drawText(
                textMeasurer = androidx.compose.ui.text.TextMeasurer(
                    androidx.compose.ui.text.TextStyle(
                        fontSize = 9.sp,
                        color = Color(0x60FFFFFF)
                    )
                ),
                text = "${el}°",
                topLeft = Offset(18f, y - 5f)
            )
        }
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
            // 卫星名称
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

            // 位置信息
            if (location != null) {
                Text(
                    text = "GPS: ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)} | 精度: ${"%.0f".format(location.accuracy)}m",
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
    onToggleInfo: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 卫星参数面板
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
                    // 三列数据
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

                    // 额外信息
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
                }
            }
        }

        // 底部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 卫星列表按钮
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
                        text = "仰角 ${"%.1f".format(elevation)}°",
                        color = if (elevation > 0) AccentGreen else AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "方位 ${"%.1f".format(azimuth)}°",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
