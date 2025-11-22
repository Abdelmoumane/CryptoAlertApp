package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class AlertActivity extends AppCompatActivity {

    private RecyclerView rvAlerts;
    private AlertAdapter alertAdapter; // لازم يكون فيه Adapter 👍

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);   // هذا ملف XML الخاص بعرض التنبيهات

        // ✔ إعداد Toolbar مع زر رجوع
        Toolbar toolbar = findViewById(R.id.toolbar_alerts);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // يظهر زر رجوع

        // 🎯 جعل زر < لونه أبيض
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon()
                    .setTint(ContextCompat.getColor(this, android.R.color.white));
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // يرجع للصفحة السابقة

        rvAlerts = findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));  // RecyclerView


        // تحميل البيانات من Room DB
        loadAlertsFromDB();
        // ⬇️ أضف هذا الجزء هنا داخل onCreate
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_alerts);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                finish();  // 🔙 يرجع لصفحة MainActivity
                return true;
            }
            else if (id == R.id.nav_alerts) {
                return true; // انت بالفعل هنا
            }
            else if (id == R.id.nav_notify) {
                showAlertDialog();  // إضافة تنبيه
                return true;
            }
            return false;
        });
    }



    // 🔁 تحميل التنبيهات من قاعدة البيانات
    private void loadAlertsFromDB() {
        new Thread(() -> {
            List<PriceAlert> alerts = AppDatabase.getDatabase(this).priceAlertDao().getAllAlerts();

            runOnUiThread(() -> {
                alertAdapter = new AlertAdapter(alerts, AlertActivity.this); // تمرير context
                rvAlerts.setAdapter(alertAdapter);
            });
        }).start();
    }

    // ➕ نافذة إضافة تنبيه
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Price Alert");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_alert, null);
        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString().trim();
            String target = etTarget.getText().toString().trim();

            if (symbol.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Please enter all data", Toast.LENGTH_SHORT).show();
                return;
            }

            double targetPrice;
            try {
                targetPrice = Double.parseDouble(target);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            PriceAlert alert = new PriceAlert();
            alert.coinSymbol = symbol.toUpperCase();
            alert.targetPrice = targetPrice;

            new Thread(() -> {
                AppDatabase.getDatabase(this).priceAlertDao().insert(alert);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Alert saved successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadAlertsFromDB(); // 🔁 تحديث القائمة
                });
            }).start();
        });

        dialog.show();
    }
}
