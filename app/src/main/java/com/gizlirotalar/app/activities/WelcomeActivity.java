package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.gizlirotalar.app.databinding.ActivityWelcomeBinding;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {
    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Uygulama açılırken internetten token doğrulaması yapıldığı için, 
        // o süre zarfında siyah/boş ekran görünmemesi adına geçici splash ekranı gösteriyoruz.
        setContentView(com.gizlirotalar.app.R.layout.activity_splash);

        FirebaseUser user = FirebaseHelper.getInstance().getAuth().getCurrentUser();
        if (user != null) {
            // Oturum cihazda kayıtlı — interneti beklemeden direkt Dashboard'a geç!
            startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
            finish();
        } else {
            // Oturum yok — giriş/kayıt menüsünü göster
            showWelcomeScreen();
        }
    }

    private void showWelcomeScreen() {
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnStart.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });

        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });
    }
}

