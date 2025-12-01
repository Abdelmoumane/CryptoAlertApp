package com.example.myapplication;




import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "price_alerts")
public class PriceAlert {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String coinSymbol;
    public double targetPrice;

    public boolean isTriggered; // ⬅ أضف هذا الحقل الجديد

    public PriceAlert() {
        this.isTriggered = false;  // ⬅ بشكل افتراضي لم يتم تشغيله
    }
}
