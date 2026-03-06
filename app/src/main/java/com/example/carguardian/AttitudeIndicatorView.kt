package com.example.carguardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

class AttitudeIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var roll = 0f
    private var pitch = 0f
    private var heading = 0f
    
    // 平滑过渡的航向值（用于阻尼效果）
    private var smoothedHeading = 0f
    private val headingDampingFactor = 0.1f  // 航向阻尼系数
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    // 自定义字体
    private val customTypeface = ResourcesCompat.getFont(context, R.font.custom_font)
    
    // 参照GPS-PFD的颜色
    private val skyColor = 0xFFA0E0FF.toInt()
    private val terrainColor = 0xFFFFE080.toInt()
    private val blackColor = 0xFF000000.toInt()
    private val whiteColor = 0xFFFFFFFF.toInt()
    private val greenColor = 0xFF00FF00.toInt()

    fun setAttitude(roll: Float, pitch: Float) {
        this.roll = roll
        this.pitch = pitch
        invalidate()
    }

    fun setHeading(heading: Float) {
        this.heading = heading
        // 应用阻尼效果，处理角度归一化问题
        // 计算最短路径的角度差
        var diff = heading - smoothedHeading
        // 处理角度跳变（如从359度到1度）
        if (diff > 180f) {
            diff -= 360f
        } else if (diff < -180f) {
            diff += 360f
        }
        smoothedHeading = smoothedHeading + diff * headingDampingFactor
        // 归一化到0-360度范围
        if (smoothedHeading < 0) smoothedHeading += 360f
        if (smoothedHeading >= 360) smoothedHeading -= 360f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f - 5f  // 水平仪整体下移5像素（从-10f改为-5f）
        // 重新计算显示区域，确保不显示到屏幕的外侧，圆形整体缩小5%
        val radius = minOf(width, height) / 2f * 0.9f - 20f

        // 绘制圆形背景
        paint.color = 0xFF000000.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 创建圆形裁剪区域
        path.reset()
        path.addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
        
        // 保存画布状态并应用裁剪
        canvas.save()
        canvas.clipPath(path)
        
        // 应用横滚旋转
        canvas.rotate(-roll, centerX, centerY)
        
        // 计算地平线位置（根据俯仰角）
        val horizonOffset = pitch * 7.8f  // 7.8像素每度，垂直放大30%，确保30度范围完全显示在圆圈内
        val horizonY = centerY + horizonOffset

        // 绘制水平线（绿色）
        paint.color = greenColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawLine(centerX - radius, horizonY, centerX + radius, horizonY, paint)

        // 绘制俯仰角刻度（最大坐标值为正负30度，绿色）
        paint.color = greenColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        
        // 绘制从-30度到+30度的刻度
        for (i in -30..30 step 5) {
            val yOffset = i * 7.8f
            val y = centerY + yOffset
            
            // 只绘制在可见范围内的刻度
            if (y > centerY - radius && y < centerY + radius) {
                val isMain = i % 10 == 0
                // 俯仰刻度水平方向缩小20%，确保不超出圆圈范围
                val lineWidth = if (isMain) radius * 0.392f else radius * 0.256f
                
                canvas.drawLine(centerX - lineWidth, y, centerX + lineWidth, y, paint)
                
                // 绘制数字（每10度，绿色，放大5%）
                if (isMain && i != 0) {
                    paint.style = Paint.Style.FILL
                    paint.textSize = 43.47f  // 放大5%（从41.4f到43.47f）
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(Math.abs(i).toString(), centerX - lineWidth - 19f, y + 13f, paint)
                    canvas.drawText(Math.abs(i).toString(), centerX + lineWidth + 19f, y + 13f, paint)
                    paint.style = Paint.Style.STROKE
                }
            }
        }

        // 恢复画布状态
        canvas.restore()

        // 绘制横滚角刻度（固定在圆周上，最大坐标值为正负30度，绿色）
        paint.color = greenColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        
        for (i in -30..30 step 10) {
            val angle = Math.toRadians(i.toDouble())
            val isMain = i % 30 == 0
            val innerRadius = radius - (if (isMain) 30f else 15f)
            
            val startX = centerX + Math.cos(angle).toFloat() * innerRadius
            val startY = centerY + Math.sin(angle).toFloat() * innerRadius
            val endX = centerX + Math.cos(angle).toFloat() * (radius - 5f)
            val endY = centerY + Math.sin(angle).toFloat() * (radius - 5f)
            
            paint.strokeWidth = if (isMain) 3f else 2f
            canvas.drawLine(startX, startY, endX, endY, paint)
            
            // 绘制横滚角数值（显示在圆圈内部，绿色，放大5%）
            // 显示10度、20度和30度的数值
            if (i != 0) {
                paint.style = Paint.Style.FILL
                paint.color = greenColor
                paint.textSize = 43.47f  // 放大5%（从41.4f到43.47f）
                paint.textAlign = Paint.Align.CENTER
                
                // 计算数值显示位置（圆圈内部）
                val textRadius = radius - 55f  // 调整位置避免遮挡
                val textX = centerX + Math.cos(angle).toFloat() * textRadius
                val textY = centerY + Math.sin(angle).toFloat() * textRadius + 13f
                
                canvas.drawText(Math.abs(i).toString(), textX, textY, paint)
                
                // 恢复绿色用于绘制刻度线
                paint.color = greenColor
                paint.style = Paint.Style.STROKE
            }
        }

        // 绘制横滚角圆环（绿色）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = greenColor
        canvas.drawCircle(centerX, centerY, radius - 5f, paint)

        // 绘制航向刻度（N、E、S、W）- 先绘制刻度线
        // 标准地图布局：上为北N，右为东E，下为南S，左为西W
        val directions = arrayOf("N", "E", "S", "W")
        val angles = arrayOf(270f, 0f, 90f, 180f)
        
        for (i in directions.indices) {
            val angle = Math.toRadians(angles[i].toDouble())
            val innerRadius = radius + 10f
            val outerRadius = radius + 25f
            
            val startX = centerX + Math.cos(angle).toFloat() * innerRadius
            val startY = centerY + Math.sin(angle).toFloat() * innerRadius
            val endX = centerX + Math.cos(angle).toFloat() * outerRadius
            val endY = centerY + Math.sin(angle).toFloat() * outerRadius
            
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        
        // 绘制航向指示V形箭头（圆圈内侧滑动，指示当前的航向）
        paint.style = Paint.Style.STROKE  // 空心箭头
        paint.strokeWidth = 2f
        paint.color = greenColor
        val headingAngle = Math.toRadians(smoothedHeading.toDouble())  // 使用平滑过渡后的航向值
        
        // 重新绘制V形箭头：V型尖角指向水平仪圆形内侧，两边边长各100像素，夹角30度
        // 箭头顶端在圆圈内部一定距离
        val arrowTipRadius = radius - 50f
        val arrowTipX = centerX + Math.cos(headingAngle).toFloat() * arrowTipRadius
        val arrowTipY = centerY + Math.sin(headingAngle).toFloat() * arrowTipRadius
        
        // 箭头两条边的长度为50像素（缩短50%），夹角30度
        val arrowLength = 50f
        val halfAngle = Math.toRadians(15.0)  // 半角15度
        
        // 计算第一条边的终点（向圆心方向延伸）
        val angle1 = headingAngle + Math.PI + halfAngle  // 反向+半角
        val end1X = arrowTipX + Math.cos(angle1).toFloat() * arrowLength
        val end1Y = arrowTipY + Math.sin(angle1).toFloat() * arrowLength
        
        // 计算第二条边的终点（向圆心方向延伸）
        val angle2 = headingAngle + Math.PI - halfAngle  // 反向-半角
        val end2X = arrowTipX + Math.cos(angle2).toFloat() * arrowLength
        val end2Y = arrowTipY + Math.sin(angle2).toFloat() * arrowLength
        
        // 绘制V形箭头（两条边从尖端向圆心方向延伸）
        path.reset()
        path.moveTo(arrowTipX, arrowTipY)
        path.lineTo(end1X, end1Y)
        path.moveTo(arrowTipX, arrowTipY)
        path.lineTo(end2X, end2Y)
        
        canvas.drawPath(path, paint)

        // 绘制横滚指针（固定在顶部）
        paint.style = Paint.Style.FILL
        paint.color = blackColor
        path.reset()
        path.moveTo(centerX, centerY - radius + 15f)
        path.lineTo(centerX - 10f, centerY - radius + 35f)
        path.lineTo(centerX + 10f, centerY - radius + 35f)
        path.close()
        canvas.drawPath(path, paint)

        // 飞机符号已删除（水平仪中心的两条白色线条）
        
        // 绘制方向文字（N、E、S、W）- 最后绘制，确保在顶层不被遮挡
        paint.style = Paint.Style.FILL
        paint.textSize = 50.4f  // 放大5%（从48f到50.4f）
        paint.textAlign = Paint.Align.CENTER
        paint.color = greenColor
        paint.typeface = customTypeface
        
        for (i in directions.indices) {
            val angle = Math.toRadians(angles[i].toDouble())
            // 调整文字位置，确保N和S不被裁剪，放大后需要调整位置
            val textRadius = radius + 44f  // 调整位置避免遮挡
            val textX = centerX + Math.cos(angle).toFloat() * textRadius
            var textY = centerY + Math.sin(angle).toFloat() * textRadius + 13f
            // N字符下移6像素（放大5%后调整）
            if (directions[i] == "N") {
                textY += 6f  // 下移6像素
            }
            // S字符下移4像素（上移5像素）
            if (directions[i] == "S") {
                textY += 4f  // 下移4像素
            }
            canvas.drawText(directions[i], textX, textY, paint)
        }
    }
}
