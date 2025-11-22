package com.example.myapplication;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;

    public static Retrofit getInstance() {

        // 🛡️ إعداد عميل احترافي - يمنع بطء / تعليق التطبيق
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // وقت الاتصال
                .readTimeout(30, TimeUnit.SECONDS)    // وقت قراءة البيانات
                .writeTimeout(30, TimeUnit.SECONDS)   // وقت الإرسال
                .retryOnConnectionFailure(true)       // إعادة المحاولة تلقائياً
                .build();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.coingecko.com/api/v3/") // ✔ صحيح
                    .client(okHttpClient)  // 👈 مهم جدًا
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
