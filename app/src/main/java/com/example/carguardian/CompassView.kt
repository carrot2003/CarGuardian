package com.example.carguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.os.Handler
import android.os.Looper

class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetHeading = 0f  // 目标航向
    private var displayHeading = 0f  // 显示航向（带阻尼）
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    // 阻尼系数（0.0-1.0），越小阻尼越大
    private val dampingFactor = 0.1f
    
    // 平滑更新处理器
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            // 计算角度差，考虑360度循环
            var diff = targetHeading - displayHeading
            
            // 处理角度跨越0/360度的情况
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            
            // 应用阻尼
            displayHeading += diff * dampingFactor
            
            // 归一化到0-360度
            if (displayHeading < 0) displayHeading += 360f
            if (displayHeading >= 360) displayHeading -= 360f
            
            invalidate()
            
            // 如果还有显著差异，继续更新
            if (Math.abs(diff) > 0.1f) {
                handler.postDelayed(this, 16)  // 约60fps
            }
        }
    }

    fun setHeading(heading: Float) {
        targetHeading = heading
        // 启动平滑更新
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 2f - 10f

        // 绘制外圆
        paint.color = 0xFF00FF00.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 绘制内圆
        paint.strokeWidth = 2f
        canvas.drawCircle(centerX, centerY, radius * 0.8f, paint)

        // 绘制刻度
        paint.strokeWidth = 2f
        for (i in 0 until 360 step 10) {
            val angle = Math.toRadians((i - displayHeading).toDouble())
            val isMain = i % 90 == 0
            val lineLength = if (isMain) 20f else 10f
            val startRadius = if (isMain) radius * 0.8f else radius * 0.85f
            val endRadius = radius * 0.9f

            val startX = centerX + Math.cos(angle).toFloat() * startRadius
            val startY = centerY + Math.sin(angle).toFloat() * startRadius
            val endX = centerX + Math.cos(angle).toFloat() * endRadius
            val endY = centerY + Math.sin(angle).toFloat() * endRadius

            paint.strokeWidth = if (isMain) 3f else 2f
            canvas.drawLine(startX, startY, endX, endY, paint)

            // 绘制方向文字
            if (isMain) {
                val textRadius = radius * 0.7f
                val textX = centerX + Math.cos(angle).toFloat() * textRadius
                val textY = centerY + Math.sin(angle).toFloat() * textRadius
                paint.style = Paint.Style.FILL
                paint.textSize = 40f  // 放大NEWS字符
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

        // 绘制飞机符号（固定在中心）
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 3f
        path.reset()
        path.moveTo(centerX, centerY - 30f)
        path.lineTo(centerX - 20f, centerY + 20f)
        path.lineTo(centerX + 20f, centerY + 20f)
        path.close()
        canvas.drawPath(path, paint)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(updateRunnable)
    }
}
