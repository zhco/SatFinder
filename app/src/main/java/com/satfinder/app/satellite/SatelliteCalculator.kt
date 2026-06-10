package com.satfinder.app.satellite

import com.satfinder.app.model.Satellite
import com.satfinder.app.model.SatellitePosition

/**
 * 卫星位置计算核心算法
 * 基于球面三角学和地球同步轨道参数
 */
object SatelliteCalculator {

    private const val EARTH_RADIUS = 6371.0    // 地球半径 km
    private const val ORBIT_HEIGHT = 35786.0   // 地球同步轨道高度 km
    private const val RATIO = EARTH_RADIUS / (EARTH_RADIUS + ORBIT_HEIGHT) // ≈ 0.1509

    /**
     * 计算卫星方位角 (Azimuth)
     * 从真北顺时针方向, 0°=北, 90°=东, 180°=南, 270°=西
     *
     * @param lat 用户纬度 (北纬为正, 南纬为负)
     * @param lon 用户经度 (东经为正, 西经为负)
     * @param satLon 卫星轨道经度 (东经为正, 西经为负)
     * @return 方位角 0~360°
     */
    fun calculateAzimuth(lat: Double, lon: Double, satLon: Double): Double {
        val phi = Math.toRadians(lat)
        val deltaLambda = Math.toRadians(satLon - lon)

        // 使用 atan2 自动处理象限
        val azimuth = Math.toDegrees(
            Math.atan2(
                Math.sin(deltaLambda),
                Math.tan(phi) * Math.cos(deltaLambda)
            )
        )

        // 转换为从正北顺时针 0°~360°
        return (azimuth + 360.0) % 360.0
    }

    /**
     * 计算卫星仰角 (Elevation)
     *
     * @param lat 用户纬度
     * @param lon 用户经度
     * @param satLon 卫星轨道经度
     * @return 仰角, 负值表示卫星在地平线以下
     */
    fun calculateElevation(lat: Double, lon: Double, satLon: Double): Double {
        val phi = Math.toRadians(lat)
        val deltaLambda = Math.toRadians(satLon - lon)

        // 中心角 γ 的余弦
        val cosGamma = Math.cos(deltaLambda) * Math.cos(phi)
        // 中心角 γ 的正弦
        val sinGamma = Math.sqrt(1.0 - cosGamma * cosGamma)

        if (sinGamma < 1e-10) return 90.0  // 用户恰好在卫星正下方

        val tanEl = (cosGamma - RATIO) / sinGamma
        return Math.toDegrees(Math.atan(tanEl))
    }

    /**
     * 计算极化角 (Polarization / Skew Angle)
     * 用于调整LNB极化方向
     *
     * @param lat 用户纬度
     * @param lon 用户经度
     * @param satLon 卫星轨道经度
     * @return 极化角 (度)
     */
    fun calculatePolarizationAngle(lat: Double, lon: Double, satLon: Double): Double {
        val phi = Math.toRadians(lat)
        val deltaLambda = Math.toRadians(satLon - lon)

        return Math.toDegrees(
            Math.atan(Math.sin(deltaLambda) / Math.tan(phi))
        )
    }

    /**
     * 计算完整的卫星位置信息
     */
    fun calculateSatellitePosition(
        satellite: Satellite,
        lat: Double,
        lon: Double
    ): SatellitePosition {
        val azimuth = calculateAzimuth(lat, lon, satellite.orbitLongitude)
        val elevation = calculateElevation(lat, lon, satellite.orbitLongitude)
        val polAngle = calculatePolarizationAngle(lat, lon, satellite.orbitLongitude)

        return SatellitePosition(
            satellite = satellite,
            azimuth = azimuth,
            elevation = elevation,
            polarizationAngle = polAngle,
            isVisible = elevation > 0
        )
    }

    /**
     * 计算卫星在屏幕上的位置
     * 将卫星方位角/仰角映射到相机预览的屏幕坐标
     *
     * @param satAzimuth 卫星真方位角
     * @param satElevation 卫星仰角
     * @param deviceAzimuth 设备当前方位角
     * @param devicePitch 设备当前俯仰角
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @param horizontalFOV 相机水平视场角 (度)
     * @param verticalFOV 相机垂直视场角 (度)
     * @return 屏幕坐标 (x, y), null表示不在视场内
     */
    fun satelliteToScreenPosition(
        satAzimuth: Double,
        satElevation: Double,
        deviceAzimuth: Double,
        devicePitch: Double,
        screenWidth: Int,
        screenHeight: Int,
        horizontalFOV: Double,
        verticalFOV: Double
    ): Pair<Float, Float>? {
        // 计算卫星相对于设备视线中心的角差
        var dAz = satAzimuth - deviceAzimuth
        // 处理跨越0°/360°的情况
        if (dAz > 180) dAz -= 360
        if (dAz < -180) dAz += 360
        val dEl = satElevation - devicePitch

        // 判断是否在视场范围内
        if (Math.abs(dAz) > horizontalFOV / 2) return null
        if (dEl < -verticalFOV / 2 || dEl > verticalFOV / 2) return null

        // 映射到屏幕坐标
        val x = (screenWidth / 2.0 + (dAz / (horizontalFOV / 2.0)) * (screenWidth / 2.0)).toFloat()
        val y = (screenHeight / 2.0 - (dEl / (verticalFOV / 2.0)) * (screenHeight / 2.0)).toFloat()

        return Pair(x, y)
    }

    /**
     * 计算卫星在屏幕边缘的箭头指示方向
     * 当卫星不在视场内时，在屏幕边缘显示方向箭头
     *
     * @return 箭头在屏幕边缘的位置 (x, y) 和旋转角度
     */
    fun getEdgeArrowPosition(
        satAzimuth: Double,
        satElevation: Double,
        deviceAzimuth: Double,
        devicePitch: Double,
        screenWidth: Int,
        screenHeight: Int,
        margin: Float = 60f
    ): Triple<Float, Float, Float> {
        var dAz = satAzimuth - deviceAzimuth
        if (dAz > 180) dAz -= 360
        if (dAz < -180) dAz += 360
        val dEl = satElevation - devicePitch

        // 归一化角度差到 -1~1 范围
        val normAz = (dAz / 180.0).coerceIn(-1.0, 1.0)
        val normEl = (dEl / 90.0).coerceIn(-1.0, 1.0)

        // 计算箭头在边缘的位置
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        val halfW = screenWidth / 2f - margin
        val halfH = screenHeight / 2f - margin

        val x = cx + normAz.toFloat() * halfW
        val y = cy - normEl.toFloat() * halfH

        // 箭头旋转角度 (指向卫星方向)
        val angle = Math.toDegrees(Math.atan2(-dEl, dAz)).toFloat()

        return Triple(
            x.coerceIn(margin, screenWidth - margin),
            y.coerceIn(margin, screenHeight - margin),
            angle
        )
    }
}
