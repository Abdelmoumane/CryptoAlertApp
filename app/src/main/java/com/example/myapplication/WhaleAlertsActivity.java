package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WhaleAlertsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whale_alerts);

        recyclerView = findViewById(R.id.recyclerWhale);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 📌 تحميل البيانات من ملف محلي فقط
        loadFromLocalJson();

        setupBottomNavigation();
    }

    // ------------------------------------------------
    // 📁 تحميل البيانات من ملف داخل التطبيق (assets/whales.json)
    // ------------------------------------------------
    private void loadFromLocalJson() {
        try {
            InputStream is = getAssets().open("whales.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            Gson gson = new Gson();

            WhaleResponse localResponse = gson.fromJson(json, WhaleResponse.class);
            List<WhaleTransaction> list = localResponse.transactions;

            recyclerView.setAdapter(new WhaleAdapter(list));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "⚠ Error reading local JSON!", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------
    // 🔽 Bottom Menu
    // ------------------------------------------------
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertActivity.class));
                return true;

            } else if (id == R.id.nav_notify) {
                return true;

            } else if (id == R.id.nav_whale_alerts) {
                return true; // نحن في هذه الشاشة الآن
            }
            return false;
        });
    }
}
