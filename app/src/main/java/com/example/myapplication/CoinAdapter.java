package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoinAdapter extends RecyclerView.Adapter<CoinAdapter.CoinViewHolder> {

    private ArrayList<Coin> coins;
    private ArrayList<Coin> coinsFull;
    private Context context;

    public CoinAdapter(Context context, ArrayList<Coin> coins) {
        this.context = context;
        this.coins = coins;
        this.coinsFull = new ArrayList<>(coins);
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

        holder.tvSymbol.setText(coin.getSymbol().toUpperCase());

        // **Precio**
        holder.tvPrice.setText(String.format(Locale.US, "$%,.2f", coin.getPrice()));

        // **Cambio**
        double change = coin.getChangePercent24h();
        holder.tvChange.setText(String.format(Locale.US, "%+.2f%%", change));
        holder.tvChange.setTextColor(
                change >= 0 ? Color.parseColor("#19A100") : Color.parseColor("#E60000")
        );

        // **Al pulsar → abrir el gráfico**
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CoinChartActivity.class);
            intent.putExtra("coin_id", coin.getId());  // Enviamos el ID correcto
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return coins.size();
    }

    public void updateList(List<Coin> newCoins) {
        coins.clear();
        coins.addAll(newCoins);
        coinsFull.clear();
        coinsFull.addAll(newCoins);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        coins.clear();
        if (text.isEmpty()) {
            coins.addAll(coinsFull);
        } else {
            text = text.toLowerCase();
            for (Coin c : coinsFull) {
                if (c.getSymbol().toLowerCase().contains(text)) {   // Solo symbol
                    coins.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class CoinViewHolder extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvPrice, tvChange;

        public CoinViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);   // Solo Symbo
            tvPrice  = itemView.findViewById(R.id.tvPrice);
            tvChange = itemView.findViewById(R.id.tvChange);
        }
    }
}
