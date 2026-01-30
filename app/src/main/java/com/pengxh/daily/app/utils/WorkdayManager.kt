package com.pengxh.daily.app.utils

import com.pengxh.daily.app.DailyTaskApplication
import com.pengxh.daily.app.sqlite.bean.HolidayBean
import java.text.SimpleDateFormat
import java.util.*

/**
 * 工作日管理工具类
 * 用于判断是否为工作日、周末、节假日
 */
object WorkdayManager {

    /**
     * 日期类型枚举
     */
    enum class DayType {
        WORKDAY,    // 工作日
        WEEKEND,    // 周末
        HOLIDAY     // 节假日
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    /**
     * 获取今天的日期类型
     */
    fun getTodayType(): DayType {
        return getDayType(Date())
    }

    /**
     * 判断指定日期的类型
     */
    fun getDayType(date: Date): DayType {
        val dateStr = dateFormat.format(date)
        return getDayType(dateStr)
    }

    /**
     * 判断指定日期字符串的类型
     * @param dateStr 日期字符串，格式：yyyy-MM-dd
     */
    fun getDayType(dateStr: String): DayType {
        // 1. 先检查是否为法定节假日或调休工作日
        val holidayDao = DailyTaskApplication.get().dataBase.holidayDao()
        val holiday = holidayDao.getHolidayByDate(dateStr)
        
        if (holiday != null) {
            return when (holiday.type) {
                0 -> DayType.HOLIDAY  // 法定节假日
                1 -> DayType.WORKDAY  // 调休工作日
                2 -> DayType.HOLIDAY  // 自定义休息日
                else -> getWeekdayType(dateStr)
            }
        }

        // 2. 如果不是节假日，按星期判断
        return getWeekdayType(dateStr)
    }

    /**
     * 根据星期判断日期类型
     */
    private fun getWeekdayType(dateStr: String): DayType {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(dateStr) ?: Date()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        return if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            DayType.WEEKEND
        } else {
            DayType.WORKDAY
        }
    }

    /**
     * 判断今天是否应该执行任务
     * @param enableWeekend 是否在周末执行
     * @param enableHoliday 是否在节假日执行
     */
    fun shouldExecuteToday(enableWeekend: Boolean, enableHoliday: Boolean): Boolean {
        return when (getTodayType()) {
            DayType.WORKDAY -> true
            DayType.WEEKEND -> enableWeekend
            DayType.HOLIDAY -> enableHoliday
        }
    }

    /**
     * 获取今天的描述文本
     */
    fun getTodayDescription(): String {
        val today = dateFormat.format(Date())
        val holidayDao = DailyTaskApplication.get().dataBase.holidayDao()
        val holiday = holidayDao.getHolidayByDate(today)
        
        return when (getTodayType()) {
            DayType.WORKDAY -> {
                if (holiday != null && holiday.type == 1) {
                    "工作日（调休）"
                } else {
                    "工作日"
                }
            }
            DayType.WEEKEND -> "周末"
            DayType.HOLIDAY -> holiday?.name ?: "节假日"
        }
    }

    /**
     * 检查是否为周末
     */
    fun isWeekend(): Boolean {
        return getTodayType() == DayType.WEEKEND
    }

    /**
     * 检查是否为节假日
     */
    fun isHoliday(): Boolean {
        return getTodayType() == DayType.HOLIDAY
    }

    /**
     * 检查是否为工作日
     */
    fun isWorkday(): Boolean {
        return getTodayType() == DayType.WORKDAY
    }

    /**
     * 添加节假日
     */
    fun addHoliday(date: String, name: String, type: Int) {
        val holiday = HolidayBean().apply {
            this.date = date
            this.name = name
            this.type = type
            this.isEnabled = true
        }
        val holidayDao = DailyTaskApplication.get().dataBase.holidayDao()
        holidayDao.insert(holiday)
    }

    /**
     * 批量添加节假日
     */
    fun addHolidays(holidays: List<HolidayBean>) {
        val holidayDao = DailyTaskApplication.get().dataBase.holidayDao()
        holidayDao.insertAll(holidays)
    }

    /**
     * 初始化2026年法定节假日（示例数据）
     */
    fun init2026Holidays() {
        val holidays = mutableListOf<HolidayBean>()
        
        // 元旦：2026年1月1日-3日
        holidays.add(createHoliday("2026-01-01", "元旦", 0))
        holidays.add(createHoliday("2026-01-02", "元旦", 0))
        holidays.add(createHoliday("2026-01-03", "元旦", 0))
        
        // 春节：2026年2月17日-23日（农历正月初一至初七）
        holidays.add(createHoliday("2026-02-17", "春节", 0))
        holidays.add(createHoliday("2026-02-18", "春节", 0))
        holidays.add(createHoliday("2026-02-19", "春节", 0))
        holidays.add(createHoliday("2026-02-20", "春节", 0))
        holidays.add(createHoliday("2026-02-21", "春节", 0))
        holidays.add(createHoliday("2026-02-22", "春节", 0))
        holidays.add(createHoliday("2026-02-23", "春节", 0))
        
        // 清明节：2026年4月4日-6日
        holidays.add(createHoliday("2026-04-04", "清明节", 0))
        holidays.add(createHoliday("2026-04-05", "清明节", 0))
        holidays.add(createHoliday("2026-04-06", "清明节", 0))
        
        // 劳动节：2026年5月1日-5日
        holidays.add(createHoliday("2026-05-01", "劳动节", 0))
        holidays.add(createHoliday("2026-05-02", "劳动节", 0))
        holidays.add(createHoliday("2026-05-03", "劳动节", 0))
        holidays.add(createHoliday("2026-05-04", "劳动节", 0))
        holidays.add(createHoliday("2026-05-05", "劳动节", 0))
        
        // 端午节：2026年6月25日-27日
        holidays.add(createHoliday("2026-06-25", "端午节", 0))
        holidays.add(createHoliday("2026-06-26", "端午节", 0))
        holidays.add(createHoliday("2026-06-27", "端午节", 0))
        
        // 中秋节：2026年10月3日-5日（与国庆连休）
        // 国庆节：2026年10月1日-8日
        holidays.add(createHoliday("2026-10-01", "国庆节", 0))
        holidays.add(createHoliday("2026-10-02", "国庆节", 0))
        holidays.add(createHoliday("2026-10-03", "国庆节/中秋节", 0))
        holidays.add(createHoliday("2026-10-04", "国庆节", 0))
        holidays.add(createHoliday("2026-10-05", "国庆节", 0))
        holidays.add(createHoliday("2026-10-06", "国庆节", 0))
        holidays.add(createHoliday("2026-10-07", "国庆节", 0))
        holidays.add(createHoliday("2026-10-08", "国庆节", 0))

        addHolidays(holidays)
    }

    private fun createHoliday(date: String, name: String, type: Int): HolidayBean {
        return HolidayBean().apply {
            this.date = date
            this.name = name
            this.type = type
            this.isEnabled = true
        }
    }

    /**
     * 清理过期的节假日数据（保留最近一年的数据）
     */
    fun cleanOldHolidays() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val oneYearAgo = dateFormat.format(calendar.time)
        val holidayDao = DailyTaskApplication.get().dataBase.holidayDao()
        holidayDao.deleteOldHolidays(oneYearAgo)
    }
}
