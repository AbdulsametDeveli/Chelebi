package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gizlirotalar.app.databinding.ActivityDashboardBinding;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {
    private ActivityDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseHelper.getInstance().getAuth().getCurrentUser();
        if (user == null) {
            // Oturum yok — giriş ekranına yönlendir
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            binding.tvWelcomeUser.setText("Merhaba, " + displayName + "!");
        }

        // Tüm haritayı aç
        binding.cardMap.setOnClickListener(v -> openMap("Tümü"));

        // 📍 Konumum — haritayı aç ve konum marker'ını göster
        binding.cardMyLocation.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("FILTER_CATEGORY", "Tümü");
            intent.putExtra("SHOW_MY_LOCATION", true);
            startActivity(intent);
        });

        // 🏷️ Spotlarım — eklediğim spotların yorumları ve beğenileri
        binding.cardMySpots.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, MySpotsActivity.class)));

        // ❤️ Favorilerim — yol tarifi ile favori spotlar
        binding.cardFavorites.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, FavoritesActivity.class)));

        // 👤 Profilim
        binding.cardProfile.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));
    }

    private void openMap(String filterCategory) {
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.putExtra("FILTER_CATEGORY", filterCategory);
        startActivity(intent);
    }
}
