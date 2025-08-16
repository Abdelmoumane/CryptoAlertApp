package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CoinChartActivity extends AppCompatActivity {

    private LineChart lineChart;
    private Button btn1D, btn7D, btn30D;
    private String coinSymbol;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_chart);

        lineChart = findViewById(R.id.lineChart);
        btn1D = findViewById(R.id.btn1D);
        btn7D = findViewById(R.id.btn7D);
        btn30D = findViewById(R.id.btn30D);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        coinSymbol = getIntent().getStringExtra("coin_id");
        if (coinSymbol == null || coinSymbol.isEmpty()) {
            Toast.makeText(this, "No coin ID passed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchHistoricalData(coinSymbol, 1); // افتراضياً 1 يوم

        btn1D.setOnClickListener(v -> fetchHistoricalData(coinSymbol, 1));
        btn7D.setOnClickListener(v -> fetchHistoricalData(coinSymbol, 7));
        btn30D.setOnClickListener(v -> fetchHistoricalData(coinSymbol, 30));

        // إعداد التنقل السفلي
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish(); // العودة إلى MainActivity
                return true;
            } else if (id == R.id.nav_notify) {
                showAlertDialog();
                return true;
            }
            return false;
        });
    }

    private void showAlertDialog() {
        // Inflate layout dialog_alert.xml
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // ملء رمز العملة تلقائياً
        etSymbol.setText(coinSymbol.toUpperCase());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnSave.setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString().trim();
            String targetStr = etTarget.getText().toString().trim();

            if (symbol.isEmpty() || targetStr.isEmpty()) {
                Toast.makeText(this, "Enter data", Toast.LENGTH_SHORT).show();
                return;
            }

            double target;
            try {
                target = Double.parseDouble(targetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid target price", Toast.LENGTH_SHORT).show();
                return;
            }

            // تنفيذ الإدخال على خيط خلفي لتسريع التطبيق
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(this);
                PriceAlert alert = new PriceAlert();
                alert.coinSymbol = symbol;
                alert.targetPrice = target;
                db.priceAlertDao().insert(alert);

                // العودة للـ Main Thread لإظهار Toast
                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert saved", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
        });

        dialog.show();
    }


    private void fetchHistoricalData(String coinId, int days) {
        CoinGeckoApi api = RetrofitClient.getInstance().create(CoinGeckoApi.class);
        api.getMarketChart(coinId, "usd", days).enqueue(new Callback<MarketChartResponse>() {
            @Override
            public void onResponse(@NonNull Call<MarketChartResponse> call, @NonNull Response<MarketChartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<List<Double>> prices = response.body().getPrices();
                    List<Entry> entries = new ArrayList<>();

                    for (int i = 0; i < prices.size(); i++) {
                        float x = i;
                        float y = prices.get(i).get(1).floatValue();
                        entries.add(new Entry(x, y));
                    }

                    LineDataSet dataSet = new LineDataSet(entries, coinId.toUpperCase() + " Price (USD)");
                    dataSet.setColor(getResources().getColor(R.color.purple_500));
                    dataSet.setValueTextColor(getResources().getColor(R.color.black));

                    LineData lineData = new LineData(dataSet);
                    lineChart.setData(lineData);
                    lineChart.invalidate();
                } else {
                    Toast.makeText(CoinChartActivity.this, "Failed to load chart data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarketChartResponse> call, @NonNull Throwable t) {
                Toast.makeText(CoinChartActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
