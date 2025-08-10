package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CoinChartActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_chart);

        lineChart = findViewById(R.id.lineChart);

        String coinId = getIntent().getStringExtra("coin_id");
        Toast.makeText(this, "Coin ID: " + coinId, Toast.LENGTH_LONG).show();
        fetchHistoricalData(coinId);


        if (coinId != null) {
            fetchHistoricalData(coinId);
        } else {
            Toast.makeText(this, "No coin ID passed", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchHistoricalData(String coinId) {
        CoinGeckoApi api = RetrofitClient.getInstance().create(CoinGeckoApi.class);
        api.getMarketChart(coinId, "usd", 30).enqueue(new Callback<MarketChartResponse>() {
            @Override
            public void onResponse(Call<MarketChartResponse> call, Response<MarketChartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<List<Double>> prices = response.body().getPrices();
                    List<Entry> entries = new ArrayList<>();

                    for (int i = 0; i < prices.size(); i++) {
                        float x = i; // يمكن استبداله بالوقت إذا أردت
                        float y = prices.get(i).get(1).floatValue();
                        entries.add(new Entry(x, y));
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "Price in USD");
                    dataSet.setColor(getResources().getColor(R.color.purple_500));
                    dataSet.setValueTextColor(getResources().getColor(R.color.black));
                    LineData lineData = new LineData(dataSet);

                    lineChart.setData(lineData);
                    lineChart.invalidate(); // تحديث الرسم البياني
                } else {
                    Toast.makeText(CoinChartActivity.this, "Failed to load chart data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MarketChartResponse> call, Throwable t) {
                Toast.makeText(CoinChartActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
