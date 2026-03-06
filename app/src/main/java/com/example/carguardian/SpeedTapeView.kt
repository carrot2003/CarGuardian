package com.example.carguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

class SpeedTapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var speed = 0f
    private var isWarning = false
    private var isDanger = false
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textRect = Rect()
    
    // 自定义字体
    private val customTypeface = ResourcesCompat.getFont(context, R.font.custom_font)
    
    // 颜色
    private val blackColor = 0xFF000000.toInt()
    private val greenColor = 0xFF00FF00.toInt()
    private val yellowColor = 0xFFFFFF00.toInt()
    private val redColor = 0xFFFF0000.toInt()
    private val bgColor = 0xFF000000.toInt()

    fun setSpeed(speed: Float, isWarning: Boolean, isDanger: Boolean) {
        this.speed = speed
        this.isWarning = isWarning
        this.isDanger = isDanger
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val tapeWidth = width - 20f
        val tapeHeight = height - 40f

        // 绘制背景
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(10f, 20f, width - 10f, height - 20f, paint)

        // 裁剪tape区域
        canvas.save()
        canvas.clipRect(10f, 20f, width - 10f, height - 20f)

        // 计算tape偏移量（每5像素代表1km/h）
        // 翻转坐标轴：屏幕上部显示较大值，下部显示较小值
        val tapeOffset = speed * 5f
        val startY = centerY + tapeOffset

        // 绘制刻度和数字
        paint.color = greenColor
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = customTypeface
        
        // 从当前速度-50到+50的范围绘制
        val startSpeed = (speed - 50).toInt() / 10 * 10
        val endSpeed = (speed + 50).toInt() / 10 * 10 + 10

        for (s in startSpeed..endSpeed step 10) {
            val y = startY - s * 5f
            
            // 只绘制在可见范围内的刻度
            if (y > 10f && y < height - 10f) {
                // 主刻度（放大10%）
                paint.strokeWidth = 3.96f
                canvas.drawLine(centerX - 39.6f, y, centerX + 39.6f, y, paint)
                
                // 数字（放大10%）
                paint.textSize = 52.8f
                paint.style = Paint.Style.FILL
                paint.getTextBounds(s.toString(), 0, s.toString().length, textRect)
                canvas.drawText(s.toString(), centerX - 79.2f, y + textRect.height() / 2f, paint)
                
                // 次刻度（每5度，放大10%）
                if (s < endSpeed - 10) {
                    val midY = startY + (s + 5) * 5f
                    if (midY > 10f && midY < height - 10f) {
                        paint.strokeWidth = 2.64f
                        canvas.drawLine(centerX - 19.8f, midY, centerX + 19.8f, midY, paint)
                    }
                }
            }
        }

        canvas.restore()

        // 绘制当前速度指示器（固定在中心）
        paint.color = greenColor
        paint.style = Paint.Style.FILL
        
        // 三角形指针
        val path = android.graphics.Path()
        path.moveTo(centerX, centerY - 15f)
        path.lineTo(centerX - 15f, centerY - 30f)
        path.lineTo(centerX + 15f, centerY - 30f)
        path.close()
        canvas.drawPath(path, paint)

        // 绘制当前速度数值（大字体，显示在坐标轴内侧靠近屏幕中心的位置）
        val textColor = when {
            isDanger -> redColor
            isWarning -> yellowColor
            else -> greenColor
        }
        
        paint.color = textColor
        paint.textSize = 115.2f  // 放大20%（从96f到115.2f）
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        // 格式化为三位数，不足用0补齐
        val speedText = String.format("%03d", speed.toInt())
        paint.getTextBounds(speedText, 0, speedText.length, textRect)
        // 显示在坐标轴内侧靠近屏幕中心的位置（右侧），右移3个字符位（约90像素）确保第三位完整显示
        canvas.drawText(speedText, centerX + 180f, centerY + textRect.height() / 2f, paint)
        
        // 绘制单位（显示在数值下方，增加垂直间距避免重叠，放大20%）
        paint.textSize = 43.2f  // 放大20%（从36f到43.2f）
        // 调整单位显示位置，确保完整显示
        val unitText = "KM/H"
        paint.getTextBounds(unitText, 0, unitText.length, textRect)
        // 进一步增加垂直间距，确保单位完整显示，左移10像素
        canvas.drawText(unitText, centerX + 180f, centerY + textRect.height() + 60f, paint)
    }
}
