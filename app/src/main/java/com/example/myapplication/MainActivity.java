package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCoins;
    private MarketAdapter adapter;
    private List<Coin> allCoinsList = new ArrayList<>();
    private EditText etSearchCoin;
    private SwipeRefreshLayout swipeRefresh;

    private TextView btnHot, btnGainers, btnLosers;

    private enum FilterType {HOT, GAINERS, LOSERS}

    private FilterType currentFilter = FilterType.HOT;

    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            fetchCoinsFromApi();
            handler.postDelayed(this, 30000); // كل 30 ثانية
        }
    };

    private void showAddAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Price Alert");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etCoin = new EditText(this);
        etCoin.setHint("Coin Symbol (e.g., BTC)");
        layout.addView(etCoin);

        EditText etPrice = new EditText(this);
        etPrice.setHint("Target Price");
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        builder.setView(layout);

        builder.setPositiveButton("Add", null); // سنضبط الـ OnClick لاحقًا
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String coin = etCoin.getText().toString().trim().toUpperCase();
            String priceStr = etPrice.getText().toString().trim();

            if (coin.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter coin and target price", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid price format", Toast.LENGTH_SHORT).show();
                return;
            }

            PriceAlert alert = new PriceAlert();
            alert.coinSymbol = coin;
            alert.targetPrice = price;

            // تشغيل الإدخال في Thread منفصل لتجنب crash
            new Thread(() -> AppDatabase.getDatabase(MainActivity.this).priceAlertDao().insert(alert)).start();

            Toast.makeText(MainActivity.this, "Alert added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvCoins = findViewById(R.id.rvCoins);
        etSearchCoin = findViewById(R.id.etSearchCoin);

        btnHot = findViewById(R.id.btnHot);
        btnGainers = findViewById(R.id.btnGainers);
        btnLosers = findViewById(R.id.btnLosers);

        rvCoins.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MarketAdapter(new ArrayList<>(), coin -> {
            Intent intent = new Intent(MainActivity.this, CoinChartActivity.class);
            intent.putExtra("coin_id", coin.getId());
            startActivity(intent);
        });

        rvCoins.setAdapter(adapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_notify) {
                showAddAlertDialog();
                return true;
            }
            return false;
        });

        PeriodicWorkRequest priceCheckRequest =
                new PeriodicWorkRequest.Builder(PriceCheckWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "price_check_work",
                ExistingPeriodicWorkPolicy.KEEP,
                priceCheckRequest
        );

        swipeRefresh.setOnRefreshListener(this::fetchCoinsFromApi);

        setupSearch();

        btnHot.setOnClickListener(v -> {
            currentFilter = FilterType.HOT;
            updateFilterButtons();
            filterCoins(etSearchCoin.getText().toString());
        });

        btnGainers.setOnClickListener(v -> {
            currentFilter = FilterType.GAINERS;
            updateFilterButtons();
            filterCoins(etSearchCoin.getText().toString());
        });

        btnLosers.setOnClickListener(v -> {
            currentFilter = FilterType.LOSERS;
            updateFilterButtons();
            filterCoins(etSearchCoin.getText().toString());
        });

        updateFilterButtons();

        fetchCoinsFromApi();
    }

    private void updateFilterButtons() {
        int activeBgColor = 0xFF000000;
        int activeTextColor = 0xFFFFFFFF;
        int inactiveTextColor = 0xFF9E9E9E;
        int transparent = 0x00000000;

        btnHot.setBackgroundColor(currentFilter == FilterType.HOT ? activeBgColor : transparent);
        btnHot.setTextColor(currentFilter == FilterType.HOT ? activeTextColor : inactiveTextColor);

        btnGainers.setBackgroundColor(currentFilter == FilterType.GAINERS ? activeBgColor : transparent);
        btnGainers.setTextColor(currentFilter == FilterType.GAINERS ? activeTextColor : inactiveTextColor);

        btnLosers.setBackgroundColor(currentFilter == FilterType.LOSERS ? activeBgColor : transparent);
        btnLosers.setTextColor(currentFilter == FilterType.LOSERS ? activeTextColor : inactiveTextColor);
    }

    private void setupSearch() {
        etSearchCoin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCoins(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchCoin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                filterCoins(etSearchCoin.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void filterCoins(String query) {
        List<Coin> filteredList = new ArrayList<>();
        String lowerQuery = (query == null) ? "" : query.toLowerCase();

        for (Coin coin : allCoinsList) {
            boolean matchesSearch = coin.getName().toLowerCase().contains(lowerQuery) ||
                    coin.getSymbol().toLowerCase().contains(lowerQuery);

            if (!matchesSearch) continue;

            switch (currentFilter) {
                case HOT:
                    filteredList.add(coin);
                    break;
                case GAINERS:
                    if (coin.getChangePercent24h() > 0) filteredList.add(coin);
                    break;
                case LOSERS:
                    if (coin.getChangePercent24h() < 0) filteredList.add(coin);
                    break;
            }
        }

        if (currentFilter == FilterType.GAINERS) {
            filteredList.sort((c1, c2) -> Double.compare(c2.getChangePercent24h(), c1.getChangePercent24h()));
        } else if (currentFilter == FilterType.LOSERS) {
            filteredList.sort((c1, c2) -> Double.compare(c1.getChangePercent24h(), c2.getChangePercent24h()));
        }

        adapter.updateData(filteredList);
    }

    private void fetchCoinsFromApi() {
        allCoinsList.clear();
        swipeRefresh.setRefreshing(true);

        int totalPages = 3;
        final int[] pagesFetched = {0};
        List<Coin> tempList = new ArrayList<>();

        CoinGeckoApi api = ApiClient.getClient().create(CoinGeckoApi.class);

        for (int page = 1; page <= totalPages; page++) {
            int currentPage = page;
            Call<List<CoinGeckoCoin>> call = api.getCoins("usd", "market_cap_desc", 250, currentPage, false);

            call.enqueue(new Callback<List<CoinGeckoCoin>>() {
                @Override
                public void onResponse(Call<List<CoinGeckoCoin>> call, Response<List<CoinGeckoCoin>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (CoinGeckoCoin c : response.body()) {
                            tempList.add(new Coin(
                                    c.getId(),
                                    c.getName(),
                                    c.getSymbol(),
                                    c.getCurrentPrice(),
                                    c.getPriceChangePercentage24h()
                            ));
                        }
                    }

                    pagesFetched[0]++;
                    if (pagesFetched[0] == totalPages) { // بعد اكتمال جميع الصفحات
                        allCoinsList.addAll(tempList);
                        filterCoins(etSearchCoin.getText().toString());
                        swipeRefresh.setRefreshing(false);
                    }
                }

                @Override
                public void onFailure(Call<List<CoinGeckoCoin>> call, Throwable t) {
                    pagesFetched[0]++;
                    if (pagesFetched[0] == totalPages) {
                        allCoinsList.addAll(tempList);
                        filterCoins(etSearchCoin.getText().toString());
                        swipeRefresh.setRefreshing(false);
                    }
                    Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}