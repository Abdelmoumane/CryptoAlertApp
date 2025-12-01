package com.example.myapplication;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class PriceService extends Service {

    Handler handler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("SERVICE_TEST", "PriceService STARTED ✔");

        startForeground(1, createNotification());  // إشعار ثابت
        handler.post(runnable);  // تشغيل التكرار

        return START_STICKY;  // يستمر حتى بعد إغلاق التطبيق
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {

            new Thread(() -> {

                AppDatabase db = AppDatabase.getDatabase(PriceService.this);
                List<PriceAlert> alerts = db.priceAlertDao().getActiveAlerts(); // فقط الغير مفعّلة

                Log.d("SERVICE_TEST", "📌 Alerts found: " + alerts.size());

                for (PriceAlert alert : alerts) {

                    double price = getMockPrice();  // لاحقًا API حقيقي

                    if (price >= alert.targetPrice) {

                        sendAlertNotification(alert.coinSymbol + " reached $" + alert.targetPrice);

                        // ⚠ بدلنا update() بـ delete() = حذف مباشر
                        db.priceAlertDao().delete(alert);
                    }
                }

            }).start();

            handler.postDelayed(this, 10000); // كل 10 ثواني
        }
    };

    private double getMockPrice() {
        return 45000 + Math.random() * 10000;
    }

    private void sendAlertNotification(String message) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "alert_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Crypto Alert 🚀")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "price_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("CryptoAlert is Running…")
                .setContentText("Monitoring prices in background")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
