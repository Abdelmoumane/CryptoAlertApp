package com.example.myapplication;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CoinLocalResponse {

    @SerializedName("coins")
    public List<Coin> coins;
}
