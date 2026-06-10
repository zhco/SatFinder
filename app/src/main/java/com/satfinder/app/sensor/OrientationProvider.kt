package com.satfinder.app.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.mutableStateOf
import com.satfinder.app.model.DeviceOrientation
import kotlin.math.sqrt

/**
 * 设备朝向传感器提供者
 * 融合加速度计和磁力计数据，计算真北方位角
 */
class OrientationProvider(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // 低通滤波系数
    private val ALPHA = 0.15f
    private var smoothedAzimuth = 0f
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f

    // 磁偏角
    private var declination = 0f
    private var declinationSet = false

    val orientationState = mutableStateOf(DeviceOrientation())

    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { mag ->
            sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
            }
        }

        updateOrientation()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不处理
    }

    private fun updateOrientation() {
        // Step 1: 从加速度计和磁力计计算旋转矩阵
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (!success) return

        // Step 2: 重映射坐标系（竖屏持握，摄像头朝向）
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_MINUS_Z,
            remappedMatrix
        )

        // Step 3: 提取方位角、俯仰角、横滚角
        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        // 转为角度
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        // 转为 0°~360°
        azimuth = (azimuth + 360f) % 360f

        // 磁偏角修正（真北）
        if (!declinationSet && orientationState.value.azimuth != 0f) {
            val loc = com.satfinder.app.location.LocationProvider(context).locationState.value
            if (loc != null) {
                val geoField = GeomagneticField(
                    loc.latitude.toFloat(),
                    loc.longitude.toFloat(),
                    loc.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                declination = geoField.declination
                declinationSet = true
            }
        }

        val trueAzimuth = (azimuth + declination + 360f) % 360f

        // 低通滤波平滑
        smoothedAzimuth = smooth(trueAzimuth, smoothedAzimuth)
        smoothedPitch = smooth(pitch, smoothedPitch)
        smoothedRoll = smooth(roll, smoothedRoll)

        orientationState.value = DeviceOrientation(
            azimuth = smoothedAzimuth,
            pitch = smoothedPitch,
            roll = smoothedRoll
        )
    }

    private fun smooth(newVal: Float, oldVal: Float): Float {
        // 处理方位角跨越0°/360°的特殊情况
        var diff = newVal - oldVal
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        var result = oldVal + ALPHA * diff
        if (result < 0) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }
}
