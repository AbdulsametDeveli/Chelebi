package com.gizlirotalar.app.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gizlirotalar.app.databinding.ActivityEditSpotBinding;
import com.gizlirotalar.app.models.Spot;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.gizlirotalar.app.utils.InputValidator;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class EditSpotActivity extends AppCompatActivity {
    private ActivityEditSpotBinding binding;
    private FirebaseHelper firebaseHelper;
    private String spotId;
    private Spot currentSpot;

    private static final String[] CATEGORIES = {
            "Kamp Alanı", "Restoran", "Otel / Konaklama", "Şelale", "Manzara Noktası",
            "Tehlikeli Bölge", "Bisiklet Rotası", "Trekking Rotası"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditSpotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        spotId = getIntent().getStringExtra("SPOT_ID");
        if (spotId == null) {
            Toast.makeText(this, "Spot ID bulunamadı!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Kategori Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEditCategory.setAdapter(catAdapter);

        // Zorluk SeekBar
        binding.seekEditDifficulty.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvEditDifficultyValue.setText((progress + 1) + "/5");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Spot verilerini yükle
        loadSpotData();

        // Kaydet butonu
        binding.btnEditSave.setOnClickListener(v -> validateAndSave());

        // İptal butonu
        binding.btnEditCancel.setOnClickListener(v -> finish());
    }

    private void loadSpotData() {
        firebaseHelper.getDb().collection("spots").document(spotId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentSpot = doc.toObject(Spot.class);
                        if (currentSpot != null) {
                            currentSpot.setId(spotId);
                            fillForm();
                        }
                    } else {
                        Toast.makeText(this, "Spot bulunamadı!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fillForm() {
        binding.etEditName.setText(currentSpot.getName());
        binding.etEditDescription.setText(currentSpot.getDescription());

        // Kategori spinner'ını ayarla
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(currentSpot.getCategory())) {
                binding.spinnerEditCategory.setSelection(i);
                break;
            }
        }

        // Zorluk
        binding.seekEditDifficulty.setProgress(currentSpot.getDifficulty() - 1);
        binding.tvEditDifficultyValue.setText(currentSpot.getDifficulty() + "/5");

        // Tehlike
        binding.switchEditDangerous.setChecked(currentSpot.isDangerous());
    }

    private void validateAndSave() {
        String name = binding.etEditName.getText().toString().trim();
        String description = binding.etEditDescription.getText().toString().trim();

        if (!InputValidator.validateSpotName(name)) {
            binding.tilName.setError("İsim 3-100 karakter arasında olmalıdır");
            return;
        }
        binding.tilName.setError(null);

        if (!InputValidator.validateDescription(description)) {
            binding.tilDescription.setError("Açıklama 10-1000 karakter arasında olmalıdır");
            return;
        }
        binding.tilDescription.setError(null);

        String category = CATEGORIES[binding.spinnerEditCategory.getSelectedItemPosition()];
        int difficulty = binding.seekEditDifficulty.getProgress() + 1;
        boolean isDangerous = binding.switchEditDangerous.isChecked();

        binding.btnEditSave.setEnabled(false);
        binding.btnEditSave.setText("Kaydediliyor...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", InputValidator.sanitizeInput(name));
        updates.put("description", InputValidator.sanitizeInput(description));
        updates.put("category", category);
        updates.put("difficulty", difficulty);
        updates.put("isDangerous", isDangerous);
        updates.put("updatedAt", System.currentTimeMillis());

        firebaseHelper.getDb().collection("spots").document(spotId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Spot başarıyla güncellendi! ✅", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnEditSave.setEnabled(true);
                    binding.btnEditSave.setText("Değişiklikleri Kaydet");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
