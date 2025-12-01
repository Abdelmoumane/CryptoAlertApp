package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private RecyclerView rvCoins;
    private MarketAdapter adapter;
    private List<Coin> allCoinsList = new ArrayList<>();
    private EditText etSearchCoin;
    private SwipeRefreshLayout swipeRefresh;

    private enum FilterType { HOT, GAINERS, LOSERS }
    private FilterType currentFilter = FilterType.HOT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔥 ثيم قبل عرض الشاشة
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, PriceService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        Log.d("SERVICE_TEST", "STARTED FROM MAIN ✔");

        // 🌙 زر الثيم (يجب أن يكون بعد setContentView)
        Switch switchTheme = findViewById(R.id.switchTheme);
        switchTheme.setChecked(isDarkMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate(); // ← مهم جدًا ليتم تحديث الثيم فورًا
        });


        // 🛡 أندرويد 13 يحتاج إذن للإشعار
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 🔗 RecyclerView + Adapter
        rvCoins = findViewById(R.id.rvCoins);
        etSearchCoin = findViewById(R.id.etSearchCoin);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        rvCoins.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarketAdapter(new ArrayList<>(), coin -> {
            Intent intent = new Intent(MainActivity.this, CoinChartActivity.class);
            intent.putExtra("coin_id", coin.getId());
            startActivity(intent);
        });
        rvCoins.setAdapter(adapter);

        setupBottomNavigation();
        setupSearch();
        setupTabs();
        loadLocalCoins();
    }

    // 📌 Bottom Navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertActivity.class));
                return true;
            }
            if (id == R.id.nav_notify) {
                showAlertDialog();
                return true;
            }
            if (id == R.id.nav_whale_alerts) {
                startActivity(new Intent(this, WhaleAlertsActivity.class));
                return true;
            }
            return true;
        });
    }

    // 📌 Dialog لإضافة تنبيه جديد
    // 📌 Dialog لإضافة تنبيه جديد
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

        // ⚠️ هنا تستبدل الكود القديم 👇 بهذا الكود:
        btnSave.setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString().trim().toUpperCase();
            String target = etTarget.getText().toString().trim();

            if (symbol.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Enter all data!", Toast.LENGTH_SHORT).show();
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
            alert.coinSymbol = symbol;
            alert.targetPrice = targetPrice;
            alert.isTriggered = false;

            new Thread(() -> {
                AppDatabase.getDatabase(this).priceAlertDao().insert(alert);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert saved! ✔", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // 🔥 نفتح صفحة التنبيهات مباشرة بعد الحفظ
                    Intent intent = new Intent(MainActivity.this, AlertActivity.class);
                    startActivity(intent);
                });
            }).start();
        });

        dialog.show();
    }



    // 🔍 Search Filter
    private void setupSearch() {
        etSearchCoin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCoins(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // 🧠 Tabs System
    private void setupTabs() {
        findViewById(R.id.btnHot).setOnClickListener(v -> {
            currentFilter = FilterType.HOT;
            filterCoins(etSearchCoin.getText().toString());
        });
        findViewById(R.id.btnGainers).setOnClickListener(v -> {
            currentFilter = FilterType.GAINERS;
            filterCoins(etSearchCoin.getText().toString());
        });
        findViewById(R.id.btnLosers).setOnClickListener(v -> {
            currentFilter = FilterType.LOSERS;
            filterCoins(etSearchCoin.getText().toString());
        });
    }

    // 🔍 Filtering Coins
    private void filterCoins(String query) {
        List<Coin> filtered = new ArrayList<>();
        String search = query.toLowerCase();

        for (Coin coin : allCoinsList) {
            if (!coin.getSymbol().toLowerCase().contains(search)) continue;
            if (currentFilter == FilterType.GAINERS && coin.getChangePercent24h() <= 0) continue;
            if (currentFilter == FilterType.LOSERS && coin.getChangePercent24h() >= 0) continue;
            filtered.add(coin);
        }
        adapter.updateData(filtered);
    }

    // 📂 Load JSON
    private void loadLocalCoins() {
        try {
            InputStream is = getAssets().open("coins.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            CoinLocalResponse response = new Gson().fromJson(json, CoinLocalResponse.class);

            allCoinsList.clear();
            allCoinsList.addAll(response.coins);
            filterCoins("");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ERROR loading JSON!", Toast.LENGTH_SHORT).show();
        }
    }

}
