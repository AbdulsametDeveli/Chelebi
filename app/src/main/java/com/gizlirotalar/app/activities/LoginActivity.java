package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gizlirotalar.app.databinding.ActivityLoginBinding;
import com.gizlirotalar.app.utils.AdminConfig;
import com.gizlirotalar.app.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        // Oturum açık mı kontrol et
        if (firebaseHelper.getAuth().getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        if (binding.etEmail.getEditText() == null || binding.etPassword.getEditText() == null) {
            return;
        }
        String email = binding.etEmail.getEditText().getText().toString().trim();
        String password = binding.etPassword.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = firebaseHelper.getAuth().getCurrentUser().getUid();
                        if (AdminConfig.isAdminEmail(email)) {
                            firebaseHelper.getDb().collection("users").document(uid).update("role", "admin");
                        }
                        Toast.makeText(LoginActivity.this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
                        Toast.makeText(LoginActivity.this, "Giriş başarısız: " + errorMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
