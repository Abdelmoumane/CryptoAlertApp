package com.example.myapplication;

public class CoinGeckoCoin {
    private String id;
    private String symbol;
    private String name;
    private String image;   // 👈 أضف هذا
    private double current_price;
    private double price_change_percentage_24h;

    public String getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getImage() { return image; }  // 👈 مهم جداً
    public double getCurrentPrice() { return current_price; }
    public double getPriceChangePercentage24h() { return price_change_percentage_24h; }
}
