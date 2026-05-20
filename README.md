# 🎵 SongFinder

Şarkıyı duyunca **Quick Settings Tile**'a bas → 10 saniye dinler → **MYT Müzik**'te açar!

## ⚡ Akış

```
Bildirim panelini aç (2 parmakla aşağı çek)
           ↓
🎙️ "Şarkı Tanı" tile'ına bas
           ↓
Mikrofon açılır — 10 sn dinler
           ↓
Durum çubuğunda:  🎙️ Dinleniyor… (9 sn)
                  🔍 Şarkı tanınıyor…
           ↓
✅ Bulundu: Artist - Şarkı Adı
           ↓
📋 Panoya kopyalandı
🎵 MYT Müzik otomatik açılır
```

## 🔑 API Key Kurulumu

### 1. ACRCloud (Ses Tanıma) — Ücretsiz 100 istek/gün
1. https://console.acrcloud.com → Sign Up
2. Console → **Projects** → **Create Project** → "Audio & Video Recognition"
3. Projeye tıkla → **Credentials** sekmesi
4. `Host`, `Access Key`, `Secret Key` değerlerini kopyala

### 2. Last.fm (Şarkı Arama) — Ücretsiz
1. https://www.last.fm/join → hesap oluştur
2. https://www.last.fm/api/account/create → API key al

### 3. local.properties dosyası
```properties
sdk.dir=/path/to/android/sdk
LASTFM_API_KEY=xxxxx
ACR_HOST=identify-eu-west-1.acrcloud.com
ACR_ACCESS_KEY=xxxxx
ACR_SECRET_KEY=xxxxx
```

### 4. GitHub Secrets (CI/CD için)
Repo → Settings → Secrets → Actions:

| Secret | Açıklama |
|--------|----------|
| `LASTFM_API_KEY` | Last.fm API key |
| `ACR_HOST` | ACRCloud host (ör. identify-eu-west-1.acrcloud.com) |
| `ACR_ACCESS_KEY` | ACRCloud access key |
| `ACR_SECRET_KEY` | ACRCloud secret key |

## 📲 Tile Ekleme
1. Bildirim panelini **tamamen** aşağı çek
2. Sağ alt **kalem/düzenle** ikonuna bas
3. **"Şarkı Tanı"** tile'ını Bluetooth/WiFi yanına sürükle
4. Kaydet

## 🏗️ Build & Release

```bash
# Debug APK
./gradlew assembleDebug

# Release (GitHub Actions otomatik yapar)
git tag v1.0.0
git push origin v1.0.0
```

## 📁 Proje Yapısı
```
app/src/main/java/com/songfinder/app/
├── recognition/
│   └── AcrCloudRecognizer.kt   ← Mikrofon kaydı + ACRCloud API
├── tile/
│   ├── SongSearchTileService.kt ← Quick Settings Tile
│   ├── TileResultAdapter.kt
│   └── MytOpenReceiver.kt      ← Bildirim butonu
├── ui/
│   ├── MainActivity.kt         ← Manuel arama + FAB mic
│   ├── TrackAdapter.kt
│   └── LastFmLauncher.kt       ← MYT Müzik açma
├── network/                    ← Last.fm Retrofit
├── viewmodel/
└── model/
```
