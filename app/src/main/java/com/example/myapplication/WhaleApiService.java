package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.GET;

public interface WhaleApiService {

    @GET("whales.json")   // ✔ هذا الصحيح
    Call<WhaleResponse> getTransactions();
}
