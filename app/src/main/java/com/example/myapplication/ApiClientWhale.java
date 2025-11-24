package com.example.myapplication;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientWhale {

    private static final String BASE_URL =
            "https://raw.githubusercontent.com/Abdelmoumane/cryptoalert-api/main/";  // ✔ هنا المشكلة كانت

    private static Retrofit retrofit;

    public static WhaleApiService getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)   // ✔ يجب أن ينتهي بـ /
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(WhaleApiService.class);
    }
}
