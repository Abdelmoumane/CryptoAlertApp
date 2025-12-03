package com.example.myapplication;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CoinGeckoApi {

    // https://api.coingecko.com/api/v3/coins/markets
    @GET("coins/markets")
    Call<List<CoinDto>> getMarketCoins(
            @Query("vs_currency") String vsCurrency,
            @Query("order") String order,
            @Query("per_page") int perPage,
            @Query("page") int page,
            @Query("sparkline") boolean sparkline,
            @Query("price_change_percentage") String priceChangePercentage
    );

    @GET("coins/{id}/ohlc")
    Call<List<List<Double>>> getOhlc(
            @Path("id") String id,
            @Query("vs_currency") String vsCurrency,
            @Query("days") int days
    );

}
