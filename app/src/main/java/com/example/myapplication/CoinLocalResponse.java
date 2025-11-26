package com.example.myapplication;

import java.util.List;

public class CoinLocalResponse {

    public List<Coin> coins;  // 👈 خليه public

    // ✔ (اختياري) لو أردت Getter
    public List<Coin> getCoins() {
        return coins;
    }
}
