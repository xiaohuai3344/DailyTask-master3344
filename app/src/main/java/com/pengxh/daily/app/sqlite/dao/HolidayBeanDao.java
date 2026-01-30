package com.pengxh.daily.app.sqlite.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.pengxh.daily.app.sqlite.bean.HolidayBean;

import java.util.List;

/**
 * 节假日数据访问对象
 */
@Dao
public interface HolidayBeanDao {

    @Insert
    void insert(HolidayBean bean);

    @Insert
    void insertAll(List<HolidayBean> beans);

    @Delete
    void delete(HolidayBean bean);

    @Update
    void update(HolidayBean bean);

    @Query("SELECT * FROM holiday_table WHERE enabled = 1 ORDER BY date ASC")
    List<HolidayBean> loadAllEnabled();

    @Query("SELECT * FROM holiday_table ORDER BY date ASC")
    List<HolidayBean> loadAll();

    @Query("SELECT * FROM holiday_table WHERE date = :date AND enabled = 1 LIMIT 1")
    HolidayBean getHolidayByDate(String date);

    @Query("SELECT * FROM holiday_table WHERE date >= :startDate AND date <= :endDate AND enabled = 1")
    List<HolidayBean> getHolidaysByDateRange(String startDate, String endDate);

    @Query("DELETE FROM holiday_table WHERE date < :date")
    void deleteOldHolidays(String date);

    @Query("SELECT COUNT(*) FROM holiday_table WHERE date = :date AND enabled = 1")
    int isHolidayExist(String date);
}
