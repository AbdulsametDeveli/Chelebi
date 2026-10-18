# Chelebi — Dijital Doğa ve Keşif Günlüğü

Chelebi, doğa tutkunlarının keşfettikleri noktaları (şelale, kamp alanı, yürüyüş rotası, tarihi yer vb.) harita üzerinde paylaşabildiği, topluluk odaklı ve konum tabanlı bir **Android** uygulamasıdır.

## Özellikler

- 🗺️ **Harita üzerinde keşif** — OpenStreetMap (OSMDroid) tabanlı harita, kategoriye göre renkli işaretçiler
- 📍 **Nokta (spot) ekleme** — konum, kategori, zorluk seviyesi, tehlike bilgisi ve fotoğraf ile
- ⭐ **Puanlama ve yorum** — topluluk tabanlı kalite kontrolü
- ❤️ **Favoriler** ve **Noktalarım** ekranları
- ⚠️ **Tehlikeli nokta bildirme** ve yönetici moderasyon paneli
- 📏 Kullanıcının konumuna göre **mesafe hesaplama** ve filtreleme
- 👤 Firebase Authentication ile kayıt / giriş, profil sayfası

## Kullanılan Teknolojiler

| Katman | Teknoloji |
|---|---|
| Dil / Ortam | Java 17, Android Studio |
| Arayüz | Material Components, ConstraintLayout, ViewBinding |
| Harita | OSMDroid 6.1 |
| Konum | Google Play Services Fused Location Provider |
| Kimlik doğrulama | Firebase Authentication |
| Veritabanı | Cloud Firestore (users, spots, ratings, favorites, reports) |
| Görsel yükleme | ImgBB API |
| Görsel gösterimi | Glide |

## Proje Yapısı

```
app/src/main/java/com/gizlirotalar/app/
├── activities/   # Ekranlar (Welcome, Login, Register, Main, AddSpot, SpotDetail, Admin, ...)
├── adapters/     # RecyclerView adapter'ları
├── models/       # Firestore POJO modelleri (User, Spot, Rating, Favorite, Report)
└── utils/        # FirebaseHelper, ImgBBUploader, AdminConfig, DistanceCalculator, InputValidator
```

## Kurulum

Gizli bilgiler (API anahtarları, Firebase yapılandırması) depoda **yer almaz**. Projeyi çalıştırmak için:

1. Depoyu klonla ve Android Studio ile aç.
2. **Firebase:** [Firebase Console](https://console.firebase.google.com/) üzerinden bir proje oluştur, paket adı `com.gizlirotalar.app` olan bir Android uygulaması ekle; Authentication (E-posta/Şifre) ve Firestore'u etkinleştir. İndirdiğin `google-services.json` dosyasını `app/` klasörüne koy. (Şablon için `app/google-services.json.example` dosyasına bak.)
3. **ImgBB:** [api.imgbb.com](https://api.imgbb.com/) adresinden ücretsiz bir API anahtarı al.
4. Proje kök dizinindeki `local.properties` dosyasına şu satırı ekle (örnek: `local.properties.example`):
   ```properties
   IMGBB_API_KEY=senin_imgbb_anahtarin
   ADMIN_EMAILS=yonetici@ornek.com
   ```
   `ADMIN_EMAILS` içindeki adreslerle kayıt olan / giriş yapan kullanıcılar yönetici paneline erişebilir.
5. Gradle senkronizasyonunu yap ve uygulamayı çalıştır.

## Geliştiriciler

- Abdulsamet Develi
- Musa Emre Toprak

*Mobil Programlama dersi projesi — 2025-2026 Bahar dönemi.*
