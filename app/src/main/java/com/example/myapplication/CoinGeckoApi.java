package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface CoinGeckoApi {

    // 📌 جلب قائمة العملات الأساسية
    @GET("coins/markets")
    Call<List<CoinGeckoCoin>> getCoins(
            @Query("vs_currency") String currency,
            @Query("order") String order,
            @Query("per_page") int perPage,
            @Query("page") int page,
            @Query("sparkline") boolean sparkline
    );

    // 📌 API للرسم البياني الخطي (Market Chart)
    @GET("coins/{id}/market_chart")
    Call<MarketChartResponse> getMarketChart(
            @Path("id") String coinId,
            @Query("vs_currency") String vsCurrency,
            @Query("days") int days
    );

    // ⚡ API للرسم الشمعة (Candlestick) الحقيقي مثل TradingView
    @GET("coins/{id}/ohlc")
    Call<List<List<Double>>> getOHLC(
            @Path("id") String coinId,
            @Query("vs_currency") String vsCurrency,
            @Query("days") int days   // 1 , 7 , 30 , 90 , 180 , 365
    );
}
