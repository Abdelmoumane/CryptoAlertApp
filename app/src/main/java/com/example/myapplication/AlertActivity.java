package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class AlertActivity extends AppCompatActivity {

    private RecyclerView rvAlerts;
    private AlertAdapter alertAdapter;
    private SharedPreferences prefs;

    // ✅ نستخدم نفس الـ Repository بتاع الهوم
    private MarketRepository marketRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // ⭐ الثيم قبل عرض الشاشة
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        // ✅ إنشاء الـ Repository
        marketRepository = new MarketRepository(this);

        // 📌 Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_alerts);
        bottomNav.setSelectedItemId(R.id.nav_alerts);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_notify) {
                showAlertDialog();
                return true;

            } else if (id == R.id.nav_whale_alerts) {
                startActivity(new Intent(this, WhaleAlertsActivity.class));
                return true;

            } else if (id == R.id.nav_alerts) {
                return true; // أنت هنا الآن
            }
            return false;
        });

        // 🧭 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_alerts);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 📌 RecyclerView
        rvAlerts = findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));

        loadAlertsFromDB();
    }

    // ⭐ تثبيت الثيم لما نرجع
    @Override
    protected void onResume() {
        super.onResume();
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // 📌 تحميل التنبيهات من Room
    private void loadAlertsFromDB() {
        new Thread(() -> {
            List<PriceAlert> alerts = AppDatabase.getDatabase(this)
                    .priceAlertDao()
                    .getAllAlerts();

            runOnUiThread(() -> {
                alertAdapter = new AlertAdapter(alerts, AlertActivity.this);
                rvAlerts.setAdapter(alertAdapter);
            });
        }).start();
    }

    // 📌 Dialog لإضافة تنبيه جديد من داخل AlertActivity
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Price Alert");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String symbolInput = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (symbolInput.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Enter all data", Toast.LENGTH_SHORT).show();
                return;
            }

            double targetPrice;
            try {
                targetPrice = Double.parseDouble(target);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ هنا نستخدم MarketRepository:
            // لو أونلاين → يتحقق من CoinGecko
            // لو أوفلاين → يرجع تلقائيًا لـ coins.json
            marketRepository.getCoins(new MarketRepository.CoinsCallback() {
                @Override
                public void onResult(List<Coin> coins) {

                    boolean found = false;
                    for (Coin c : coins) {
                        if (c.getSymbol().equalsIgnoreCase(symbolInput)
                                || c.getId().equalsIgnoreCase(symbolInput)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        runOnUiThread(() ->
                                Toast.makeText(AlertActivity.this,
                                        "Coin not found (online/offline list)",
                                        Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // ✅ العملة موجودة → نحفظ التنبيه في Room
                    PriceAlert alert = new PriceAlert();
                    alert.coinSymbol = symbolInput.toUpperCase();
                    alert.targetPrice = targetPrice;
                    alert.isTriggered = false;

                    new Thread(() -> {
                        AppDatabase.getDatabase(AlertActivity.this)
                                .priceAlertDao()
                                .insert(alert);

                        runOnUiThread(() -> {
                            Toast.makeText(AlertActivity.this,
                                    "Alert Saved!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadAlertsFromDB(); // تحديث القائمة
                        });
                    }).start();
                }
            });
        });

        dialog.show();
    }

    // 🛡 لتفادي إعادة بناء الشاشة عند تغيير الثيم
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
