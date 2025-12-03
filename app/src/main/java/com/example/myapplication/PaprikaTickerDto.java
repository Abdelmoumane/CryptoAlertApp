package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class PaprikaTickerDto {

    public String id;     // "btc-bitcoin"
    public String name;   // "Bitcoin"
    public String symbol; // "BTC"

    public Quotes quotes;

    public static class Quotes {
        @SerializedName("USD")
        public UsdQuote usd;
    }

    public static class UsdQuote {
        public double price;

        @SerializedName("percent_change_24h")
        public double percentChange24h;
    }
}
