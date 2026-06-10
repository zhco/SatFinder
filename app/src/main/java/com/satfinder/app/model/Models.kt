package com.satfinder.app.model

/**
 * 卫星数据模型
 * @param name 卫星名称
 * @param orbitLongitude 轨道经度（东经为正，西经为负）
 * @param band 频段 (Ku/C/Ka)
 * @param polarization 极化方式 (圆极化/线极化)
 * @param kuDownStart Ku波段下行起始频率 (GHz)
 * @param kuDownEnd Ku波段下行结束频率 (GHz)
 * @param cDownStart C波段下行起始频率 (GHz)
 * @param cDownEnd C波段下行结束频率 (GHz)
 * @param region 覆盖区域
 * @param isDefault 是否为默认卫星
 */
data class Satellite(
    val name: String,
    val orbitLongitude: Double,  // 正=东经, 负=西经
    val band: String,
    val polarization: String,
    val kuDownStart: Double = 0.0,
    val kuDownEnd: Double = 0.0,
    val cDownStart: Double = 0.0,
    val cDownEnd: Double = 0.0,
    val region: String,
    val isDefault: Boolean = false
)

/**
 * 卫星计算结果
 */
data class SatellitePosition(
    val satellite: Satellite,
    val azimuth: Double,       // 方位角 (0°=北, 顺时针, 0~360°)
    val elevation: Double,     // 仰角 (0~90°)
    val polarizationAngle: Double,  // 极化角
    val isVisible: Boolean     // 是否在地平线以上
)

/**
 * 设备朝向
 */
data class DeviceOrientation(
    val azimuth: Float = 0f,    // 真北方位角 (0~360°)
    val pitch: Float = 0f,      // 俯仰角
    val roll: Float = 0f        // 横滚角
)

/**
 * GPS位置
 */
data class GpsLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f
)
