package com.example.myapplication;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CoinPaprikaApi {

    @GET("v1/tickers")
    Call<List<PaprikaTickerDto>> getTickers(
            @Query("quotes") String quotes  // "USD"
    );

    @GET("v1/coins/{coin_id}/ohlcv/today")
    Call<List<CoinOhlcvResponse>> getOhlcvToday(
            @Path("coin_id") String coinId
    );
}
