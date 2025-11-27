package com.example.myapplication;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

        // ⭐ الثيم يتم ضبطه هنا فقط — مرة واحدة فقط قبل setContentView()
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🌗 Theme Switcher
        Switch switchTheme = findViewById(R.id.switchTheme);
        switchTheme.setChecked(isDarkMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            // ⭐ نغلق الـActivity بالكامل ونفتحه بثيم جديد
            finish();
            startActivity(getIntent());
        });

        // باقي الكود عادي...



        // 🛡 الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        testWorker(); // 🚀 Worker

        // 🔗 Views
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



    // 🟢 Worker
    private void testWorker() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PriceCheckWorker.class)
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        testWorker();
                    }
                });
    }

    // 🔽 Bottom Navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertActivity.class));
                return true;
            }
            else if (id == R.id.nav_notify) {
                showAlertDialog();
                return true;
            }
            else if (id == R.id.nav_whale_alerts) {
                startActivity(new Intent(this, WhaleAlertsActivity.class));
                return true;
            }
            return true;
        });
    }

    // 📌 Dialog لإضافة تنبيه
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
            String symbol = etSymbol.getText().toString().trim().toUpperCase();
            String target = etTarget.getText().toString().trim();

            if (symbol.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Enter all data!", Toast.LENGTH_SHORT).show();
                return;
            }

            PriceAlert alert = new PriceAlert();
            alert.coinSymbol = symbol;
            alert.targetPrice = Double.parseDouble(target);

            new Thread(() -> {
                AppDatabase.getDatabase(this).priceAlertDao().insert(alert);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert saved!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }).start();
        });

        dialog.show();
    }

    // 🔍 Search
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
            updateTabUI();
            filterCoins(etSearchCoin.getText().toString());
        });
        findViewById(R.id.btnGainers).setOnClickListener(v -> {
            currentFilter = FilterType.GAINERS;
            updateTabUI();
            filterCoins(etSearchCoin.getText().toString());
        });
        findViewById(R.id.btnLosers).setOnClickListener(v -> {
            currentFilter = FilterType.LOSERS;
            updateTabUI();
            filterCoins(etSearchCoin.getText().toString());
        });
    }

    // 🎨 لون التاب حسب الحالة
    private void updateTabUI() {
        int activeColor = getResources().getColor(R.color.textPrimary);
        int inactiveColor = getResources().getColor(R.color.textSecondary);

        TextView btnHot = findViewById(R.id.btnHot);
        TextView btnGainers = findViewById(R.id.btnGainers);
        TextView btnLosers = findViewById(R.id.btnLosers);

        btnHot.setSelected(currentFilter == FilterType.HOT);
        btnGainers.setSelected(currentFilter == FilterType.GAINERS);
        btnLosers.setSelected(currentFilter == FilterType.LOSERS);

        btnHot.setTextColor(btnHot.isSelected() ? activeColor : inactiveColor);
        btnGainers.setTextColor(btnGainers.isSelected() ? activeColor : inactiveColor);
        btnLosers.setTextColor(btnLosers.isSelected() ? activeColor : inactiveColor);
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
        updateTabUI();
    }

    // 📂 Load Local JSON
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
