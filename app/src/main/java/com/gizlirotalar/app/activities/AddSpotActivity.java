package com.gizlirotalar.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.gizlirotalar.app.databinding.ActivityAddSpotBinding;
import com.gizlirotalar.app.models.Spot;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.gizlirotalar.app.utils.ImgBBUploader;

import java.io.File;
import java.io.IOException;

public class AddSpotActivity extends AppCompatActivity {
    private ActivityAddSpotBinding binding;
    private FirebaseHelper firebaseHelper;
    private double lat = 40.7731;
    private double lng = 30.3948;

    private Uri cameraImageUri;       // kamera çekiminin geçici URI'si
    private Bitmap selectedBitmap;    // seçilen/çekilen bitmap
    private String uploadedImageUrl = ""; // imgBB'ye yüklendikten sonra URL

    // Galeri seçici launcher
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        showImagePreview(selectedBitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Kamera launcher
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), cameraImageUri);
                        showImagePreview(selectedBitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Kamera izni launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openCamera();
                else Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddSpotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        lat = getIntent().getDoubleExtra("LAT", lat);
        lng = getIntent().getDoubleExtra("LNG", lng);

        // Kategori spinner
        String[] categories = new String[]{
                "Kamp Alanı", "Restoran", "Otel / Konaklama", "Şelale",
                "Manzara Noktası", "Tehlikeli Bölge", "Bisiklet Rotası", "Trekking Rotası"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        binding.spinnerCategory.setAdapter(adapter);

        binding.btnAddPhoto.setOnClickListener(v -> showPhotoSourceDialog());

        // Fotoğraf önizlemesini gizle/göster için tıklama
        binding.ivPhotoPreview.setOnClickListener(v -> showPhotoSourceDialog());

        // Türkçe karakter desteği: inputType'ı kod tarafında zorla
        android.widget.EditText etName = binding.etName.getEditText();
        android.widget.EditText etDesc = binding.etDescription.getEditText();
        if (etName != null) {
            etName.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }
        if (etDesc != null) {
            etDesc.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            etDesc.setHorizontallyScrolling(false);
            etDesc.setMaxLines(10);
        }

        binding.btnSave.setOnClickListener(v -> saveSpot());
    }

    /** Kamera mı galeri mi diye sorar */
    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Fotoğraf Seç")
                .setItems(new String[]{"📷 Kamera ile Çek", "🖼️ Galeriden Seç"}, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("chelebi_", ".jpg", getExternalCacheDir());
            cameraImageUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Kamera açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePreview(Bitmap bitmap) {
        binding.ivPhotoPreview.setVisibility(View.VISIBLE);
        binding.btnAddPhoto.setText("Fotoğrafı Değiştir");
        Glide.with(this).load(bitmap).into(binding.ivPhotoPreview);
    }

    private void saveSpot() {
        if (binding.etName.getEditText() == null || binding.etDescription.getEditText() == null) {
            return;
        }
        String name = binding.etName.getEditText().getText().toString().trim();
        String desc = binding.etDescription.getEditText().getText().toString().trim();
        String category = binding.spinnerCategory.getSelectedItem().toString();
        int difficulty = binding.seekDifficulty.getProgress() + 1;
        // Tehlikeli bölge kategorisi seçildiyse otomatik olarak tehlikeli say
        boolean isDangerous = "Tehlikeli Bölge".equals(category);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(desc)) {
            Toast.makeText(this, "Lütfen ad ve açıklama girin", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Kaydediliyor...");

        if (selectedBitmap != null && uploadedImageUrl.isEmpty()) {
            // Fotoğraf seçildi ama henüz yüklenmedi → önce imgBB'ye yükle
            Toast.makeText(this, "Fotoğraf yükleniyor...", Toast.LENGTH_SHORT).show();
            ImgBBUploader.upload(selectedBitmap, new ImgBBUploader.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    uploadedImageUrl = imageUrl;
                    saveToFirestore(name, desc, category, difficulty, isDangerous, imageUrl);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(AddSpotActivity.this,
                            "Fotoğraf yüklenemedi, spot fotoğrafsız kaydediliyor.", Toast.LENGTH_SHORT).show();
                    saveToFirestore(name, desc, category, difficulty, isDangerous, "");
                }
            });
        } else {
            saveToFirestore(name, desc, category, difficulty, isDangerous, uploadedImageUrl);
        }
    }

    private void saveToFirestore(String name, String desc, String category,
                                  int difficulty, boolean isDangerous, String imageUrl) {
        if (firebaseHelper.getAuth().getCurrentUser() == null) {
            Toast.makeText(this, "Oturum süresi dolmuş, lütfen tekrar giriş yapın", Toast.LENGTH_SHORT).show();
            binding.btnSave.setEnabled(true);
            binding.btnSave.setText("Kaydet");
            return;
        }
        String uid = firebaseHelper.getAuth().getCurrentUser().getUid();
        String email = firebaseHelper.getAuth().getCurrentUser().getEmail();

        Spot spot = new Spot(name, desc, category, lat, lng, difficulty, isDangerous,
                imageUrl, uid, email, System.currentTimeMillis());

        firebaseHelper.getDb().collection("spots").add(spot)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(AddSpotActivity.this, "Nokta başarıyla eklendi! 🎉", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText("Kaydet");
                    Toast.makeText(AddSpotActivity.this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
