# SatFinder - 卫星寻星器

Android卫星寻星指示器应用，默认卫星为中星9C（户户通），同时包含全球常用直播电视卫星。

## 功能特性

- **AR寻星指示**：使用相机+传感器实时显示卫星在天空中的位置
- **精确计算**：基于球面三角学的方位角、仰角、极化角计算
- **GPS定位**：自动获取当前位置
- **传感器融合**：加速度计+磁力计融合，磁偏角修正
- **全球卫星**：内置20+颗全球常用直播电视卫星数据
- **边缘指示**：卫星不在视场内时显示方向箭头

## 默认卫星：中星9C (Chinasat 9C)

- 轨道位置：92.2°E
- 频段：Ku BSS / Ka BSS
- 极化：圆极化
- 覆盖：中国全境、南海

## 支持的卫星

### 中国及周边
| 卫星 | 轨道经度 | 频段 |
|------|---------|------|
| 中星9C | 92.2°E | Ku BSS/Ka BSS |
| 中星9B | 101.4°E | Ku BSS |
| 中星6E | 115.5°E | C/Ku |
| 中星6B | 115.5°E | C/Ku |
| 中星6C | 130.0°E | C |
| 亚太6C | 134.0°E | C/Ku/Ka |
| 亚太9号 | 142.0°E | C/Ku |
| 亚洲9号 | 122.0°E | Ku/C/Ka |
| 泰星5号 | 78.5°E | Ku/C |
| GSAT-30 | 83.0°E | Ku/C |

### 欧洲
| 卫星 | 轨道经度 | 频段 |
|------|---------|------|
| Hot Bird 13F/13G | 13.0°E | Ku |
| Astra 1 | 19.2°E | Ku |
| Astra 2 | 28.2°E | Ku |
| Eutelsat 7 West A | 7.0°W | Ku/C |

### 非洲/中东
| 卫星 | 轨道经度 | 频段 |
|------|---------|------|
| Nilesat 102/201 | 7.0°W | Ku |
| Badr-5/6/7 | 26.0°E | Ku/C |
| Intelsat 20 | 68.5°E | Ku/C |

### 美洲
| 卫星 | 轨道经度 | 频段 |
|------|---------|------|
| Galaxy 19 | 97.0°W | Ku |
| SES-1 | 101.0°W | Ku/C |
| Intelsat 905 | 137.0°W | Ku/C |

## 技术栈

- **语言**：Kotlin
- **UI框架**：Jetpack Compose
- **相机**：CameraX
- **定位**：Google Play Services Location (FusedLocationProviderClient)
- **传感器**：SensorManager (加速度计 + 磁力计)
- **CI/CD**：GitHub Actions

## 计算算法验证

方位角和仰角基于标准球面三角学公式：
- 方位角：Az = atan2(sin(Δλ), tan(φ)·cos(Δλ))
- 仰角：El = atan((cos(Δλ)·cos(φ) - R_E/R_c) / sin(γ))
- 极化角：Skew = atan(sin(Δλ) / tan(φ))

其中 R_E = 6371km（地球半径），R_c = 42157km（地心到卫星距离）

## 构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# 运行测试
./gradlew testDebugUnitTest
```

## 许可证

MIT License
