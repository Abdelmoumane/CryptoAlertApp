package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

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

    private enum FilterType { HOT, GAINERS, LOSERS }
    private FilterType currentFilter = FilterType.HOT;

    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            fetchCoinsFromApi();
            handler.postDelayed(this, 30000); // كل 30 ثانية
        }
    };

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
            // نرسل id بدلاً من symbol
            Intent intent = new Intent(MainActivity.this, CoinChartActivity.class);
            intent.putExtra("coin_id", coin.getId());
            startActivity(intent);
        });

        rvCoins.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> fetchCoinsFromApi());

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
        int activeBgColor = 0xFF000000;  // أسود كامل للخلفية النشطة
        int activeTextColor = 0xFFFFFFFF;    // أبيض للنص النشط
        int inactiveTextColor = 0xFF9E9E9E;  // رمادي فاتح للنص غير النشط
        int transparent = 0x00000000;        // شفاف

        btnHot.setBackgroundColor(currentFilter == FilterType.HOT ? activeBgColor : transparent);
        btnHot.setTextColor(currentFilter == FilterType.HOT ? activeTextColor : inactiveTextColor);

        btnGainers.setBackgroundColor(currentFilter == FilterType.GAINERS ? activeBgColor : transparent);
        btnGainers.setTextColor(currentFilter == FilterType.GAINERS ? activeTextColor : inactiveTextColor);

        btnLosers.setBackgroundColor(currentFilter == FilterType.LOSERS ? activeBgColor : transparent);
        btnLosers.setTextColor(currentFilter == FilterType.LOSERS ? activeTextColor : inactiveTextColor);
    }

    private void setupSearch() {
        etSearchCoin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCoins(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
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

        switch (currentFilter) {
            case GAINERS:
                filteredList.sort((c1, c2) -> Double.compare(c2.getChangePercent24h(), c1.getChangePercent24h()));
                break;
            case LOSERS:
                filteredList.sort((c1, c2) -> Double.compare(c1.getChangePercent24h(), c2.getChangePercent24h()));
                break;
        }

        adapter.updateData(filteredList);
    }

    private void fetchCoinsFromApi() {
        allCoinsList.clear();
        fetchPage(1);
        fetchPage(2);
        fetchPage(3);
    }

    private void fetchPage(int page) {
        CoinGeckoApi api = ApiClient.getClient().create(CoinGeckoApi.class);
        Call<List<CoinGeckoCoin>> call = api.getCoins("usd", "market_cap_desc", 250, page, false);

        call.enqueue(new Callback<List<CoinGeckoCoin>>() {
            @Override
            public void onResponse(Call<List<CoinGeckoCoin>> call, Response<List<CoinGeckoCoin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (CoinGeckoCoin c : response.body()) {
                        allCoinsList.add(new Coin(
                                c.getId(),
                                c.getName(),
                                c.getSymbol(),
                                c.getCurrentPrice(),
                                c.getPriceChangePercentage24h()
                        ));
                    }
                    filterCoins(etSearchCoin.getText().toString());
                }
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<List<CoinGeckoCoin>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(runnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }
}
