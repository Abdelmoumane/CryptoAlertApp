package com.example.myapplication;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BinanceApi {

    // GET https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=24
    @GET("api/v3/klines")
    Call<List<List<Object>>> getKlines(
            @Query("symbol") String symbol,
            @Query("interval") String interval,
            @Query("limit") int limit
    );
}
