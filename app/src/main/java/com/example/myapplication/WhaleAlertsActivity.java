package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WhaleAlertsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whale_alerts);

        // 🔹 1) إعداد RecyclerView
        recyclerView = findViewById(R.id.recyclerWhale);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 2) استدعاء الـ API
        loadWhaleData();

        // 🔹 3) Bottom Navigation (يجب أن يكون هنا!!)
        setupBottomNavigation();
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
                // ممكن تضيف Dialog هنا
                return true;

            } else if (id == R.id.nav_whale_alerts) {
                return true;  // نحن في هذه الشاشة 👍
            }
            return false;
        });
    }

    private void loadWhaleData() {

        ApiClientWhale.getClient().getTransactions()
                .enqueue(new Callback<WhaleResponse>() {
                    @Override
                    public void onResponse(Call<WhaleResponse> call, Response<WhaleResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<WhaleTransaction> list = response.body().transactions;
                            recyclerView.setAdapter(new WhaleAdapter(list));
                        } else {
                            Toast.makeText(WhaleAlertsActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<WhaleResponse> call, Throwable t) {
                        Toast.makeText(WhaleAlertsActivity.this,
                                "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


