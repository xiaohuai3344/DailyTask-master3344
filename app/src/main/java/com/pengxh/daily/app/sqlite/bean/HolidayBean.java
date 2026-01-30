package com.pengxh.daily.app.sqlite.bean;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 节假日数据实体
 * 用于存储法定节假日和自定义休息日
 */
@Entity(tableName = "holiday_table")
public class HolidayBean {
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * 日期，格式：yyyy-MM-dd
     */
    private String date;

    /**
     * 节假日名称，如：春节、国庆节、元旦等
     */
    private String name;

    /**
     * 节假日类型：0-法定节假日，1-调休工作日，2-自定义休息日
     */
    private int type;

    /**
     * 是否启用
     */
    private boolean enabled;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
