package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;
import com.example.myapplication.MarketChartResponse;


public interface CoinGeckoApi {
    // API endpoint لجلب قائمة العملات
    @GET("coins/markets")
    Call<List<CoinGeckoCoin>> getCoins(
            @Query("vs_currency") String currency,  // مثل "usd"
            @Query("order") String order,
            @Query("per_page") int perPage,
            @Query("page") int page,
            @Query("sparkline") boolean sparkline
    );

    @GET("coins/{id}/market_chart")
    Call<MarketChartResponse> getMarketChart(
            @Path("id") String coinId,
            @Query("vs_currency") String vsCurrency,
            @Query("days") int days
    );

}
