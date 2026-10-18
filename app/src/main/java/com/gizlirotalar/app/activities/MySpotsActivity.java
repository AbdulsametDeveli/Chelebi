package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gizlirotalar.app.R;
import com.gizlirotalar.app.databinding.ActivityMySpotsBinding;
import com.gizlirotalar.app.models.Spot;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MySpotsActivity extends AppCompatActivity {
    private ActivityMySpotsBinding binding;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private List<Spot> mySpotList = new ArrayList<>();
    private MySpotAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMySpotsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Spotlarım");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseHelper = FirebaseHelper.getInstance();

        if (firebaseHelper.getAuth().getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = firebaseHelper.getAuth().getCurrentUser().getUid();

        adapter = new MySpotAdapter(mySpotList);
        binding.rvMySpots.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMySpots.setAdapter(adapter);

        loadMySpots();
    }

    private void loadMySpots() {
        firebaseHelper.getDb().collection("spots")
                .whereEqualTo("addedBy", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mySpotList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Spot spot = doc.toObject(Spot.class);
                        spot.setId(doc.getId());
                        mySpotList.add(spot);
                    }
                    adapter.notifyDataSetChanged();

                    if (mySpotList.isEmpty()) {
                        binding.tvNoMySpots.setVisibility(View.VISIBLE);
                        binding.rvMySpots.setVisibility(View.GONE);
                    } else {
                        binding.tvNoMySpots.setVisibility(View.GONE);
                        binding.rvMySpots.setVisibility(View.VISIBLE);
                        // Her spot için istatistikleri yükle
                        for (int i = 0; i < mySpotList.size(); i++) {
                            loadSpotStats(mySpotList.get(i), i);
                        }
                    }
                });
    }

    // Her spot için yorum/favori istatistiklerini çek
    private void loadSpotStats(Spot spot, int position) {
        // Değerlendirmeler
        firebaseHelper.getDb().collection("ratings")
                .whereEqualTo("spotId", spot.getId())
                .get()
                .addOnSuccessListener(ratingSnap -> {
                    int count = ratingSnap.size();
                    float avg = 0;
                    String lastComment = "";
                    for (QueryDocumentSnapshot doc : ratingSnap) {
                        Long r = doc.getLong("rating");
                        if (r != null) avg += r;
                        String c = doc.getString("comment");
                        if (c != null && !c.isEmpty()) lastComment = c;
                    }
                    if (count > 0) avg /= count;

                    spot.setCommentCount(count);
                    spot.setAvgRating(avg);
                    spot.setLastComment(lastComment);

                    // Favoriler
                    firebaseHelper.getDb().collection("favorites")
                            .whereEqualTo("spotId", spot.getId())
                            .get()
                            .addOnSuccessListener(favSnap -> {
                                spot.setFavoriteCount(favSnap.size());
                                adapter.notifyItemChanged(position);
                            });
                });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    // ─── İç Adapter ───────────────────────────────────────────
    class MySpotAdapter extends RecyclerView.Adapter<MySpotAdapter.VH> {
        private final List<Spot> list;
        MySpotAdapter(List<Spot> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_spot, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Spot s = list.get(pos);
            h.tvName.setText(s.getName());
            h.tvCategory.setText(s.getCategory());

            int cnt = s.getCommentCount();
            h.tvRatingCount.setText("⭐ " + cnt + " değerlendirme");
            h.tvCommentCount.setText(String.valueOf(cnt));
            h.tvFavoriteCount.setText(String.valueOf(s.getFavoriteCount()));

            if (s.getAvgRating() > 0) {
                h.tvAvgRating.setText(String.format("%.1f", s.getAvgRating()));
            } else {
                h.tvAvgRating.setText("—");
            }

            if (s.getLastComment() != null && !s.getLastComment().isEmpty()) {
                h.tvLastComment.setVisibility(View.VISIBLE);
                h.tvLastComment.setText("💬 \"" + s.getLastComment() + "\"");
            } else {
                h.tvLastComment.setVisibility(View.GONE);
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MySpotsActivity.this, SpotDetailActivity.class);
                intent.putExtra("SPOT_ID", s.getId());
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvRatingCount, tvAvgRating, tvFavoriteCount, tvCommentCount, tvLastComment;
            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvMySpotName);
                tvCategory = v.findViewById(R.id.tvMySpotCategory);
                tvRatingCount = v.findViewById(R.id.tvMySpotRatingCount);
                tvAvgRating = v.findViewById(R.id.tvMySpotAvgRating);
                tvFavoriteCount = v.findViewById(R.id.tvMySpotFavoriteCount);
                tvCommentCount = v.findViewById(R.id.tvMySpotCommentCount);
                tvLastComment = v.findViewById(R.id.tvMySpotLastComment);
            }
        }
    }
}
