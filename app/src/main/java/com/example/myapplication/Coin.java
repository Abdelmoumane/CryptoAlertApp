package com.example.myapplication;

public class Coin {
    private String id;
    private String symbol;
    private double price;
    private double changePercent24h;

    public Coin(String id,  String symbol, double price, double changePercent24h) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.changePercent24h = changePercent24h;
    }

    public String getId() {
        return id;
    }



    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public double getChangePercent24h() {
        return changePercent24h;
    }
}