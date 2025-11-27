package com.example.myapplication;

import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;


public class WhaleAdapter extends RecyclerView.Adapter<WhaleAdapter.WhaleViewHolder> {

    private List<WhaleTransaction> list;

    public WhaleAdapter(List<WhaleTransaction> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public WhaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_whale, parent, false);  // 👈 xml file
        return new WhaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WhaleViewHolder holder, int position) {
        WhaleTransaction item = list.get(position);

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        String formattedAmount = formatter.format(item.amount_usd);

        // 🪙 اسم العملة - أبيض Bold
        holder.tvSymbol.setText(item.symbol.toUpperCase());
        holder.tvSymbol.setTextColor(Color.WHITE);

        // 🔢 لون المبلغ → إذا أكثر من 100 مليون = أحمر 👇
        String amountText = formattedAmount + " #" + item.symbol.toUpperCase()
                + " (" + formattedAmount + " USD)";

        if (item.amount_usd > 100_000_000) {
            holder.tvAmount.setTextColor(Color.parseColor("#FF4D4D")); // أحمر
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#00FF7F")); // أخضر
        }
        holder.tvAmount.setText(amountText);

        // 🔁 السطر الثاني → من وإلى مع ألوان تويتر 💙
        String fromToText = "from <font color='#9E9E9E'>" + item.from.owner + "</font> " +
                "to <font color='#1DA1F2'>" + item.to.owner + "</font>"; // Twitter Blue

        holder.tvFromTo.setText(Html.fromHtml(fromToText));
        holder.itemView.setOnClickListener(v -> {
            // ممكن تفتح صفحة Details هنا
            // OR Toast للبداية 👇
            Toast.makeText(v.getContext(),
                    item.symbol.toUpperCase() + " clicked",
                    Toast.LENGTH_SHORT).show();
        });

    }




    @Override
    public int getItemCount() {
        return list.size();
    }

    static class WhaleViewHolder extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvAmount, tvFromTo;

        public WhaleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvFromTo = itemView.findViewById(R.id.tvFromTo);
        }
    }
}
