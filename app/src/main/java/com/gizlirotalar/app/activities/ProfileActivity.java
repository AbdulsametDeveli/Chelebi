package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gizlirotalar.app.R;
import com.gizlirotalar.app.databinding.ActivityProfileBinding;
import com.gizlirotalar.app.models.Favorite;
import com.gizlirotalar.app.models.User;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private List<Favorite> favoriteList = new ArrayList<>();
    private FavoriteListAdapter favoriteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profilim");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseHelper = FirebaseHelper.getInstance();

        if (firebaseHelper.getAuth().getCurrentUser() == null) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = firebaseHelper.getAuth().getCurrentUser().getUid();

        // Favori listesi
        favoriteAdapter = new FavoriteListAdapter();
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFavorites.setAdapter(favoriteAdapter);

        loadProfile();
        loadUserStats();
        loadFavorites();

        binding.btnLogout.setOnClickListener(v -> {
            firebaseHelper.getAuth().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfile() {
        firebaseHelper.getDb().collection("users").document(currentUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                binding.tvProfileName.setText(user.getDisplayName());
                                binding.tvProfileEmail.setText(user.getEmail());

                                String roleText = "admin".equals(user.getRole()) ? "Yönetici" : "Kullanıcı";
                                binding.tvProfileRole.setText(roleText);
                                binding.tvProfileRoleDetail.setText(roleText);

                                if (user.getCreatedAt() > 0) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("tr", "TR"));
                                    binding.tvProfileDate.setText(sdf.format(new Date(user.getCreatedAt())));
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Profil yüklenemedi", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserStats() {
        // Eklenen spot sayısı
        firebaseHelper.getDb().collection("spots")
                .whereEqualTo("addedBy", currentUserId).get()
                .addOnSuccessListener(snap ->
                        binding.tvStatSpotsAdded.setText(String.valueOf(snap.size())));

        // Verilen değerlendirme sayısı
        firebaseHelper.getDb().collection("ratings")
                .whereEqualTo("userId", currentUserId).get()
                .addOnSuccessListener(snap ->
                        binding.tvStatRatingsGiven.setText(String.valueOf(snap.size())));

        // Favori sayısı
        firebaseHelper.getDb().collection("favorites")
                .whereEqualTo("userId", currentUserId).get()
                .addOnSuccessListener(snap ->
                        binding.tvStatFavorites.setText(String.valueOf(snap.size())));
    }

    private void loadFavorites() {
        firebaseHelper.getDb().collection("favorites")
                .whereEqualTo("userId", currentUserId).get()
                .addOnSuccessListener(querySnapshot -> {
                    favoriteList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Favorite fav = doc.toObject(Favorite.class);
                        fav.setId(doc.getId());
                        favoriteList.add(fav);
                    }
                    favoriteAdapter.notifyDataSetChanged();

                    if (favoriteList.isEmpty()) {
                        binding.tvNoFavorites.setVisibility(View.VISIBLE);
                        binding.rvFavorites.setVisibility(View.GONE);
                    } else {
                        binding.tvNoFavorites.setVisibility(View.GONE);
                        binding.rvFavorites.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void removeFavorite(Favorite favorite, int position) {
        firebaseHelper.getDb().collection("favorites").document(favorite.getId()).delete()
                .addOnSuccessListener(v -> {
                    favoriteList.remove(position);
                    favoriteAdapter.notifyItemRemoved(position);
                    loadUserStats(); // istatistikleri güncelle
                    Toast.makeText(this, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                    if (favoriteList.isEmpty()) {
                        binding.tvNoFavorites.setVisibility(View.VISIBLE);
                    }
                });
    }

    // İç adapter sınıfı
    private class FavoriteListAdapter extends RecyclerView.Adapter<FavoriteListAdapter.FavViewHolder> {
        @Override
        public FavViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_favorite, parent, false);
            return new FavViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FavViewHolder holder, int position) {
            Favorite fav = favoriteList.get(position);
            holder.tvName.setText(fav.getSpotName());
            holder.btnRemove.setOnClickListener(v -> removeFavorite(fav, position));
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, SpotDetailActivity.class);
                intent.putExtra("SPOT_ID", fav.getSpotId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return favoriteList.size(); }

        class FavViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageButton btnRemove;
            FavViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvFavoriteName);
                btnRemove = itemView.findViewById(R.id.btnRemoveFavorite);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserStats();
        loadFavorites();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
