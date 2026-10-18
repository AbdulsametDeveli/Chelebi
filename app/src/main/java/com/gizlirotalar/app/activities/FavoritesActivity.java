package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.gizlirotalar.app.R;
import com.gizlirotalar.app.databinding.ActivityFavoritesBinding;
import com.gizlirotalar.app.models.Favorite;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private ActivityFavoritesBinding binding;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private List<FavItem> favItems = new ArrayList<>();
    private FavAdapter adapter;

    // Her favori için spot koordinatlarını da tutmak istiyoruz
    static class FavItem {
        String favoriteId, spotId, spotName, category;
        double lat, lng;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Favorilerim");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseHelper = FirebaseHelper.getInstance();

        if (firebaseHelper.getAuth().getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = firebaseHelper.getAuth().getCurrentUser().getUid();

        adapter = new FavAdapter(favItems);
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        firebaseHelper.getDb().collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    favItems.clear();
                    List<String> spotIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Favorite fav = doc.toObject(Favorite.class);
                        FavItem item = new FavItem();
                        item.favoriteId = doc.getId();
                        item.spotId = fav.getSpotId();
                        item.spotName = fav.getSpotName();
                        favItems.add(item);
                        spotIds.add(fav.getSpotId());
                    }

                    if (favItems.isEmpty()) {
                        binding.tvNoFavorites.setVisibility(View.VISIBLE);
                        binding.rvFavorites.setVisibility(View.GONE);
                        return;
                    }

                    binding.tvNoFavorites.setVisibility(View.GONE);
                    binding.rvFavorites.setVisibility(View.VISIBLE);

                    // Her spot için koordinat + kategori çek
                    for (int i = 0; i < favItems.size(); i++) {
                        final int idx = i;
                        final FavItem item = favItems.get(i);
                        firebaseHelper.getDb().collection("spots").document(item.spotId).get()
                                .addOnSuccessListener(spotDoc -> {
                                    if (spotDoc.exists()) {
                                        Double lat = spotDoc.getDouble("latitude");
                                        Double lng = spotDoc.getDouble("longitude");
                                        String cat = spotDoc.getString("category");
                                        item.lat = lat != null ? lat : 0;
                                        item.lng = lng != null ? lng : 0;
                                        item.category = cat != null ? cat : "";
                                        adapter.notifyItemChanged(idx);
                                    }
                                });
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    /** Google Maps ile yol tarifi aç */
    private void openNavigation(FavItem item) {
        if (item.lat == 0 && item.lng == 0) {
            Toast.makeText(this, "Konum bilgisi henüz yükleniyor, lütfen bekleyin.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri gmmIntentUri = Uri.parse(
                "google.navigation:q=" + item.lat + "," + item.lng + "&mode=d");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Google Maps yüklü değilse tarayıcıda aç
            Uri browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=" + item.lat + "," + item.lng);
            startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    // ─── İç Adapter ───────────────────────────────────────────
    class FavAdapter extends RecyclerView.Adapter<FavAdapter.VH> {
        private final List<FavItem> list;
        FavAdapter(List<FavItem> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_nav, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FavItem item = list.get(pos);
            h.tvName.setText(item.spotName);
            h.tvCategory.setText(item.category != null ? item.category : "");

            // Yol tarifi butonu
            h.btnNavigate.setOnClickListener(v -> openNavigation(item));

            // Karta tıklayınca spot detayına git
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, SpotDetailActivity.class);
                intent.putExtra("SPOT_ID", item.spotId);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory;
            MaterialButton btnNavigate;
            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvFavSpotName);
                tvCategory = v.findViewById(R.id.tvFavSpotCategory);
                btnNavigate = v.findViewById(R.id.btnNavigate);
            }
        }
    }
}
