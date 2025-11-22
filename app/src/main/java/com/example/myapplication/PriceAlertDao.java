package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PriceAlertDao {

    @Insert
    void insert(PriceAlert alert);

    @Query("SELECT * FROM price_alerts")
    List<PriceAlert> getAllAlerts();

    @Delete
    void deleteAlert(PriceAlert alert);  // ← الآن يقبل الكائن مباشرة

    @Update
    void updateAlert(PriceAlert alert);  // لتعديل التنبيه لاحقًا
}
