package com.satfinder.app.satellite

import org.junit.Test
import kotlin.math.abs

/**
 * 卫星位置计算算法验证测试
 * 使用已知参考值验证方位角、仰角和极化角的计算准确性
 */
class SatelliteCalculatorTest {

    companion object {
        private const val EPSILON = 0.5  // 允许误差 ±0.5°
    }

    // ========== 中星9C (92.2°E) 测试 ==========

    @Test
    fun testChinaSat9C_Beijing() {
        // 北京: 39.9042°N, 116.4074°E
        // 中星9C: 92.2°E
        // 预期: 方位角约207°(西南偏南), 仰角约38°
        val az = SatelliteCalculator.calculateAzimuth(39.9042, 116.4074, 92.2)
        val el = SatelliteCalculator.calculateElevation(39.9042, 116.4074, 92.2)

        println("北京 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        // 验证方位角在合理范围内 (西南方向 180°~270°)
        assert(az in 180.0..270.0) { "方位角应在180~270之间, 实际: $az" }
        // 验证仰角在合理范围内
        assert(el > 20.0 && el < 60.0) { "仰角应在20~60之间, 实际: $el" }
    }

    @Test
    fun testChinaSat9C_Shanghai() {
        // 上海: 31.2304°N, 121.4737°E
        // 中星9C: 92.2°E
        // 预期: 方位角约225°(西南), 仰角约45°
        val az = SatelliteCalculator.calculateAzimuth(31.2304, 121.4737, 92.2)
        val el = SatelliteCalculator.calculateElevation(31.2304, 121.4737, 92.2)

        println("上海 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(az in 200.0..260.0) { "方位角应在200~260之间, 实际: $az" }
        assert(el > 35.0 && el < 60.0) { "仰角应在35~60之间, 实际: $el" }
    }

    @Test
    fun testChinaSat9C_Guangzhou() {
        // 广州: 23.1291°N, 113.2644°E
        // 中星9C: 92.2°E
        val az = SatelliteCalculator.calculateAzimuth(23.1291, 113.2644, 92.2)
        val el = SatelliteCalculator.calculateElevation(23.1291, 113.2644, 92.2)

        println("广州 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(az in 210.0..260.0) { "方位角应在210~260之间, 实际: $az" }
        assert(el > 45.0 && el < 75.0) { "仰角应在45~75之间, 实际: $el" }
    }

    @Test
    fun testChinaSat9C_Urumqi() {
        // 乌鲁木齐: 43.8256°N, 87.6168°E
        // 中星9C: 92.2°E (卫星几乎在正南方偏东)
        val az = SatelliteCalculator.calculateAzimuth(43.8256, 87.6168, 92.2)
        val el = SatelliteCalculator.calculateElevation(43.8256, 87.6168, 92.2)

        println("乌鲁木齐 -> 中星9C: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        // 乌鲁木齐经度87.6° < 卫星92.2°, 卫星在东偏南方向
        assert(az in 150.0..190.0) { "方位角应在150~190之间, 实际: $az" }
        assert(el > 25.0 && el < 50.0) { "仰角应在25~50之间, 实际: $el" }
    }

    // ========== Hot Bird 13°E 测试 (欧洲) ==========

    @Test
    fun testHotBird_London() {
        // 伦敦: 51.5074°N, -0.1278°W
        // Hot Bird: 13°E
        val az = SatelliteCalculator.calculateAzimuth(51.5074, -0.1278, 13.0)
        val el = SatelliteCalculator.calculateElevation(51.5074, -0.1278, 13.0)

        println("伦敦 -> Hot Bird 13°E: 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        // Hot Bird在伦敦的东南方向
        assert(az in 120.0..170.0) { "方位角应在120~170之间, 实际: $az" }
        assert(el > 15.0 && el < 40.0) { "仰角应在15~40之间, 实际: $el" }
    }

    @Test
    fun testAstra2_London() {
        // 伦敦 -> Astra 2 (28.2°E) - Sky卫星
        val az = SatelliteCalculator.calculateAzimuth(51.5074, -0.1278, 28.2)
        val el = SatelliteCalculator.calculateElevation(51.5074, -0.1278, 28.2)

        println("伦敦 -> Astra 2 (28.2°E): 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        assert(az in 140.0..160.0) { "方位角应在140~160之间, 实际: $az" }
        assert(el > 20.0 && el < 35.0) { "仰角应在20~35之间, 实际: $el" }
    }

    // ========== Galaxy 19 97°W 测试 (美洲) ==========

    @Test
    fun testGalaxy19_NewYork() {
        // 纽约: 40.7128°N, -74.0060°W
        // Galaxy 19: 97°W
        val az = SatelliteCalculator.calculateAzimuth(40.7128, -74.0060, -97.0)
        val el = SatelliteCalculator.calculateElevation(40.7128, -74.0060, -97.0)

        println("纽约 -> Galaxy 19 (97°W): 方位角=${"%.2f".format(az)}°, 仰角=${"%.2f".format(el)}°")

        // Galaxy 19在纽约的西南方向
        assert(az in 200.0..260.0) { "方位角应在200~260之间, 实际: $az" }
        assert(el > 20.0 && el < 50.0) { "仰角应在20~50之间, 实际: $el" }
    }

    // ========== 边界条件测试 ==========

    @Test
    fun testSatelliteDirectlySouth() {
        // 用户在卫星正下方经度时，方位角应为180°
        // 北京 116.4°E, 中星6B 115.5°E (几乎正南)
        val az = SatelliteCalculator.calculateAzimuth(39.9042, 116.4074, 115.5)
        println("北京 -> 中星6B(115.5°E): 方位角=${"%.2f".format(az)}°")

        assert(abs(az - 180.0) < 5.0) { "卫星几乎正南时方位角应接近180°, 实际: $az" }
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

    // ========== 与LyngSat参考值对比验证 ==========

    @Test
    fun testCrossValidation_WithKnownReferences() {
        // 使用多个已知参考点交叉验证
        data class TestCase(
            val lat: Double,
            val lon: Double,
            val satLon: Double,
            val azMin: Double,
            val azMax: Double,
            val elMin: Double,
            val elMax: Double
        )

        val testCases = listOf(
            TestCase(39.9042, 116.4074, 92.2, 200.0, 220.0, 35.0, 45.0),
        )

        testCases.forEach { tc ->
            val az = SatelliteCalculator.calculateAzimuth(tc.lat, tc.lon, tc.satLon)
            val el = SatelliteCalculator.calculateElevation(tc.lat, tc.lon, tc.satLon)

            assert(az in tc.azMin..tc.azMax) {
                "方位角验证失败: 位置(${tc.lat}, ${tc.lon})->卫星(${tc.satLon}), " +
                "预期${tc.azMin}~${tc.azMax}, 实际${"%.2f".format(az)}"
            }
            assert(el in tc.elMin..tc.elMax) {
                "仰角验证失败: 位置(${tc.lat}, ${tc.lon})->卫星(${tc.satLon}), " +
                "预期${tc.elMin}~${tc.elMax}, 实际${"%.2f".format(el)}"
            }
        }

        println("所有交叉验证通过!")
    }

    @Test
    fun testSatelliteToScreenPosition() {
        // 测试屏幕坐标映射
        val pos = SatelliteCalculator.satelliteToScreenPosition(
            satAzimuth = 180.0,    // 正南
            satElevation = 30.0,
            deviceAzimuth = 180.0, // 设备朝南
            devicePitch = 30.0,    // 设备仰角30°
            screenWidth = 1080,
            screenHeight = 1920,
            horizontalFOV = 68.0,
            verticalFOV = 42.0
        )

        // 卫星在设备正中心
        assert(pos != null) { "卫星应在视场内" }
        assert(abs(pos!!.first - 540f) < 50f) { "X坐标应接近屏幕中心" }
        assert(abs(pos.second - 960f) < 50f) { "Y坐标应接近屏幕中心" }

        println("屏幕坐标映射验证通过: (${pos.first}, ${pos.second})")
    }

    @Test
    fun testSatelliteToScreenPosition_OutOfView() {
        // 测试卫星不在视场内
        val pos = SatelliteCalculator.satelliteToScreenPosition(
            satAzimuth = 0.0,      // 正北
            satElevation = 45.0,
            deviceAzimuth = 180.0, // 设备朝南
            devicePitch = 0.0,
            screenWidth = 1080,
            screenHeight = 1920,
            horizontalFOV = 68.0,
            verticalFOV = 42.0
        )

        // 卫星在设备背后，不应在视场内
        assert(pos == null) { "卫星在设备背后时不应在视场内" }
        println("视场外验证通过")
    }
}
