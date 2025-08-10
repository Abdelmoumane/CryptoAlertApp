package com.example.myapplication;

import java.util.List;

public class MarketChartResponse {
    private List<List<Double>> prices;

    public List<List<Double>> getPrices() {
        return prices;
    }

    public void setPrices(List<List<Double>> prices) {
        this.prices = prices;
    }
}
