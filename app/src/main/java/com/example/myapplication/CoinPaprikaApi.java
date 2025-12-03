package com.example.myapplication;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CoinPaprikaApi {

    // قائمة العملات (تستخدمها في MarketRepository)
    @GET("v1/tickers")
    Call<List<PaprikaTickerDto>> getTickers(
            @Query("quotes") String quotes // "usd"
    );

    // بيانات OHLCV للتشارت
    @GET("v1/coins/{coin_id}/ohlcv/historical")
    Call<List<CoinOhlcvResponse>> getHistoricalOhlcv(
            @Path("coin_id") String coinId,  // btc-bitcoin مثلاً
            @Query("start") String startDate,
            @Query("end") String endDate
    );
}
