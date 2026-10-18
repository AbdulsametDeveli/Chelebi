package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gizlirotalar.app.databinding.ActivityRegisterBinding;
import com.gizlirotalar.app.models.User;
import com.gizlirotalar.app.utils.AdminConfig;
import com.gizlirotalar.app.utils.FirebaseHelper;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        binding.btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        if (binding.etName.getEditText() == null || binding.etEmail.getEditText() == null
                || binding.etPassword.getEditText() == null) {
            return;
        }
        String name = binding.etName.getEditText().getText().toString().trim();
        String email = binding.etEmail.getEditText().getText().toString().trim();
        String password = binding.etPassword.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Şifre en az 6 karakter olmalıdır", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(name, email);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
                        Toast.makeText(RegisterActivity.this, "Kayıt başarısız: " + errorMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String name, String email) {
        String uid = firebaseHelper.getAuth().getCurrentUser().getUid();
        String role = "user";

        if (AdminConfig.isAdminEmail(email)) {
            role = "admin";
        }

        User user = new User(email, name, role, System.currentTimeMillis());

        firebaseHelper.getDb().collection("users").document(uid).set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
                        Toast.makeText(RegisterActivity.this, "Veritabanı hatası: " + errorMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
