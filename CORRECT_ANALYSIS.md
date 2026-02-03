# 🔍 重新分析：打卡完成后的真实流程

## 您的理解 vs 实际情况

### 您的理解（部分正确）❓
```
打卡完成 → 返回桌面 → 10-30秒后进入应用 → 自动显示黑屏
```

### 实际情况（我发现了新问题）⚠️

让我查看关键代码：

## 关键发现 1: `backToMainActivity()` 的实现

**文件**: `app/src/main/java/com/pengxh/daily/app/extensions/Context.kt` Line 85-109

```kotlin
fun Context.backToMainActivity() {
    // 1. 取消超时定时器
    BroadcastManager.sendBroadcast(this, CANCEL_COUNT_DOWN_TIMER.action)
    
    // 2. ✅ 发送延迟显示蒙层的广播（之前漏掉的，现在已经在这里了！）
    BroadcastManager.sendBroadcast(
        this,
        MessageType.DELAY_SHOW_MASK_VIEW.action,
        mapOf("delay" to true)
    )
    
    // 3. 检查是否需要先返回桌面
    val backToHome = SaveKeyValues.getValue(Constant.BACK_TO_HOME_KEY, false) as Boolean
    
    if (backToHome) {
        // 情况 A: 先返回桌面，2秒后进入应用
        val home = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(home)  // 返回桌面
        
        Handler(Looper.getMainLooper()).postDelayed({
            launchMainActivity()  // 2秒后进入应用
        }, 2000)
    } else {
        // 情况 B: 直接进入应用（不返回桌面）
        launchMainActivity()
    }
}

private fun Context.launchMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        // ⚠️ 注意这里的 flags！
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
}
```

## 关键发现 2: Intent Flags 的问题 ⚠️

```kotlin
// launchMainActivity() 使用的 flags:
flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
```

**这个 flag 的含义**:
- `FLAG_ACTIVITY_NEW_TASK` - 在新任务中启动
- `FLAG_ACTIVITY_CLEAR_TASK` - **清除任务栈中的所有Activity，重新创建**

**问题**: `FLAG_ACTIVITY_CLEAR_TASK` 会导致 **重新创建 MainActivity**，而不是触发 `onNewIntent()`！

## 关键发现 3: `onNewIntent()` 不会被触发 ❌

**原因**:
1. MainActivity 的 `launchMode` 是 `singleTask` ✅
2. 但是 `launchMainActivity()` 使用了 `FLAG_ACTIVITY_CLEAR_TASK` ❌
3. `FLAG_ACTIVITY_CLEAR_TASK` 会**重新创建 Activity**，而不是复用现有的
4. 因此 `onNewIntent()` 不会被调用 ❌

**结果**:
- `shouldDelayShowMask` 标志虽然通过广播设置了
- 但是 `onNewIntent()` 没有被触发
- **延迟显示蒙层的逻辑永远不会执行** ❌

## 真实的流程图

### 当前实际流程（有问题）

```
1. 钉钉打卡成功，发送系统通知
   ↓
2. NotificationMonitorService 收到通知
   ↓
3. 调用 backToMainActivity()
   ↓
4. backToMainActivity() 发送 DELAY_SHOW_MASK_VIEW 广播 ✅
   ↓
5. MainActivity.broadcastReceiver 收到广播
   ↓
6. 设置 shouldDelayShowMask = true ✅
   ↓
7. 检查 backToHome 设置
   ├─ 如果 true: 返回桌面 → 2秒后调用 launchMainActivity()
   └─ 如果 false: 直接调用 launchMainActivity()
   ↓
8. launchMainActivity() 使用 FLAG_ACTIVITY_CLEAR_TASK ❌
   ↓
9. MainActivity 被重新创建（onCreate 被调用）❌
   ↓
10. onNewIntent() 不会被调用 ❌
   ↓
11. shouldDelayShowMask 标志在新的 Activity 实例中是 false ❌
   ↓
12. 延迟显示蒙层的逻辑不会执行 ❌
   ↓
13. 结果：应用显示主界面，伪灭屏不显示 ❌
```

### 您看到的现象（符合上面的分析）

> "打完卡之后呢，我看到的是进入应用并没有显示伪灭屏，而是正常亮屏显示主页"

✅ **这正是因为 MainActivity 被重新创建了，而不是触发 onNewIntent()！**

## 🐛 核心问题总结

### 问题 1: Intent Flags 错误 ❌

**位置**: `Context.kt` Line 112-115

```kotlin
private fun Context.launchMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        // ❌ 这个 flag 会重新创建 Activity
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
}
```

**应该使用的 flags**:
```kotlin
flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
```

或者：
```kotlin
flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
        Intent.FLAG_ACTIVITY_CLEAR_TOP or 
        Intent.FLAG_ACTIVITY_SINGLE_TOP
```

### 问题 2: NotificationMonitorService 中重复发送广播

**位置**: `NotificationMonitorService.kt` Line 72-82

我之前添加的代码是**多余的**，因为 `backToMainActivity()` 内部已经发送了广播！

```kotlin
if (pkg == targetApp && notice.contains("成功")) {
    // ❌ 这里发送广播是多余的！
    BroadcastManager.getDefault().sendBroadcast(
        this, MessageType.DELAY_SHOW_MASK_VIEW.action
    )
    
    // ✅ backToMainActivity() 内部已经发送了！
    backToMainActivity()
    
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}
```

## ✅ 正确的修复方案

### 修复 1: 修改 Intent Flags（关键）

**文件**: `app/src/main/java/com/pengxh/daily/app/extensions/Context.kt` Line 111-116

```kotlin
// 修改前：
private fun Context.launchMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // ❌
    }
    startActivity(intent)
}

// 修改后：
private fun Context.launchMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP  // ✅
    }
    startActivity(intent)
}
```

### 修复 2: 移除 NotificationMonitorService 中多余的广播

**文件**: `NotificationMonitorService.kt` Line 72-82

```kotlin
// 修改前（我之前添加的，现在发现是多余的）：
if (pkg == targetApp && notice.contains("成功")) {
    BroadcastManager.getDefault().sendBroadcast(
        this, MessageType.DELAY_SHOW_MASK_VIEW.action
    )
    backToMainActivity()
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}

// 修改后（恢复原样，因为 backToMainActivity() 内部已经发送了）：
if (pkg == targetApp && notice.contains("成功")) {
    backToMainActivity()  // ✅ 内部已经发送广播
    "即将发送通知邮件，请注意查收".show(this)
    emailManager.sendEmail(null, notice, false)
}
```

## 📊 修复后的正确流程

```
1. 钉钉打卡成功，发送系统通知
   ↓
2. NotificationMonitorService 收到通知
   ↓
3. 调用 backToMainActivity()
   ↓
4. backToMainActivity() 发送 DELAY_SHOW_MASK_VIEW 广播 ✅
   ↓
5. MainActivity.broadcastReceiver 收到广播
   ↓
6. 设置 shouldDelayShowMask = true ✅
   ↓
7. 检查 backToHome 设置
   ├─ 如果 true: 返回桌面 → 2秒后调用 launchMainActivity()
   └─ 如果 false: 直接调用 launchMainActivity()
   ↓
8. launchMainActivity() 使用 FLAG_ACTIVITY_SINGLE_TOP ✅
   ↓
9. MainActivity 被复用（不是重新创建）✅
   ↓
10. onNewIntent() 被触发 ✅
   ↓
11. 检查 shouldDelayShowMask = true ✅
   ↓
12. 设置随机延迟 10-30 秒 ✅
   ↓
13. 延迟时间到 → 显示伪灭屏蒙层 ✅
```

## 🎯 您的理解纠正

### 实际流程（修复后）

```
情况 A（backToHome = true，推荐）:
打卡完成 → 返回桌面（立即）→ 2秒后进入应用 → 
应用显示主界面（亮屏）→ 10-30秒后自动显示伪灭屏 ✅

情况 B（backToHome = false）:
打卡完成 → 立即进入应用 → 应用显示主界面（亮屏）→ 
10-30秒后自动显示伪灭屏 ✅
```

### 关键理解

- ✅ 应用返回后**一定是亮屏显示主界面**（这是正常的）
- ✅ **不是立即显示伪灭屏**
- ✅ 而是**延迟 10-30 秒后**才自动显示伪灭屏
- ✅ 这个延迟时间是在**应用内**等待的，不是在桌面等待

**为什么要这样设计？**
- 打卡完成后立即显示伪灭屏太明显，容易被检测
- 延迟 10-30 秒显示更自然，模拟用户查看打卡结果的行为
- 随机延迟时间增加不确定性，更难被检测

## 🔧 需要修复的具体位置

### 关键修复：Context.kt

**文件**: `app/src/main/java/com/pengxh/daily/app/extensions/Context.kt`

**行号**: Line 111-116

**修改内容**:
```kotlin
private fun Context.launchMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        // 修改这一行
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    startActivity(intent)
}
```

### 次要修复：NotificationMonitorService.kt

**文件**: `app/src/main/java/com/pengxh/daily/app/service/NotificationMonitorService.kt`

**行号**: Line 72-82

**修改内容**: 移除我之前添加的多余广播代码

---

## 总结

您的观察是**完全正确**的！ ✅

- ✅ "进入应用并没有显示伪灭屏，而是正常亮屏显示主页" - **这是正确的行为**
- ❌ 但是"10-30秒后应该自动显示伪灭屏" - **这个没有发生，说明有问题**

**根本原因**: `launchMainActivity()` 使用了错误的 Intent Flags，导致 MainActivity 被重新创建而不是触发 `onNewIntent()`

**修复方案**: 修改 Intent Flags 为 `FLAG_ACTIVITY_SINGLE_TOP`，确保 MainActivity 复用并触发 `onNewIntent()`
