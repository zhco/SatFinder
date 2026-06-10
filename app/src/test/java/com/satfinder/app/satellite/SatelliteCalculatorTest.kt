package com.satfinder.app.satellite

import org.junit.Test
import kotlin.math.abs

/**
 * 卫星位置计算算法验证测试
 * 使用ECF矢量法精确计算值验证
 */
class SatelliteCalculatorTest {

    companion object {
        private const val EPSILON = 0.5
    }

    @Test
    fun testChinaSat9C_Beijing() {
        // 北京: 39.9042°N, 116.4074°E -> 中星9C: 92.2°E
        val az = SatelliteCalculator.calculateAzimuth(39.9042, 116.4074, 92.2)
        val el = SatelliteCalculator.calculateElevation(39.9042, 116.4074, 92.2)
        println("北京 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")
        // 方位角应在西南方向 (180~270)
        assert(az in 180.0..270.0) { "方位角应在180~270之间, 实际: $az" }
        assert(el > 20.0 && el < 60.0) { "仰角应在20~60之间, 实际: $el" }
    }

    @Test
    fun testChinaSat9C_Changsha() {
        // 长沙: 28.1544°N, 112.9716°E -> 中星9C: 92.2°E
        // ECF矢量法精确值: Az≈218.80°, El≈49.94°
        val az = SatelliteCalculator.calculateAzimuth(28.1544, 112.9716, 92.2)
        val el = SatelliteCalculator.calculateElevation(28.1544, 112.9716, 92.2)
        println("长沙 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")
        assert(abs(az - 218.80) < EPSILON) { "方位角偏差过大: 预期218.80, 实际$az" }
        assert(abs(el - 49.94) < EPSILON) { "仰角偏差过大: 预期49.94, 实际$el" }
    }

    @Test
    fun testChinaSat9C_Shanghai() {
        val az = SatelliteCalculator.calculateAzimuth(31.2304, 121.4737, 92.2)
        val el = SatelliteCalculator.calculateElevation(31.2304, 121.4737, 92.2)
        println("上海 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")
        assert(az in 200.0..260.0) { "方位角应在200~260之间, 实际: $az" }
        assert(el > 35.0 && el < 60.0) { "仰角应在35~60之间, 实际: $el" }
    }

    @Test
    fun testChinaSat9C_Guangzhou() {
        val az = SatelliteCalculator.calculateAzimuth(23.1291, 113.2644, 92.2)
        val el = SatelliteCalculator.calculateElevation(23.1291, 113.2644, 92.2)
        println("广州 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")
        assert(az in 200.0..260.0) { "方位角应在200~260之间, 实际: $az" }
        assert(el > 45.0 && el < 75.0) { "仰角应在45~75之间, 实际: $el" }
    }

    @Test
    fun testChinaSat9C_Urumqi() {
        val az = SatelliteCalculator.calculateAzimuth(43.8256, 87.6168, 92.2)
        val el = SatelliteCalculator.calculateElevation(43.8256, 87.6168, 92.2)
        println("乌鲁木齐 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")
        // 乌鲁木齐经度87.6 < 卫星92.2, 卫星在东方偏南
        assert(az in 140.0..190.0) { "方位角应在140~190之间, 实际: $az" }
        assert(el > 25.0 && el < 50.0) { "仰角应在25~50之间, 实际: $el" }
    }

    @Test
    fun testSatelliteDirectlySouth() {
        // 北京 116.4°E, 中星6B 115.5°E (几乎正南)
        val az = SatelliteCalculator.calculateAzimuth(39.9042, 116.4074, 115.5)
        println("北京 -> 中星6B(115.5°E): 方位角=${"%.2f".format(az)}°")
        assert(abs(az - 180.0) < 5.0) { "卫星几乎正南时方位角应接近180°, 实际: $az" }
    }

    @Test
    fun testSatelliteBelowHorizon() {
        val el = SatelliteCalculator.calculateElevation(39.9042, 116.4074, -97.0)
        println("北京 -> Galaxy 19(97°W): 仰角=${"%.2f".format(el)}°")
        assert(el < 0) { "不可见卫星仰角应为负值, 实际: $el" }
    }

    @Test
    fun testPolarizationAngle() {
        val pol = SatelliteCalculator.calculatePolarizationAngle(39.9042, 116.4074, 92.2)
        println("北京 -> 中星9C: 极化角=${"%.2f".format(pol)}°")
        assert(abs(pol) < 90.0) { "极化角绝对值应小于90°, 实际: $pol" }
    }

    @Test
    fun testSatelliteToScreenPosition() {
        val pos = SatelliteCalculator.satelliteToScreenPosition(
            satAzimuth = 180.0, satElevation = 30.0,
            deviceAzimuth = 180.0, devicePitch = 30.0,
            screenWidth = 1080, screenHeight = 1920,
            horizontalFOV = 68.0, verticalFOV = 42.0
        )
        assert(pos != null) { "卫星应在视场内" }
        assert(abs(pos!!.first - 540f) < 50f) { "X坐标应接近屏幕中心" }
        assert(abs(pos.second - 960f) < 50f) { "Y坐标应接近屏幕中心" }
        println("屏幕坐标映射验证通过: (${pos.first}, ${pos.second})")
    }

    @Test
    fun testSatelliteToScreenPosition_OutOfView() {
        val pos = SatelliteCalculator.satelliteToScreenPosition(
            satAzimuth = 0.0, satElevation = 45.0,
            deviceAzimuth = 180.0, devicePitch = 0.0,
            screenWidth = 1080, screenHeight = 1920,
            horizontalFOV = 68.0, verticalFOV = 42.0
        )
        assert(pos == null) { "卫星在设备背后时不应在视场内" }
        println("视场外验证通过")
    }
}
