package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class PriceCheckWorker extends Worker {

    private static final String CHANNEL_ID = "price_alert_channel";

    public PriceCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    // 🟢 استدعاء الـ Worker من أي مكان (حتى من Receiver)
    public static void enqueueWork(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PriceCheckWorker.class)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        List<PriceAlert> alerts = db.priceAlertDao().getAllAlerts();

        for (PriceAlert alert : alerts) {

            // 🟣 السعر الآن من JSON فقط (Offline Mode)
            double currentPrice = getPriceFromLocalData(alert.coinSymbol);

            if (currentPrice == -1) continue;

            if (currentPrice >= alert.targetPrice) {
                sendNotification(alert.coinSymbol, currentPrice);
                db.priceAlertDao().deleteAlert(alert);  // حذف بعد الإشعار
            }
        }

        return Result.success();
    }


    private void sendNotification(String coin, double price) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Price Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("🚀 " + coin + " Price Alert!")
                .setContentText("Current Price: $" + price)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // 🟣 جلب السعر من JSON فقط (بدون إنترنت)
    private double getPriceFromLocalData(String coinSymbol) {
        try {
            InputStream is = getApplicationContext().getAssets().open("coins.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            CoinLocalResponse response = new Gson().fromJson(json, CoinLocalResponse.class);

            for (Coin coin : response.coins) {
                if (coin.getId().equalsIgnoreCase(coinSymbol)) {
                    return coin.getPrice();   // ← السعر الحالي من JSON فقط!!!!
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;  // لو فشل
    }

}
