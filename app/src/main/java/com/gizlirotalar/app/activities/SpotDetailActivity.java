package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.gizlirotalar.app.adapters.RatingAdapter;
import com.gizlirotalar.app.databinding.ActivitySpotDetailBinding;
import com.gizlirotalar.app.models.Favorite;
import com.gizlirotalar.app.models.Rating;
import com.gizlirotalar.app.models.Report;
import com.gizlirotalar.app.models.Spot;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SpotDetailActivity extends AppCompatActivity {
    private ActivitySpotDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private String spotId;
    private Spot currentSpot;
    private boolean isFavorite = false;
    private String currentUserId;
    private String currentUserName;

    private List<Rating> ratingList = new ArrayList<>();
    private RatingAdapter ratingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpotDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();

        if (firebaseHelper.getAuth().getCurrentUser() != null) {
            currentUserId = firebaseHelper.getAuth().getCurrentUser().getUid();
            currentUserName = firebaseHelper.getAuth().getCurrentUser().getEmail();
        }

        spotId = getIntent().getStringExtra("SPOT_ID");
        if (spotId == null) {
            Toast.makeText(this, "Nokta ID'si bulunamadı!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Rating listesi
        ratingAdapter = new RatingAdapter(ratingList);
        binding.rvRatings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRatings.setAdapter(ratingAdapter);

        // Kullanıcı adını al
        loadUserName();

        // Verileri yükle
        loadSpotDetails();
        checkFavoriteStatus();
        loadRatings();

        // Favori butonu
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());

        // Değerlendirme gönder
        binding.btnSubmitRating.setOnClickListener(v -> submitRating());

        // Rapor gönder
        binding.btnReport.setOnClickListener(v -> showReportDialog());

        // 🗺️ Yol Tarifi Al
        binding.btnNavigate.setOnClickListener(v -> openNavigation());

        // Türkçe karakter desteği: yorum alanı
        android.widget.EditText etComment = binding.etComment.getEditText();
        if (etComment != null) {
            etComment.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            etComment.setHorizontallyScrolling(false);
            etComment.setMaxLines(5);
        }
    }

    /** Google Maps ile yol tarifi açar */
    private void openNavigation() {
        if (currentSpot == null) {
            Toast.makeText(this, "Konum bilgisi yükleniyor, lütfen bekleyin.", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = currentSpot.getLatitude();
        double lng = currentSpot.getLongitude();

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Google Maps yüklü değilse tarayıcıda aç
            Uri browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
            startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
        }
    }

    private void loadUserName() {
        if (currentUserId == null) return;
        firebaseHelper.getDb().collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        if (name != null && !name.isEmpty()) {
                            currentUserName = name;
                        }
                    }
                });
    }

    private void loadSpotDetails() {
        firebaseHelper.getDb().collection("spots").document(spotId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            currentSpot = document.toObject(Spot.class);
                            if (currentSpot != null) {
                                currentSpot.setId(spotId);
                                displaySpotDetails();
                            }
                        } else {
                            Toast.makeText(this, "Böyle bir nokta yok", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
                        Toast.makeText(this, "Hata: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displaySpotDetails() {
        binding.tvName.setText(currentSpot.getName());
        binding.tvCategory.setText(currentSpot.getCategory());
        binding.tvDescription.setText(currentSpot.getDescription());
        binding.tvDifficulty.setText("Zorluk: " + currentSpot.getDifficulty() + "/5");
        binding.tvAddedBy.setText("Ekleyen: " + currentSpot.getAddedByName());

        if (currentSpot.isDangerous()) {
            binding.tvDanger.setText("⚠️ TEHLİKELİ BÖLGE");
            binding.tvDanger.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            binding.tvDanger.setText("GÜVENLİ BÖLGE");
            binding.tvDanger.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }

        if (currentSpot.getImageUrl() != null && !currentSpot.getImageUrl().isEmpty()) {
            binding.ivSpotImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(currentSpot.getImageUrl()).into(binding.ivSpotImage);
        } else {
            binding.ivSpotImage.setVisibility(View.GONE);
        }
    }

    // =============================
    // FAVORİ SİSTEMİ
    // =============================

    private void checkFavoriteStatus() {
        if (currentUserId == null) return;
        firebaseHelper.getDb().collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("spotId", spotId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    isFavorite = !querySnapshot.isEmpty();
                    updateFavoriteButton();
                });
    }

    private void toggleFavorite() {
        if (currentUserId == null) {
            Toast.makeText(this, "Giriş yapmanız gerekiyor", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnFavorite.setEnabled(false);

        if (isFavorite) {
            // Favori'den çıkar
            firebaseHelper.getDb().collection("favorites")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("spotId", spotId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                        isFavorite = false;
                        updateFavoriteButton();
                        binding.btnFavorite.setEnabled(true);
                        Toast.makeText(this, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Favoriye ekle
            String spotName = currentSpot != null ? currentSpot.getName() : "";
            Favorite favorite = new Favorite(currentUserId, spotId, spotName);
            firebaseHelper.getDb().collection("favorites").add(favorite)
                    .addOnSuccessListener(ref -> {
                        isFavorite = true;
                        updateFavoriteButton();
                        binding.btnFavorite.setEnabled(true);
                        Toast.makeText(this, "Favorilere eklendi ❤️", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        binding.btnFavorite.setEnabled(true);
                        Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            binding.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    // =============================
    // DEĞERLENDİRME SİSTEMİ
    // =============================

    private void loadRatings() {
        firebaseHelper.getDb().collection("ratings")
                .whereEqualTo("spotId", spotId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ratingList.clear();
                    float totalRating = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Rating rating = doc.toObject(Rating.class);
                        rating.setId(doc.getId());
                        ratingList.add(rating);
                        totalRating += rating.getRating();
                    }
                    ratingAdapter.notifyDataSetChanged();

                    if (ratingList.isEmpty()) {
                        binding.tvNoRatings.setVisibility(View.VISIBLE);
                        binding.rvRatings.setVisibility(View.GONE);
                        binding.tvAverageRating.setText("Henüz puan yok");
                        binding.ratingBarAverage.setRating(0);
                    } else {
                        binding.tvNoRatings.setVisibility(View.GONE);
                        binding.rvRatings.setVisibility(View.VISIBLE);
                        float avg = totalRating / ratingList.size();
                        binding.ratingBarAverage.setRating(avg);
                        binding.tvAverageRating.setText(
                                String.format("%.1f (%d değerlendirme)", avg, ratingList.size()));
                    }
                });
    }

    private void submitRating() {
        if (currentUserId == null) {
            Toast.makeText(this, "Giriş yapmanız gerekiyor", Toast.LENGTH_SHORT).show();
            return;
        }

        int ratingValue = (int) binding.ratingBarUser.getRating();
        if (ratingValue == 0) {
            Toast.makeText(this, "Lütfen bir puan seçin (1-5 yıldız)", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = "";
        if (binding.etComment.getEditText() != null) {
            comment = binding.etComment.getEditText().getText().toString().trim();
        }

        binding.btnSubmitRating.setEnabled(false);
        binding.btnSubmitRating.setText("Gönderiliyor...");

        Rating rating = new Rating(spotId, currentUserId, currentUserName, ratingValue, comment);

        firebaseHelper.getDb().collection("ratings").add(rating)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Değerlendirmeniz eklendi! ⭐", Toast.LENGTH_SHORT).show();
                    binding.ratingBarUser.setRating(0);
                    if (binding.etComment.getEditText() != null) {
                        binding.etComment.getEditText().setText("");
                    }
                    binding.btnSubmitRating.setEnabled(true);
                    binding.btnSubmitRating.setText("Değerlendir");
                    loadRatings(); // Yenile
                })
                .addOnFailureListener(e -> {
                    binding.btnSubmitRating.setEnabled(true);
                    binding.btnSubmitRating.setText("Değerlendir");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // =============================
    // RAPOR SİSTEMİ
    // =============================

    private void showReportDialog() {
        if (currentUserId == null) {
            Toast.makeText(this, "Giriş yapmanız gerekiyor", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] reasons = {"Hatalı Bilgi", "Uygunsuz İçerik", "Yanlış Konum", "Diğer"};

        new AlertDialog.Builder(this)
                .setTitle("Bildiri Sebebi Seçin")
                .setItems(reasons, (dialog, which) -> {
                    String selectedReason = reasons[which];
                    showReportDescriptionDialog(selectedReason);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showReportDescriptionDialog(String reason) {
        EditText etDescription = new EditText(this);
        etDescription.setHint("Açıklama (opsiyonel)");
        etDescription.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Ek Açıklama")
                .setView(etDescription)
                .setPositiveButton("Gönder", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    submitReport(reason, description);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void submitReport(String reason, String description) {
        String spotName = currentSpot != null ? currentSpot.getName() : "";
        Report report = new Report(spotId, spotName, currentUserId, currentUserName, reason, description);

        firebaseHelper.getDb().collection("reports").add(report)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Bildiriniz gönderildi. Teşekkürler! 🙏", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
