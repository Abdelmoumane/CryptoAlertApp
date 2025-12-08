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

    private MarketRepository marketRepository;

    private CandleStickChart candleChart;
    private ImageButton btnZoomIn, btnZoomOut;
    private BottomNavigationView bottomNavigationView;
    private List<Long> timestamps = new ArrayList<>();

    private Button btn1D;
    private TextView tvCoinName, tvCurrentPrice;

    private String coinId;      // De MarketRepository (por ejemplo btc-bitcoin, sol-solana ... si lo necesitas más adelante)
    private String coinSymbol;  // BTC, ETH ...
    private double coinPrice;

    // Binance solamente
    private BinanceApi binanceApi;
    private String binanceSymbol;   // Como BTCUSDT, ETHUSDT ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_chart);

        marketRepository = new MarketRepository(this);

        // Enlazar vistas
        candleChart = findViewById(R.id.candleChart);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tvCoinName = findViewById(R.id.tvCoinName);
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        btn1D = findViewById(R.id.btn1D);   // Asegúrate de que el botón exista en el XML

        // Recibir datos desde MainActivity
        Intent intent = getIntent();
        coinId = intent.getStringExtra("coin_id");
        coinSymbol = intent.getStringExtra("coin_symbol");
        coinPrice = intent.getDoubleExtra("coin_price", -1);

        if (coinSymbol == null || coinSymbol.isEmpty()) {
            coinSymbol = "BTC";
        }
        if (coinId == null || coinId.isEmpty()) {
            coinId = "btc-bitcoin";
        }

        tvCoinName.setText(coinSymbol.toUpperCase(Locale.ROOT) + " / USD");
        if (coinPrice >= 0) {
            tvCurrentPrice.setText(String.format(Locale.getDefault(), "$%.2f", coinPrice));
        } else {
            tvCurrentPrice.setText("");
        }

        // Convertimos el símbolo al formato de Binance (por ejemplo BTC → BTCUSDT)
        binanceSymbol = mapToBinanceSymbol(coinSymbol);

        // Configurar Retrofit para Binance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.binance.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        binanceApi = retrofit.create(BinanceApi.class);

        // Estilo del chart
        setupChartStyle();
        setActive(btn1D);

        // Primera carga: 1D = 24 velas de 1 hora
        loadChartData1D();

        // Único botón 1D
        btn1D.setOnClickListener(v -> {
            setActive(btn1D);
            loadChartData1D();
        });

        // Zoom
        btnZoomIn.setOnClickListener(v -> candleChart.zoomIn());
        btnZoomOut.setOnClickListener(v -> candleChart.zoomOut());

        // Bottom Navigation (como lo tenías)
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

    // Convertimos el símbolo de la moneda al formato de Binance
    private String mapToBinanceSymbol(String symbol) {
        String s = symbol.toUpperCase(Locale.ROOT);
        switch (s) {
            case "BTC": return "BTCUSDT";
            case "ETH": return "ETHUSDT";
            case "SOL": return "SOLUSDT";
            case "ADA": return "ADAUSDT";
            case "XRP": return "XRPUSDT";
            case "DOGE": return "DOGEUSDT";
            case "DOT": return "DOTUSDT";
            case "LINK": return "LINKUSDT";
            case "LTC": return "LTCUSDT";
            case "AVAX": return "AVAXUSDT";
            default:    return s + "USDT";
        }
    }

    // Elige la clave adecuada dentro de charts.json para los datos locales
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
            default:     return sym.toLowerCase(Locale.ROOT);
        }
    }

    // Cargar 24 velas de 1 hora desde Binance, con fallback al JSON mock
    private void loadChartData1D() {
        String localKey = getLocalChartKey();
        Log.d(TAG, "loadChartData1D() binanceSymbol=" + binanceSymbol +
                ", localKey=" + localKey);

        if (binanceApi == null) {
            Log.e(TAG, "Using LOCAL data. reason=no_binance_api");
            loadLocalChartData(localKey, "1d", true);
            return;
        }

        Log.d(TAG, "Requesting 24 * 1h candles from Binance, symbol=" + binanceSymbol);

        Call<List<List<Object>>> call =
                binanceApi.getKlines(binanceSymbol, "1h", 24);

        call.enqueue(new Callback<List<List<Object>>>() {
            @Override
            public void onResponse(Call<List<List<Object>>> call,
                                   Response<List<List<Object>>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Using LOCAL data. reason=binance_http_error, code=" + response.code());
                    loadLocalChartData(localKey, "1d", true);
                    return;
                }

                List<List<Object>> data = response.body();
                if (data.isEmpty()) {
                    Log.e(TAG, "Using LOCAL data. reason=binance_empty_body");
                    loadLocalChartData(localKey, "1d", true);
                    return;
                }

                List<CandleEntry> entries = new ArrayList<>();
                timestamps.clear();

                try {
                    for (int i = 0; i < data.size(); i++) {
                        List<Object> c = data.get(i);

                        long openTime = ((Number) c.get(0)).longValue();
                        float open  = Float.parseFloat(c.get(1).toString());
                        float high  = Float.parseFloat(c.get(2).toString());
                        float low   = Float.parseFloat(c.get(3).toString());
                        float close = Float.parseFloat(c.get(4).toString());

                        timestamps.add(openTime);
                        entries.add(new CandleEntry(i, high, low, open, close));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Using LOCAL data. reason=binance_parse_error: " + e.getMessage(), e);
                    loadLocalChartData(localKey, "1d", true);
                    return;
                }

                if (entries.isEmpty()) {
                    Log.e(TAG, "Using LOCAL data. reason=entries_empty_after_parse");
                    loadLocalChartData(localKey, "1d", true);
                    return;
                }

                Log.d(TAG, "ONLINE data OK from Binance. candleCount=" + entries.size());

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
            public void onFailure(Call<List<List<Object>>> call, Throwable t) {
                Log.e(TAG, "Using LOCAL data. reason=binance_failure: " + t.getMessage(), t);
                loadLocalChartData(localKey, "1d", true);
            }
        });
    }

    // Cargar datos desde charts.json (mock offline)
    private void loadLocalChartData(String coinKey, String period, boolean showToast) {
        Log.d(TAG, "loadLocalChartData() coinKey=" + coinKey + ", period=" + period);

        try {
            InputStream is = getAssets().open("charts.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JsonObject allCharts = new Gson().fromJson(json, JsonObject.class);

            JsonObject coinData = allCharts.getAsJsonObject(coinKey.toLowerCase(Locale.ROOT));

            if (coinData == null || !coinData.has(period)) {
                Log.e(TAG, "NO LOCAL DATA for coinKey=" + coinKey + ", period=" + period);
                Toast.makeText(this,
                        "No local chart data for " + coinKey + " (" + period + ")",
                        Toast.LENGTH_SHORT).show();
                candleChart.clear();
                candleChart.invalidate();
                return;
            }

            if (showToast) {
                Toast.makeText(this,
                        "Using offline mock chart data (" + period + ")",
                        Toast.LENGTH_SHORT).show();
            }

            JsonArray ohlcArray = coinData.getAsJsonArray(period);
            if (ohlcArray.size() == 0) {
                Log.e(TAG, "LOCAL DATA ARRAY EMPTY for coinKey=" + coinKey);
                candleChart.clear();
                candleChart.invalidate();
                return;
            }

            List<CandleEntry> entries = new ArrayList<>();
            timestamps.clear();

            for (int i = 0; i < ohlcArray.size(); i++) {
                JsonObject obj = ohlcArray.get(i).getAsJsonObject();

                long time = obj.get("time").getAsLong();
                float open  = obj.get("open").getAsFloat();
                float high  = obj.get("high").getAsFloat();
                float low   = obj.get("low").getAsFloat();
                float close = obj.get("close").getAsFloat();

                timestamps.add(time);
                entries.add(new CandleEntry(i, high, low, open, close));
            }

            Log.d(TAG, "LOCAL data loaded. entries=" + entries.size());

            CandleDataSet dataSet =
                    new CandleDataSet(entries, coinSymbol.toUpperCase(Locale.ROOT));
            dataSet.setDecreasingColor(Color.RED);
            dataSet.setIncreasingColor(Color.GREEN);
            dataSet.setShadowColor(Color.GRAY);
            dataSet.setDrawValues(false);

            candleChart.setData(new CandleData(dataSet));
            candleChart.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Error reading LOCAL charts.json: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading local data!", Toast.LENGTH_SHORT).show();
            candleChart.clear();
            candleChart.invalidate();
        }
    }

    // Estilo del chart + eje X por horas
    private void setupChartStyle() {
        CustomMarkerView markerView =
                new CustomMarkerView(this, R.layout.marker_view, timestamps);
        candleChart.setMarker(markerView);

        candleChart.setBackgroundColor(Color.BLACK);
        candleChart.getDescription().setEnabled(false);
        candleChart.setPinchZoom(true);

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat fmt =
                    new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < timestamps.size()) {
                    return fmt.format(new Date(timestamps.get(index)));
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
        activeBtn.setTextColor(Color.BLACK);
    }

    // Verifica que el símbolo exista en coins.json (mismo código anterior)
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

    // Diálogo para añadir alerta (como lo tenías)
    // 📌 Diálogo para añadir alerta desde la pantalla del chart
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Price Alert");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Usar el mismo símbolo mostrado en el chart
        if (coinSymbol != null) {
            etSymbol.setText(coinSymbol.toUpperCase(Locale.ROOT));
            etSymbol.setEnabled(false);
        }

        btnSave.setOnClickListener(v -> {
            String symbolInput = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (target.isEmpty()) {
                Toast.makeText(CoinChartActivity.this, "Enter target price", Toast.LENGTH_SHORT).show();
                return;
            }

            double targetPrice;
            try {
                targetPrice = Double.parseDouble(target);
            } catch (NumberFormatException e) {
                Toast.makeText(CoinChartActivity.this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            if (symbolInput.isEmpty()) {
                Toast.makeText(CoinChartActivity.this, "Symbol is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String symbolUpper = symbolInput.toUpperCase(Locale.ROOT);

            // ⛔ Para que no pulse más de una vez
            btnSave.setEnabled(false);

            // 1️⃣ Primero intentamos verificar la moneda con MarketRepository (mismos datos de Home)
            marketRepository.getCoins(false, coins -> {
                boolean existsInOnline = false;

                for (Coin c : coins) {
                    if (c.getSymbol().equalsIgnoreCase(symbolUpper)
                            || c.getId().equalsIgnoreCase(symbolUpper)) {
                        existsInOnline = true;
                        break;
                    }
                }

                // 2️⃣ Si no está en los datos de Home, hacemos fallback a coins.json (mock)
                boolean existsFinal = existsInOnline || isValidCoinSymbol(symbolUpper);

                runOnUiThread(() -> {
                    if (!existsFinal) {
                        Toast.makeText(CoinChartActivity.this,
                                "Coin not found (online & offline)", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        return;
                    }

                    // La moneda existe → guardamos la alerta en Room
                    PriceAlert alert = new PriceAlert();
                    alert.coinSymbol = symbolUpper;
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
            });
        });

        dialog.show();
    }

}
