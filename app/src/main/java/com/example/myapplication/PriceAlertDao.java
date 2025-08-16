package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PriceAlertDao {

    @Insert
    void insert(PriceAlert alert);

    @Query("SELECT * FROM price_alerts")
    List<PriceAlert> getAllAlerts();

    @Query("DELETE FROM price_alerts WHERE id = :id")
    void deleteAlert(int id);
}

