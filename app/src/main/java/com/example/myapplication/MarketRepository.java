package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MarketRepository {

    private static final String TAG = "MarketRepository";
    private static final String BASE_URL = "https://api.coinpaprika.com/";

    private final Context appContext;
    private final CoinPaprikaApi api;

    public interface CoinsCallback {
        void onResult(List<Coin> coins);
    }

    public MarketRepository(Context context) {
        this.appContext = context.getApplicationContext();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(CoinPaprikaApi.class);
    }

    // Versión abreviada por si la necesitas en otros lugares
    public void getCoins(CoinsCallback callback) {
        getCoins(true, callback);
    }

    // Intenta primero CoinPaprika → si falla vuelve al JSON local
    public void getCoins(boolean showToast, CoinsCallback callback) {

        Call<List<PaprikaTickerDto>> call = api.getTickers("USD");

        call.enqueue(new Callback<List<PaprikaTickerDto>>() {
            @Override
            public void onResponse(Call<List<PaprikaTickerDto>> call,
                                   Response<List<PaprikaTickerDto>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "Paprika API not successful, using local JSON");
                    loadFromLocal(showToast, callback);
                    return;
                }

                List<PaprikaTickerDto> dtoList = response.body();
                List<Coin> result = new ArrayList<>();

                int count = 0;
                for (PaprikaTickerDto dto : dtoList) {
                    if (dto == null || dto.quotes == null || dto.quotes.usd == null) continue;

                    double price = dto.quotes.usd.price;
                    double change24h = dto.quotes.usd.percentChange24h;

                    Coin c = new Coin(
                            dto.id,   // Ejemplo: "btc-bitcoin" → lo usamos en el chart
                            dto.symbol != null ? dto.symbol.toUpperCase(Locale.ROOT) : "",
                            dto.name,
                            price,
                            change24h
                    );
                    result.add(c);

                    // Nos quedamos con las primeras 50 monedas
                    count++;
                    if (count >= 50) break;
                }

                if (result.isEmpty()) {
                    Log.w(TAG, "Paprika returned empty list, using local JSON");
                    loadFromLocal(showToast, callback);
                    return;
                }

                if (showToast) {
                    Toast.makeText(appContext,
                            "Using live data (CoinPaprika API)",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                callback.onResult(result);
            }

            @Override
            public void onFailure(Call<List<PaprikaTickerDto>> call, Throwable t) {
                Log.e(TAG, "Paprika API error: " + t.getMessage());
                loadFromLocal(showToast, callback);
            }
        });
    }

    // Offline desde coins.json
    private void loadFromLocal(boolean showToast, CoinsCallback callback) {
        try {
            if (showToast) {
                Toast.makeText(appContext,
                        "Using offline mock data (coins.json)",
                        Toast.LENGTH_SHORT
                ).show();
            }

            InputStream is = appContext.getAssets().open("coins.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            CoinLocalResponse response = new Gson().fromJson(json, CoinLocalResponse.class);

            if (response != null && response.coins != null) {
                callback.onResult(response.coins);
            } else {
                callback.onResult(new ArrayList<>());
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback.onResult(new ArrayList<>());
        }
    }
}
