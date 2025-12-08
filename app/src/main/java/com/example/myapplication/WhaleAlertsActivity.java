package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WhaleAlertsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //  Tema antes de mostrar la pantalla
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whale_alerts);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_whale_alerts);

        Toolbar toolbar = findViewById(R.id.toolbar_whales);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerWhale);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //  Cargar los datos solo desde un archivo local
        loadFromLocalJson();

        setupBottomNavigation();
    }

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
                return true; // Estás en esta pantalla ahora
            }
            return false;
        });
    }

    //  Para evitar la reconstrucción al cambiar el tema (evita el parpadeo)
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
