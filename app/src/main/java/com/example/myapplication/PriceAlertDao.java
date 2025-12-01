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

    @Update
    void update(PriceAlert alert);  // ← 🔄 Room سيستخدم هذا لتحديث isTriggered

    @Delete
    void delete(PriceAlert alert);

    @Query("SELECT * FROM price_alerts")
    List<PriceAlert> getAllAlerts();   // لعرضها في AlertActivity

    // 🟢 مهم جدًا للخدمة → للحصول على فقط *الغير مفعّلة*
    @Query("SELECT * FROM price_alerts WHERE isTriggered = 0")
    List<PriceAlert> getActiveAlerts();  // 🔥 هذا هو اللي تستعمله في PriceService
}
