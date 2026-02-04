package com.pengxh.daily.app.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.pengxh.daily.app.sqlite.DatabaseWrapper
import com.pengxh.daily.app.utils.BroadcastManager
import com.pengxh.daily.app.utils.Constant
import com.pengxh.daily.app.utils.EmailManager
import com.pengxh.daily.app.utils.LogFileManager
import com.pengxh.daily.app.utils.MessageType
import com.pengxh.daily.app.utils.WorkdayManager
import com.pengxh.kt.lite.utils.SaveKeyValues
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 任务自动启动检测服务
 * 功能：定时检测工作日是否有任务但未启动，自动启动并发送邮件通知
 */
class TaskAutoStartService : Service() {

    companion object {
        private const val CHECK_INTERVAL = 5 * 60 * 1000L // 5分钟检测一次
        private const val TAG = "TaskAutoStartService"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val emailManager by lazy { EmailManager.getInstance(this) }
    private var lastCheckDate = ""
    private var hasCheckedToday = false
    private var taskStartedToday = false

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndStartTask()
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogFileManager.writeLog("TaskAutoStartService: 服务已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogFileManager.writeLog("TaskAutoStartService: 服务已启动")
        
        // 启动定时检测
        handler.post(checkRunnable)
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        LogFileManager.writeLog("TaskAutoStartService: 服务已停止")
    }

    /**
     * 检测并自动启动任务
     */
    private fun checkAndStartTask() {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            // 每天重置检测状态
            if (currentDate != lastCheckDate) {
                lastCheckDate = currentDate
                hasCheckedToday = false
                taskStartedToday = false
                LogFileManager.writeLog("TaskAutoStartService: 新的一天开始，重置检测状态")
            }

            // 如果今天已经检测过并启动了任务，则不再检测
            if (hasCheckedToday && taskStartedToday) {
                return
            }

            // 只在工作时间段检测（7:00 - 10:00）
            if (currentHour < 7 || currentHour >= 10) {
                return
            }

            // 检查是否是工作日
            val enableWeekend = SaveKeyValues.getValue(
                Constant.ENABLE_WEEKEND_KEY,
                Constant.DEFAULT_ENABLE_WEEKEND
            ) as Boolean
            val enableHoliday = SaveKeyValues.getValue(
                Constant.ENABLE_HOLIDAY_KEY,
                Constant.DEFAULT_ENABLE_HOLIDAY
            ) as Boolean

            if (!WorkdayManager.shouldExecuteToday(enableWeekend, enableHoliday)) {
                LogFileManager.writeLog("TaskAutoStartService: 今天不是工作日，跳过检测")
                hasCheckedToday = true
                return
            }

            // 检查是否有今天的打卡任务
            val allTasks = DatabaseWrapper.loadAllTask()
            if (allTasks.isEmpty()) {
                LogFileManager.writeLog("TaskAutoStartService: 没有配置打卡任务")
                hasCheckedToday = true
                return
            }

            // 检查任务是否已启动（通过检查是否有任务正在运行）
            val isTaskRunning = SaveKeyValues.getValue(
                "task_is_running",
                false
            ) as Boolean

            val autoStartEnabled = SaveKeyValues.getValue(
                Constant.TASK_AUTO_START_KEY,
                false
            ) as Boolean

            // 如果任务未启动且自动启动已开启
            if (!isTaskRunning && autoStartEnabled) {
                LogFileManager.writeLog("TaskAutoStartService: 检测到任务未启动，准备自动启动")
                
                // 自动启动任务
                BroadcastManager.getDefault().sendBroadcast(
                    this,
                    MessageType.START_DAILY_TASK.action
                )
                
                taskStartedToday = true
                hasCheckedToday = true
                
                // 发送邮件通知
                val taskCount = allTasks.size
                val todayDescription = WorkdayManager.getTodayDescription()
                val message = """
                    检测时间：$currentDate $currentTime
                    检测结果：任务未启动
                    
                    今日状态：$todayDescription
                    任务数量：$taskCount 个
                    
                    自动操作：已自动启动任务
                    
                    任务列表：
                    ${allTasks.joinToString("\n") { "  - ${it.taskTime}" }}
                    
                    提示：如果不需要自动启动功能，请发送"暂停循环"来关闭。
                """.trimIndent()
                
                emailManager.sendEmail(
                    "任务自动启动通知",
                    message,
                    false
                )
                
                LogFileManager.writeLog("TaskAutoStartService: 任务已自动启动，邮件通知已发送")
            } else if (isTaskRunning) {
                LogFileManager.writeLog("TaskAutoStartService: 任务已在运行中")
                hasCheckedToday = true
            } else if (!autoStartEnabled) {
                LogFileManager.writeLog("TaskAutoStartService: 自动启动功能未开启")
                hasCheckedToday = true
            }
            
        } catch (e: Exception) {
            LogFileManager.writeLog("TaskAutoStartService: 检测出错 - ${e.message}")
            e.printStackTrace()
        }
    }
}
