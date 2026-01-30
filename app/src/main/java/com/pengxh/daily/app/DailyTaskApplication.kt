package com.pengxh.daily.app

import android.app.Application
import androidx.room.Room.databaseBuilder
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pengxh.daily.app.sqlite.DailyTaskDataBase
import com.pengxh.daily.app.utils.LogFileManager
import com.pengxh.kt.lite.utils.SaveKeyValues
import com.tencent.bugly.crashreport.CrashReport


/**
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @date: 2019/12/25 13:19
 */
class DailyTaskApplication : Application() {

    companion object {
        private lateinit var application: DailyTaskApplication

        fun get(): DailyTaskApplication = application

        internal fun initApplication(app: DailyTaskApplication) {
            application = app
        }
    }

    lateinit var dataBase: DailyTaskDataBase

    override fun onCreate() {
        super.onCreate()
        initApplication(this)
        SaveKeyValues.initSharedPreferences(this)
        LogFileManager.initLogFile(this)

        val isDebugMode = BuildConfig.DEBUG
        CrashReport.initCrashReport(this, "ecbdc9baf5", isDebugMode)

        // 数据库迁移策略
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建节假日表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS holiday_table (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                // 创建索引以提高查询效率
                database.execSQL("CREATE INDEX IF NOT EXISTS index_holiday_date ON holiday_table(date)")
            }
        }

        dataBase = databaseBuilder(this, DailyTaskDataBase::class.java, "DailyTask.db")
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
    }
}