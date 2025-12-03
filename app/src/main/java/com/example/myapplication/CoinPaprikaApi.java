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


}
