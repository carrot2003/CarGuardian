package com.example.carguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class AttitudeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var roll = 0f  // 左右倾角
    private var pitch = 0f  // 前后倾角
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun setAttitude(roll: Float, pitch: Float) {
        this.roll = roll
        this.pitch = pitch
        invalidate()
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

        // 绘制水平线（根据roll旋转）
        canvas.save()
        canvas.rotate(-roll, centerX, centerY)

        // 绘制地平线
        val horizonY = centerY + pitch * 2f
        paint.strokeWidth = 3f
        canvas.drawLine(centerX - radius, horizonY, centerX + radius, horizonY, paint)

        // 绘制天空（上半部分）
        paint.style = Paint.Style.FILL
        paint.color = 0xFF003300.toInt()
        path.reset()
        path.moveTo(centerX - radius, horizonY)
        path.lineTo(centerX + radius, horizonY)
        path.lineTo(centerX + radius, centerY - radius)
        path.lineTo(centerX - radius, centerY - radius)
        path.close()
        canvas.drawPath(path, paint)

        // 绘制地面（下半部分）
        paint.color = 0xFF001100.toInt()
        path.reset()
        path.moveTo(centerX - radius, horizonY)
        path.lineTo(centerX + radius, horizonY)
        path.lineTo(centerX + radius, centerY + radius)
        path.lineTo(centerX - radius, centerY + radius)
        path.close()
        canvas.drawPath(path, paint)

        // 绘制刻度线
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        for (i in -3..3) {
            if (i == 0) continue
            val y = horizonY + i * 20f
            if (y > centerY - radius && y < centerY + radius) {
                canvas.drawLine(centerX - 30f, y, centerX + 30f, y, paint)
            }
        }

        canvas.restore()

        // 绘制飞机符号（固定在中心）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0xFF00FF00.toInt()

        // 中心点
        canvas.drawCircle(centerX, centerY, 5f, paint)

        // 左翼
        canvas.drawLine(centerX, centerY, centerX - 40f, centerY, paint)

        // 右翼
        canvas.drawLine(centerX, centerY, centerX + 40f, centerY, paint)

        // 上翼
        canvas.drawLine(centerX, centerY, centerX, centerY - 30f, paint)

        // 下翼
        canvas.drawLine(centerX, centerY, centerX, centerY + 30f, paint)

        // 绘制倾角指示器
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL

        // Roll指示
        canvas.drawText("R: ${String.format("%.1f", roll)}°", centerX, centerY - radius - 20f, paint)

        // Pitch指示
        canvas.drawText("P: ${String.format("%.1f", pitch)}°", centerX, centerY + radius + 20f, paint)
    }
}
