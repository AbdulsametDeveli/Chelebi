package com.gizlirotalar.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gizlirotalar.app.adapters.ReportAdapter;
import com.gizlirotalar.app.adapters.SpotAdapter;
import com.gizlirotalar.app.databinding.ActivityAdminBinding;
import com.gizlirotalar.app.models.Report;
import com.gizlirotalar.app.models.Spot;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private ActivityAdminBinding binding;
    private FirebaseHelper firebaseHelper;
    private SpotAdapter spotAdapter;
    private ReportAdapter reportAdapter;
    private List<Spot> spotList = new ArrayList<>();
    private List<Report> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        checkAdminAccess();

        // Spot Adapter
        binding.rvSpots.setLayoutManager(new LinearLayoutManager(this));
        spotAdapter = new SpotAdapter(spotList, new SpotAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(Spot spot, int position) {
                showDeleteDialog(spot, position);
            }
            @Override
            public void onItemClick(Spot spot) {
                Intent intent = new Intent(AdminActivity.this, SpotDetailActivity.class);
                intent.putExtra("SPOT_ID", spot.getId());
                startActivity(intent);
            }
            @Override
            public void onEditClick(Spot spot) {
                Intent intent = new Intent(AdminActivity.this, EditSpotActivity.class);
                intent.putExtra("SPOT_ID", spot.getId());
                startActivity(intent);
            }
        });
        binding.rvSpots.setAdapter(spotAdapter);

        // Report Adapter
        binding.rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(reportList, new ReportAdapter.OnReportActionListener() {
            @Override
            public void onApprove(Report report, int position) {
                handleReport(report, position, "approved");
            }
            @Override
            public void onReject(Report report, int position) {
                handleReport(report, position, "rejected");
            }
        });
        binding.rvReports.setAdapter(reportAdapter);

        // Tab Layout
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Spotlar"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Raporlar"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.rvSpots.setVisibility(View.VISIBLE);
                    binding.rvReports.setVisibility(View.GONE);
                    binding.tvNoReports.setVisibility(View.GONE);
                } else {
                    binding.rvSpots.setVisibility(View.GONE);
                    binding.rvReports.setVisibility(View.VISIBLE);
                    if (reportList.isEmpty()) {
                        binding.tvNoReports.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadStatistics();
        loadAllSpots();
        loadPendingReports();
    }

    private void checkAdminAccess() {
        if (firebaseHelper.getAuth().getCurrentUser() == null) { finish(); return; }
        String uid = firebaseHelper.getAuth().getCurrentUser().getUid();
        firebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if (!"admin".equals(role)) {
                            Toast.makeText(this, "Bu sayfaya erişim yetkiniz yok!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else { finish(); }
                })
                .addOnFailureListener(e -> finish());
    }

    private void loadStatistics() {
        // Toplam spot
        firebaseHelper.getDb().collection("spots").get()
                .addOnSuccessListener(snap -> binding.tvStatSpots.setText(String.valueOf(snap.size())));

        // Toplam kullanıcı
        firebaseHelper.getDb().collection("users").get()
                .addOnSuccessListener(snap -> binding.tvStatUsers.setText(String.valueOf(snap.size())));

        // Bekleyen rapor
        firebaseHelper.getDb().collection("reports")
                .whereEqualTo("status", "pending").get()
                .addOnSuccessListener(snap -> binding.tvStatReports.setText(String.valueOf(snap.size())));
    }

    private void loadAllSpots() {
        firebaseHelper.getDb().collection("spots").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        spotList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Spot spot = doc.toObject(Spot.class);
                            spot.setId(doc.getId());
                            spotList.add(spot);
                        }
                        spotAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadPendingReports() {
        firebaseHelper.getDb().collection("reports")
                .whereEqualTo("status", "pending").get()
                .addOnSuccessListener(querySnapshot -> {
                    reportList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Report report = doc.toObject(Report.class);
                        report.setId(doc.getId());
                        reportList.add(report);
                    }
                    reportAdapter.notifyDataSetChanged();
                    if (reportList.isEmpty() && binding.rvReports.getVisibility() == View.VISIBLE) {
                        binding.tvNoReports.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void handleReport(Report report, int position, String newStatus) {
        firebaseHelper.getDb().collection("reports").document(report.getId())
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    String msg = "approved".equals(newStatus) ? "Rapor onaylandı" : "Rapor reddedildi";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    reportList.remove(position);
                    reportAdapter.notifyItemRemoved(position);
                    loadStatistics(); // istatistikleri güncelle

                    // Onaylanan raporda spot'u silmek isteyip istemediğini sor
                    if ("approved".equals(newStatus)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Spot Silinsin mi?")
                                .setMessage(report.getSpotName() + " isimli spot silinsin mi?")
                                .setPositiveButton("Evet, Sil", (d, w) -> {
                                    firebaseHelper.getDb().collection("spots")
                                            .document(report.getSpotId()).delete()
                                            .addOnSuccessListener(a -> {
                                                Toast.makeText(this, "Spot silindi!", Toast.LENGTH_SHORT).show();
                                                loadAllSpots();
                                                loadStatistics();
                                            });
                                })
                                .setNegativeButton("Hayır", null)
                                .show();
                    }
                });
    }

    private void showDeleteDialog(Spot spot, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Silmeyi Onayla")
                .setMessage(spot.getName() + " noktasını silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet", (d, w) -> deleteSpot(spot, position))
                .setNegativeButton("İptal", null)
                .show();
    }

    private void deleteSpot(Spot spot, int position) {
        firebaseHelper.getDb().collection("spots").document(spot.getId()).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Silindi!", Toast.LENGTH_SHORT).show();
                    spotList.remove(position);
                    spotAdapter.notifyItemRemoved(position);
                    loadStatistics();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
        loadAllSpots();
        loadPendingReports();
    }
}
