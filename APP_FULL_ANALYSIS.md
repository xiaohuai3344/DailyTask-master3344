# 📱 DailyTask 应用完整功能分析

## 🎯 应用核心目的

**钉钉自动打卡应用**，通过定时任务、通知监听、伪灭屏等技术实现全自动打卡功能。

---

## 📋 应用架构组件

### 1. 四个主要 Activity

1. **MainActivity** - 主界面，任务管理和执行
2. **SettingsActivity** - 设置界面
3. **EmailConfigActivity** - 邮件配置
4. **TaskConfigActivity** - 任务配置
5. **NoticeRecordActivity** - 通知记录
6. **QuestionAndAnswerActivity** - 常见问题

### 2. 四个核心 Service

1. **FloatingWindowService** - 悬浮窗服务
2. **ForegroundRunningService** - 前台保活服务
3. **NotificationMonitorService** - 通知监听服务
4. **CountDownTimerService** - 倒计时服务

### 3. 关键权限

- `SYSTEM_ALERT_WINDOW` - 悬浮窗权限
- `SYSTEM_OVERLAY_WINDOW` - 系统覆盖窗口权限
- `FOREGROUND_SERVICE` - 前台服务
- `BIND_NOTIFICATION_LISTENER_SERVICE` - 通知监听权限
- `INTERNET` / `ACCESS_NETWORK_STATE` - 网络相关

---

## 🚀 应用启动流程（从启动到结束完整分析）

### 阶段 1: 应用启动（MainActivity.onCreate）

```
1. 注册广播接收器
   └─ 监听 8 种消息类型：
      ├─ SHOW_MASK_VIEW - 显示伪灭屏蒙层
      ├─ HIDE_MASK_VIEW - 隐藏伪灭屏蒙层
      ├─ DELAY_SHOW_MASK_VIEW - 延迟显示蒙层（打卡完成后）
      ├─ RESET_DAILY_TASK - 重置每日任务
      ├─ UPDATE_RESET_TICK_TIME - 更新重置倒计时
      ├─ START_DAILY_TASK - 启动每日任务
      ├─ STOP_DAILY_TASK - 停止每日任务
      └─ CANCEL_COUNT_DOWN_TIMER - 取消倒计时器

2. 注册 EventBus
   └─ 用于监听悬浮窗倒计时事件

3. 启动核心服务
   ├─ FloatingWindowService - 悬浮窗服务（需要悬浮窗权限）
   ├─ ForegroundRunningService - 前台保活服务
   └─ CountDownTimerService - 倒计时服务（绑定）

4. 初始化手势检测器
   └─ 监听上下滑动手势控制伪灭屏

5. 加载任务数据
   └─ 从数据库加载所有打卡任务（时间点列表）

6. 显示工具栏时间和日期
   └─ 实时显示：日期 + 周几 + [工作日/周末/节假日]
```

### 阶段 2: 用户设置打卡时间点

```
1. 用户点击"添加任务"
   └─ 显示时间选择器（时:分:秒）
   
2. 选择打卡时间（如 09:00:00, 18:00:00）
   └─ 保存到数据库（SQLite）
   
3. 显示任务列表
   └─ 按时间顺序显示所有打卡时间点
```

### 阶段 3: 启动任务执行

```
1. 用户点击"启动"按钮
   
2. 检查工作日/周末/节假日设置
   ├─ 如果启用"周末暂停" + 今天是周末 → 不执行，提示"今天是周末"
   ├─ 如果启用"节假日暂停" + 今天是节假日 → 不执行，提示"今天是节假日"
   └─ 否则继续执行
   
3. 启动任务调度 (dailyTaskRunnable)
   ├─ 计算下一个待执行任务的索引
   ├─ 如果所有任务已完成 → 停止，发送"今日任务完成"邮件
   └─ 否则继续
   
4. 获取当前待执行任务
   └─ 例如：第1个任务 09:00:00
   
5. 计算时间差
   ├─ 当前时间：08:55:30
   ├─ 任务时间：09:00:00
   └─ 时间差：4分30秒 = 270秒
   
6. 启动倒计时 (CountDownTimerService)
   └─ 在悬浮窗显示："距离第1个任务还有 4分30秒"
```

### 阶段 4: 倒计时执行（CountDownTimerService）

```
1. 倒计时进行中
   ├─ 每秒更新悬浮窗显示："距离第1个任务还有 4分29秒"
   ├─ 每秒更新悬浮窗显示："距离第1个任务还有 4分28秒"
   └─ ...
   
2. 倒计时接近 0 秒（例如剩余 10 秒）
   └─ 显示伪灭屏蒙层
      ├─ 隐藏状态栏和导航栏
      ├─ 显示全屏黑色蒙层
      ├─ 蒙层上显示时钟（会自动移动位置）
      └─ 隐藏悬浮窗
      
3. 倒计时结束（0 秒）
   └─ 发送广播打开钉钉应用
```

### 阶段 5: 打开钉钉应用

```
1. 执行 openApplication() 扩展函数
   ├─ 通过包名启动钉钉应用
   └─ 钉钉应用会自动进入打卡界面（需要钉钉内部配置）
   
2. 启动超时定时器
   └─ 默认 120 秒超时
      ├─ 如果 120 秒内收到打卡成功通知 → 正常流程
      └─ 如果 120 秒后未收到通知 → 发送异常邮件
```

### 阶段 6: 监听打卡成功通知（NotificationMonitorService）

```
1. 钉钉打卡成功后会发送系统通知
   └─ 通知内容包含"成功"关键字
   
2. NotificationMonitorService 拦截到通知
   ├─ 判断：包名是钉钉 && 内容包含"成功"
   └─ 执行成功流程
   
3. 执行成功流程
   ├─ 调用 backToMainActivity() - 返回 MainActivity
   ├─ 发送 DELAY_SHOW_MASK_VIEW 广播 - 标记需要延迟显示蒙层
   └─ 发送打卡成功邮件
```

### 阶段 7: 打卡完成后返回（⚠️ 关键流程）

```
1. MainActivity.onNewIntent() 被触发
   └─ 应用从后台返回前台
   
2. 检查 shouldDelayShowMask 标志
   └─ 如果为 true（打卡完成返回）
   
3. 延迟显示蒙层（10-30 秒随机延迟）
   ├─ 取消之前的延迟任务
   ├─ 生成随机延迟时间：10000 + Random(21000) = 10-30秒
   ├─ 创建延迟任务 delayShowMaskRunnable
   └─ mainHandler.postDelayed(delayShowMaskRunnable, delayTime)
   
4. 延迟时间到达
   └─ 如果蒙层未显示 → 调用 showMaskView()
      ├─ 隐藏状态栏和导航栏
      ├─ 显示全屏黑色蒙层
      ├─ 蒙层上显示时钟
      └─ 隐藏悬浮窗
```

### 阶段 8: 执行下一个任务

```
1. 取消超时定时器
   
2. 发送 CANCEL_COUNT_DOWN_TIMER 广播
   
3. MainActivity 收到广播
   └─ 重新执行 dailyTaskRunnable
   
4. 重复阶段 3-7
   └─ 直到所有任务执行完毕
```

---

## 🎨 核心功能详细说明

### 功能 1: 伪灭屏（Mask View）

**目的**: 防止真正熄屏，保持应用前台运行

**实现方式**:
```kotlin
private fun showMaskView() {
    // 1. 隐藏悬浮窗
    BroadcastManager.sendBroadcast(HIDE_FLOATING_WINDOW)
    
    // 2. 隐藏状态栏和导航栏
    insetsController.hide(WindowInsetsCompat.Type.statusBars())
    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
    
    // 3. 显示全屏黑色蒙层
    binding.maskView.visibility = View.VISIBLE
    
    // 4. 设置最低亮度
    window.setScreenBrightness(BRIGHTNESS_OVERRIDE_OFF)
    
    // 5. 显示移动时钟（防烧屏）
    mainHandler.postDelayed(clockAnimationRunnable, 30000)
}
```

**控制方式**:
- ✅ 音量下键切换
- ✅ 上下滑动手势切换（需要开启）
- ✅ 远程指令控制

### 功能 2: 周末/节假日自动暂停

**数据来源**: `WorkdayManager` + 内置 2026 年节假日数据

**实现逻辑**:
```kotlin
private fun startExecuteTask() {
    // 1. 获取设置
    val enableWeekend = SaveKeyValues.getValue(ENABLE_WEEKEND_KEY, false)
    val enableHoliday = SaveKeyValues.getValue(ENABLE_HOLIDAY_KEY, false)
    
    // 2. 检查今天是否应该执行
    if (!WorkdayManager.shouldExecuteToday(enableWeekend, enableHoliday)) {
        val dayDesc = WorkdayManager.getTodayDescription()
        // 显示提示："今天是周末/节假日，任务不会执行"
        return
    }
    
    // 3. 继续执行任务...
}
```

**工具栏显示**:
```
周六 [周末]
周日 [周末]
2026年01月01日 [元旦]
2026年01月28日 [春节]
```

### 功能 3: 打卡完成后自动恢复暗色（⚠️ 核心功能）

**触发条件**: 收到钉钉打卡成功通知

**实现流程**:

```kotlin
// 步骤 1: NotificationMonitorService 收到打卡成功通知
if (pkg == targetApp && notice.contains("成功")) {
    // 返回主界面
    backToMainActivity()
    
    // 发送延迟显示蒙层的广播（关键！）
    // ❌ 这里没有直接发送广播！
}

// 步骤 2: MainActivity 的 broadcastReceiver 监听 DELAY_SHOW_MASK_VIEW
MessageType.DELAY_SHOW_MASK_VIEW -> {
    shouldDelayShowMask = true  // 标记需要延迟显示
}

// 步骤 3: MainActivity.onNewIntent() 触发（应用回到前台）
override fun onNewIntent(intent: Intent) {
    if (shouldDelayShowMask) {
        shouldDelayShowMask = false
        
        // 取消之前的延迟任务
        delayShowMaskRunnable?.let { mainHandler.removeCallbacks(it) }
        
        // 随机延迟 10-30 秒
        val delayTime = (10000 + Random().nextInt(21000)).toLong()
        
        delayShowMaskRunnable = Runnable {
            if (!binding.maskView.isVisible) {
                showMaskView()  // 显示伪灭屏蒙层
            }
        }
        mainHandler.postDelayed(delayShowMaskRunnable!!, delayTime)
    }
}
```

### 功能 4: 悬浮窗显示（FloatingWindowService）

**显示内容**:
- 当前状态（等待中/执行中/已完成）
- 倒计时显示
- 任务进度

### 功能 5: 远程指令控制

**支持的指令** (通过微信/QQ/企业微信等发送):
- `电量` - 查询手机电量
- `启动` - 启动任务
- `停止` - 停止任务
- `开始循环` - 开启循环任务
- `暂停循环` - 暂停循环任务
- `息屏` - 显示伪灭屏
- `亮屏` - 隐藏伪灭屏
- `考勤记录` - 查询今日打卡记录
- `打卡` (可自定义) - 立即打开钉钉

### 功能 6: 邮件通知

**通知类型**:
- 任务启动/停止通知
- 打卡成功通知
- 打卡异常通知（超时未收到通知）
- 任务完成通知
- 电量查询通知

---

## 🔍 **问题分析：为什么"打卡完成后自动恢复暗色"可能不工作？**

### 可能的问题点

#### 问题 1: 缺少 DELAY_SHOW_MASK_VIEW 广播发送

**当前代码**:
```kotlin
// NotificationMonitorService.kt Line 73-77
if (pkg == targetApp && notice.contains("成功")) {
    backToMainActivity()  // ✅ 这个会触发
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
    
    // ❌ 缺少这个：
    // BroadcastManager.sendBroadcast(this, MessageType.DELAY_SHOW_MASK_VIEW.action)
}
```

**修复方案**:
```kotlin
if (pkg == targetApp && notice.contains("成功")) {
    // 发送延迟显示蒙层的广播
    BroadcastManager.getDefault().sendBroadcast(
        this, MessageType.DELAY_SHOW_MASK_VIEW.action
    )
    
    backToMainActivity()
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}
```

#### 问题 2: onNewIntent() 未被触发

**原因**: 
- `MainActivity` 的 `launchMode` 是 `singleTask`
- `backToMainActivity()` 可能没有正确触发 `onNewIntent()`

**当前实现**:
```kotlin
// MainActivity AndroidManifest.xml
<activity
    android:name=".ui.MainActivity"
    android:launchMode="singleTask"  // ✅ 正确
    ...>
```

**检查 backToMainActivity() 实现**:
```kotlin
fun backToMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    startActivity(intent)
}
```

#### 问题 3: 延迟显示可能被中断

**场景**:
- 用户在延迟期间操作手机
- 系统回收 MainActivity
- Handler 被清理

**当前保护措施**:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    // ✅ 已经有清理代码
    delayShowMaskRunnable?.let { mainHandler.removeCallbacks(it) }
}
```

#### 问题 4: shouldDelayShowMask 标志未正确设置

**触发条件**:
1. NotificationMonitorService 收到打卡成功通知
2. 发送 `DELAY_SHOW_MASK_VIEW` 广播
3. MainActivity 的 `broadcastReceiver` 收到广播
4. 设置 `shouldDelayShowMask = true`

**可能的问题**:
- 广播未发送 ❌
- MainActivity 未注册该广播 (已注册 ✅)
- 广播发送时 MainActivity 不在前台 ❌

---

## 🐛 发现的核心问题

### **问题：NotificationMonitorService 中缺少发送 DELAY_SHOW_MASK_VIEW 广播**

**位置**: `NotificationMonitorService.kt` Line 73-77

**当前代码**:
```kotlin
if (pkg == targetApp && notice.contains("成功")) {
    backToMainActivity()
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}
```

**缺少的代码**:
```kotlin
// 应该在调用 backToMainActivity() 之前发送广播
BroadcastManager.getDefault().sendBroadcast(
    this, MessageType.DELAY_SHOW_MASK_VIEW.action
)
```

**完整修复后的代码**:
```kotlin
if (pkg == targetApp && notice.contains("成功")) {
    // 1. 先发送延迟显示蒙层的广播（关键！）
    BroadcastManager.getDefault().sendBroadcast(
        this, MessageType.DELAY_SHOW_MASK_VIEW.action
    )
    
    // 2. 返回主界面
    backToMainActivity()
    
    // 3. 发送通知邮件
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}
```

---

## ✅ 修复方案

需要修改 `NotificationMonitorService.kt` 的第 72-77 行：

```kotlin
// 目标应用打卡通知
if (pkg == targetApp && notice.contains("成功")) {
    // ⚠️ 先发送延迟显示蒙层的广播，确保 MainActivity 收到标志
    BroadcastManager.getDefault().sendBroadcast(
        this, MessageType.DELAY_SHOW_MASK_VIEW.action
    )
    
    // 然后返回主界面（会触发 onNewIntent）
    backToMainActivity()
    
    // 最后发送邮件通知
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}
```

**修复后的完整流程**:

```
1. 钉钉发送打卡成功通知
   ↓
2. NotificationMonitorService 拦截通知
   ↓
3. 发送 DELAY_SHOW_MASK_VIEW 广播 ✅
   ↓
4. MainActivity.broadcastReceiver 收到广播
   ↓
5. 设置 shouldDelayShowMask = true ✅
   ↓
6. backToMainActivity() 触发 onNewIntent()
   ↓
7. onNewIntent() 检查 shouldDelayShowMask
   ↓
8. 设置随机延迟 10-30 秒
   ↓
9. 延迟时间到 → 显示伪灭屏蒙层 ✅
```

---

## 📊 完整流程时间线

```
08:55:00 - 应用启动，显示伪灭屏
08:55:30 - 用户添加任务：09:00:00, 18:00:00
08:56:00 - 用户点击"启动"
08:56:01 - 开始倒计时（距离第1个任务 3分59秒）
08:59:50 - 倒计时剩余 10 秒，显示伪灭屏蒙层
09:00:00 - 倒计时结束，打开钉钉应用
09:00:05 - 钉钉打卡成功，发送系统通知
09:00:06 - NotificationMonitorService 收到通知
09:00:06 - ❌ 应该发送 DELAY_SHOW_MASK_VIEW 广播（当前缺少）
09:00:06 - 调用 backToMainActivity()，触发 onNewIntent()
09:00:06 - ❌ onNewIntent() 检查 shouldDelayShowMask（因为没有广播，所以是 false）
09:00:06 - ❌ 立即显示伪灭屏（不是延迟 10-30 秒）
```

**修复后的时间线**:
```
09:00:06 - NotificationMonitorService 收到通知
09:00:06 - ✅ 发送 DELAY_SHOW_MASK_VIEW 广播
09:00:06 - ✅ MainActivity.broadcastReceiver 收到广播，设置 shouldDelayShowMask = true
09:00:06 - ✅ 调用 backToMainActivity()，触发 onNewIntent()
09:00:06 - ✅ onNewIntent() 检查 shouldDelayShowMask（为 true）
09:00:06 - ✅ 设置随机延迟 15 秒（10-30 秒之间）
09:00:21 - ✅ 延迟时间到，自动显示伪灭屏蒙层
```

---

## 🎯 总结

**核心问题**: `NotificationMonitorService` 在收到打卡成功通知后，没有发送 `DELAY_SHOW_MASK_VIEW` 广播，导致 `MainActivity` 无法知道需要延迟显示蒙层。

**修复位置**: `app/src/main/java/com/pengxh/daily/app/service/NotificationMonitorService.kt` Line 72-77

**修复内容**: 在 `backToMainActivity()` 之前添加广播发送代码

**影响**: 修复后，打卡完成后会延迟 10-30 秒（随机）再显示伪灭屏蒙层，更加自然，不易被检测。
