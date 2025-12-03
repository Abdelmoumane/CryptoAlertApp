package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MarketRepository {

    private static final String TAG = "MarketRepository";
    private static final String BASE_URL = "https://api.coingecko.com/api/v3/";

    private final Context appContext;
    private final CoinGeckoApi api;

    public interface CoinsCallback {
        void onResult(List<Coin> coins);
    }

    public MarketRepository(Context context) {
        this.appContext = context.getApplicationContext();

        OkHttpClient client = new OkHttpClient.Builder()
                // ⏱ زودنا التايم أوت شوية عشان ما يرجعش للـ mock بسرعة
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(CoinGeckoApi.class);
    }

    // 🔹 تحاول CoinGecko → لو فشل ترجع للـ JSON المحلي
    // showToast = true → نعرض رسائل live / offline
    public void getCoins(boolean showToast, CoinsCallback callback) {

        Call<List<CoinDto>> call = api.getMarketCoins(
                "usd",
                "market_cap_desc",
                50,
                1,
                false,
                "24h"
        );

        call.enqueue(new Callback<List<CoinDto>>() {
            @Override
            public void onResponse(Call<List<CoinDto>> call, Response<List<CoinDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "API not successful, using local JSON. code=" + response.code());
                    loadFromLocal(showToast, callback);
                    return;
                }

                if (showToast) {
                    Toast.makeText(appContext,
                            "Using live data (CoinGecko API)",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                List<CoinDto> dtoList = response.body();
                List<Coin> result = new ArrayList<>();

                for (CoinDto dto : dtoList) {
                    Coin c = new Coin(
                            dto.id,
                            dto.symbol.toUpperCase(),
                            dto.name,
                            dto.current_price,
                            dto.price_change_percentage_24h
                    );
                    result.add(c);
                }

                callback.onResult(result);
            }

            @Override
            public void onFailure(Call<List<CoinDto>> call, Throwable t) {
                Log.e(TAG, "API error: " + t.getMessage());
                loadFromLocal(showToast, callback);
            }
        });
    }

    // 🔻 لو مفيش نت / أو API وقعت → نقرأ coins.json
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
