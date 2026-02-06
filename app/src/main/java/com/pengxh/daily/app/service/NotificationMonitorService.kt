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