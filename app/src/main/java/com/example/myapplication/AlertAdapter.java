package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<PriceAlert> alertList;
    private Context context;

    public AlertAdapter(List<PriceAlert> alertList, Context context) {
        this.alertList = alertList;
        this.context = context;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        PriceAlert alert = alertList.get(position);

        holder.tvSymbol.setText(alert.coinSymbol);
        holder.tvTargetPrice.setText("$" + alert.targetPrice);

        // 🔁 زر تعديل
        holder.btnEdit.setOnClickListener(v -> showEditDialog(alert));

        // ❌ زر حذف
        holder.btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                AppDatabase.getDatabase(context).priceAlertDao().deleteAlert(alert);
                ((AlertActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Alert deleted", Toast.LENGTH_SHORT).show();
                    refreshList();
                });
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    // 📌 تحديث القائمة بعد حذف أو تعديل
    private void refreshList() {
        new Thread(() -> {
            alertList = AppDatabase.getDatabase(context).priceAlertDao().getAllAlerts();
            ((AlertActivity) context).runOnUiThread(this::notifyDataSetChanged);
        }).start();
    }

    private void showEditDialog(PriceAlert alert) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Alert");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etSymbol.setText(alert.coinSymbol);
        etTarget.setText(String.valueOf(alert.targetPrice));

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newSymbol = etSymbol.getText().toString().trim().toUpperCase();
            double newPrice = Double.parseDouble(etTarget.getText().toString());

            alert.coinSymbol = newSymbol;
            alert.targetPrice = newPrice;

            new Thread(() -> {
                AppDatabase.getDatabase(context).priceAlertDao().updateAlert(alert);
                ((AlertActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Alert updated!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    refreshList();
                });
            }).start();
        });

        dialog.show();
    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvTargetPrice;
        Button btnEdit, btnDelete;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvTargetPrice = itemView.findViewById(R.id.tvTargetPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
