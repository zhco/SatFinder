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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

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

    val satPosition = remember(location, selectedSatellite) {
        if (location != null) {
            SatelliteCalculator.calculateSatellitePosition(
                selectedSatellite, location.latitude, location.longitude
            )
        } else null
    }

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

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            TopBar(selectedSatellite, satPosition, orientation)

            // 中央罗盘区域
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CompassView(
                    modifier = Modifier.fillMaxSize(),
                    orientation = orientation,
                    satPosition = satPosition
                )

                // 右侧仰角条
                ElevationBar(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    satPosition = satPosition
                )
            }

            // 底部信息面板
            BottomInfoPanel(
                satPosition = satPosition,
                selectedSatellite = selectedSatellite,
                location = location,
                visibleCount = visibleSatellites.size,
                onToggleList = { showSatelliteList = !showSatelliteList },
                onManualLocation = { showManualLocation = true }
            )
        }

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

        if (showManualLocation) {
            ManualLocationDialog(
                locationProvider = locationProvider,
                onDismiss = { showManualLocation = false }
            )
        }
    }
}

@Composable
fun TopBar(
    selectedSatellite: Satellite,
    satPosition: SatellitePosition?,
    orientation: DeviceOrientation
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedSatellite.name.split(" ")[0]} 卫星寻星",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            val statusText = if (satPosition != null) {
                val dAz = kotlin.math.abs(satPosition.azimuth - orientation.azimuth)
                val dEl = kotlin.math.abs(satPosition.elevation - orientation.pitch)
                when {
                    dAz < 3 && dEl < 3 -> "已对准目标!"
                    dAz < 10 && dEl < 10 -> "接近目标，请微调方向..."
                    else -> "转动手机对准卫星方向"
                }
            } else "等待定位..."

            Text(
                text = statusText,
                color = if (statusText == "已对准目标!") AccentGreen else AccentOrange,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CompassView(
    modifier: Modifier = Modifier,
    orientation: DeviceOrientation,
    satPosition: SatellitePosition?
) {
    // 计算卫星相对于设备的方向
    var dAz by remember { mutableStateOf(0.0) }
    var pointerX by remember { mutableStateOf(0f) }
    var pointerY by remember { mutableStateOf(0f) }
    var diffText by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val radius = kotlin.math.min(w, h) * 0.42f

            // 外圈
            drawCircle(
                color = Color(0x30FFFFFF),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )

            // 内圈
            drawCircle(
                color = Color(0x15FFFFFF),
                radius = radius * 0.85f,
                center = Offset(cx, cy),
                style = Stroke(width = 1f)
            )

            // 刻度线
            for (deg in 0 until 360 step 5) {
                val angle = Math.toRadians(deg.toDouble() - orientation.azimuth.toDouble())
                val isMajor = deg % 30 == 0
                val innerR = if (isMajor) radius * 0.78f else radius * 0.85f
                val outerR = radius * 0.92f

                val x1 = cx + innerR * cos(angle).toFloat()
                val y1 = cy + innerR * sin(angle).toFloat()
                val x2 = cx + outerR * cos(angle).toFloat()
                val y2 = cy + outerR * sin(angle).toFloat()

                drawLine(
                    color = if (isMajor) Color(0x60FFFFFF) else Color(0x30FFFFFF),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (isMajor) 2f else 1f
                )
            }

            // 中心点
            drawCircle(
                color = Color(0x40FFFFFF),
                radius = 4f,
                center = Offset(cx, cy)
            )

            // 绘制卫星指针和标记
            satPosition?.let { pos ->
                var azDiff = pos.azimuth - orientation.azimuth
                if (azDiff > 180) azDiff -= 360
                if (azDiff < -180) azDiff += 360

                val pointerAngle = Math.toRadians(azDiff)
                val pointerLen = radius * 0.55f
                val px = cx + pointerLen * cos(pointerAngle).toFloat()
                val py = cy + pointerLen * sin(pointerAngle).toFloat()

                // 指针线
                drawLine(
                    color = AccentBlue,
                    start = Offset(cx, cy),
                    end = Offset(px, py),
                    strokeWidth = 4f
                )

                // 指针头部圆点
                drawCircle(
                    color = AccentBlue,
                    radius = 8f,
                    center = Offset(px, py)
                )

                // 红色十字卫星标记
                val crossSize = 12f
                drawLine(
                    color = AccentRed,
                    start = Offset(px - crossSize, py),
                    end = Offset(px + crossSize, py),
                    strokeWidth = 3f
                )
                drawLine(
                    color = AccentRed,
                    start = Offset(px, py - crossSize),
                    end = Offset(px, py + crossSize),
                    strokeWidth = 3f
                )
            }
        }

        // 方向标签 (使用Compose Text叠加在Canvas上)
        val directions = listOf(
            Triple("北", 0f, Alignment.TopCenter),
            Triple("东", 90f, Alignment.CenterEnd),
            Triple("南", 180f, Alignment.BottomCenter),
            Triple("西", 270f, Alignment.CenterStart)
        )

        directions.forEach { (label, deg, align) ->
            val angle = Math.toRadians(deg - orientation.azimuth.toDouble())
            val labelR = 0.30f // 相对于Box宽度的比例
            val offsetX = (labelR * cos(angle)).toFloat()
            val offsetY = (labelR * sin(angle)).toFloat()

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = align
            ) {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(
                        x = (offsetX * 200).dp,
                        y = (offsetY * 200).dp
                    )
                )
            }
        }

        // 角度差文字
        satPosition?.let { pos ->
            var azDiff = pos.azimuth - orientation.azimuth
            if (azDiff > 180) azDiff -= 360
            if (azDiff < -180) azDiff += 360

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val angleRad = Math.toRadians(azDiff)
                val textR = 0.20f
                Text(
                    text = "${"%.1f".format(kotlin.math.abs(azDiff))}°",
                    color = AccentBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(
                        x = (textR * cos(angleRad) * 200).dp,
                        y = (textR * sin(angleRad) * 200).dp
                    )
                )
            }

            // 卫星名称
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val nameR = 0.38f
                Text(
                    text = pos.satellite.name.split(" ")[0],
                    color = AccentRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(
                        x = (nameR * cos(angleRad) * 200).dp,
                        y = (nameR * sin(angleRad) * 200).dp + 20.dp
                    )
                )
            }
        }
    }
}

@Composable
fun ElevationBar(
    modifier: Modifier = Modifier,
    satPosition: SatellitePosition?
) {
    Box(modifier = modifier.width(50.dp).height(200.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barWidth = w * 0.5f
            val barLeft = (w - barWidth) / 2f

            // 背景条
            drawRect(
                color = Color(0x20FFFFFF),
                topLeft = Offset(barLeft, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, h)
            )

            // 刻度线
            for (el in listOf(0, 15, 30, 45, 60, 75, 90)) {
                val y = h - (el / 90f) * h
                drawLine(
                    color = Color(0x60FFFFFF),
                    start = Offset(barLeft + barWidth, y),
                    end = Offset(barLeft + barWidth + 8f, y),
                    strokeWidth = 1f
                )
            }

            // 当前仰角指示
            satPosition?.let { pos ->
                val elY = h - (pos.elevation.toFloat() / 90f) * h
                drawRect(
                    color = AccentBlue.copy(alpha = 0.6f),
                    topLeft = Offset(barLeft, elY.coerceIn(0f, h)),
                    size = androidx.compose.ui.geometry.Size(barWidth, (h - elY).coerceIn(0f, h))
                )
                drawLine(
                    color = AccentBlue,
                    start = Offset(barLeft - 4f, elY),
                    end = Offset(barLeft + barWidth + 4f, elY),
                    strokeWidth = 2f
                )
            }
        }

        // 刻度标签
        Column(
            modifier = Modifier.fillMaxHeight().padding(start = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(90, 75, 60, 45, 30, 15, 0).forEach { el ->
                Text("${el}°", color = TextDim, fontSize = 9.sp)
            }
        }

        // 仰角数值
        satPosition?.let { pos ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    "${"%.1f".format(pos.elevation)}°",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // 标签
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text("仰角", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

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
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (satPosition != null) {
            // 三列大数字
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigNumberColumn(
                    label = "方位角",
                    value = "${"%.1f".format(satPosition.azimuth)}°",
                    color = AccentRed
                )
                BigNumberColumn(
                    label = "仰角",
                    value = "${"%.1f".format(satPosition.elevation)}°",
                    color = AccentBlue
                )
                BigNumberColumn(
                    label = "极化角",
                    value = "${"%.1f".format(satPosition.polarizationAngle)}°",
                    color = AccentYellow
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 位置信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (location != null) {
                    InfoRowItem(
                        icon = Icons.Default.LocationOn,
                        text = "${"%.5f".format(location.latitude)}°N"
                    )
                    InfoRowItem(
                        icon = Icons.Default.LocationOn,
                        text = "${"%.5f".format(location.longitude)}°E"
                    )
                    InfoRowItem(
                        icon = Icons.Default.SignalCellularAlt,
                        text = "±${"%.1f".format(location.accuracy)}m"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 方位偏差
            Text(
                text = "方位偏差: ${"%.1f".format(satPosition.azimuth)}°",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Text(
                text = "等待GPS定位...",
                color = AccentOrange,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp)
            )
            TextButton(
                onClick = onManualLocation,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("手动输入经纬度", color = AccentCyan)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 卫星信息条
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedSatellite.name.split(" ")[0]} | ${selectedSatellite.orbitLongitude}°E | ${selectedSatellite.band}",
                color = TextDim,
                fontSize = 12.sp
            )
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onToggleList,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
            ) {
                Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("卫星列表 (${visibleCount})", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onManualLocation,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange)
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("手动定位", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BigNumberColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextDim, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = text, color = TextSecondary, fontSize = 12.sp)
    }
}

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
        title = { Text("选择卫星", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())
            ) {
                satelliteList.forEach { satellite ->
                    val elevation = if (currentLocation != null) {
                        SatelliteCalculator.calculateElevation(currentLocation.latitude, currentLocation.longitude, satellite.orbitLongitude)
                    } else null
                    val azimuth = if (currentLocation != null) {
                        SatelliteCalculator.calculateAzimuth(currentLocation.latitude, currentLocation.longitude, satellite.orbitLongitude)
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
            TextButton(onClick = onDismiss) { Text("关闭", color = AccentCyan) }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0x40FF9100) else DarkCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
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
                    Text("${satellite.orbitLongitude}°${if (satellite.orbitLongitude >= 0) "E" else "W"}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(satellite.band, color = TextSecondary, fontSize = 12.sp)
                }
            }
            if (elevation != null && azimuth != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("仰角 %.1f°".format(elevation), color = if (elevation > 0) AccentGreen else AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("方位 %.1f°".format(azimuth), color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ManualLocationDialog(
    locationProvider: LocationProvider,
    onDismiss: () -> Unit
) {
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    val cityPresets = listOf(
        "北京" to "39.9042,116.4074",
        "上海" to "31.2304,121.4737",
        "广州" to "23.1291,113.2644",
        "成都" to "30.5728,104.0668",
        "乌鲁木齐" to "43.8256,87.6168"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动输入位置", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("输入经纬度坐标（北纬为正，东经为正）", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it; errorText = "" },
                    label = { Text("纬度", color = TextSecondary) },
                    placeholder = { Text("例: 39.9042", color = TextDim) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it; errorText = "" },
                    label = { Text("经度", color = TextSecondary) },
                    placeholder = { Text("例: 116.4074", color = TextDim) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = errorText, color = AccentRed, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("快速选择城市:", color = TextSecondary, fontSize = 12.sp)
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
                        Text("$city ($coords)", color = AccentCyan, fontSize = 12.sp)
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
                    errorText = "请输入有效的经纬度"
                }
            }) {
                Text("确定", color = AccentGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}
