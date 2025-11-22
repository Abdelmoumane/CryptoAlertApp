package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

// ⚠ مهم جدًا للأنيميشن:
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CoinChartActivity extends AppCompatActivity {

    private CandleStickChart candleChart;
    private Button btn1D, btn7D, btn30D;
    private String coinSymbol;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnZoomIn, btnZoomOut;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_chart);

        candleChart = findViewById(R.id.candleChart);
        btn1D = findViewById(R.id.btn1D);
        btn7D = findViewById(R.id.btn7D);
        btn30D = findViewById(R.id.btn30D);
        btnZoomIn   = findViewById(R.id.btnZoomIn);
        btnZoomOut  = findViewById(R.id.btnZoomOut);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // ⚡ تحميل الأنيميشن
        Animation fadeIn  = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // 🔍 زر التكبير +
        btnZoomIn.setOnClickListener(v -> {
            candleChart.zoomIn();
            v.startAnimation(fadeIn);   // تأثير حركة راقية
        });

        // 🔎 زر التصغير -
        btnZoomOut.setOnClickListener(v -> {
            candleChart.zoomOut();
            v.startAnimation(fadeOut);
        });

        setupChartSettings();

        coinSymbol = getIntent().getStringExtra("coin_id");
        if (coinSymbol == null || coinSymbol.isEmpty()) {
            Toast.makeText(this, "Error: No Coin ID passed!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchOHLCData(coinSymbol, 1);

        btn1D.setOnClickListener(v -> fetchOHLCData(coinSymbol, 1));
        btn7D.setOnClickListener(v -> fetchOHLCData(coinSymbol, 7));
        btn30D.setOnClickListener(v -> fetchOHLCData(coinSymbol, 30));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                finish();
                return true;
            }
            else if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertActivity.class));
                return true;
            }
            else if (id == R.id.nav_notify) {
                showAlertDialog();
                return true;
            }
            return false;
        });
    }

    private void setupChartSettings() {
        candleChart.setDragEnabled(true);
        candleChart.setScaleEnabled(true);
        candleChart.setPinchZoom(true);
        candleChart.setDoubleTapToZoomEnabled(true);
        candleChart.getDescription().setEnabled(false);

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void fetchOHLCData(String coinId, int days) {
        CoinGeckoApi api = RetrofitClient.getInstance().create(CoinGeckoApi.class);
        api.getOHLC(coinId, "usd", days).enqueue(new Callback<List<List<Double>>>() {
            @Override
            public void onResponse(@NonNull Call<List<List<Double>>> call,
                                   @NonNull Response<List<List<Double>>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<CandleEntry> candleEntries = new ArrayList<>();
                    List<Long> timestamps = new ArrayList<>();

                    for (int i = 0; i < response.body().size(); i++) {
                        List<Double> data = response.body().get(i);

                        timestamps.add(data.get(0).longValue());

                        candleEntries.add(new CandleEntry(
                                i, data.get(2).floatValue(), data.get(3).floatValue(),
                                data.get(1).floatValue(), data.get(4).floatValue()
                        ));
                    }

                    candleChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int index = (int) value;
                            if (index >= 0 && index < timestamps.size()) {
                                return new SimpleDateFormat("dd MMM", Locale.getDefault())
                                        .format(new Date(timestamps.get(index)));
                            }
                            return "";
                        }
                    });

                    CandleDataSet dataSet = new CandleDataSet(candleEntries,
                            coinId.toUpperCase() + " / USD");
                    dataSet.setDecreasingColor(Color.RED);
                    dataSet.setIncreasingColor(Color.GREEN);
                    dataSet.setShadowColor(Color.WHITE);
                    dataSet.setNeutralColor(Color.GRAY);
                    dataSet.setDrawValues(false);

                    candleChart.setData(new CandleData(dataSet));
                    candleChart.invalidate();

                    candleChart.setVisibleXRangeMinimum(5);
                    candleChart.setVisibleXRangeMaximum(60);
                    candleChart.moveViewToX(candleEntries.size());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<List<Double>>> call,
                                  @NonNull Throwable t) {
                Toast.makeText(CoinChartActivity.this, "Error loading chart!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlertDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etSymbol.setText(coinSymbol.toUpperCase());

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        btnSave.setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (symbol.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Enter all data", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try { price = Double.parseDouble(target); }
            catch (Exception e) { Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show(); return; }

            Executors.newSingleThreadExecutor().execute(() -> {
                PriceAlert alert = new PriceAlert();
                alert.coinSymbol = symbol;
                alert.targetPrice = price;
                AppDatabase.getDatabase(this).priceAlertDao().insert(alert);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert saved!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
        });

        dialog.show();
    }
}
