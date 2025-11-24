package com.example.myapplication;

public class WhaleTransaction {
    public String symbol;
    public String transaction_type;
    public String hash;
    public From from;
    public To to;
    public double amount;
    public double amount_usd;

    public static class From {
        public String owner;
    }

    public static class To {
        public String owner;
    }
}
