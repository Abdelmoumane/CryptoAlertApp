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
    void update(PriceAlert alert);  // Room usará esto para actualizar isTriggered

    @Delete
    void delete(PriceAlert alert);

    @Query("SELECT * FROM price_alerts")
    List<PriceAlert> getAllAlerts();   // Para mostrarlas en AlertActivity

    // Importante para el servicio: obtener solo las no activadas
    @Query("SELECT * FROM price_alerts WHERE isTriggered = 0")
    List<PriceAlert> getActiveAlerts();  // Este es el que se usa en PriceService
}
