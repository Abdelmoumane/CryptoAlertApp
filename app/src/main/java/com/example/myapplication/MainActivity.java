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
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;





import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private RecyclerView rvCoins;
    private MarketAdapter adapter;
    private List<Coin> allCoinsList = new ArrayList<>();
    private EditText etSearchCoin;

    private SwipeRefreshLayout swipeRefresh;


    private MarketRepository marketRepository;   //  الريبو الجديد

    private enum FilterType { HOT, GAINERS, LOSERS }
    private FilterType currentFilter = FilterType.HOT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //  ثيم قبل عرض الشاشة
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  إنشاء الـ Repository
        marketRepository = new MarketRepository(this);

        // 🚀 تشغيل Foreground Service
        Intent serviceIntent = new Intent(this, PriceService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        Log.d("SERVICE_TEST", "STARTED FROM MAIN ✔");

        // 🌙 زر الثيم
        Switch switchTheme = findViewById(R.id.switchTheme);
        switchTheme.setChecked(isDarkMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate();
        });

        // 🛡 إذن الإشعارات لأندرويد 13+
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

// 🌀 سحب للتحديث
        swipeRefresh.setOnRefreshListener(() -> {
            loadCoins(true);   // يعيد تحميل البيانات من CoinGecko أو JSON
        });

        rvCoins.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarketAdapter(new ArrayList<>(), coin -> {
            Intent intent = new Intent(MainActivity.this, CoinChartActivity.class);
            intent.putExtra("coin_id", coin.getId());
            intent.putExtra("coin_symbol", coin.getSymbol());
            intent.putExtra("coin_price", coin.getPrice());
            startActivity(intent);
        });
        rvCoins.setAdapter(adapter);

        setupBottomNavigation();
        setupSearch();
        setupTabs();

        //  بدل loadLocalCoins()
        loadCoins(false);
    }

    //  تحميل العملات من الريبو (CoinGecko أو JSON لو مفيش نت)
//  تحميل العملات من الريبو (CoinGecko أو JSON لو مفيش نت)
// تحميل العملات من CoinPaprika أو coins.json
    private void loadCoins(boolean showToast) {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }

        marketRepository.getCoins(showToast, coins -> {
            runOnUiThread(() -> {
                allCoinsList.clear();
                allCoinsList.addAll(coins);
                filterCoins("");

                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            });
        });
    }




    //  يتأكد أن الرمز موجود في الـ list (سواء جاية من API أو JSON)
    private boolean isValidCoinSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) return false;

        for (Coin coin : allCoinsList) {
            if (coin.getSymbol().equalsIgnoreCase(symbol)
                    || coin.getId().equalsIgnoreCase(symbol)) {
                return true;
            }
        }
        return false;
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

            //  تحقق أن العملة موجودة في القائمة الحالية
            if (!isValidCoinSymbol(symbolInput)) {
                Toast.makeText(this, "Coin not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String symbol = symbolInput.toUpperCase();

            PriceAlert alert = new PriceAlert();
            alert.coinSymbol = symbol;
            alert.targetPrice = targetPrice;
            alert.isTriggered = false;

            new Thread(() -> {
                AppDatabase.getDatabase(this).priceAlertDao().insert(alert);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert saved! ✔", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    Intent intent = new Intent(MainActivity.this, AlertActivity.class);
                    startActivity(intent);
                });
            }).start();
        });

        dialog.show();
    }

    //  Search Filter
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
            // فلترة بالبحث
            if (!coin.getSymbol().toLowerCase().contains(search)) continue;

            // فلترة حسب التاب الحالي
            if (currentFilter == FilterType.GAINERS && coin.getChangePercent24h() <= 0) continue;
            if (currentFilter == FilterType.LOSERS && coin.getChangePercent24h() >= 0) continue;

            filtered.add(coin);
        }

        // ✅ ترتيب حسب نوع الفلتر
        if (currentFilter == FilterType.GAINERS) {
            // من أعلى ربح إلى أقل (مثلاً 30%، 20%، 5%...)
            filtered.sort((c1, c2) ->
                    Double.compare(c2.getChangePercent24h(), c1.getChangePercent24h()));
        } else if (currentFilter == FilterType.LOSERS) {
            // من أكبر خسارة إلى أقل (مثلاً -25%، -10%، -3%...)
            filtered.sort((c1, c2) ->
                    Double.compare(Math.abs(c2.getChangePercent24h()),
                            Math.abs(c1.getChangePercent24h())));
            // لو تفضّلهم من الأقرب للصفر للأبعد، خليه:
            // filtered.sort((c1, c2) -> Double.compare(c1.getChangePercent24h(), c2.getChangePercent24h()));
        }

        adapter.updateData(filtered);
    }



}
