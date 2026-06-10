package com.satfinder.app.satellite

import com.satfinder.app.model.Satellite

/**
 * 全球常用直播电视卫星数据库
 * 包含中星9C（默认）及世界各区域主要直播卫星
 */
object SatelliteDatabase {

    // ========== 默认卫星：中星9C ==========
    val chinaSat9C = Satellite(
        name = "中星9C (Chinasat 9C)",
        orbitLongitude = 92.2,
        band = "Ku BSS / Ka BSS",
        polarization = "圆极化",
        kuDownStart = 11.7,
        kuDownEnd = 12.2,
        region = "中国全境、南海",
        isDefault = true
    )

    // ========== 中国及周边区域 ==========
    private val chinaSat9B = Satellite(
        name = "中星9B (Chinasat 9B)",
        orbitLongitude = 101.4,
        band = "Ku BSS",
        polarization = "圆极化",
        kuDownStart = 11.7,
        kuDownEnd = 12.2,
        region = "中国领土和领水"
    )

    private val chinaSat6E = Satellite(
        name = "中星6E (Chinasat 6E)",
        orbitLongitude = 115.5,
        band = "C / Ku",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "中国、东南亚、澳大利亚"
    )

    private val chinaSat6B = Satellite(
        name = "中星6B (Chinasat 6B)",
        orbitLongitude = 115.5,
        band = "C / Ku",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "中国、东亚、南亚、东南亚"
    )

    private val chinaSat6C = Satellite(
        name = "中星6C (Chinasat 6C)",
        orbitLongitude = 130.0,
        band = "C",
        polarization = "线极化",
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "中国及周边、澳大利亚"
    )

    private val apstar6C = Satellite(
        name = "亚太6C (Apstar 6C)",
        orbitLongitude = 134.0,
        band = "C / Ku / Ka",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "亚太地区"
    )

    private val apstar9 = Satellite(
        name = "亚太9号 (Apstar 9)",
        orbitLongitude = 142.0,
        band = "C / Ku",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "亚太地区"
    )

    private val asiaSat9 = Satellite(
        name = "亚洲9号 (AsiaSat 9)",
        orbitLongitude = 122.0,
        band = "Ku / C / Ka",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "东亚、东南亚、澳大利亚"
    )

    private val thaicom5 = Satellite(
        name = "泰星5号 (Thaicom 5)",
        orbitLongitude = 78.5,
        band = "Ku / C",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "泰国、东南亚、印度"
    )

    private val gsat30 = Satellite(
        name = "GSAT-30",
        orbitLongitude = 83.0,
        band = "Ku / C",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "印度、南亚"
    )

    private val expressAM5 = Satellite(
        name = "Express AM5",
        orbitLongitude = 140.0,
        band = "C / Ku",
        polarization = "线极化",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "俄罗斯远东"
    )

    // ========== 欧洲区域 ==========
    private val hotBird13 = Satellite(
        name = "Hot Bird 13F/13G",
        orbitLongitude = 13.0,
        band = "Ku",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        region = "欧洲、中东、北非"
    )

    private val astra1 = Satellite(
        name = "Astra 1 (19.2°E)",
        orbitLongitude = 19.2,
        band = "Ku",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        region = "德国、法国、欧洲"
    )

    private val astra2 = Satellite(
        name = "Astra 2 (28.2°E)",
        orbitLongitude = 28.2,
        band = "Ku",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        region = "英国、爱尔兰"
    )

    private val eutelsat7W = Satellite(
        name = "Eutelsat 7 West A",
        orbitLongitude = -7.0,
        band = "Ku / C",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        region = "北非、中东、南欧"
    )

    // ========== 非洲/中东区域 ==========
    private val nilesat = Satellite(
        name = "Nilesat 102/201",
        orbitLongitude = -7.0,
        band = "Ku",
        polarization = "线极化 (H/V)",
        kuDownStart = 11.7,
        kuDownEnd = 12.5,
        region = "北非、中东"
    )

    private val badr5 = Satellite(
        name = "Badr-5/6/7 (26°E)",
        orbitLongitude = 26.0,
        band = "Ku / C",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        region = "中东、北非"
    )

    private val intelsat20 = Satellite(
        name = "Intelsat 20 (68.5°E)",
        orbitLongitude = 68.5,
        band = "Ku / C",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.7,
        kuDownEnd = 12.75,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "非洲、中东、亚洲"
    )

    // ========== 美洲区域 ==========
    private val galaxy19 = Satellite(
        name = "Galaxy 19 (97°W)",
        orbitLongitude = -97.0,
        band = "Ku",
        polarization = "线极化 (H/V)",
        kuDownStart = 11.7,
        kuDownEnd = 12.2,
        region = "北美（美国、加拿大、墨西哥）"
    )

    private val ses1 = Satellite(
        name = "SES-1 (101°W)",
        orbitLongitude = -101.0,
        band = "Ku / C",
        polarization = "线极化 (H/V)",
        kuDownStart = 11.7,
        kuDownEnd = 12.2,
        cDownStart = 3.4,
        cDownEnd = 4.2,
        region = "北美"
    )

    private val intelsat905 = Satellite(
        name = "Intelsat 905 (137°W)",
        orbitLongitude = -137.0,
        band = "Ku / C",
        polarization = "线极化 (H/V)",
        kuDownStart = 10.95,
        kuDownEnd = 11.7,
        region = "美洲"
    )

    // ========== 所有卫星列表 ==========
    val allSatellites: List<Satellite> = listOf(
        // 默认卫星排第一
        chinaSat9C,
        // 中国及周边
        chinaSat9B,
        chinaSat6E,
        chinaSat6B,
        chinaSat6C,
        apstar6C,
        apstar9,
        asiaSat9,
        thaicom5,
        gsat30,
        expressAM5,
        // 欧洲
        hotBird13,
        astra1,
        astra2,
        eutelsat7W,
        // 非洲/中东
        nilesat,
        badr5,
        intelsat20,
        // 美洲
        galaxy19,
        ses1,
        intelsat905
    )

    /**
     * 根据用户位置筛选可见卫星（仰角 > 0°）
     */
    fun getVisibleSatellites(lat: Double, lon: Double): List<Satellite> {
        return allSatellites.filter { sat ->
            SatelliteCalculator.calculateElevation(lat, lon, sat.orbitLongitude) > 0
        }
    }

    /**
     * 按区域分组
     */
    fun getSatellitesByRegion(): Map<String, List<Satellite>> {
        return allSatellites.groupBy { sat ->
            when {
                sat.orbitLongitude in 60.0..180.0 && sat.region.contains("中国") -> "中国及周边"
                sat.orbitLongitude in 60.0..180.0 -> "亚太/南亚"
                sat.orbitLongitude in -30.0..50.0 -> "欧洲"
                sat.orbitLongitude in -30.0..50.0 && sat.region.contains("北非") -> "非洲/中东"
                sat.orbitLongitude < 0 -> "美洲"
                else -> "其他"
            }
        }
    }
}
