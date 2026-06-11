package com.satfinder.app.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableStateOf
import com.satfinder.app.model.DeviceOrientation

/**
 * 设备朝向传感器提供者
 * 优先使用TYPE_ROTATION_VECTOR融合传感器（最稳定准确）
 * 回退到加速度计+磁力计手动融合
 */
class OrientationProvider(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorValues = FloatArray(4)
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // 低通滤波系数
    private val ALPHA = 0.1f
    private var smoothedAzimuth = Float.NaN
    private var smoothedPitch = Float.NaN
    private var smoothedRoll = Float.NaN

    // 最小变化阈值（度）
    private val MIN_CHANGE_THRESHOLD = 0.3f
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL_MS = 80L // ~12fps

    // 磁偏角
    private var declination = 0f
    private var declinationSet = false

    // 位置提供者引用
    private var locationProvider: com.satfinder.app.location.LocationProvider? = null

    val orientationState = mutableStateOf(DeviceOrientation())

    private var isRunning = false
    private var useRotationVector = false

    /** 设置位置提供者，用于计算磁偏角 */
    fun setLocationProvider(provider: com.satfinder.app.location.LocationProvider) {
        this.locationProvider = provider
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        // 优先使用TYPE_ROTATION_VECTOR（硬件融合传感器，最准确）
        val rotVecSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotVecSensor != null) {
            useRotationVector = true
            sensorManager.registerListener(this, rotVecSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // 回退到加速度计+磁力计
            useRotationVector = false
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accel ->
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { mag ->
                sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (useRotationVector) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                System.arraycopy(event.values, 0, rotationVectorValues, 0, 4)
                updateOrientationFromRotationVector()
            }
        } else {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                }
            }
            updateOrientationFromAccelerometerMag()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不处理
    }

    /**
     * 使用TYPE_ROTATION_VECTOR计算朝向（推荐方式）
     * rotation_vector已经由硬件融合了加速度计、陀螺仪、磁力计
     */
    private fun updateOrientationFromRotationVector() {
        val success = SensorManager.getRotationMatrixFromVector(
            rotationMatrix, rotationVectorValues
        )
        if (success.not()) return

        // 重映射坐标系（竖屏持握）
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_MINUS_Z,
            remappedMatrix
        )

        SensorManager.getOrientation(remappedMatrix, orientationAngles)
        applyOrientation()
    }

    /**
     * 使用加速度计+磁力计计算朝向（回退方式）
     */
    private fun updateOrientationFromAccelerometerMag() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (!success) return

        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_MINUS_Z,
            remappedMatrix
        )

        SensorManager.getOrientation(remappedMatrix, orientationAngles)
        applyOrientation()
    }

    /**
     * 统一处理方位角、俯仰角、横滚角
     */
    private fun applyOrientation() {
        // 转为角度
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        // 方位角转为 0°~360°
        azimuth = (azimuth + 360f) % 360f

        // 磁偏角修正（真北）
        val loc = locationProvider?.locationState?.value
        if (loc != null && !declinationSet) {
            val geoField = GeomagneticField(
                loc.latitude.toFloat(),
                loc.longitude.toFloat(),
                loc.altitude.toFloat(),
                System.currentTimeMillis()
            )
            declination = geoField.declination
            declinationSet = true
        }

        val trueAzimuth = (azimuth + declination + 360f) % 360f

        // 低通滤波平滑
        // 方位角需要处理0°/360°跨越
        smoothedAzimuth = smoothAngle(trueAzimuth, smoothedAzimuth)
        // pitch和roll不需要环绕处理（范围-180~180）
        smoothedPitch = smoothValue(pitch, smoothedPitch)
        smoothedRoll = smoothValue(roll, smoothedRoll)

        // 限制更新频率
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < UPDATE_INTERVAL_MS) return

        val current = orientationState.value
        val azDiff = angleDiff(smoothedAzimuth, current.azimuth)
        val pitchDiff = kotlin.math.abs(smoothedPitch - current.pitch)
        val rollDiff = kotlin.math.abs(smoothedRoll - current.roll)

        if (azDiff < MIN_CHANGE_THRESHOLD && pitchDiff < MIN_CHANGE_THRESHOLD && rollDiff < MIN_CHANGE_THRESHOLD) {
            return
        }

        lastUpdateTime = now
        orientationState.value = DeviceOrientation(
            azimuth = smoothedAzimuth,
            pitch = smoothedPitch,
            roll = smoothedRoll
        )
    }

    /**
     * 方位角平滑（处理0°/360°跨越）
     */
    private fun smoothAngle(newVal: Float, oldVal: Float): Float {
        if (java.lang.Float.isNaN(oldVal)) return newVal
        var diff = newVal - oldVal
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        var result = oldVal + ALPHA * diff
        if (result < 0) result += 360f
        if (result >= 360f) result -= 360f
        return result
    }

    /**
     * 普通值平滑（pitch/roll，不环绕）
     */
    private fun smoothValue(newVal: Float, oldVal: Float): Float {
        if (java.lang.Float.isNaN(oldVal)) return newVal
        return oldVal + ALPHA * (newVal - oldVal)
    }

    private fun angleDiff(a: Float, b: Float): Float {
        var diff = kotlin.math.abs(a - b)
        if (diff > 180f) diff = 360f - diff
        return diff
    }
}
