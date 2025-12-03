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

    private Handler handler = new Handler();
    private MarketRepository marketRepository;   // ✅ نفس الريبو بتاع الهوم

    @Override
    public void onCreate() {
        super.onCreate();
        // نستخدم ApplicationContext جوّه الريبو
        marketRepository = new MarketRepository(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("SERVICE_TEST", "PriceService STARTED ✔");

        // إشعار ثابت للخدمة (Foreground Service)
        startForeground(1, createNotification());

        // تشغيل التكرار
        handler.post(runnable);

        return START_STICKY;
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {

            // شغل Room في ثريد منفصل
            new Thread(() -> {

                AppDatabase db = AppDatabase.getDatabase(PriceService.this);
                List<PriceAlert> alerts = db.priceAlertDao().getActiveAlerts(); // فقط الغير مفعّلة

                Log.d("SERVICE_TEST", "📌 Alerts found: " + alerts.size());

                if (alerts.isEmpty()) return;

                // 🟢 نجيب الأسعار من CoinGecko أو من coins.json (عن طريق الـ Repository)
                marketRepository.getCoins(coins -> {

                    // نكمل برضه في ثريد عادي عشان Room
                    new Thread(() -> {

                        for (PriceAlert alert : alerts) {

                            // نحاول نلاقي العملة اللي لها نفس الرمز أو الـ id
                            Coin match = null;
                            for (Coin c : coins) {
                                if (c.getSymbol().equalsIgnoreCase(alert.coinSymbol)
                                        || c.getId().equalsIgnoreCase(alert.coinSymbol)) {
                                    match = c;
                                    break;
                                }
                            }

                            if (match == null) continue;

                            double price = match.getPrice();

                            Log.d("ALERT_DEBUG",
                                    "Coin=" + alert.coinSymbol +
                                            " current=" + price +
                                            " target=" + alert.targetPrice);

                            // لو السعر وصل أو عدّى الهدف
                            if (price >= alert.targetPrice) {

                                sendAlertNotification(
                                        match.getSymbol().toUpperCase()
                                                + " reached $" + alert.targetPrice
                                );

                                // حذف التنبيه بعد ما يشتغل → يختفي من My Alerts
                                AppDatabase.getDatabase(PriceService.this)
                                        .priceAlertDao()
                                        .delete(alert);
                            }
                        }

                    }).start();
                });

            }).start();

            // ⏱ كل 10 ثواني نعيد التشييك
            handler.postDelayed(this, 10000);
        }
    };

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
