# Bug修复说明 - 打卡后自动恢复暗色功能

## 修复日期
2026年02月02日

## 问题描述
在钉钉自动打卡完成之后，软件界面会直接变亮，需要手动操作才能变回暗色隐藏屏幕。

### 期望行为
- 打卡时允许界面变亮（以便查看打卡结果）
- 打卡成功后等待 10-30 秒（随机时间）
- 然后自动恢复暗色（黑色蒙层）

## 修复内容

### 1. 新增消息类型 (`MessageType.kt`)
在 `app/src/main/java/com/pengxh/daily/app/utils/MessageType.kt` 中添加：
```kotlin
DELAY_SHOW_MASK_VIEW("com.pengxh.daily.app.BROADCAST_DELAY_SHOW_MASK_VIEW_ACTION")
```

### 2. 修改打卡返回逻辑 (`Context.kt`)
在 `app/src/main/java/com/pengxh/daily/app/extensions/Context.kt` 的 `backToMainActivity()` 方法中：
- 添加延迟显示蒙层的广播通知
- 通知 MainActivity 需要延迟恢复暗色

### 3. 修改主界面逻辑 (`MainActivity.kt`)
在 `app/src/main/java/com/pengxh/daily/app/ui/MainActivity.kt` 中：

#### 添加新变量
```kotlin
private var delayShowMaskRunnable: Runnable? = null
private var shouldDelayShowMask = false
```

#### 注册新消息类型
在 `actions` 列表中添加 `MessageType.DELAY_SHOW_MASK_VIEW.action`

#### 处理延迟显示蒙层消息
在 `broadcastReceiver` 中添加：
```kotlin
MessageType.DELAY_SHOW_MASK_VIEW -> {
    shouldDelayShowMask = true
    Log.d(kTag, "onReceive: 收到延迟显示蒙层请求")
}
```

#### 修改 `onNewIntent()` 方法
- 检查 `shouldDelayShowMask` 标志
- 如果是打卡返回，延迟 10-30 秒（随机）后显示蒙层
- 否则立即显示蒙层（保持原有行为）

#### 清理延迟任务
在 `onDestroy()` 中清理 `delayShowMaskRunnable`

## 技术细节

### 延迟时间
- 使用随机延迟时间：10-30 秒
- 计算公式：`10000 + Random().nextInt(21000)` 毫秒

### 流程说明
1. 打卡完成 → NotificationMonitorService 收到打卡成功通知
2. 调用 `backToMainActivity()` → 发送 `DELAY_SHOW_MASK_VIEW` 广播
3. MainActivity 设置 `shouldDelayShowMask = true`
4. `onNewIntent()` 被调用 → 启动延迟任务（10-30秒）
5. 延迟时间到 → 自动调用 `showMaskView()` 恢复暗色

### 日志输出
添加了详细的日志输出，便于调试：
- "收到延迟显示蒙层请求"
- "打卡完成，延迟显示蒙层"
- "将在 X 秒后恢复暗色"
- "延迟时间到，自动恢复暗色"

## 测试建议

1. **正常打卡流程测试**
   - 启动任务
   - 等待自动打卡
   - 观察打卡完成后界面保持亮屏
   - 确认 10-30 秒后自动恢复暗色

2. **手动打卡测试**
   - 发送"打卡"指令
   - 观察行为是否与自动打卡一致

3. **边界情况测试**
   - 延迟期间手动切换亮屏/暗屏
   - 延迟期间重启应用
   - 多次连续打卡

## 兼容性
- 不影响其他功能
- 保持原有"息屏"/"亮屏"指令正常工作
- 保持手动按音量下键切换功能正常工作

## 文件修改清单
1. `app/src/main/java/com/pengxh/daily/app/utils/MessageType.kt`
2. `app/src/main/java/com/pengxh/daily/app/extensions/Context.kt`
3. `app/src/main/java/com/pengxh/daily/app/ui/MainActivity.kt`

## 注意事项
- 延迟时间为随机值（10-30秒），避免固定时间被检测
- 如果用户在延迟期间手动操作，不会影响手动操作结果
- 延迟任务在 Activity 销毁时会被正确清理，避免内存泄漏
