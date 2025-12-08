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

import java.util.ArrayList;
import java.util.List;

public class PriceService extends Service {

    private static final String TAG = "PriceCheckDebug";

    private final Handler handler = new Handler();
    private MarketRepository marketRepository;   // el mismo Repository que en Home
    private boolean isLoopStarted = false;       // para no iniciar el loop más de una vez

    @Override
    public void onCreate() {
        super.onCreate();
        // Usamos ApplicationContext dentro del repo
        marketRepository = new MarketRepository(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("SERVICE_TEST", "PriceService STARTED ✔");

        // Notificación fija del servicio (Foreground Service)
        startForeground(1, createNotification());

        // Empezar el check solo una vez
        if (!isLoopStarted) {
            isLoopStarted = true;
            handler.post(runnable);
        } else {
            Log.d("SERVICE_TEST", "Loop already running, skip extra post");
        }

        return START_STICKY;
    }

    // Función auxiliar para programar el siguiente check después de 10 segundos
    private void scheduleNext() {
        Log.d(TAG, "scheduleNext() called → next check in 10s");
        handler.postDelayed(runnable, 10_000);
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "Runnable started");

            // Trabajamos en un hilo separado por Room
            new Thread(() -> {

                AppDatabase db = AppDatabase.getDatabase(PriceService.this);
                List<PriceAlert> alerts = db.priceAlertDao().getActiveAlerts(); // solo las no activadas

                Log.d(TAG, "📌 Alerts found: " + alerts.size());

                if (alerts.isEmpty()) {
                    // No hay alerts → no necesitamos obtener precios ahora
                    scheduleNext();
                    return;
                }

                // Hacemos una copia final de la lista para usarla dentro del callback
                List<PriceAlert> currentAlerts = new ArrayList<>(alerts);

                // Obtenemos precios de CoinPaprika o de coins.json (a través del Repository)
                // false = sin Toast (el servicio corre en segundo plano)
                marketRepository.getCoins(false, coins -> {
                    // El propio callback corre en un hilo (si quieres otro hilo no hay problema)
                    new Thread(() -> {

                        try {
                            AppDatabase innerDb = AppDatabase.getDatabase(PriceService.this);

                            Log.d(TAG, "Coins loaded for price check: " + coins.size());

                            for (PriceAlert alert : currentAlerts) {
                                Coin match = null;

                                for (Coin c : coins) {
                                    if (c.getSymbol().equalsIgnoreCase(alert.coinSymbol)
                                            || c.getId().equalsIgnoreCase(alert.coinSymbol)) {
                                        match = c;
                                        break;
                                    }
                                }

                                if (match == null) {
                                    Log.d(TAG, "No coin found for alert symbol=" + alert.coinSymbol);
                                    continue;
                                }

                                double price = match.getPrice();

                                Log.d(TAG,
                                        "Checking alert → Coin=" + alert.coinSymbol +
                                                " current=" + price +
                                                " target=" + alert.targetPrice);

                                if (price >= alert.targetPrice) {

                                    sendAlertNotification(
                                            match.getSymbol().toUpperCase()
                                                    + " reached $" + alert.targetPrice
                                    );

                                    Log.d(TAG, "🔥 ALERT TRIGGERED for " + alert.coinSymbol +
                                            " @ " + alert.targetPrice);

                                    // Aquí elegí borrar la alerta después del trigger (como tu código)
                                    innerDb.priceAlertDao().delete(alert);
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error while processing alerts: " + e.getMessage(), e);
                        } finally {
                            // 👈 Importante: después de terminar siempre programamos la siguiente ronda
                            scheduleNext();
                        }

                    }).start();
                });

            }).start();
        }
    };

    private void sendAlertNotification(String message) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted, skipping notification");
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
        isLoopStarted = false;   // si el servicio muere y vuelve, permitimos iniciar un loop nuevo
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
