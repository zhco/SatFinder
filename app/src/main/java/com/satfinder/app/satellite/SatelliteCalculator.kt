package com.satfinder.app.satellite

import com.satfinder.app.model.Satellite
import com.satfinder.app.model.SatellitePosition
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 卫星位置计算核心算法
 * 使用ECF矢量法（地心固定坐标系）计算方位角和仰角
 * 参考: CelesTrak (Dr. T.S. Kelso) 和 Australian Space Academy
 */
object SatelliteCalculator {

    // 使用WGS84椭球参数
    private const val EARTH_RADIUS = 6378.137   // 地球赤道半径 km
    private const val ORBIT_RADIUS = 42164.0     // 地球同步轨道半径 km (地球半径+35786km)

    /**
     * 使用ECF矢量法计算卫星方位角 (Azimuth)
     * 从真北顺时针方向, 0°=北, 90°=东, 180°=南, 270°=西
     *
     * 原理: 在观测者本地ENU(东-北-天)坐标系中分解卫星方向矢量,
     * 方位角 = atan2(East, North)
     *
     * @param lat 用户纬度 (北纬为正, 南纬为负)
     * @param lon 用户经度 (东经为正, 西经为负)
     * @param satLon 卫星轨道经度 (东经为正, 西经为负)
     * @return 方位角 0~360°
     */
    fun calculateAzimuth(lat: Double, lon: Double, satLon: Double): Double {
        val phi = Math.toRadians(lat)
        val theta = Math.toRadians(lon)
        val lambda = Math.toRadians(satLon)

        // 卫星在ECF坐标系中的位置
        val sx = ORBIT_RADIUS * cos(lambda)
        val sy = ORBIT_RADIUS * sin(lambda)
        val sz = 0.0

        // 观测者在ECF坐标系中的位置
        val ox = EARTH_RADIUS * cos(phi) * cos(theta)
        val oy = EARTH_RADIUS * cos(phi) * sin(theta)
        val oz = EARTH_RADIUS * sin(phi)

        // 距离矢量
        val rx = sx - ox
        val ry = sy - oy
        val rz = sz - oz

        // 在ENU坐标系中分解
        val east = rx * (-sin(theta)) + ry * cos(theta)
        val north = rx * (-sin(phi) * cos(theta)) + ry * (-sin(phi) * sin(theta)) + rz * cos(phi)

        // 方位角 = atan2(East, North)
        val azimuth = Math.toDegrees(atan2(east, north))
        return (azimuth + 360.0) % 360.0
    }

    /**
     * 使用ECF矢量法计算卫星仰角 (Elevation)
     *
     * @param lat 用户纬度
     * @param lon 用户经度
     * @param satLon 卫星轨道经度
     * @return 仰角, 负值表示卫星在地平线以下
     */
    fun calculateElevation(lat: Double, lon: Double, satLon: Double): Double {
        val phi = Math.toRadians(lat)
        val theta = Math.toRadians(lon)
        val lambda = Math.toRadians(satLon)

        // 卫星在ECF坐标系中的位置
        val sx = ORBIT_RADIUS * cos(lambda)
        val sy = ORBIT_RADIUS * sin(lambda)
        val sz = 0.0

        // 观测者在ECF坐标系中的位置
        val ox = EARTH_RADIUS * cos(phi) * cos(theta)
        val oy = EARTH_RADIUS * cos(phi) * sin(theta)
        val oz = EARTH_RADIUS * sin(phi)

        // 距离矢量
        val rx = sx - ox
        val ry = sy - oy
        val rz = sz - oz

        // 在ENU坐标系中分解
        val east = rx * (-sin(theta)) + ry * cos(theta)
        val north = rx * (-sin(phi) * cos(theta)) + ry * (-sin(phi) * sin(theta)) + rz * cos(phi)
        val up = rx * cos(phi) * cos(theta) + ry * cos(phi) * sin(theta) + rz * sin(phi)

        // 仰角 = atan2(Up, sqrt(East² + North²))
        val horizontal = sqrt(east * east + north * north)
        return Math.toDegrees(atan2(up, horizontal))
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
        // 使用观测者经度 - 卫星经度
        val B = Math.toRadians(lon - satLon)

        // 标准极化角公式
        val pol = Math.toDegrees(atan2(sin(B), tan(phi)))
        return pol
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
        var dAz = satAzimuth - deviceAzimuth
        if (dAz > 180) dAz -= 360
        if (dAz < -180) dAz += 360
        val dEl = satElevation - devicePitch

        if (Math.abs(dAz) > horizontalFOV / 2) return null
        if (dEl < -verticalFOV / 2 || dEl > verticalFOV / 2) return null

        val x = (screenWidth / 2.0 + (dAz / (horizontalFOV / 2.0)) * (screenWidth / 2.0)).toFloat()
        val y = (screenHeight / 2.0 - (dEl / (verticalFOV / 2.0)) * (screenHeight / 2.0)).toFloat()

        return Pair(x, y)
    }

    /**
     * 计算卫星在屏幕边缘的箭头指示方向
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

        val normAz = (dAz / 180.0).coerceIn(-1.0, 1.0)
        val normEl = (dEl / 90.0).coerceIn(-1.0, 1.0)

        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        val halfW = screenWidth / 2f - margin
        val halfH = screenHeight / 2f - margin

        val x = cx + normAz.toFloat() * halfW
        val y = cy - normEl.toFloat() * halfH

        val angle = Math.toDegrees(Math.atan2(-dEl, dAz)).toFloat()

        return Triple(
            x.coerceIn(margin, screenWidth - margin),
            y.coerceIn(margin, screenHeight - margin),
            angle
        )
    }
}
