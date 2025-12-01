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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // ⭐ استخدم الثيم قبل الشاشة
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

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

            } else if (id == R.id.nav_whale_alerts) {  // 🐋 هنا الإضافة الجديدة
                startActivity(new Intent(this, WhaleAlertsActivity.class));
                return true;

            } else if (id == R.id.nav_alerts) {
                return true; // انت هنا الآن
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

        loadAlertsFromDB();  // تم إصلاحها 👈
    }

    // ⭐ لكي يظل الثيم ثابت لما نرجع
    @Override
    protected void onResume() {
        super.onResume();
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void loadAlertsFromDB() {
        new Thread(() -> {
            List<PriceAlert> alerts = AppDatabase.getDatabase(this)
                    .priceAlertDao()
                    .getAllAlerts();  // ← مهم جدًا!

            runOnUiThread(() -> {
//                if (alerts.isEmpty()) {
//                    Toast.makeText(this, "No alerts found", Toast.LENGTH_SHORT).show();
//                }

                alertAdapter = new AlertAdapter(alerts, AlertActivity.this);
                rvAlerts.setAdapter(alertAdapter);
            });
        }).start();
    }




    // 📌 Dialog لإضافة تنبيه جديد
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
            String symbol = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (symbol.isEmpty() || target.isEmpty()) {
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

            PriceAlert alert = new PriceAlert();
            alert.coinSymbol = symbol.toUpperCase();
            alert.targetPrice = targetPrice;

            new Thread(() -> {
                AppDatabase.getDatabase(this).priceAlertDao().insert(alert);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert Saved!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadAlertsFromDB();  // تحديث القائمة
                });
            }).start();
        });

        dialog.show();
    }
    // 🛡 لتفادي إعادة بناء الشاشة عند تغيير الثيم (يمنع الفلاش والاهتزاز)
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
