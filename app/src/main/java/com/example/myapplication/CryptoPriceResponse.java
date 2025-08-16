package com.example.myapplication;

import java.util.Map;

public class CryptoPriceResponse {
    private Map<String, Map<String, Double>> prices;

    public double getPrice() {
        if (prices != null && prices.containsKey("bitcoin")) {
            Map<String, Double> coin = prices.get("bitcoin");
            if (coin != null && coin.containsKey("usd")) {
                return coin.get("usd");
            }
        }
        return -1;
    }
}
