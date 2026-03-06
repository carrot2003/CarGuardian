![ae8d01a1b4809329846664b8546a2356](https://github.com/user-attachments/assets/46caeea2-ee4f-4a36-ad0a-68a480b37e53)

# CarGuardian

CarGuardian是一款为驾驶员设计的车辆姿态监控应用，提供实时的姿态仪表显示，包括俯仰角、横滚角、速度、高度和航向等信息。

## 功能特性

- **姿态指示器**：实时显示车辆的俯仰角和横滚角
- **速度表**：显示当前车速，超过80km/h时显示黄色警告，超过120km/h时显示红色警告并闪烁
- **高度表**：显示当前车辆海拔高度
- **航向指示器**：显示车辆当前航向
- **GPS信息**：显示当前连接的卫星数量
- **日期时间**：实时显示当前日期、星期和时间
- **归零功能**：可以校准姿态仪表的零点位置
- **全屏显示**：应用启动后自动进入全屏模式，保持屏幕常亮

## 技术实现

- **开发语言**：Kotlin
- **传感器**：加速度传感器、磁场传感器、陀螺仪传感器
- **定位**：GPS定位系统
- **UI**：自定义View实现各种仪表显示
- **数据存储**：SharedPreferences存储归零点数据

## 项目结构

```
CarGuardian/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/carguardian/
│   │   │   │   ├── MainActivity.kt          # 主活动
│   │   │   │   ├── AttitudeIndicatorView.kt  # 姿态指示器视图
│   │   │   │   ├── SpeedTapeView.kt          # 速度表视图
│   │   │   │   ├── AltitudeTapeView.kt       # 高度表视图
│   │   │   │   ├── AttitudeView.kt           # 姿态视图
│   │   │   │   ├── CompassView.kt            # 指南针视图
│   │   │   │   ├── CombinedPFDView.kt        # 组合PFD视图
│   │   │   │   └── BootReceiver.kt           # 开机启动接收器
│   │   │   ├── res/                          # 资源文件
│   │   │   └── AndroidManifest.xml           # 应用清单
│   └── build.gradle.kts                      # 应用构建配置
└── build.gradle.kts                          # 项目构建配置
```

## 核心功能说明

### 姿态计算
- 使用加速度传感器计算车辆的俯仰角和横滚角
- 使用磁场传感器计算航向
- 应用阻尼效果，使数值变化更加平滑

### 速度和高度
- 通过GPS获取当前速度（转换为km/h）
- 通过GPS获取当前海拔高度
- 速度超过阈值时显示警告

### 归零功能
- 点击归零按钮可以设置当前姿态为零点
- 归零点数据会保存到SharedPreferences中
- 首次启动时会自动归零（等待2秒让传感器稳定）

### 界面显示
- 姿态指示器使用自定义View绘制
- 速度表和高度表使用滚动条效果
- 实时更新日期时间信息
- 全屏显示，隐藏导航栏

## 使用方法

1. 安装应用到Android设备
2. 授予应用位置权限
3. 启动应用，等待传感器初始化
4. 点击归零按钮校准姿态仪表
5. 驾驶过程中，应用会实时显示车辆状态

## 权限要求

- `ACCESS_FINE_LOCATION`：获取精确的GPS位置信息
- `ACCESS_COARSE_LOCATION`：获取粗略的位置信息

## 注意事项

- 应用需要在车辆行驶过程中使用，以获取准确的GPS数据
- 首次使用时可能需要几分钟时间来获取GPS信号
- 为获得最佳效果，建议将设备固定在车辆仪表盘上
- 速度警告阈值可根据需要在代码中调整

## 未来计划

- 添加更多仪表显示选项
- 实现数据记录和分析功能
- 添加夜间模式
- 支持自定义警告阈值
- 实现仪表盘主题切换

## 开发环境

- Android Studio
- Kotlin
- Android SDK 34+
- 最小支持Android版本：API 21+

## 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目！

## 许可证

MIT License
