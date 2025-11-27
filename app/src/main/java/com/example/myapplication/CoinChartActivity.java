package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CoinChartActivity extends AppCompatActivity {

    private CandleStickChart candleChart;
    private ImageButton btnZoomIn, btnZoomOut;
    private BottomNavigationView bottomNavigationView;
    private List<Long> timestamps = new ArrayList<>();
    private String coinSymbol;

    private Button btn1D, btn7D, btn30D;
    private TextView tvCoinName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_chart);

        // 1️⃣ ربط العناصر
        candleChart = findViewById(R.id.candleChart);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tvCoinName = findViewById(R.id.tvCoinName);
        btn1D = findViewById(R.id.btn1D);
        btn7D = findViewById(R.id.btn7D);
        btn30D = findViewById(R.id.btn30D);

        // 2️⃣ استقبال اسم العملة
        coinSymbol = getIntent().getStringExtra("coin_id");
        if (coinSymbol == null || coinSymbol.isEmpty()) {
            coinSymbol = "bitcoin";
        }

        // 3️⃣ عرض اسم العملة
        tvCoinName.setText(coinSymbol.toUpperCase() + " / USD");

        // 4️⃣ تحميل البيانات
        loadLocalChartData(coinSymbol, "1d");
        setupChartStyle();

        // ⏱ الفترات الزمنية
        btn1D.setOnClickListener(v -> {
            loadLocalChartData(coinSymbol, "1d");
            setActive(btn1D);
        });
        btn7D.setOnClickListener(v -> {
            loadLocalChartData(coinSymbol, "7d");
            setActive(btn7D);
        });
        btn30D.setOnClickListener(v -> {
            loadLocalChartData(coinSymbol, "30d");
            setActive(btn30D);
        });

        // 🔍 زووم
        btnZoomIn.setOnClickListener(v -> candleChart.zoomIn());
        btnZoomOut.setOnClickListener(v -> candleChart.zoomOut());

        // 🚀 Bottom Navigation — تمت إضافة showAlertDialog 👌
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertActivity.class));
                return true;
            } else if (id == R.id.nav_notify) {
                showAlertDialog(); // 🔥 الآن يعمل بدون أخطاء
                return true;
            } else if (id == R.id.nav_whale_alerts) {
                startActivity(new Intent(this, WhaleAlertsActivity.class));
                return true;
            }
            return false;
        });
    }

    // 🟢 تحميل البيانات من JSON حسب الفترة
    private void loadLocalChartData(String coinId, String period) {
        try {
            InputStream is = getAssets().open("charts.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JsonObject allCharts = new Gson().fromJson(json, JsonObject.class);
            JsonObject coinData = allCharts.getAsJsonObject(coinId.toLowerCase());

            if (coinData == null || !coinData.has(period)) {
                Toast.makeText(this, "No data for: " + period, Toast.LENGTH_SHORT).show();
                return;
            }

            JsonArray ohlcArray = coinData.getAsJsonArray(period);
            List<CandleEntry> entries = new ArrayList<>();
            timestamps.clear();

            for (int i = 0; i < ohlcArray.size(); i++) {
                JsonObject obj = ohlcArray.get(i).getAsJsonObject();

                long time = obj.get("time").getAsLong();
                float open = obj.get("open").getAsFloat();
                float high = obj.get("high").getAsFloat();
                float low = obj.get("low").getAsFloat();
                float close = obj.get("close").getAsFloat();

                timestamps.add(time);
                entries.add(new CandleEntry(i, high, low, open, close));
            }

            CandleDataSet dataSet = new CandleDataSet(entries, coinSymbol.toUpperCase());
            dataSet.setDecreasingColor(Color.RED);
            dataSet.setIncreasingColor(Color.GREEN);
            dataSet.setShadowColor(Color.GRAY);
            dataSet.setDrawValues(false);

            candleChart.setData(new CandleData(dataSet));
            candleChart.invalidate();

        } catch (Exception e) {
            Toast.makeText(this, "Error loading data!", Toast.LENGTH_SHORT).show();
        }
    }

    // 🧠 تنسيق الشارت
    private void setupChartStyle() {
        CustomMarkerView markerView = new CustomMarkerView(this, R.layout.marker_view, timestamps);
        candleChart.setMarker(markerView);

        candleChart.setBackgroundColor(Color.BLACK);
        candleChart.getDescription().setEnabled(false);
        candleChart.setPinchZoom(true);

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < timestamps.size()) {
                    return new SimpleDateFormat("dd MMM", Locale.getDefault())
                            .format(new Date(timestamps.get(index)));
                }
                return "";
            }
        });

        YAxis left = candleChart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        candleChart.getAxisRight().setEnabled(false);
    }

    private void setActive(Button activeBtn) {
        btn1D.setTextColor(Color.GRAY);
        btn7D.setTextColor(Color.GRAY);
        btn30D.setTextColor(Color.GRAY);

        activeBtn.setTextColor(Color.BLACK);
    }

    // 📌 🟢🟢 تم إضافة الميثود المفقود هنا (حل نهائي)
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Price Alert");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etSymbol.setText(coinSymbol.toUpperCase());
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
}
