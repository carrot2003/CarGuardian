package com.example.carguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.os.Handler
import android.os.Looper

class CombinedPFDView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 航向数据
    private var targetHeading = 0f
    private var displayHeading = 0f
    
    // 姿态数据
    private var roll = 0f
    private var pitch = 0f
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    // 阻尼系数
    private val dampingFactor = 0.1f
    
    // 平滑更新处理器
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            var diff = targetHeading - displayHeading
            
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            
            displayHeading += diff * dampingFactor
            
            if (displayHeading < 0) displayHeading += 360f
            if (displayHeading >= 360) displayHeading -= 360f
            
            invalidate()
            
            if (Math.abs(diff) > 0.1f) {
                handler.postDelayed(this, 16)
            }
        }
    }

    fun setHeading(heading: Float) {
        targetHeading = heading
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    fun setAttitude(roll: Float, pitch: Float) {
        this.roll = roll
        this.pitch = pitch
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 2f - 20f

        // 绘制外圆（罗盘）
        paint.color = 0xFF00FF00.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 绘制内圆（姿态仪）
        paint.strokeWidth = 2f
        canvas.drawCircle(centerX, centerY, radius * 0.7f, paint)

        // 绘制罗盘刻度
        paint.strokeWidth = 2f
        for (i in 0 until 360 step 10) {
            val angle = Math.toRadians((i - displayHeading).toDouble())
            val isMain = i % 90 == 0
            val lineLength = if (isMain) 20f else 10f
            val startRadius = if (isMain) radius * 0.9f else radius * 0.92f
            val endRadius = radius * 0.98f

            val startX = centerX + Math.cos(angle).toFloat() * startRadius
            val startY = centerY + Math.sin(angle).toFloat() * startRadius
            val endX = centerX + Math.cos(angle).toFloat() * endRadius
            val endY = centerY + Math.sin(angle).toFloat() * endRadius

            paint.strokeWidth = if (isMain) 3f else 2f
            canvas.drawLine(startX, startY, endX, endY, paint)

            // 绘制方向文字
            if (isMain) {
                val textRadius = radius * 0.82f
                val textX = centerX + Math.cos(angle).toFloat() * textRadius
                val textY = centerY + Math.sin(angle).toFloat() * textRadius
                paint.style = Paint.Style.FILL
                paint.textSize = 36f
                paint.textAlign = Paint.Align.CENTER
                val text = when (i) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> ""
                }
                canvas.drawText(text, textX, textY + 12f, paint)
            }
        }

        // 绘制姿态仪（内圆区域）
        canvas.save()
        canvas.rotate(-roll, centerX, centerY)

        val attitudeRadius = radius * 0.7f
        val horizonY = centerY + pitch * 2f

        // 绘制地平线
        paint.strokeWidth = 3f
        canvas.drawLine(centerX - attitudeRadius, horizonY, centerX + attitudeRadius, horizonY, paint)

        // 绘制天空（上半部分）
        paint.style = Paint.Style.FILL
        paint.color = 0xFF003300.toInt()
        path.reset()
        path.moveTo(centerX - attitudeRadius, horizonY)
        path.lineTo(centerX + attitudeRadius, horizonY)
        path.lineTo(centerX + attitudeRadius, centerY - attitudeRadius)
        path.lineTo(centerX - attitudeRadius, centerY - attitudeRadius)
        path.close()
        canvas.drawPath(path, paint)

        // 绘制地面（下半部分）
        paint.color = 0xFF001100.toInt()
        path.reset()
        path.moveTo(centerX - attitudeRadius, horizonY)
        path.lineTo(centerX + attitudeRadius, horizonY)
        path.lineTo(centerX + attitudeRadius, centerY + attitudeRadius)
        path.lineTo(centerX - attitudeRadius, centerY + attitudeRadius)
        path.close()
        canvas.drawPath(path, paint)

        // 绘制刻度线
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        for (i in -3..3) {
            if (i == 0) continue
            val y = horizonY + i * 15f
            if (y > centerY - attitudeRadius && y < centerY + attitudeRadius) {
                canvas.drawLine(centerX - 25f, y, centerX + 25f, y, paint)
            }
        }

        canvas.restore()

        // 绘制飞机符号（固定在中心）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0xFF00FF00.toInt()

        // 中心点
        canvas.drawCircle(centerX, centerY, 4f, paint)

        // 左翼
        canvas.drawLine(centerX, centerY, centerX - 35f, centerY, paint)

        // 右翼
        canvas.drawLine(centerX, centerY, centerX + 35f, centerY, paint)

        // 上翼
        canvas.drawLine(centerX, centerY, centerX, centerY - 25f, paint)

        // 下翼
        canvas.drawLine(centerX, centerY, centerX, centerY + 25f, paint)

        // 绘制航向数值（中心）
        paint.style = Paint.Style.FILL
        paint.textSize = 32f
        paint.textAlign = Paint.Align.CENTER
        paint.color = 0xFF00FF00.toInt()
        canvas.drawText(String.format("%03d°", displayHeading.toInt()), centerX, centerY + 10f, paint)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(updateRunnable)
    }
}
