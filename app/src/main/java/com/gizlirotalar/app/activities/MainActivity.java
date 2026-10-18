package com.gizlirotalar.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import com.gizlirotalar.app.R;
import com.gizlirotalar.app.databinding.ActivityMainBinding;
import com.gizlirotalar.app.models.Spot;
import com.gizlirotalar.app.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MapView map;
    private FirebaseHelper firebaseHelper;
    private boolean isAdmin = false;
    private String selectedCategory = "Tümü";
    private List<Spot> allSpots = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private Marker userLocationMarker; // Kullanıcının konumunu gösteren marker

    // Sabit kategori listesi
    private static final String[] CATEGORIES = {
            "Tümü", "Kamp Alanı", "Restoran", "Otel / Konaklama", "Şelale", "Manzara Noktası",
            "Tehlikeli Bölge", "Bisiklet Rotası", "Trekking Rotası"
    };

    // Konum izni launcher
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineGranted != null && fineGranted) {
                    checkGpsAndGetLocation();
                } else if (coarseGranted != null && coarseGranted) {
                    checkGpsAndGetLocation();
                } else {
                    Toast.makeText(this, "Konum izni verilmedi. Harita varsayılan konumda açılacak.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = FirebaseHelper.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        map = binding.mapView;

        if (getIntent().hasExtra("FILTER_CATEGORY")) {
            selectedCategory = getIntent().getStringExtra("FILTER_CATEGORY");
        }

        // Kullanıcı rolünü kontrol et
        checkUserRole();

        // Harita ayarları
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.getController().setZoom(12.0);
            map.getController().setCenter(new GeoPoint(40.7731, 30.3948)); // Varsayılan konum

            MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    return false;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    Intent intent = new Intent(MainActivity.this, AddSpotActivity.class);
                    intent.putExtra("LAT", p.getLatitude());
                    intent.putExtra("LNG", p.getLongitude());
                    startActivity(intent);
                    return false;
                }
            };
            MapEventsOverlay overlayEvents = new MapEventsOverlay(mReceive);
            map.getOverlays().add(overlayEvents);
        }

        // Kategori filtresi Spinner ayarı
        setupCategoryFilter();

        binding.fabAddSpot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddSpotActivity.class);
            if (map != null) {
                GeoPoint center = (GeoPoint) map.getMapCenter();
                intent.putExtra("LAT", center.getLatitude());
                intent.putExtra("LNG", center.getLongitude());
            }
            startActivity(intent);
        });

        loadSpotsFromFirestore();

        // Konum iznini kontrol et ve kullanıcı konumunu al
        requestLocationPermission();
    }

    // ============================
    // KONUM İZNİ VE GPS KONTROL
    // ============================

    /**
     * Konum izni verilmiş mi kontrol eder. Verilmemişse izin ister.
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // İzin zaten var, GPS açık mı kontrol et
            checkGpsAndGetLocation();
        } else {
            // İzin iste
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * GPS (konum servisi) açık mı kontrol eder. Kapalıysa uyarı gösterir.
     */
    private void checkGpsAndGetLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            // GPS kapalı — kullanıcıya uyarı göster
            new AlertDialog.Builder(this)
                    .setTitle("📍 Konum Servisi Kapalı")
                    .setMessage("Konumunuzu haritada görebilmemiz için lütfen konum servisini (GPS) açın.")
                    .setPositiveButton("Ayarlara Git", (dialog, which) -> {
                        // Kullanıcıyı cihazın konum ayarlarına yönlendir
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Şimdilik Geç", (dialog, which) -> {
                        Toast.makeText(this, "Harita varsayılan konumda açıldı.", Toast.LENGTH_SHORT).show();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            // GPS açık — kullanıcının konumunu al
            getUserLocation();
        }
    }

    /**
     * Kullanıcının anlık konumunu alır ve haritayı o noktaya odaklar.
     */
    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // hafızada (cache) tutar. Bu yüzden her seferinde en güncel, taze GPS konumunu 
        // almak için doğrudan aktif konum isteği gönderiyoruz.
        requestActiveLocation();
    }

    /**
     * Aktif GPS sorgusu ile güncel konumu alır (getLastLocation null dönerse)
     */
    private void requestActiveLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(this, "Konumunuz aranıyor...", Toast.LENGTH_SHORT).show();

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && map != null) {
                    showUserOnMap(location);
                } else {
                    Toast.makeText(MainActivity.this, "Konum alınamadı, varsayılan konumda açılıyor.", Toast.LENGTH_SHORT).show();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }

    /**
     * Kullanıcının konumunu haritada gösterir
     */
    private void showUserOnMap(Location location) {
        GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Haritayı kullanıcının konumuna odakla
        map.getController().setZoom(15.0);
        map.getController().animateTo(userPoint);

        // Kullanıcının konumunu gösteren marker
        if (userLocationMarker != null) {
            map.getOverlays().remove(userLocationMarker);
        }
        userLocationMarker = new Marker(map);
        userLocationMarker.setPosition(userPoint);
        userLocationMarker.setTitle("\uD83D\uDCCD Benim Konumum");
        userLocationMarker.setSnippet("Şu an buradasınız");
        userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Mavi renkli marker (kullanıcı konumu)
        Drawable myIcon = ContextCompat.getDrawable(this,
                org.osmdroid.library.R.drawable.marker_default);
        if (myIcon != null) {
            Drawable wrappedIcon = DrawableCompat.wrap(myIcon.mutate());
            DrawableCompat.setTint(wrappedIcon, android.graphics.Color.BLUE);
            userLocationMarker.setIcon(wrappedIcon);
        }

        map.getOverlays().add(userLocationMarker);
        map.invalidate();

        Toast.makeText(this, "\uD83D\uDCCD Konumunuz bulundu!", Toast.LENGTH_SHORT).show();
    }

    // ============================
    // FİLTRE VE KATEGORİ
    // ============================

    /**
     * Kategori filtreleme Spinner'ını ayarlar
     */
    private void setupCategoryFilter() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CATEGORIES);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(filterAdapter);

        // Dashboard'dan gelen kategori varsa Spinner'ı o pozisyona taşı
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(selectedCategory)) {
                binding.spinnerFilter.setSelection(i, false);
                break;
            }
        }

        binding.spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean isFirstCall = true;

            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (isFirstCall) {
                    isFirstCall = false;
                    if (!allSpots.isEmpty()) applyFilter();
                    return;
                }
                selectedCategory = CATEGORIES[position];
                applyFilter();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // boş
            }
        });
    }

    /**
     * Firestore'dan kullanıcının rolünü kontrol eder.
     */
    private void checkUserRole() {
        if (firebaseHelper.getAuth().getCurrentUser() == null) return;

        String uid = firebaseHelper.getAuth().getCurrentUser().getUid();
        firebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        isAdmin = "admin".equals(role);
                        invalidateOptionsMenu();
                    }
                });
    }

    /**
     * Firestore'dan tüm noktaları çeker ve listeye kaydeder
     */
    private void loadSpotsFromFirestore() {
        firebaseHelper.getDb().collection("spots").get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && map != null) {
                    allSpots.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Spot spot = document.toObject(Spot.class);
                        spot.setId(document.getId());
                        allSpots.add(spot);
                    }
                    applyFilter();
                }
            });
    }

    /**
     * Seçili kategoriye göre haritadaki marker'ları filtreler
     */
    private void applyFilter() {
        if (map == null) return;

        // Mevcut overlay'leri temizle (MapEventsOverlay ve userLocationMarker hariç)
        map.getOverlays().removeIf(overlay ->
                overlay instanceof Marker && overlay != userLocationMarker);

        for (Spot spot : allSpots) {
            // Kategori filtresi
            boolean categoryMatch = "Tümü".equals(selectedCategory) || selectedCategory.equals(spot.getCategory());

            if (categoryMatch) {
                Marker marker = new Marker(map);
                marker.setPosition(new GeoPoint(spot.getLatitude(), spot.getLongitude()));
                marker.setTitle(spot.getName());
                marker.setSnippet(spot.getCategory());

                // Kategoriye göre marker rengi belirle
                Drawable defaultIcon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default);
                if (defaultIcon != null) {
                    Drawable wrappedIcon = DrawableCompat.wrap(defaultIcon.mutate());
                    int color = getCategoryColor(spot.getCategory());
                    DrawableCompat.setTint(wrappedIcon, color);
                    marker.setIcon(wrappedIcon);
                }

                marker.setOnMarkerClickListener((m, mapView) -> {
                    Intent intent = new Intent(MainActivity.this, SpotDetailActivity.class);
                    intent.putExtra("SPOT_ID", spot.getId());
                    startActivity(intent);
                    return true;
                });

                map.getOverlays().add(marker);
            }
        }
        map.invalidate();
    }

    private int getCategoryColor(String category) {
        if (category == null) return android.graphics.Color.BLUE;
        switch (category) {
            case "Kamp Alanı": return android.graphics.Color.GREEN;
            case "Restoran": return android.graphics.Color.RED;
            case "Otel / Konaklama": return android.graphics.Color.parseColor("#FF9800");
            case "Şelale": return android.graphics.Color.CYAN;
            case "Manzara Noktası": return android.graphics.Color.parseColor("#9C27B0");
            case "Tehlikeli Bölge": return android.graphics.Color.BLACK;
            case "Bisiklet Rotası": return android.graphics.Color.YELLOW;
            case "Trekking Rotası": return android.graphics.Color.parseColor("#795548");
            default: return android.graphics.Color.BLUE;
        }
    }

    // ============================
    // MENÜ
    // ============================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem adminItem = menu.findItem(R.id.action_admin);
        if (adminItem != null) {
            adminItem.setVisible(isAdmin);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            firebaseHelper.getAuth().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_admin) {
            if (isAdmin) {
                startActivity(new Intent(MainActivity.this, AdminActivity.class));
            } else {
                Toast.makeText(this, "Bu sayfaya erişim yetkiniz yok!", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
            loadSpotsFromFirestore();

            // Ayarlardan döndüğünde GPS tekrar kontrol et
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    getUserLocation();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
