package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "CoinChartDebug";

    private CandleStickChart candleChart;
    private ImageButton btnZoomIn, btnZoomOut;
    private BottomNavigationView bottomNavigationView;
    private List<Long> timestamps = new ArrayList<>();

    private Button btn1D;
    private TextView tvCoinName, tvCurrentPrice;

    private String coinId;      // id من CoinPaprika (مثلاً btc-bitcoin)
    private String coinSymbol;  // BTC, ETH...
    private double coinPrice;

    private CoinPaprikaApi paprikaApi;

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

        // 2️⃣ استقبال البيانات من MainActivity
        Intent intent = getIntent();
        coinId = intent.getStringExtra("coin_id");          // من Paprika (مثلاً btc-bitcoin)
        coinSymbol = intent.getStringExtra("coin_symbol");  // BTC, ETH...
        coinPrice = intent.getDoubleExtra("coin_price", -1);

        if (coinId == null || coinId.isEmpty()) {
            coinId = "btc-bitcoin";
        }
        if (coinSymbol == null || coinSymbol.isEmpty()) {
            coinSymbol = "BTC";
        }

        Log.d(TAG, "onCreate: coinId=" + coinId +
                ", coinSymbol=" + coinSymbol +
                ", coinPrice=" + coinPrice);

        // 3️⃣ عرض اسم العملة + السعر
        tvCoinName.setText(coinSymbol.toUpperCase(Locale.ROOT) + " / USD");
        if (coinPrice >= 0) {
            tvCurrentPrice.setText(
                    String.format(Locale.getDefault(), "$%.2f", coinPrice)
            );
        } else {
            tvCurrentPrice.setText("");
        }

        // 4️⃣ تهيئة Retrofit لـ CoinPaprika
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.coinpaprika.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        paprikaApi = retrofit.create(CoinPaprikaApi.class);

        // 5️⃣ شكل الشارت
        setupChartStyle();

        // أول تحميل (شمعة اليوم)
        loadTodayCandle();

        // زر 1D يعيد التحميل فقط
        btn1D.setOnClickListener(v -> loadTodayCandle());

        // زووم
        btnZoomIn.setOnClickListener(v -> candleChart.zoomIn());
        btnZoomOut.setOnClickListener(v -> candleChart.zoomOut());

        // Bottom Navigation
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

    // 🟢 نحاول أولاً CoinPaprika (شمعة اليوم) → لو فشل نرجع للـ charts.json (1d)
    private void loadTodayCandle() {
        String localKey = getLocalChartKey();

        if (paprikaApi == null || coinId == null || coinId.isEmpty()) {
            loadLocalChartData(localKey, true);
            return;
        }

        Log.d(TAG, "Requesting OHLCV TODAY from CoinPaprika: coinId=" + coinId);

        Call<List<CoinOhlcvResponse>> call =
                paprikaApi.getOhlcvToday(coinId);

        call.enqueue(new Callback<List<CoinOhlcvResponse>>() {
            @Override
            public void onResponse(Call<List<CoinOhlcvResponse>> call,
                                   Response<List<CoinOhlcvResponse>> response) {

                List<CoinOhlcvResponse> list = response.body();

                if (!response.isSuccessful() || list == null || list.isEmpty()) {

                    Log.e(TAG, "Paprika response NOT successful. " +
                            "code=" + response.code() +
                            ", message=" + response.message());

                    try {
                        if (response.errorBody() != null) {
                            String errorText = response.errorBody().string();
                            Log.e(TAG, "Paprika errorBody: " + errorText);
                        } else {
                            Log.e(TAG, "Paprika errorBody is null");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody", e);
                    }

                    if (list == null) {
                        Log.e(TAG, "Paprika body is null");
                    } else {
                        Log.e(TAG, "Paprika body size = " + list.size());
                    }

                    loadLocalChartData(localKey, true);
                    return;
                }

                Log.d(TAG, "Paprika response OK. items = " + list.size());

                List<CandleEntry> entries = new ArrayList<>();
                timestamps.clear();

                for (int i = 0; i < list.size(); i++) {
                    CoinOhlcvResponse o = list.get(i);

                    long time = parsePaprikaTime(o.time_open);
                    float open = (float) o.open;
                    float high = (float) o.high;
                    float low  = (float) o.low;
                    float close= (float) o.close;

                    timestamps.add(time);
                    entries.add(new CandleEntry(i, high, low, open, close));
                }

                if (entries.isEmpty()) {
                    loadLocalChartData(localKey, true);
                    return;
                }

                CandleDataSet dataSet =
                        new CandleDataSet(entries, coinSymbol.toUpperCase(Locale.ROOT));
                dataSet.setDecreasingColor(Color.RED);
                dataSet.setIncreasingColor(Color.GREEN);
                dataSet.setShadowColor(Color.GRAY);
                dataSet.setDrawValues(false);

                candleChart.setData(new CandleData(dataSet));
                candleChart.invalidate();
            }

            @Override
            public void onFailure(Call<List<CoinOhlcvResponse>> call, Throwable t) {
                Log.e(TAG, "Paprika onFailure: " + t.getMessage());
                loadLocalChartData(localKey, true);
            }
        });
    }

    // 🕒 تحويل time_open من "2024-11-29T00:00:00Z" إلى millis
    private long parsePaprikaTime(String timeStr) {
        if (timeStr == null) return 0L;
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
            Date d = sdf.parse(timeStr);
            return d != null ? d.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // 🟢 اختيار المفتاح الصحيح في charts.json
    private String getLocalChartKey() {
        if (coinSymbol == null) return "bitcoin";
        String sym = coinSymbol.toUpperCase(Locale.ROOT);
        switch (sym) {
            case "BTC":  return "bitcoin";
            case "ETH":  return "ethereum";
            case "SOL":  return "solana";
            case "ADA":  return "cardano";
            case "XRP":  return "xrp";
            case "DOGE": return "dogecoin";
            case "DOT":  return "polkadot";
            case "LINK": return "chainlink";
            case "LTC":  return "litecoin";
            case "AVAX": return "avalanche";
            default:
                return sym.toLowerCase(Locale.ROOT);
        }
    }

    // 🟢 تحميل 1d من charts.json
    private void loadLocalChartData(String coinKey, boolean showToast) {
        try {
            Log.d(TAG, "Loading local chart data: key=" + coinKey + ", period=1d");

            InputStream is = getAssets().open("charts.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JsonObject allCharts = new Gson().fromJson(json, JsonObject.class);

            JsonObject coinData = allCharts.getAsJsonObject(coinKey.toLowerCase(Locale.ROOT));

            if (coinData == null || !coinData.has("1d")) {
                Toast.makeText(this,
                        "No local chart data for " + coinKey + " (1d)",
                        Toast.LENGTH_SHORT).show();
                candleChart.clear();
                candleChart.invalidate();
                return;
            }

            if (showToast) {
                Toast.makeText(this,
                        "Using offline mock chart data (1d)",
                        Toast.LENGTH_SHORT).show();
            }

            JsonArray ohlcArray = coinData.getAsJsonArray("1d");
            if (ohlcArray.size() == 0) {
                candleChart.clear();
                candleChart.invalidate();
                return;
            }

            List<CandleEntry> entries = new ArrayList<>();
            timestamps.clear();

            for (int i = 0; i < ohlcArray.size(); i++) {
                JsonObject obj = ohlcArray.get(i).getAsJsonObject();

                long time = obj.get("time").getAsLong();
                float open = obj.get("open").getAsFloat();
                float high = obj.get("high").getAsFloat();
                float low  = obj.get("low").getAsFloat();
                float close= obj.get("close").getAsFloat();

                timestamps.add(time);
                entries.add(new CandleEntry(i, high, low, open, close));
            }

            CandleDataSet dataSet =
                    new CandleDataSet(entries, coinSymbol.toUpperCase(Locale.ROOT));
            dataSet.setDecreasingColor(Color.RED);
            dataSet.setIncreasingColor(Color.GREEN);
            dataSet.setShadowColor(Color.GRAY);
            dataSet.setDrawValues(false);

            candleChart.setData(new CandleData(dataSet));
            candleChart.invalidate();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading local data!", Toast.LENGTH_SHORT).show();
            candleChart.clear();
            candleChart.invalidate();
        }
    }

    // 🎨 إعداد شكل الشارت
    private void setupChartStyle() {
        CustomMarkerView markerView = new CustomMarkerView(this,
                R.layout.marker_view, timestamps);
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

    // ✅ يتحقق من وجود الرمز في coins.json (لـ Dialog التنبيه)
    private boolean isValidCoinSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) return false;

        try {
            InputStream is = getAssets().open("coins.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            CoinLocalResponse response = new Gson().fromJson(json, CoinLocalResponse.class);

            if (response == null || response.coins == null) return false;

            for (Coin c : response.coins) {
                if (c.getSymbol().equalsIgnoreCase(symbol)
                        || c.getId().equalsIgnoreCase(symbol)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

        if (coinSymbol != null) {
            etSymbol.setText(coinSymbol.toUpperCase(Locale.ROOT));
            etSymbol.setEnabled(false);
        }

        btnSave.setOnClickListener(v -> {
            String symbolInput = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (target.isEmpty()) {
                Toast.makeText(this, "Enter target price", Toast.LENGTH_SHORT).show();
                return;
            }

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
            alert.coinSymbol = symbolInput.toUpperCase(Locale.ROOT);
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
