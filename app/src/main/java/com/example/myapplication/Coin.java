package com.example.myapplication;

public class Coin {
    private String id;
    private String symbol;
    private double price;
    private double change_24h;  // هذا مهم لـ GAINERS & LOSERS

    public Coin(String id, String symbol, double price, double change_24h) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.change_24h = change_24h;
    }

    public String getId() { return id; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getChangePercent24h() { return change_24h; }
}
