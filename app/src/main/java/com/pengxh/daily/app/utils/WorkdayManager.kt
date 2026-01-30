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
     * 初始化2026年法定节假日（根据国务院办公厅通知）
     * 数据来源：国务院办公厅关于2026年部分节假日安排的通知
     */
    fun init2026Holidays() {
        val holidays = mutableListOf<HolidayBean>()
        
        // 元旦：1月1日（周四）至3日（周六）放假调休，共3天
        holidays.add(createHoliday("2026-01-01", "元旦", 0))
        holidays.add(createHoliday("2026-01-02", "元旦", 0))
        holidays.add(createHoliday("2026-01-03", "元旦", 0))
        
        // 春节：2月15日（农历腊月二十八、周日）至23日（农历正月初七、周一）放假调休，共9天
        // 2月14日（周六）、2月28日（周六）上班
        holidays.add(createHoliday("2026-02-15", "春节", 0))
        holidays.add(createHoliday("2026-02-16", "春节", 0))
        holidays.add(createHoliday("2026-02-17", "春节", 0))
        holidays.add(createHoliday("2026-02-18", "春节", 0))
        holidays.add(createHoliday("2026-02-19", "春节", 0))
        holidays.add(createHoliday("2026-02-20", "春节", 0))
        holidays.add(createHoliday("2026-02-21", "春节", 0))
        holidays.add(createHoliday("2026-02-22", "春节", 0))
        holidays.add(createHoliday("2026-02-23", "春节", 0))
        // 调休工作日
        holidays.add(createHoliday("2026-02-14", "春节调休", 1))
        holidays.add(createHoliday("2026-02-28", "春节调休", 1))
        
        // 清明节：4月4日（周六）至6日（周一）放假，共3天
        holidays.add(createHoliday("2026-04-04", "清明节", 0))
        holidays.add(createHoliday("2026-04-05", "清明节", 0))
        holidays.add(createHoliday("2026-04-06", "清明节", 0))
        
        // 劳动节：5月1日（周五）至5日（周二）放假调休，共5天
        // 5月9日（周六）上班
        holidays.add(createHoliday("2026-05-01", "劳动节", 0))
        holidays.add(createHoliday("2026-05-02", "劳动节", 0))
        holidays.add(createHoliday("2026-05-03", "劳动节", 0))
        holidays.add(createHoliday("2026-05-04", "劳动节", 0))
        holidays.add(createHoliday("2026-05-05", "劳动节", 0))
        // 调休工作日
        holidays.add(createHoliday("2026-05-09", "劳动节调休", 1))
        
        // 端午节：6月19日（周五）至21日（周日）放假，共3天
        holidays.add(createHoliday("2026-06-19", "端午节", 0))
        holidays.add(createHoliday("2026-06-20", "端午节", 0))
        holidays.add(createHoliday("2026-06-21", "端午节", 0))
        
        // 中秋节：9月25日（周五）至27日（周日）放假，共3天
        holidays.add(createHoliday("2026-09-25", "中秋节", 0))
        holidays.add(createHoliday("2026-09-26", "中秋节", 0))
        // 注意：9月27日既是中秋节假期，也被国庆节借用作为调休，这里标记为中秋节
        holidays.add(createHoliday("2026-09-27", "中秋节", 0))
        
        // 国庆节：10月1日（周四）至7日（周三）放假调休，共7天
        // 10月10日（周六）上班（9月27日已被中秋节占用）
        holidays.add(createHoliday("2026-10-01", "国庆节", 0))
        holidays.add(createHoliday("2026-10-02", "国庆节", 0))
        holidays.add(createHoliday("2026-10-03", "国庆节", 0))
        holidays.add(createHoliday("2026-10-04", "国庆节", 0))
        holidays.add(createHoliday("2026-10-05", "国庆节", 0))
        holidays.add(createHoliday("2026-10-06", "国庆节", 0))
        holidays.add(createHoliday("2026-10-07", "国庆节", 0))
        // 调休工作日
        holidays.add(createHoliday("2026-10-10", "国庆节调休", 1))

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
