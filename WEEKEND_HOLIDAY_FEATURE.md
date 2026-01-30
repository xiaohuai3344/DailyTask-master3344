# 周末/节假日自动暂停功能 - 实现说明

## 功能概述

为 DailyTask 应用新增**周末/节假日自动暂停**功能，允许用户配置在周末和法定节假日是否执行打卡任务。

## 功能特性

### ✅ 已实现功能

1. **周末自动识别**
   - 自动识别周六、周日
   - 可配置是否在周末执行打卡任务
   - 关闭后周末将自动暂停所有任务

2. **节假日自动识别**
   - 内置2026年法定节假日数据
   - 包含：元旦、春节、清明节、劳动节、端午节、中秋节、国庆节
   - 支持调休工作日识别
   - 可配置是否在节假日执行打卡任务

3. **智能日期判断**
   - 优先检查节假日数据库
   - 支持法定节假日、调休工作日、自定义休息日
   - 自动降级到周末判断

4. **用户界面**
   - 任务配置界面新增两个开关
   - 主界面工具栏显示今天的日期类型（工作日/周末/节假日）
   - 实时提示当前日期状态

5. **数据持久化**
   - 新增 `holiday_table` 数据库表
   - 数据库版本从 v1 升级到 v2
   - 自动迁移脚本，不影响现有数据

## 技术实现

### 1. 数据库设计

#### 新增数据表：`holiday_table`

```sql
CREATE TABLE holiday_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    date TEXT NOT NULL,              -- 日期，格式：yyyy-MM-dd
    name TEXT NOT NULL,              -- 节假日名称
    type INTEGER NOT NULL,           -- 类型：0-法定节假日，1-调休工作日，2-自定义休息日
    enabled INTEGER NOT NULL DEFAULT 1  -- 是否启用
)
```

#### 数据库迁移

文件：`DailyTaskApplication.kt`
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS holiday_table (...)""")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_holiday_date ON holiday_table(date)")
    }
}
```

### 2. 核心工具类

#### WorkdayManager.kt

**职责**：日期类型判断和节假日管理

**主要方法**：
```kotlin
// 获取今天的日期类型
fun getTodayType(): DayType  // 返回：WORKDAY/WEEKEND/HOLIDAY

// 判断今天是否应该执行任务
fun shouldExecuteToday(enableWeekend: Boolean, enableHoliday: Boolean): Boolean

// 获取今天的描述文本
fun getTodayDescription(): String  // 返回：工作日/周末/节假日名称

// 初始化节假日数据
fun init2026Holidays()

// 添加节假日
fun addHoliday(date: String, name: String, type: Int)
```

**判断逻辑**：
```
1. 查询数据库是否有该日期的记录
   ├─ 有记录 → 根据 type 返回对应类型
   │   ├─ type=0 → HOLIDAY（法定节假日）
   │   ├─ type=1 → WORKDAY（调休工作日）
   │   └─ type=2 → HOLIDAY（自定义休息日）
   │
   └─ 无记录 → 根据星期判断
       ├─ 周六/周日 → WEEKEND
       └─ 周一～周五 → WORKDAY
```

### 3. 用户界面修改

#### TaskConfigActivity.kt

**新增配置项**：
- `weekendSwitch`: 周末打卡开关
- `holidaySwitch`: 节假日打卡开关

**配置存储**：
- `ENABLE_WEEKEND_KEY`: 是否启用周末打卡（默认：false）
- `ENABLE_HOLIDAY_KEY`: 是否启用节假日打卡（默认：false）
- `HOLIDAY_INIT_KEY`: 节假日数据是否已初始化（默认：false）

#### MainActivity.kt

**修改点1**：工具栏显示日期类型
```kotlin
binding.toolbar.apply {
    title = "${parts[2]} [${WorkdayManager.getTodayDescription()}]"
    subtitle = "${parts[0]} ${parts[1]}"
}
```

**修改点2**：任务启动前检查
```kotlin
private fun startExecuteTask() {
    val enableWeekend = SaveKeyValues.getValue(Constant.ENABLE_WEEKEND_KEY, false) as Boolean
    val enableHoliday = SaveKeyValues.getValue(Constant.ENABLE_HOLIDAY_KEY, false) as Boolean
    
    if (!WorkdayManager.shouldExecuteToday(enableWeekend, enableHoliday)) {
        val dayDesc = WorkdayManager.getTodayDescription()
        "今天是${dayDesc}，已设置为休息日，任务不会执行".show(this)
        return
    }
    
    // 正常执行任务...
}
```

### 4. 内置节假日数据

**2026年法定节假日**：
- 元旦：1月1日-3日
- 春节：2月17日-23日
- 清明节：4月4日-6日
- 劳动节：5月1日-5日
- 端午节：6月25日-27日
- 国庆节/中秋节：10月1日-8日

## 使用方法

### 步骤1：打开任务配置

进入 "设置" → "任务配置"

### 步骤2：配置周末/节假日

- **周末打卡**开关：
  - 开启（绿色）：周末继续执行任务
  - 关闭（灰色）：周末自动暂停任务

- **节假日打卡**开关：
  - 开启（绿色）：法定节假日继续执行任务
  - 关闭（灰色）：法定节假日自动暂停任务

### 步骤3：查看日期状态

返回主界面，顶部工具栏会显示：
- 工作日：`星期一 [工作日]`
- 周末：`星期六 [周末]`
- 节假日：`星期三 [春节]`
- 调休：`星期六 [工作日（调休）]`

### 步骤4：启动任务

点击"启动"按钮：
- 如果今天是休息日且未开启对应开关，会提示："今天是周末，已设置为休息日，任务不会执行"
- 如果今天是工作日或已开启对应开关，正常启动任务

## 文件清单

### 新增文件

1. `app/src/main/java/com/pengxh/daily/app/sqlite/bean/HolidayBean.java`
   - 节假日数据实体类

2. `app/src/main/java/com/pengxh/daily/app/sqlite/dao/HolidayBeanDao.java`
   - 节假日数据访问对象

3. `app/src/main/java/com/pengxh/daily/app/utils/WorkdayManager.kt`
   - 工作日管理工具类

### 修改文件

1. `app/src/main/java/com/pengxh/daily/app/DailyTaskApplication.kt`
   - 添加数据库迁移逻辑

2. `app/src/main/java/com/pengxh/daily/app/sqlite/DailyTaskDataBase.java`
   - 数据库版本升级到 v2
   - 添加 HolidayBeanDao

3. `app/src/main/java/com/pengxh/daily/app/utils/Constant.kt`
   - 添加配置常量

4. `app/src/main/java/com/pengxh/daily/app/ui/TaskConfigActivity.kt`
   - 添加周末/节假日配置界面

5. `app/src/main/res/layout/activity_task_config.xml`
   - 添加两个开关控件

6. `app/src/main/java/com/pengxh/daily/app/ui/MainActivity.kt`
   - 添加日期类型判断逻辑
   - 显示日期类型到工具栏

## 配置说明

### SharedPreferences 存储

| Key | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `ENABLE_WEEKEND_KEY` | Boolean | false | 周末是否执行任务 |
| `ENABLE_HOLIDAY_KEY` | Boolean | false | 节假日是否执行任务 |
| `HOLIDAY_INIT_KEY` | Boolean | false | 节假日数据是否已初始化 |

### 数据库数据

节假日数据在首次进入"任务配置"界面时自动初始化，无需手动操作。

## 后续扩展

### 未来可实现功能

1. **节假日管理界面**（优先级：中）
   - 查看所有节假日
   - 添加自定义休息日
   - 编辑/删除节假日
   - 导入/导出节假日数据

2. **节假日API集成**（优先级：中）
   - 集成免费节假日API（如：天行数据、聚合数据）
   - 自动同步最新节假日数据
   - 支持多年份数据

3. **高级调度策略**（优先级：低）
   - 工作日/周末/节假日使用不同的打卡时间
   - 节假日前一天提前打卡
   - 调休工作日特殊处理

## 编译说明

### 环境要求

- Java 17+ （Android Gradle Plugin 要求）
- Android Studio Hedgehog+ 
- Gradle 8.0+

### 编译命令

```bash
# 赋予执行权限
chmod +x gradlew

# 编译 Debug 版本
./gradlew assembleDebug

# 编译 Release 版本
./gradlew assembleRelease
```

### 如果遇到 Java 版本问题

错误信息：`Android Gradle plugin requires Java 17 to run`

**解决方案1**：更新 `gradle.properties`
```properties
org.gradle.java.home=/path/to/java17
```

**解决方案2**：使用环境变量
```bash
export JAVA_HOME=/path/to/java17
./gradlew assembleDebug
```

## 测试建议

### 功能测试

1. **周末测试**
   - 关闭"周末打卡"开关
   - 周六或周日点击"启动"按钮
   - 验证是否提示"今天是周末，已设置为休息日..."

2. **节假日测试**
   - 修改系统日期到节假日（如：2026-01-01）
   - 关闭"节假日打卡"开关
   - 点击"启动"按钮
   - 验证是否提示"今天是元旦，已设置为休息日..."

3. **工作日测试**
   - 确保今天是工作日
   - 点击"启动"按钮
   - 验证任务正常启动

4. **界面测试**
   - 查看主界面工具栏是否显示日期类型
   - 切换日期类型，验证显示是否正确

### 数据库测试

使用 Android Studio 的 Database Inspector 查看：
- `holiday_table` 表是否创建成功
- 节假日数据是否正确写入
- 数据库版本是否正确升级到 v2

## 注意事项

1. **首次运行**：应用会在首次进入"任务配置"界面时自动初始化2026年节假日数据
2. **数据备份**：数据库升级会保留所有现有数据，无需担心数据丢失
3. **性能影响**：日期判断逻辑在任务启动时执行一次，对性能影响极小
4. **兼容性**：新功能向下兼容，旧版本升级后自动获得新功能

## 更新日志

**版本：2.2.6.0 （预计发布日期：2026-02-01）**

新增功能：
- ✅ 周末/节假日自动暂停
- ✅ 智能日期类型识别
- ✅ 主界面显示日期类型
- ✅ 内置2026年法定节假日

优化改进：
- ✅ 数据库结构优化
- ✅ 任务配置界面优化

---

**文档创建时间**：2026-01-30  
**作者**：AI Assistant  
**功能状态**：✅ 核心功能已完成，待Java 17环境编译测试
