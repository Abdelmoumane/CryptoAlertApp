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

import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PriceService extends Service {

    Handler handler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("SERVICE_TEST", "PriceService STARTED ✔");

        // إشعار ثابت للخدمة
        startForeground(1, createNotification());

        // تشغيل التكرار
        handler.post(runnable);

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

                    //  نجيب السعر من coins.json بدل random
                    double price = getPriceForCoin(alert.coinSymbol);

                    Log.d("ALERT_DEBUG",
                            "Coin=" + alert.coinSymbol +
                                    " current=" + price +
                                    " target=" + alert.targetPrice);

                    // لو السعر وصل أو تجاوز الهدف
                    if (price >= alert.targetPrice) {

                        sendAlertNotification(alert.coinSymbol + " reached $" + alert.targetPrice);

                        // حذف التنبيه بعد ما يشتغل → يختفي من My Alerts
                        db.priceAlertDao().delete(alert);
                    }
                }

            }).start();

            // ⏱ كل 10 ثواني يعيد التشييك
            handler.postDelayed(this, 10000);
        }
    };

    /**
     *  تجيب سعر العملة من coins.json
     * تطابق بالـ id أو بالـ symbol (عشان لو أنت كتبت BTC أو bitcoin)
     */
    private double getPriceForCoin(String coinSymbol) {
        try {
            InputStream is = getAssets().open("coins.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            CoinLocalResponse response = new Gson().fromJson(json, CoinLocalResponse.class);

            for (Coin coin : response.coins) {
                if (coin.getId().equalsIgnoreCase(coinSymbol)
                        || coin.getSymbol().equalsIgnoreCase(coinSymbol)) {
                    return coin.getPrice();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // لو مش لاقي العملة في JSON → نرجع لسعر عشوائي احتياطي
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
