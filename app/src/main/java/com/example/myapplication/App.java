package com.example.myapplication;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 🔔 Create channels once only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel priceChannel = new NotificationChannel(
                    "price_channel",
                    "Price Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationChannel alertChannel = new NotificationChannel(
                    "alert_channel",
                    "Price Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(priceChannel);
            manager.createNotificationChannel(alertChannel);
        }
    }
}
