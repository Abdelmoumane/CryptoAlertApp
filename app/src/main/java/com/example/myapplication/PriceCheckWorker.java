package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

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
            double currentPrice = getPriceFromAPI(alert.coinSymbol); // تابع لجلب السعر

            if (currentPrice >= alert.targetPrice) {
                sendNotification(alert.coinSymbol, alert.targetPrice);
                db.priceAlertDao().deleteAlert(alert.id); // حذف التنبيه بعد الإشعار
            }
        }

        return Result.success();
    }

    private double getPriceFromAPI(String coinSymbol) {
        // TODO: هنا ضع استدعاء Retrofit أو أي API لجلب السعر الحالي
        // مؤقتًا للتجربة:
        return 50000; // مثال
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
                .setContentTitle("Price Alert!")
                .setContentText(coin + " reached $" + price)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
