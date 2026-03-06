package com.example.carguardian

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carguardian.R

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var attitudeIndicatorView: AttitudeIndicatorView
    private lateinit var speedTapeView: SpeedTapeView
    private lateinit var altitudeTapeView: AltitudeTapeView
    private lateinit var rollText: TextView
    private lateinit var pitchText: TextView
    private lateinit var satelliteText: TextView
    private lateinit var dateText: TextView
    private lateinit var weekText: TextView
    private lateinit var timeText: TextView
    private lateinit var headingText: TextView
    private lateinit var zeroButton: Button

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager

    // 传感器数据
    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)
    private var gyroscopeValues = FloatArray(3)

    // 归零点数据
    private var zeroRoll = 0f
    private var zeroPitch = 0f
    private var zeroYaw = 0f

    // 当前显示数据
    private var currentHeading = 0f
    private var currentRoll = 0f
    private var currentPitch = 0f
    private var currentSpeed = 0f
    private var currentAltitude = 0f
    private var currentSatellites = 0
    
    // 原始航向（不受归零影响，用于航向指示箭头）
    private var rawHeading = 0f

    // 平滑过渡数据（用于阻尼效果）
    private var smoothedRoll = 0f
    private var smoothedPitch = 0f
    private var smoothedHeading = 0f  // 底部航向数值的平滑过渡
    
    // 阻尼系数（值越小阻尼效果越强）
    private val dampingFactor = 0.05f  // 增加阻尼效果，减少数值跳动

    // 闪烁动画
    private var isBlinking = false
    private val blinkHandler = Handler(Looper.getMainLooper())
    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (isBlinking) {
                val color = if (satelliteText.currentTextColor == Color.RED) {
                    Color.TRANSPARENT
                } else {
                    Color.RED
                }
                satelliteText.setTextColor(color)
                blinkHandler.postDelayed(this, 500)
            }
        }
    }

    // 日期时间更新定时器
    private val dateTimeHandler = Handler(Looper.getMainLooper())
    private val dateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            dateTimeHandler.postDelayed(this, 1000)  // 每秒更新一次
        }
    }

    private val prefsName = "CarGuardianPrefs"
    private val keyZeroRoll = "zeroRoll"
    private val keyZeroPitch = "zeroPitch"
    private val keyZeroYaw = "zeroYaw"
    private val keyFirstLaunch = "firstLaunch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置全屏显示
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 初始化视图
        attitudeIndicatorView = findViewById(R.id.attitudeIndicatorView)
        speedTapeView = findViewById(R.id.speedTapeView)
        altitudeTapeView = findViewById(R.id.altitudeTapeView)
        rollText = findViewById(R.id.rollText)
        pitchText = findViewById(R.id.pitchText)
        satelliteText = findViewById(R.id.satelliteText)
        dateText = findViewById(R.id.dateText)
        weekText = findViewById(R.id.weekText)
        timeText = findViewById(R.id.timeText)
        headingText = findViewById(R.id.headingText)
        zeroButton = findViewById(R.id.zeroButton)

        // 初始化传感器管理器
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // 检查权限
        checkPermissions()

        // 加载归零点
        loadZeroPoint()

        // 归零按钮点击事件
        zeroButton.setOnClickListener {
            setZeroPoint()
        }

        // 检查是否首次启动
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val firstLaunch = prefs.getBoolean(keyFirstLaunch, true)
        if (firstLaunch) {
            // 首次启动，延迟2秒后自动归零（等待传感器稳定）
            Handler(Looper.getMainLooper()).postDelayed({
                setZeroPoint()
                prefs.edit().putBoolean(keyFirstLaunch, false).apply()
            }, 2000)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerSensors()
        startLocationUpdates()
        // 启动日期时间更新定时器
        dateTimeHandler.post(dateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterSensors()
        stopLocationUpdates()
        // 停止日期时间更新定时器
        dateTimeHandler.removeCallbacks(dateTimeRunnable)
    }

    private fun registerSensors() {
        // 注册加速度传感器
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // 注册磁场传感器
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // 注册陀螺仪传感器
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    private fun startLocationUpdates() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    this
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(it.values, 0, accelerometerValues, 0, 3)
                    calculateAttitude()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(it.values, 0, magnetometerValues, 0, 3)
                    calculateAttitude()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    System.arraycopy(it.values, 0, gyroscopeValues, 0, 3)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不需要处理
    }

    private fun calculateAttitude() {
        // 使用加速度传感器数据直接计算倾角
        // Z轴数据表示前后俯仰角（pitch）
        // Y轴数据表示水平倾角（roll）
        
        // 计算加速度向量的模
        val magnitude = Math.sqrt(
            accelerometerValues[0].toDouble() * accelerometerValues[0] +
            accelerometerValues[1].toDouble() * accelerometerValues[1] +
            accelerometerValues[2].toDouble() * accelerometerValues[2]
        ).toFloat()
        
        if (magnitude > 0) {
            // 归一化加速度向量
            val ax = accelerometerValues[0] / magnitude
            val ay = accelerometerValues[1] / magnitude
            val az = accelerometerValues[2] / magnitude
            
            // 计算倾角（使用加速度传感器数据）
            // Y轴数据表示前后俯仰角（pitch）
            var pitch = Math.toDegrees(Math.asin(-ay.toDouble())).toFloat()
            
            // X轴数据表示水平倾角（roll）
            var roll = Math.toDegrees(Math.atan2(ax.toDouble(), az.toDouble())).toFloat()
            
            // 计算航向（使用磁力计数据）
            // 屏幕为横屏，屏幕正上方为N
            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)
            
            var azimuth = 0f
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerValues,
                magnetometerValues
            )
            
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                if (azimuth < 0) azimuth += 360f
            }
            
            // 保存原始航向（不受归零影响，用于航向指示箭头）
            rawHeading = azimuth
            
            // 减去归零点
            currentHeading = azimuth - zeroYaw
            currentRoll = roll - zeroRoll
            currentPitch = pitch - zeroPitch
            
            // 归一化航向
            if (currentHeading < 0) currentHeading += 360f
            if (currentHeading >= 360) currentHeading -= 360f
            
            // 应用阻尼效果（平滑过渡）
            smoothedRoll = smoothedRoll + (currentRoll - smoothedRoll) * dampingFactor
            smoothedPitch = smoothedPitch + (currentPitch - smoothedPitch) * dampingFactor
            smoothedHeading = smoothedHeading + (currentHeading - smoothedHeading) * dampingFactor  // 底部航向数值阻尼
            
            updateUI()
        }
    }

    private fun setZeroPoint() {
        // 使用加速度传感器数据直接计算倾角（与calculateAttitude方法一致）
        val magnitude = Math.sqrt(
            accelerometerValues[0].toDouble() * accelerometerValues[0] +
            accelerometerValues[1].toDouble() * accelerometerValues[1] +
            accelerometerValues[2].toDouble() * accelerometerValues[2]
        ).toFloat()
        
        if (magnitude > 0) {
            // 归一化加速度向量
            val ax = accelerometerValues[0] / magnitude
            val ay = accelerometerValues[1] / magnitude
            val az = accelerometerValues[2] / magnitude
            
            // 计算倾角（使用加速度传感器数据）
            // Y轴数据表示前后俯仰角（pitch）
            var pitch = Math.toDegrees(Math.asin(-ay.toDouble())).toFloat()
            
            // X轴数据表示水平倾角（roll）
            var roll = Math.toDegrees(Math.atan2(ax.toDouble(), az.toDouble())).toFloat()
            
            // 计算航向（使用磁力计数据）
            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)
            
            var azimuth = 0f
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerValues,
                magnetometerValues
            )
            
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                if (azimuth < 0) azimuth += 360f
            }
            
            // 保存归零点
            zeroRoll = roll
            zeroPitch = pitch
            zeroYaw = azimuth
            
            saveZeroPoint()
            
            // 显示归零成功提示
            android.widget.Toast.makeText(this, "归零成功", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // 显示归零失败提示
            android.widget.Toast.makeText(this, "归零失败，请确保传感器正常工作", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveZeroPoint() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.edit()
            .putFloat(keyZeroRoll, zeroRoll)
            .putFloat(keyZeroPitch, zeroPitch)
            .putFloat(keyZeroYaw, zeroYaw)
            .apply()
    }

    private fun loadZeroPoint() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        zeroRoll = prefs.getFloat(keyZeroRoll, 0f)
        zeroPitch = prefs.getFloat(keyZeroPitch, 0f)
        zeroYaw = prefs.getFloat(keyZeroYaw, 0f)
    }

    override fun onLocationChanged(location: Location) {
        // 正常显示速度，始终使用GPS速度
        currentSpeed = location.speed * 3.6f  // 转换为KM/H
        currentAltitude = location.altitude.toFloat()

        // 使用GPS bearing计算航向（反映车辆在地表的实际运动方向）
        // GPS bearing定义：0=正北，90=正东，180=正南，270=正西
        // 只有在速度大于1km/h时才使用GPS bearing，避免静止时数据不准确
        if (currentSpeed > 1f && location.hasBearing()) {
            val gpsBearing = location.bearing
            if (gpsBearing >= 0) {
                rawHeading = gpsBearing
                // 减去归零点
                currentHeading = rawHeading - zeroYaw
                // 归一化航向
                if (currentHeading < 0) currentHeading += 360f
                if (currentHeading >= 360) currentHeading -= 360f
            }
        }

        // 获取卫星数量（通过LocationManager获取）
        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLocation != null) {
                // Android不直接提供卫星数量，这里使用GPS extras
                val extras = gpsLocation.extras
                if (extras != null) {
                    currentSatellites = extras.getInt("satellites", 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateUI()
    }

    private fun updateUI() {
        // 更新姿态仪视图（使用平滑过渡后的数据，横屏模式下交换roll和pitch）
        attitudeIndicatorView.setAttitude(smoothedPitch, smoothedRoll)
        // 更新航向（使用原始航向，不受归零影响）
        attitudeIndicatorView.setHeading(rawHeading)

        // 更新速度表视图
        val isWarning = currentSpeed >= 80f
        val isDanger = currentSpeed >= 120f
        speedTapeView.setSpeed(currentSpeed, isWarning, isDanger)

        // 更新高度表视图
        altitudeTapeView.setAltitude(currentAltitude)

        // 更新倾角数值（使用平滑过渡后的数据）
        rollText.text = String.format("俯仰: %.1f°", smoothedRoll)
        pitchText.text = String.format("水平: %.1f°", smoothedPitch)
        
        // 更新航向数值（使用平滑过渡后的数据）
        headingText.text = String.format("航向: %.0f°", smoothedHeading)

        // 速度颜色变化逻辑（用于其他文本）
        when {
            currentSpeed >= 120f -> {
                // 红色闪烁警示
                if (!isBlinking) {
                    isBlinking = true
                    blinkHandler.post(blinkRunnable)
                }
                updateTextColor(Color.RED)
            }
            currentSpeed >= 80f -> {
                // 黄色
                isBlinking = false
                blinkHandler.removeCallbacks(blinkRunnable)
                updateTextColor(Color.YELLOW)
            }
            else -> {
                // 绿色
                isBlinking = false
                blinkHandler.removeCallbacks(blinkRunnable)
                updateTextColor(Color.GREEN)
            }
        }

        // 更新卫星数量
        satelliteText.text = "卫星: $currentSatellites"
    }

    private fun updateTextColor(color: Int) {
        rollText.setTextColor(color)
        pitchText.setTextColor(color)
        satelliteText.setTextColor(color)
    }

    override fun onProviderEnabled(provider: String) {
        // GPS启用
    }

    override fun onProviderDisabled(provider: String) {
        // GPS禁用
    }

    private fun updateDateTime() {
        val calendar = java.util.Calendar.getInstance()
        
        // 获取日期
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1  // 月份从0开始
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        // 获取星期
        val weekDays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
        val weekDay = weekDays[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        
        // 获取时间
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val second = calendar.get(java.util.Calendar.SECOND)
        
        // 格式化日期字符串（不包含星期）
        val dateString = String.format("%04d-%02d-%02d", year, month, day)
        
        // 格式化时间字符串
        val timeString = String.format("%02d:%02d:%02d", hour, minute, second)
        
        // 更新TextView
        dateText.text = dateString
        weekText.text = weekDay
        timeText.text = timeString
    }
}
