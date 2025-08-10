package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class AlertActivity extends AppCompatActivity {
    EditText etSymbol, etTarget;
    Button btnSave;
    @Override
    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_alert);
        etSymbol = findViewById(R.id.etSymbol);
        etTarget = findViewById(R.id.etTarget);
        btnSave = findViewById(R.id.btnSave);

        // إذا جئنا من MainActivity، املأ الحقول
        Intent intent = getIntent();
        if(intent!=null){
            String name = intent.getStringExtra("name");
            String symbol = intent.getStringExtra("symbol");
            double price = intent.getDoubleExtra("price", 0);
            etSymbol.setText(symbol.toUpperCase());
            // ممكن نعرض السعر الحالي كمرجع
        }

        btnSave.setOnClickListener(v -> {
            String sSymbol = etSymbol.getText().toString().trim();
            String sTarget = etTarget.getText().toString().trim();
            if(sSymbol.isEmpty() || sTarget.isEmpty()){ Toast.makeText(this,"Enter data",Toast.LENGTH_SHORT).show(); return; }
            // حفظ التنبيه (SharedPreferences/Room) — سنستخدم Room لاحقاً
            saveAlert(sSymbol, Double.parseDouble(sTarget));
            finish();
        });
    }

    private void saveAlert(String symbol, double target){
        // مؤقت: SharedPreferences أو Room (أفضل) — هنا مجرد مثال بسيط
        SharedPreferences prefs = getSharedPreferences("alerts", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("alerts_set", new HashSet<>());
        set.add(symbol + ":" + target);
        prefs.edit().putStringSet("alerts_set", set).apply();
        Toast.makeText(this,"Alert saved",Toast.LENGTH_SHORT).show();
    }
}
