package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class PriceCheckWorker extends Worker {

    private static final String CHANNEL_ID = "price_alert_channel";

    public PriceCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        List<PriceAlert> alerts = db.priceAlertDao().getAllAlerts();

        for (PriceAlert alert : alerts) {



            double currentPrice = fetchPriceFromAPI(alert.coinSymbol);

            if (currentPrice == -1) continue;  // لو فشل الـ API

            if (currentPrice >= alert.targetPrice) {
                sendNotification(alert.coinSymbol, currentPrice);
                db.priceAlertDao().deleteAlert(alert);  // ⚠ حذف بعد الإشعار
            }
        }

        return Result.success();
    }

    /**
     * 🟢 جلب السعر الحقيقي من CoinGecko API
     */
    private double fetchPriceFromAPI(String coinSymbol) {
        CoinGeckoApi api = ApiClient.getClient().create(CoinGeckoApi.class);
        Call<List<CoinGeckoCoin>> call = api.getSingleCoin(coinSymbol, "usd");

        try {
            Response<List<CoinGeckoCoin>> response = call.execute();  // 🔥 Sync (مسموح داخل Worker)
            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                return response.body().get(0).getCurrentPrice();  // السعر الحقيقي
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;  // فشل API
    }

    /**
     * 🔔 إرسال الإشعار
     */
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
}
