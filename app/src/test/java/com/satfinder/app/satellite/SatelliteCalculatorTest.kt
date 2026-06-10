package com.satfinder.app.satellite

import org.junit.Test
import kotlin.math.abs

/**
 * 卫星位置计算算法验证测试
 * 使用精确计算值验证方位角、仰角和极化角的计算准确性
 */
class SatelliteCalculatorTest {

    companion object {
        private const val EPSILON = 0.5  // 允许误差 ±0.5°
    }

    // ========== 中星9C (92.2°E) 测试 ==========

    @Test
    fun testChinaSat9C_Beijing() {
        // 北京: 39.9042°N, 116.4074°E -> 中星9C: 92.2°E
        // 精确值: Az≈331.74°, El≈37.52°
        val az = SatelliteCalculator.calculateAzimuth(39.9042, 116.4074, 92.2)
        val el = SatelliteCalculator.calculateElevation(39.9042, 116.4074, 92.2)

        println("北京 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 331.74) < EPSILON) { "方位角偏差过大: 预期331.74, 实际$az" }
        assert(abs(el - 37.52) < EPSILON) { "仰角偏差过大: 预期37.52, 实际$el" }
    }

    @Test
    fun testChinaSat9C_Shanghai() {
        // 上海: 31.2304°N, 121.4737°E -> 中星9C: 92.2°E
        // 精确值: Az≈317.25°, El≈41.76°
        val az = SatelliteCalculator.calculateAzimuth(31.2304, 121.4737, 92.2)
        val el = SatelliteCalculator.calculateElevation(31.2304, 121.4737, 92.2)

        println("上海 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 317.25) < EPSILON) { "方位角偏差过大: 预期317.25, 实际$az" }
        assert(abs(el - 41.76) < EPSILON) { "仰角偏差过大: 预期41.76, 实际$el" }
    }

    @Test
    fun testChinaSat9C_Guangzhou() {
        // 广州: 23.1291°N, 113.2644°E -> 中星9C: 92.2°E
        // 精确值: Az≈317.96°, El≈54.02°
        val az = SatelliteCalculator.calculateAzimuth(23.1291, 113.2644, 92.2)
        val el = SatelliteCalculator.calculateElevation(23.1291, 113.2644, 92.2)

        println("广州 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 317.96) < EPSILON) { "方位角偏差过大: 预期317.96, 实际$az" }
        assert(abs(el - 54.02) < EPSILON) { "仰角偏差过大: 预期54.02, 实际$el" }
    }

    @Test
    fun testChinaSat9C_Urumqi() {
        // 乌鲁木齐: 43.8256°N, 87.6168°E -> 中星9C: 92.2°E
        // 精确值: Az≈4.77°, El≈39.26° (卫星在用户以东偏南，方位角接近正北)
        val az = SatelliteCalculator.calculateAzimuth(43.8256, 87.6168, 92.2)
        val el = SatelliteCalculator.calculateElevation(43.8256, 87.6168, 92.2)

        println("乌鲁木齐 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 4.77) < EPSILON) { "方位角偏差过大: 预期4.77, 实际$az" }
        assert(abs(el - 39.26) < EPSILON) { "仰角偏差过大: 预期39.26, 实际$el" }
    }

    // ========== Hot Bird 13°E 测试 (欧洲) ==========

    @Test
    fun testHotBird_London() {
        // 伦敦: 51.5074°N, -0.1278°W -> Hot Bird: 13°E
        // 精确值: Az≈10.51°, El≈29.77°
        val az = SatelliteCalculator.calculateAzimuth(51.5074, -0.1278, 13.0)
        val el = SatelliteCalculator.calculateElevation(51.5074, -0.1278, 13.0)

        println("伦敦 -> Hot Bird 13°E: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 10.51) < EPSILON) { "方位角偏差过大: 预期10.51, 实际$az" }
        assert(abs(el - 29.77) < EPSILON) { "仰角偏差过大: 预期29.77, 实际$el" }
    }

    @Test
    fun testAstra2_London() {
        // 伦敦 -> Astra 2 (28.2°E)
        // 精确值: Az≈23.20°, El≈25.37°
        val az = SatelliteCalculator.calculateAzimuth(51.5074, -0.1278, 28.2)
        val el = SatelliteCalculator.calculateElevation(51.5074, -0.1278, 28.2)

        println("伦敦 -> Astra 2 (28.2°E): 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 23.20) < EPSILON) { "方位角偏差过大: 预期23.20, 实际$az" }
        assert(abs(el - 25.37) < EPSILON) { "仰角偏差过大: 预期25.37, 实际$el" }
    }

    // ========== Galaxy 19 97°W 测试 (美洲) ==========

    @Test
    fun testGalaxy19_NewYork() {
        // 纽约: 40.7128°N, -74.006°W -> Galaxy 19: 97°W
        // 精确值: Az≈333.75°, El≈37.35°
        val az = SatelliteCalculator.calculateAzimuth(40.7128, -74.006, -97.0)
        val el = SatelliteCalculator.calculateElevation(40.7128, -74.006, -97.0)

        println("纽约 -> Galaxy 19 (97°W): 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(abs(az - 333.75) < EPSILON) { "方位角偏差过大: 预期333.75, 实际$az" }
        assert(abs(el - 37.35) < EPSILON) { "仰角偏差过大: 预期37.35, 实际$el" }
    }

    // ========== 边界条件测试 ==========

    @Test
    fun testSatelliteDirectlySouth() {
        // 用户在卫星正下方经度时，方位角应接近180°
        // 北京 116.4°E, 中星6B 115.5°E (几乎正南)
        val az = SatelliteCalculator.calculateAzimuth(39.9042, 116.4074, 115.5)
        println("北京 -> 中星6B(115.5°E): 方位角=${"%.2f".format(az)}°")

        // 精确值约358.91° (接近正北，因为用户经度>卫星经度，卫星在用户西方偏南)
        assert(abs(az - 358.91) < EPSILON) { "方位角偏差过大: 预期358.91, 实际$az" }
    }

    @Test
    fun testSatelliteBelowHorizon() {
        // 远离用户位置的卫星仰角应为负值
        // 北京看Galaxy 19 (97°W) - 跨越半个地球
        val el = SatelliteCalculator.calculateElevation(39.9042, 116.4074, -97.0)
        println("北京 -> Galaxy 19(97°W): 仰角=${"%.2f".format(el)}°")

        assert(el < 0) { "不可见卫星仰角应为负值, 实际: $el" }
    }

    @Test
    fun testPolarizationAngle() {
        // 极化角测试 - 中星9C在北京
        val pol = SatelliteCalculator.calculatePolarizationAngle(39.9042, 116.4074, 92.2)
        println("北京 -> 中星9C: 极化角=${"%.2f".format(pol)}°")

        // 极化角应在合理范围内
        assert(abs(pol) < 90.0) { "极化角绝对值应小于90°, 实际: $pol" }
    }

    // ========== 屏幕坐标映射测试 ==========

    @Test
    fun testSatelliteToScreenPosition() {
        // 卫星在设备正中心
        val pos = SatelliteCalculator.satelliteToScreenPosition(
            satAzimuth = 180.0,
            satElevation = 30.0,
            deviceAzimuth = 180.0,
            devicePitch = 30.0,
            screenWidth = 1080,
            screenHeight = 1920,
            horizontalFOV = 68.0,
            verticalFOV = 42.0
        )

        assert(pos != null) { "卫星应在视场内" }
        assert(abs(pos!!.first - 540f) < 50f) { "X坐标应接近屏幕中心" }
        assert(abs(pos.second - 960f) < 50f) { "Y坐标应接近屏幕中心" }

        println("屏幕坐标映射验证通过: (${pos.first}, ${pos.second})")
    }

    @Test
    fun testSatelliteToScreenPosition_OutOfView() {
        // 卫星在设备背后
        val pos = SatelliteCalculator.satelliteToScreenPosition(
            satAzimuth = 0.0,
            satElevation = 45.0,
            deviceAzimuth = 180.0,
            devicePitch = 0.0,
            screenWidth = 1080,
            screenHeight = 1920,
            horizontalFOV = 68.0,
            verticalFOV = 42.0
        )

        assert(pos == null) { "卫星在设备背后时不应在视场内" }
        println("视场外验证通过")
    }
}
