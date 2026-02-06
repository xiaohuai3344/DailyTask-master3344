package com.pengxh.daily.app.service

import android.app.Notification
import android.os.BatteryManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pengxh.daily.app.extensions.backToMainActivity
import com.pengxh.daily.app.extensions.openApplication
import com.pengxh.daily.app.sqlite.DatabaseWrapper
import com.pengxh.daily.app.sqlite.bean.NotificationBean
import com.pengxh.daily.app.utils.BroadcastManager
import com.pengxh.daily.app.utils.Constant
import com.pengxh.daily.app.utils.EmailManager
import com.pengxh.daily.app.utils.LogFileManager
import com.pengxh.daily.app.utils.MessageType
import com.pengxh.kt.lite.extensions.show
import com.pengxh.kt.lite.extensions.timestampToCompleteDate
import com.pengxh.kt.lite.utils.SaveKeyValues

/**
 * @description: 状态栏监听服务
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @date: 2019/12/25 23:17
 */
class NotificationMonitorService : NotificationListenerService() {

    private val kTag = "MonitorService"
    private val emailManager by lazy { EmailManager(this) }
    private val batteryManager by lazy { getSystemService(BatteryManager::class.java) }
    private val auxiliaryApp = arrayOf(
        Constant.WECHAT, Constant.WEWORK, Constant.QQ, Constant.TIM, Constant.ZFB
    )

    /**
     * 有可用的并且和通知管理器连接成功时回调
     */
    override fun onListenerConnected() {
        BroadcastManager.getDefault().sendBroadcast(
            this, MessageType.NOTICE_LISTENER_CONNECTED.action
        )
    }

    /**
     * 当有新通知到来时会回调
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        // 获取接收消息APP的包名
        val pkg = sbn.packageName
        // 获取接收消息的标题
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        // 获取接收消息的内容
        val notice = extras.getString(Notification.EXTRA_TEXT)
        if (notice.isNullOrBlank()) {
            return
        }

        val targetApp = Constant.getTargetApp()

        // 保存指定包名的通知，其他的一律不保存
        if (pkg == targetApp || pkg in auxiliaryApp) {
            NotificationBean().apply {
                packageName = pkg
                notificationTitle = title
                notificationMsg = notice
                postTime = System.currentTimeMillis().timestampToCompleteDate()
            }.also {
                DatabaseWrapper.insertNotice(it)
            }
        }

        // 目标应用打卡通知
        if (pkg == targetApp) {
            when {
                // 判断打卡失败（优先判断失败，避免误判）
                notice.contains("失败") || notice.contains("异常") || notice.contains("错误") -> {
                    // 打卡失败，分析失败原因
                    val failureReason = analyzeClockInFailure(notice, title)
                    LogFileManager.writeLog("收到打卡失败通知: $notice, 原因: $failureReason")
                    
                    // 发送包含失败原因的邮件通知
                    emailManager.sendEmail(
                        "打卡失败通知",
                        "打卡失败，原因：$failureReason\n\n原始通知：$notice",
                        false
                    )
                    
                    // 广播打卡失败消息，触发重试机制
                    BroadcastManager.getDefault().sendBroadcast(
                        this,
                        MessageType.CLOCK_IN_FAILED.action,
                        mapOf("reason" to failureReason, "notification" to notice)
                    )
                }
                // 判断打卡成功（扩展关键词，支持多种成功表述）
                isClockInSuccess(notice) -> {
                    // 打卡成功
                    LogFileManager.writeLog("收到打卡成功通知: $notice")
                    
                    // 取消超时定时器，停止重试
                    BroadcastManager.getDefault().sendBroadcast(
                        this,
                        MessageType.CANCEL_COUNT_DOWN_TIMER.action
                    )
                    
                    // backToMainActivity() 内部已经发送了 DELAY_SHOW_MASK_VIEW 广播
                    backToMainActivity()
                    "即将发送通知邮件，请注意查收".show(this)
                    emailManager.sendEmail("打卡成功通知", notice, false)
                }
            }
        }

        // 其他消息指令 - 来自辅助应用（微信、企业微信、QQ、TIM、支付宝）
        if (pkg in auxiliaryApp) {
            // ✅ 修复：使用严格的指令匹配，避免误触发
            // 支持两种格式：
            // 1. 直接发送精确指令：如 "停止"、"启动"
            // 2. 带前缀的指令：如 "指令:停止"、"#启动"
            
            val command = when {
                notice.startsWith("指令:") -> notice.removePrefix("指令:").trim()
                notice.startsWith("#") -> notice.removePrefix("#").trim()
                else -> notice.trim()  // 使用原始内容（需要严格匹配）
            }
            
            when (command) {
                "电量" -> {
                    val capacity = batteryManager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                    )
                    emailManager.sendEmail(
                        "查询手机电量通知", "当前手机剩余电量为：${capacity}%", false
                    )
                }

                "启动", "启动任务", "开始打卡" -> {
                    LogFileManager.writeLog("收到启动任务指令: $notice")
                    BroadcastManager.getDefault().sendBroadcast(
                        this, MessageType.START_DAILY_TASK.action
                    )
                    emailManager.sendEmail(
                        "任务启动确认",
                        "收到启动指令，任务已启动。来源: $pkg",
                        false
                    )
                }

                "停止", "停止任务", "停止打卡" -> {
                    LogFileManager.writeLog("收到停止任务指令: $notice")
                    BroadcastManager.getDefault().sendBroadcast(
                        this, MessageType.STOP_DAILY_TASK.action
                    )
                    emailManager.sendEmail(
                        "任务停止确认",
                        "收到停止指令，任务已停止。来源: $pkg",
                        false
                    )
                }

                "开始循环" -> {
                    SaveKeyValues.putValue(Constant.TASK_AUTO_START_KEY, true)
                    emailManager.sendEmail(
                        "循环任务状态通知", "循环任务状态已更新为：开启", false
                    )
                }

                "暂停循环" -> {
                    SaveKeyValues.putValue(Constant.TASK_AUTO_START_KEY, false)
                    emailManager.sendEmail(
                        "循环任务状态通知", "循环任务状态已更新为：暂停", false
                    )
                }

                "息屏" -> {
                    BroadcastManager.getDefault().sendBroadcast(
                        this, MessageType.SHOW_MASK_VIEW.action
                    )
                }

                "亮屏" -> {
                    BroadcastManager.getDefault().sendBroadcast(
                        this, MessageType.HIDE_MASK_VIEW.action
                    )
                }

                "考勤记录" -> {
                    var record = ""
                    var index = 1
                    DatabaseWrapper.loadCurrentDayNotice().forEach {
                        if (it.notificationMsg.contains("考勤打卡")) {
                            record += "【第${index}次】${it.notificationMsg}，时间：${it.postTime}\r\n"
                            index++
                        }
                    }
                    emailManager.sendEmail("当天考勤记录通知", record, false)
                }

                "重试打卡" -> {
                    LogFileManager.writeLog("收到远程重试打卡指令")
                    openApplication(true)
                    emailManager.sendEmail(
                        "重试打卡通知",
                        "已收到远程指令，正在尝试重新打卡",
                        false
                    )
                }

                "添加任务" -> {
                    handleAddTask(notice)
                }

                "修改任务" -> {
                    handleModifyTask(notice)
                }

                "删除任务" -> {
                    handleDeleteTask(notice)
                }

                "查询任务", "任务列表" -> {
                    handleQueryTasks()
                }

                "重启应用", "重启" -> {
                    handleRestartApp()
                }

                "查询配置", "配置信息" -> {
                    handleQueryConfig()
                }

                "修改超时" -> {
                    handleModifyTimeout(notice)
                }

                "应用状态", "状态" -> {
                    handleQueryStatus()
                }

                "日志查询" -> {
                    handleQueryLogs(notice)
                }

                "版本信息", "版本" -> {
                    handleQueryVersion()
                }

                "延迟息屏" -> {
                    handleDelayMaskView(notice)
                }

                "帮助", "help" -> {
                    handleShowHelp()
                }

                else -> {
                    // ✅ 不是有效指令，检查是否是任务名称
                    val key = SaveKeyValues.getValue(Constant.TASK_NAME_KEY, "打卡") as String
                    if (command == key) {
                        openApplication(true)
                    }
                    // 其他消息一律忽略，不做任何处理
                }
            }
        }
    }

    /**
     * 处理添加任务指令
     * 格式: 指令:添加任务 HH:mm
     */
    private fun handleAddTask(notice: String) {
        try {
            // 解析时间参数
            val parts = notice.split(" ", "　").filter { it.isNotBlank() }
            if (parts.size < 2) {
                emailManager.sendEmail(
                    "添加任务失败",
                    "指令格式错误，正确格式：\n指令:添加任务 HH:mm\n\n示例：\n指令:添加任务 09:00",
                    false
                )
                return
            }
            
            val time = parts[1]
            // 验证时间格式
            if (!time.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                emailManager.sendEmail(
                    "添加任务失败",
                    "时间格式错误：$time\n正确格式：HH:mm（如 09:00）",
                    false
                )
                return
            }
            
            // 检查任务是否已存在
            if (DatabaseWrapper.isTaskTimeExist(time)) {
                emailManager.sendEmail(
                    "添加任务失败",
                    "任务时间 $time 已存在，无法重复添加",
                    false
                )
                return
            }
            
            // 添加任务
            val bean = com.pengxh.daily.app.sqlite.bean.DailyTaskBean()
            bean.time = time
            DatabaseWrapper.insert(bean)
            
            LogFileManager.writeLog("成功添加任务: $time")
            emailManager.sendEmail(
                "添加任务成功",
                "已成功添加打卡任务：$time\n\n当前所有任务：\n${getAllTasksString()}",
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("添加任务失败: ${e.message}")
            emailManager.sendEmail(
                "添加任务失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理修改任务指令
     * 格式: 指令:修改任务 旧时间 新时间
     */
    private fun handleModifyTask(notice: String) {
        try {
            val parts = notice.split(" ", "　", "->", "→").filter { it.isNotBlank() }
            if (parts.size < 3) {
                emailManager.sendEmail(
                    "修改任务失败",
                    "指令格式错误，正确格式：\n指令:修改任务 旧时间 新时间\n\n示例：\n指令:修改任务 09:00 09:15",
                    false
                )
                return
            }
            
            val oldTime = parts[1]
            val newTime = parts[2]
            
            // 验证时间格式
            if (!oldTime.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) ||
                !newTime.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                emailManager.sendEmail(
                    "修改任务失败",
                    "时间格式错误\n正确格式：HH:mm（如 09:00）",
                    false
                )
                return
            }
            
            // 检查旧任务是否存在
            if (!DatabaseWrapper.isTaskTimeExist(oldTime)) {
                emailManager.sendEmail(
                    "修改任务失败",
                    "任务 $oldTime 不存在\n\n当前所有任务：\n${getAllTasksString()}",
                    false
                )
                return
            }
            
            // 检查新时间是否已被占用
            if (oldTime != newTime && DatabaseWrapper.isTaskTimeExist(newTime)) {
                emailManager.sendEmail(
                    "修改任务失败",
                    "新时间 $newTime 已被其他任务占用",
                    false
                )
                return
            }
            
            // 修改任务
            val tasks = DatabaseWrapper.loadAllTask()
            val task = tasks.find { it.time == oldTime }
            if (task != null) {
                task.time = newTime
                DatabaseWrapper.updateTask(task)
                
                LogFileManager.writeLog("成功修改任务: $oldTime -> $newTime")
                emailManager.sendEmail(
                    "修改任务成功",
                    "已将任务时间从 $oldTime 修改为 $newTime\n\n当前所有任务：\n${getAllTasksString()}",
                    false
                )
            } else {
                emailManager.sendEmail(
                    "修改任务失败",
                    "任务 $oldTime 不存在",
                    false
                )
            }
        } catch (e: Exception) {
            LogFileManager.writeLog("修改任务失败: ${e.message}")
            emailManager.sendEmail(
                "修改任务失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理删除任务指令
     * 格式: 指令:删除任务 HH:mm
     */
    private fun handleDeleteTask(notice: String) {
        try {
            val parts = notice.split(" ", "　").filter { it.isNotBlank() }
            if (parts.size < 2) {
                emailManager.sendEmail(
                    "删除任务失败",
                    "指令格式错误，正确格式：\n指令:删除任务 HH:mm\n\n示例：\n指令:删除任务 09:00",
                    false
                )
                return
            }
            
            val time = parts[1]
            
            // 检查任务是否存在
            if (!DatabaseWrapper.isTaskTimeExist(time)) {
                emailManager.sendEmail(
                    "删除任务失败",
                    "任务 $time 不存在\n\n当前所有任务：\n${getAllTasksString()}",
                    false
                )
                return
            }
            
            // 删除任务
            val tasks = DatabaseWrapper.loadAllTask()
            val task = tasks.find { it.time == time }
            if (task != null) {
                DatabaseWrapper.deleteTask(task)
                
                LogFileManager.writeLog("成功删除任务: $time")
                emailManager.sendEmail(
                    "删除任务成功",
                    "已成功删除任务：$time\n\n剩余任务：\n${getAllTasksString()}",
                    false
                )
            }
        } catch (e: Exception) {
            LogFileManager.writeLog("删除任务失败: ${e.message}")
            emailManager.sendEmail(
                "删除任务失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理查询任务指令
     */
    private fun handleQueryTasks() {
        try {
            val tasksString = getAllTasksString()
            val taskCount = DatabaseWrapper.loadAllTask().size
            
            emailManager.sendEmail(
                "任务列表查询",
                "当前共有 $taskCount 个打卡任务：\n\n$tasksString\n\n提示：发送'指令:添加任务 HH:mm'可添加新任务",
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("查询任务失败: ${e.message}")
            emailManager.sendEmail(
                "查询任务失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 获取所有任务的字符串表示
     */
    private fun getAllTasksString(): String {
        val tasks = DatabaseWrapper.loadAllTask().sortedBy { it.time }
        return if (tasks.isEmpty()) {
            "暂无任务"
        } else {
            tasks.mapIndexed { index, task ->
                "${index + 1}. ${task.time}"
            }.joinToString("\n")
        }
    }

    /**
     * 处理重启应用指令
     */
    private fun handleRestartApp() {
        try {
            LogFileManager.writeLog("收到重启应用指令")
            emailManager.sendEmail(
                "应用重启通知",
                "已收到重启指令，应用将在3秒后重启...",
                false
            )
            
            // 延迟3秒后重启
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    // 重启应用
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    
                    // 杀掉当前进程
                    android.os.Process.killProcess(android.os.Process.myPid())
                } catch (e: Exception) {
                    LogFileManager.writeLog("重启应用失败: ${e.message}")
                }
            }, 3000)
        } catch (e: Exception) {
            LogFileManager.writeLog("重启应用失败: ${e.message}")
            emailManager.sendEmail(
                "应用重启失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理查询配置指令
     */
    private fun handleQueryConfig() {
        try {
            val timeout = SaveKeyValues.getValue(Constant.STAY_DD_TIMEOUT_KEY, Constant.DEFAULT_OVER_TIME) as Int
            val autoStart = SaveKeyValues.getValue(Constant.TASK_AUTO_START_KEY, false) as Boolean
            val taskName = SaveKeyValues.getValue(Constant.TASK_NAME_KEY, "打卡") as String
            val resetHour = SaveKeyValues.getValue(Constant.RESET_TIME_KEY, Constant.DEFAULT_RESET_HOUR) as Int
            val randomTime = SaveKeyValues.getValue(Constant.RANDOM_TIME_KEY, false) as Boolean
            val enableWeekend = SaveKeyValues.getValue(Constant.ENABLE_WEEKEND_KEY, false) as Boolean
            val enableHoliday = SaveKeyValues.getValue(Constant.ENABLE_HOLIDAY_KEY, false) as Boolean
            
            val config = StringBuilder()
            config.append("【系统配置信息】\n\n")
            config.append("⏱ 打卡超时时间：${timeout}秒\n")
            config.append("🔄 自动启动任务：${if (autoStart) "已开启" else "已关闭"}\n")
            config.append("📝 任务名称关键词：$taskName\n")
            config.append("🕐 每日重置时间：${resetHour}:00\n")
            config.append("🎲 随机时间：${if (randomTime) "已开启" else "已关闭"}\n")
            config.append("📅 周末打卡：${if (enableWeekend) "已开启" else "已关闭"}\n")
            config.append("🎊 节假日打卡：${if (enableHoliday) "已开启" else "已关闭"}\n\n")
            config.append("📋 任务数量：${DatabaseWrapper.loadAllTask().size}个\n\n")
            config.append("提示：发送'指令:修改超时 秒数'可修改超时时间")
            
            emailManager.sendEmail(
                "系统配置查询",
                config.toString(),
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("查询配置失败: ${e.message}")
            emailManager.sendEmail(
                "查询配置失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理修改超时时间指令
     * 格式: 指令:修改超时 秒数
     */
    private fun handleModifyTimeout(notice: String) {
        try {
            val parts = notice.split(" ", "　").filter { it.isNotBlank() }
            if (parts.size < 2) {
                emailManager.sendEmail(
                    "修改超时时间失败",
                    "指令格式错误，正确格式：\n指令:修改超时 秒数\n\n示例：\n指令:修改超时 45",
                    false
                )
                return
            }
            
            val timeoutStr = parts[1]
            val timeout = timeoutStr.toIntOrNull()
            
            if (timeout == null || timeout < 10 || timeout > 300) {
                emailManager.sendEmail(
                    "修改超时时间失败",
                    "超时时间必须是10-300之间的整数（单位：秒）\n当前值：$timeoutStr",
                    false
                )
                return
            }
            
            SaveKeyValues.putValue(Constant.STAY_DD_TIMEOUT_KEY, timeout)
            LogFileManager.writeLog("成功修改超时时间: $timeout 秒")
            emailManager.sendEmail(
                "修改超时时间成功",
                "已将打卡超时时间修改为：$timeout 秒",
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("修改超时时间失败: ${e.message}")
            emailManager.sendEmail(
                "修改超时时间失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理查询应用状态指令
     */
    private fun handleQueryStatus() {
        try {
            val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isTaskRunning = SaveKeyValues.getValue("task_is_running", false) as Boolean
            val taskCount = DatabaseWrapper.loadAllTask().size
            val noticeCount = DatabaseWrapper.loadCurrentDayNotice().size
            
            val status = StringBuilder()
            status.append("【应用运行状态】\n\n")
            status.append("📱 设备电量：${capacity}%\n")
            status.append("▶️ 任务状态：${if (isTaskRunning) "运行中" else "已停止"}\n")
            status.append("📋 任务数量：${taskCount}个\n")
            status.append("📝 今日通知：${noticeCount}条\n")
            status.append("⏰ 当前时间：${System.currentTimeMillis().timestampToCompleteDate()}\n\n")
            status.append("提示：发送'指令:查询任务'可查看所有任务")
            
            emailManager.sendEmail(
                "应用状态查询",
                status.toString(),
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("查询状态失败: ${e.message}")
            emailManager.sendEmail(
                "查询状态失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理日志查询指令
     * 格式: 指令:日志查询 [行数]
     */
    private fun handleQueryLogs(notice: String) {
        try {
            val parts = notice.split(" ", "　").filter { it.isNotBlank() }
            val lines = if (parts.size >= 2) {
                parts[1].toIntOrNull() ?: 10
            } else {
                10
            }
            
            val logs = LogFileManager.readLastLogs(lines)
            emailManager.sendEmail(
                "日志查询结果",
                "最近 $lines 条日志：\n\n$logs",
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("查询日志失败: ${e.message}")
            emailManager.sendEmail(
                "查询日志失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理版本信息查询指令
     */
    private fun handleQueryVersion() {
        try {
            val versionName = com.pengxh.daily.app.BuildConfig.VERSION_NAME
            val versionCode = com.pengxh.daily.app.BuildConfig.VERSION_CODE
            
            val info = StringBuilder()
            info.append("【版本信息】\n\n")
            info.append("📦 应用名称：DailyTask\n")
            info.append("🏷 版本号：$versionName\n")
            info.append("🔢 构建号：$versionCode\n")
            info.append("📅 当前日期：${System.currentTimeMillis().timestampToCompleteDate()}\n\n")
            info.append("GitHub: https://github.com/xiaohuai3344/DailyTask-master3344")
            
            emailManager.sendEmail(
                "版本信息查询",
                info.toString(),
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("查询版本失败: ${e.message}")
            emailManager.sendEmail(
                "查询版本失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理延迟息屏指令
     * 格式: 指令:延迟息屏 秒数
     */
    private fun handleDelayMaskView(notice: String) {
        try {
            val parts = notice.split(" ", "　").filter { it.isNotBlank() }
            if (parts.size < 2) {
                emailManager.sendEmail(
                    "延迟息屏失败",
                    "指令格式错误，正确格式：\n指令:延迟息屏 秒数\n\n示例：\n指令:延迟息屏 10",
                    false
                )
                return
            }
            
            val delayStr = parts[1]
            val delay = delayStr.toIntOrNull()
            
            if (delay == null || delay < 1 || delay > 60) {
                emailManager.sendEmail(
                    "延迟息屏失败",
                    "延迟时间必须是1-60之间的整数（单位：秒）\n当前值：$delayStr",
                    false
                )
                return
            }
            
            // 延迟后显示遮罩
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                BroadcastManager.getDefault().sendBroadcast(
                    this, MessageType.SHOW_MASK_VIEW.action
                )
            }, delay * 1000L)
            
            LogFileManager.writeLog("设置延迟息屏: $delay 秒")
            emailManager.sendEmail(
                "延迟息屏设置成功",
                "将在 $delay 秒后自动息屏",
                false
            )
        } catch (e: Exception) {
            LogFileManager.writeLog("延迟息屏失败: ${e.message}")
            emailManager.sendEmail(
                "延迟息屏失败",
                "执行出错：${e.message}",
                false
            )
        }
    }

    /**
     * 处理帮助指令
     */
    private fun handleShowHelp() {
        val help = StringBuilder()
        help.append("【远程指令帮助】\n\n")
        help.append("指令格式：指令:<command> [参数]\n\n")
        help.append("【任务控制】\n")
        help.append("• 启动/停止 - 启动/停止打卡任务\n")
        help.append("• 添加任务 HH:mm - 添加新任务\n")
        help.append("• 修改任务 旧时间 新时间\n")
        help.append("• 删除任务 HH:mm\n")
        help.append("• 查询任务 - 查看所有任务\n")
        help.append("• 重试打卡 - 立即重试\n\n")
        help.append("【系统管理】\n")
        help.append("• 开始循环/暂停循环\n")
        help.append("• 重启应用\n")
        help.append("• 修改超时 秒数\n")
        help.append("• 查询配置\n\n")
        help.append("【状态查询】\n")
        help.append("• 电量\n")
        help.append("• 考勤记录\n")
        help.append("• 应用状态\n")
        help.append("• 日志查询 [行数]\n")
        help.append("• 版本信息\n\n")
        help.append("【显示控制】\n")
        help.append("• 息屏/亮屏\n")
        help.append("• 延迟息屏 秒数\n\n")
        help.append("示例：\n")
        help.append("指令:添加任务 09:00\n")
        help.append("指令:修改超时 45\n")
        help.append("#查询任务")
        
        emailManager.sendEmail(
            "远程指令帮助文档",
            help.toString(),
            false
        )
    }

    /**
     * 当有通知移除时会回调
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    override fun onListenerDisconnected() {
        BroadcastManager.getDefault().sendBroadcast(
            this, MessageType.NOTICE_LISTENER_DISCONNECTED.action
        )
    }

    /**
     * 判断是否打卡成功
     */
    private fun isClockInSuccess(notice: String): Boolean {
        val successKeywords = arrayOf(
            "成功", "完成", "签到", "正常", "已打卡", "考勤正常",
            "打卡成功", "签到成功", "考勤成功", "已签到"
        )
        return successKeywords.any { notice.contains(it) }
    }
    
    /**
     * 分析打卡失败原因
     */
    private fun analyzeClockInFailure(notice: String, title: String): String {
        return when {
            // 网络相关
            notice.contains("网络") || notice.contains("连接失败") || notice.contains("网络异常") -> 
                "网络连接异常，请检查网络设置"
            
            // 时间相关
            notice.contains("不在打卡时间") || notice.contains("时间不对") || notice.contains("打卡时间") -> 
                "不在规定的打卡时间范围内"
            
            notice.contains("已经打过卡") || notice.contains("重复打卡") -> 
                "今日已打卡，无需重复打卡"
            
            // 定位相关
            notice.contains("定位") || notice.contains("位置") || notice.contains("范围外") || notice.contains("考勤地点") -> 
                "定位失败或不在考勤范围内，请检查GPS定位"
            
            // 权限相关
            notice.contains("权限") || notice.contains("无法访问") -> 
                "应用权限不足，请检查权限设置"
            
            // 账号相关
            notice.contains("登录") || notice.contains("账号") || notice.contains("认证") -> 
                "账号登录状态异常，请重新登录"
            
            // 服务器相关
            notice.contains("服务器") || notice.contains("系统繁忙") || notice.contains("请稍后") -> 
                "服务器繁忙或维护中，请稍后重试"
            
            // 人脸识别相关
            notice.contains("人脸") || notice.contains("面部") || notice.contains("识别失败") -> 
                "人脸识别失败，请确保光线充足且正对摄像头"
            
            // Wi-Fi相关
            notice.contains("WiFi") || notice.contains("无线网络") -> 
                "需要连接指定Wi-Fi网络"
            
            // 其他
            notice.contains("异常") || notice.contains("错误") -> 
                "打卡过程出现异常: ${notice.take(50)}"
            
            else -> 
                "未知原因，请查看详细通知: ${notice.take(50)}"
        }
    }
}