package com.example.carguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

class AltitudeTapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var altitude = 0f
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textRect = Rect()
    
    // 自定义字体
    private val customTypeface = ResourcesCompat.getFont(context, R.font.custom_font)
    
    // 颜色
    private val blackColor = 0xFF000000.toInt()
    private val greenColor = 0xFF00FF00.toInt()
    private val bgColor = 0xFF000000.toInt()

    fun setAltitude(altitude: Float) {
        this.altitude = altitude
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val tapeWidth = width - 20f
        val tapeHeight = height - 40f

        // 绘制背景（使用视图实际尺寸）
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 绘制刻度和数字
        paint.color = greenColor
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = customTypeface
        
        // 从当前高度-100到+100的范围绘制
        val startAlt = (altitude - 100).toInt() / 50 * 50
        val endAlt = (altitude + 100).toInt() / 50 * 50 + 50

        for (a in startAlt..endAlt step 50) {
            // 计算相对于当前高度的偏移（每2像素代表1米）
            // 翻转坐标轴：屏幕上部显示较大值，下部显示较小值
            // 较大的值应该在上方（y较小），所以用减号
            val offset = (a - altitude) * 2f
            val y = centerY - offset
            
            // 只绘制在可见范围内的刻度
            if (y > 0f && y < height) {
                // 主刻度（放大10%）
                paint.strokeWidth = 3.96f
                canvas.drawLine(centerX - 39.6f, y, centerX + 39.6f, y, paint)
                
                // 数字（放大10%）
                paint.textSize = 52.8f
                paint.style = Paint.Style.FILL
                paint.getTextBounds(a.toString(), 0, a.toString().length, textRect)
                canvas.drawText(a.toString(), centerX + 94.2f, y + textRect.height() / 2f, paint)
                
                // 次刻度（每25米，放大10%）
                if (a < endAlt - 50) {
                    val midOffset = (a + 25 - altitude) * 2f
                    val midY = centerY - midOffset
                    if (midY > 0f && midY < height) {
                        paint.strokeWidth = 2.64f
                        canvas.drawLine(centerX - 19.8f, midY, centerX + 19.8f, midY, paint)
                    }
                }
            }
        }

        // 绘制当前高度指示器（固定在中心）
        paint.color = greenColor
        paint.style = Paint.Style.FILL
        
        // 三角形指针
        val path = android.graphics.Path()
        path.moveTo(centerX, centerY - 15f)
        path.lineTo(centerX - 15f, centerY - 30f)
        path.lineTo(centerX + 15f, centerY - 30f)
        path.close()
        canvas.drawPath(path, paint)

        // 绘制当前高度数值（大字体，显示在坐标轴内侧靠近屏幕中心的位置）
        paint.color = greenColor
        paint.textSize = 115.2f  // 放大20%（从96f到115.2f）
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        // 格式化为四位数，不足用0补齐
        val altText = String.format("%04d", altitude.toInt())
        paint.getTextBounds(altText, 0, altText.length, textRect)
        // 显示在坐标轴内侧靠近屏幕中心的位置（左侧），再右移半个字符位确保显示
        canvas.drawText(altText, centerX - 168f, centerY + textRect.height() / 2f, paint)
        
        // 绘制单位（显示在数值下方，增加垂直间距避免重叠，类似速度单位的显示方式，放大20%）
        paint.textSize = 43.2f  // 放大20%（从36f到43.2f）
        val unitText = "M"
        paint.getTextBounds(unitText, 0, unitText.length, textRect)
        // 增加垂直间距，确保单位完整显示，左移10像素
        canvas.drawText(unitText, centerX - 160f, centerY + textRect.height() + 60f, paint)
    }
}
