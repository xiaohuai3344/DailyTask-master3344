# DailyTask - 编译说明文档

## ✅ 开发环境配置完成状态

### 已完成
- ✅ Java 17 已安装并配置
- ✅ Gradle 构建脚本已准备就绪
- ✅ 源代码已完成周末/节假日功能开发
- ✅ Git 提交已完成（2个commits）

### 待配置
- ⚠️ Android SDK 需要在本地环境安装

---

## 📋 编译环境要求

### 必需软件

1. **Java Development Kit (JDK)**
   - 版本：JDK 17+
   - 推荐：OpenJDK 17 或 Oracle JDK 17
   - 下载：https://adoptium.net/ 或 https://www.oracle.com/java/technologies/downloads/

2. **Android Studio**
   - 版本：Hedgehog (2023.1.1) 或更新版本
   - 下载：https://developer.android.com/studio
   - 包含：Android SDK、Android SDK Platform-Tools、Android SDK Build-Tools

3. **Git**
   - 用于版本控制和代码同步
   - 下载：https://git-scm.com/downloads

---

## 🔧 环境配置步骤

### 步骤1：安装Java 17

#### Windows
```powershell
# 使用 Chocolatey
choco install openjdk17

# 或手动下载安装
# https://adoptium.net/temurin/releases/?version=17
```

#### macOS
```bash
# 使用 Homebrew
brew install openjdk@17

# 配置环境变量
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
```

#### Linux (Ubuntu/Debian)
```bash
# 使用 apt
sudo apt-get update
sudo apt-get install openjdk-17-jdk

# 验证安装
java -version
```

### 步骤2：安装Android Studio

1. 下载Android Studio：https://developer.android.com/studio
2. 运行安装程序
3. 选择"Standard"安装类型
4. 等待SDK组件下载完成（约3-5GB）

### 步骤3：配置Android SDK

#### 方法1：通过Android Studio（推荐）

1. 打开Android Studio
2. 进入 `Tools` > `SDK Manager`
3. 在 `SDK Platforms` 标签页选择：
   - ✅ Android 15.0 (API 36)
   - ✅ Android 14.0 (API 34)
   - ✅ Android 13.0 (API 33)
4. 在 `SDK Tools` 标签页选择：
   - ✅ Android SDK Build-Tools 34+
   - ✅ Android SDK Platform-Tools
   - ✅ Android SDK Command-line Tools
5. 点击 `Apply` 开始下载

#### 方法2：配置 local.properties

在项目根目录创建 `local.properties` 文件：

**Windows**:
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

**macOS**:
```properties
sdk.dir=/Users/YourUsername/Library/Android/sdk
```

**Linux**:
```properties
sdk.dir=/home/YourUsername/Android/Sdk
```

或设置环境变量：
```bash
export ANDROID_HOME=/path/to/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

---

## 🚀 编译步骤

### 1. 克隆项目（如果还没有）

```bash
git clone <repository-url>
cd DailyTask
```

### 2. 检查环境

```bash
# 验证Java版本
java -version
# 应该显示：openjdk version "17.x.x"

# 验证Gradle
./gradlew --version

# 验证Android SDK
echo $ANDROID_HOME
# 或 ls $ANDROID_HOME
```

### 3. 编译Debug版本

```bash
# 清理项目
./gradlew clean

# 编译Debug APK
./gradlew assembleDailyDebug

# 成功后，APK位于：
# app/build/outputs/apk/daily/debug/DT_YYYYMMDD_2.2.6.0.apk
```

### 4. 编译Release版本（签名版本）

```bash
# 确保签名密钥存在
ls app/DailyTask.jks

# 编译Release APK
./gradlew assembleDailyRelease

# 成功后，APK位于：
# app/build/outputs/apk/daily/release/DT_YYYYMMDD_2.2.6.0.apk
```

---

## 📦 编译输出

### APK命名格式
```
DT_YYYYMMDD_X.X.X.X.apk
```

示例：`DT_20260130_2.2.6.0.apk`

### APK位置

**Debug版本**：
```
app/build/outputs/apk/daily/debug/
```

**Release版本**：
```
app/build/outputs/apk/daily/release/
```

---

## 🐛 常见问题解决

### 问题1：SDK location not found

**错误信息**：
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME 
environment variable or by setting the sdk.dir path in your project's 
local properties file
```

**解决方案**：
1. 创建 `local.properties` 文件
2. 添加 SDK 路径：`sdk.dir=/path/to/android/sdk`
3. 或设置环境变量：`export ANDROID_HOME=/path/to/android/sdk`

### 问题2：Java版本不匹配

**错误信息**：
```
Android Gradle plugin requires Java 17 to run. 
You are currently using Java 11.
```

**解决方案**：
```bash
# 安装Java 17
sudo apt-get install openjdk-17-jdk

# 或设置Gradle使用Java 17
echo "org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64" >> gradle.properties
```

### 问题3：Build Tools版本缺失

**错误信息**：
```
Failed to find Build Tools revision X.X.X
```

**解决方案**：
1. 打开Android Studio SDK Manager
2. 安装对应版本的Build Tools
3. 或修改 `build.gradle` 中的 `buildToolsVersion`

### 问题4：网络问题导致依赖下载失败

**解决方案**：
```bash
# 使用国内镜像（在项目根目录 build.gradle）
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        google()
        mavenCentral()
    }
}
```

### 问题5：Gradle Daemon问题

**解决方案**：
```bash
# 停止所有Gradle Daemon
./gradlew --stop

# 重新编译
./gradlew clean assembleDailyDebug
```

---

## 🔍 验证编译结果

### 1. 检查APK是否生成

```bash
ls -lh app/build/outputs/apk/daily/debug/
```

### 2. 查看APK信息

```bash
# 使用aapt（Android Asset Packaging Tool）
aapt dump badging app/build/outputs/apk/daily/debug/DT_*.apk | grep -E "package|versionCode|versionName"
```

### 3. 安装到设备测试

```bash
# 通过ADB安装
adb install app/build/outputs/apk/daily/debug/DT_*.apk

# 查看日志
adb logcat | grep DailyTask
```

---

## 📱 测试设备要求

### 最低要求
- Android 8.0 (API 26) 或更高版本
- 1GB RAM 或以上
- 50MB 可用存储空间

### 推荐配置
- Android 12+ (API 31+)
- 2GB RAM 或以上
- 支持通知权限管理
- 支持悬浮窗权限

---

## 🎯 Android Studio中打开项目

### 步骤1：打开项目

1. 启动Android Studio
2. 选择 `File` > `Open`
3. 导航到项目目录
4. 选择项目根目录（包含build.gradle的目录）
5. 点击 `OK`

### 步骤2：等待同步

- Android Studio会自动同步Gradle
- 首次同步需要下载依赖（可能需要5-10分钟）
- 等待底部状态栏显示 "Gradle sync finished"

### 步骤3：运行项目

1. 连接Android设备或启动模拟器
2. 点击工具栏的 ▶️ (Run) 按钮
3. 或使用快捷键：`Shift + F10` (Windows/Linux) 或 `Control + R` (macOS)

---

## 📊 项目结构说明

```
DailyTask/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/           # Kotlin/Java源代码
│   │   │   │   └── com/pengxh/daily/app/
│   │   │   │       ├── sqlite/  # 数据库相关
│   │   │   │       ├── utils/   # 工具类（含WorkdayManager）
│   │   │   │       ├── ui/      # Activity界面
│   │   │   │       └── service/ # 后台服务
│   │   │   ├── res/            # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   └── test/               # 单元测试
│   ├── build.gradle            # 应用级构建配置
│   └── DailyTask.jks          # 签名密钥文件
├── gradle/                    # Gradle Wrapper
├── build.gradle              # 项目级构建配置
├── settings.gradle           # 项目设置
└── local.properties         # 本地配置（需创建）
```

---

## 🌟 新功能验证清单

编译完成后，请验证以下新功能：

### ✅ 周末/节假日功能

1. **配置界面**
   - [ ] 打开应用 → 设置 → 任务配置
   - [ ] 查看是否有"周末打卡"开关
   - [ ] 查看是否有"节假日打卡"开关

2. **主界面显示**
   - [ ] 主界面顶部工具栏显示日期类型
   - [ ] 格式：`星期X [工作日/周末/节假日]`

3. **任务调度测试**
   - [ ] 在周末关闭"周末打卡"，点击"启动"
   - [ ] 应提示："今天是周末，已设置为休息日..."

4. **节假日数据**
   - [ ] 修改系统日期到2026-02-15（春节）
   - [ ] 查看主界面是否显示"[春节]"
   - [ ] 关闭"节假日打卡"，点击"启动"
   - [ ] 应提示："今天是春节，已设置为休息日..."

5. **调休工作日**
   - [ ] 修改系统日期到2026-02-14（周六，春节调休）
   - [ ] 查看主界面是否显示"[工作日（调休）]"
   - [ ] 点击"启动"应正常执行

---

## 📖 相关文档

- **功能说明**：`WEEKEND_HOLIDAY_FEATURE.md`
- **使用指南**：`WEEKEND_HOLIDAY_USAGE.md`
- **数据修正**：`HOLIDAY_DATA_CORRECTION.md`
- **升级计划**：`FEATURE_UPGRADE_PLAN.md`

---

## 🆘 获取帮助

### 技术支持

1. **查看文档**：项目根目录的 Markdown 文档
2. **查看日志**：`adb logcat | grep DailyTask`
3. **QQ群**：560354109（①群）、643595483（②群）
4. **GitHub Issues**：提交详细的问题描述

### 提交问题时请包含

- 设备信息（品牌、型号、Android版本）
- 错误日志（logcat输出）
- 复现步骤
- 预期结果 vs 实际结果

---

## 📝 版本信息

- **当前版本**：2.2.6.0
- **最低支持**：Android 8.0 (API 26)
- **目标版本**：Android 15 (API 36)
- **编译工具**：AGP 8.x + Gradle 8.x
- **开发语言**：Kotlin + Java

---

## ✨ 快速命令参考

```bash
# 清理项目
./gradlew clean

# 编译Debug版本
./gradlew assembleDailyDebug

# 编译Release版本
./gradlew assembleDailyRelease

# 安装Debug版本到设备
./gradlew installDailyDebug

# 查看所有任务
./gradlew tasks

# 查看依赖
./gradlew app:dependencies

# 停止Gradle Daemon
./gradlew --stop
```

---

**文档创建时间**：2026-01-30  
**适用版本**：DailyTask 2.2.6.0+  
**Java要求**：JDK 17+  
**Gradle版本**：8.0+
