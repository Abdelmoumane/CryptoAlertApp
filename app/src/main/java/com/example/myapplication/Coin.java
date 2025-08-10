package com.example.myapplication;

public class Coin {
    private String id;
    private String name;
    private String symbol;
    private double price;
    private double changePercent24h;

    public Coin(String id, String name, String symbol, double price, double changePercent24h) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.price = price;
        this.changePercent24h = changePercent24h;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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
