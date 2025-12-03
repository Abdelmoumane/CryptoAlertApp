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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CoinChartActivity extends AppCompatActivity {

    private CandleStickChart candleChart;
    private ImageButton btnZoomIn, btnZoomOut;
    private BottomNavigationView bottomNavigationView;
    private List<Long> timestamps = new ArrayList<>();

    private Button btn1D, btn7D, btn30D;
    private TextView tvCoinName, tvCurrentPrice;

    private String coinId;      // id في CoinGecko / charts.json (bitcoin, solana…)
    private String coinSymbol;  // الرمز في الواجهة (BTC, SOL…)
    private String chartId;     // المفتاح المستخدم في charts.json

    private CoinGeckoApi api;   // ✅ لطلب الـ OHLC من CoinGecko

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
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        btn1D = findViewById(R.id.btn1D);
        btn7D = findViewById(R.id.btn7D);
        btn30D = findViewById(R.id.btn30D);

        // 2️⃣ استقبال البيانات من MainActivity
        Intent intent = getIntent();
        coinId = intent.getStringExtra("coin_id");          // bitcoin, solana...
        coinSymbol = intent.getStringExtra("coin_symbol");  // BTC, SOL...
        double coinPrice = intent.getDoubleExtra("coin_price", -1);

        // قيم افتراضية لو في نقص في الـ extras
        if (coinId == null && coinSymbol == null) {
            coinId = "bitcoin";
            coinSymbol = "BTC";
        }

        if (coinSymbol == null || coinSymbol.isEmpty()) {
            coinSymbol = (coinId != null) ? coinId : "BTC";
        }

        chartId = (coinId != null) ? coinId.toLowerCase() : coinSymbol.toLowerCase();

        // 3️⃣ عرض اسم العملة + السعر
        tvCoinName.setText(coinSymbol.toUpperCase() + " / USD");

        if (coinPrice >= 0) {
            tvCurrentPrice.setText(String.format(Locale.getDefault(), "$%.2f", coinPrice));
        } else {
            tvCurrentPrice.setText("");
        }

        // 4️⃣ تهيئة Retrofit للـ CoinGecko
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.coingecko.com/api/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(CoinGeckoApi.class);

        // 5️⃣ إعداد شكل الشارت + تحميل أول داتا
        setupChartStyle();
        setActive(btn1D);
        loadChartData("1d");   // 👈 بدل loadLocalChartData

        // ⏱ الفترات الزمنية
        btn1D.setOnClickListener(v -> {
            setActive(btn1D);
            loadChartData("1d");
        });
        btn7D.setOnClickListener(v -> {
            setActive(btn7D);
            loadChartData("7d");
        });
        btn30D.setOnClickListener(v -> {
            setActive(btn30D);
            loadChartData("30d");
        });

        // 🔍 زووم
        btnZoomIn.setOnClickListener(v -> candleChart.zoomIn());
        btnZoomOut.setOnClickListener(v -> candleChart.zoomOut());

        // 🚀 Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertActivity.class));
                return true;
            } else if (id == R.id.nav_notify) {
                showAlertDialog();
                return true;
            } else if (id == R.id.nav_whale_alerts) {
                startActivity(new Intent(this, WhaleAlertsActivity.class));
                return true;
            }
            return false;
        });
    }

    // 🔵 يحاول أولاً CoinGecko → لو فشل يرجع للـ charts.json
    private void loadChartData(String period) {
        int days;
        switch (period) {
            case "7d":
                days = 7;
                break;
            case "30d":
                days = 30;
                break;
            default:
                days = 1;
        }

        Call<List<List<Double>>> call = api.getOhlc(chartId, "usd", days);

        call.enqueue(new Callback<List<List<Double>>>() {
            @Override
            public void onResponse(Call<List<List<Double>>> call,
                                   Response<List<List<Double>>> response) {
                List<List<Double>> data = response.body();

                if (!response.isSuccessful() || data == null || data.isEmpty()) {
                    // 🟥 API فشل → نستخدم JSON
                    loadLocalChartData(chartId, period, true);
                    return;
                }

                List<CandleEntry> entries = new ArrayList<>();
                timestamps.clear();

                // كل عنصر: [timestamp, open, high, low, close]
                for (int i = 0; i < data.size(); i++) {
                    List<Double> point = data.get(i);
                    long time = point.get(0).longValue();
                    float open = point.get(1).floatValue();
                    float high = point.get(2).floatValue();
                    float low = point.get(3).floatValue();
                    float close = point.get(4).floatValue();

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
            }

            @Override
            public void onFailure(Call<List<List<Double>>> call, Throwable t) {
                // 🌐 مفيش نت / Error → نرجع للـ JSON المحلي
                loadLocalChartData(chartId, period, true);
            }
        });
    }

    // ✅ التحقق من أن الرمز موجود في coins.json (للـ Alert Dialog فقط)
    private boolean isValidCoinSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) return false;

        try {
            InputStream is = getAssets().open("coins.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            CoinLocalResponse response = new Gson().fromJson(json, CoinLocalResponse.class);

            for (Coin coin : response.coins) {
                if (coin.getSymbol().equalsIgnoreCase(symbol)
                        || coin.getId().equalsIgnoreCase(symbol)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 🟢 تحميل البيانات من JSON حسب الفترة (Mock) + Toast بسيط
    private void loadLocalChartData(String coinKey, String period, boolean showToast) {
        try {
            InputStream is = getAssets().open("charts.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JsonObject allCharts = new Gson().fromJson(json, JsonObject.class);
            JsonObject coinData = allCharts.getAsJsonObject(coinKey.toLowerCase());

            if (coinData == null || !coinData.has(period)) {
                Toast.makeText(this, "No data for: " + period, Toast.LENGTH_SHORT).show();
                return;
            }

            if (showToast) {
                Toast.makeText(this,
                        "Using offline mock chart data (" + period + ")",
                        Toast.LENGTH_SHORT).show();
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
            e.printStackTrace();
            Toast.makeText(this, "Error loading data!", Toast.LENGTH_SHORT).show();
        }
    }

    // 🧠 تنسيق الشارت (نفس اللي كان عندك)
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

    // 📌 Dialog لإضافة تنبيه من شاشة الشارت
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Price Alert");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // نستخدم نفس الرمز المعروض في الشاشة
        if (coinSymbol != null) {
            etSymbol.setText(coinSymbol.toUpperCase());
            etSymbol.setEnabled(false);
        }

        btnSave.setOnClickListener(v -> {
            String symbolInput = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (target.isEmpty()) {
                Toast.makeText(this, "Enter target price", Toast.LENGTH_SHORT).show();
                return;
            }

            // تأكيد أن العملة موجودة في coins.json
            if (!isValidCoinSymbol(symbolInput)) {
                Toast.makeText(this, "Coin not found in coins.json", Toast.LENGTH_SHORT).show();
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
            alert.coinSymbol = symbolInput.toUpperCase();
            alert.targetPrice = targetPrice;
            alert.isTriggered = false;

            new Thread(() -> {
                AppDatabase.getDatabase(CoinChartActivity.this)
                        .priceAlertDao()
                        .insert(alert);

                runOnUiThread(() -> {
                    Toast.makeText(CoinChartActivity.this,
                            "Alert Saved!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }).start();
        });

        dialog.show();
    }
}