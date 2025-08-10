package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HotAdapter extends RecyclerView.Adapter<HotAdapter.HotVH>{
    private List<Coin> list;
    private OnItemClick listener;
    public interface OnItemClick { void onClick(Coin coin); }

    public HotAdapter(List<Coin> list, OnItemClick listener){
        this.list = list; this.listener = listener;
    }

    @NonNull @Override
    public HotVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hot_coin,parent,false);
        return new HotVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HotVH holder, int position){
        Coin c = list.get(position);
        holder.symbol.setText(c.getSymbol().toUpperCase());
        holder.price.setText("$" + String.format("%.2f", c.getPrice()));
        double ch = c.getChangePercent24h();
        holder.change.setText(String.format("%.2f", ch) + "%");
        holder.change.setTextColor(ch >= 0
                ? holder.itemView.getResources().getColor(R.color.gain)
                : holder.itemView.getResources().getColor(R.color.loss));
        holder.itemView.setOnClickListener(v -> listener.onClick(c));
    }


    @Override public int getItemCount(){ return list.size(); }

    static class HotVH extends RecyclerView.ViewHolder {
        TextView symbol, price, change;

        public HotVH(@NonNull View v){
            super(v);
            symbol = v.findViewById(R.id.tvHotSymbol);
            price = v.findViewById(R.id.tvHotPrice);
            change = v.findViewById(R.id.tvHotChange);
        }
    }
}
