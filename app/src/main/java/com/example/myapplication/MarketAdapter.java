package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.CoinViewHolder> {

    public interface OnCoinClickListener {
        void onCoinClick(Coin coin);
    }

    private List<Coin> coins;
    private OnCoinClickListener listener;

    public MarketAdapter(List<Coin> coins, OnCoinClickListener listener) {
        this.coins = coins;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CoinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coin, parent, false);
        return new CoinViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CoinViewHolder holder, int position) {
        Coin coin = coins.get(position);
        holder.bind(coin, listener);
    }

    @Override
    public int getItemCount() {
        return coins.size();
    }

    public void updateData(List<Coin> newCoins) {
        this.coins = newCoins;
        notifyDataSetChanged();
    }

    static class CoinViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSymbol, tvPrice, tvChange;

        public CoinViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCoinName);
            tvSymbol = itemView.findViewById(R.id.tvCoinSymbol);
            tvPrice = itemView.findViewById(R.id.tvCoinPrice);
            tvChange = itemView.findViewById(R.id.tvCoinChange);
        }

        void bind(final Coin coin, final OnCoinClickListener listener) {
            tvName.setText(coin.getName());
            tvSymbol.setText(coin.getSymbol().toUpperCase());
            tvPrice.setText(String.format("$%.2f", coin.getPrice()));

            double change = coin.getChangePercent24h();
            tvChange.setText(String.format("%.2f%%", change));
            tvChange.setTextColor(change >= 0 ? Color.GREEN : Color.RED);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCoinClick(coin);
                }
            });
        }
    }
}
