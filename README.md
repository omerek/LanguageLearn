# Öbek İngilizce

Anadili Türkçe olanlara İngilizceyi A1'den C1'e öğreten, tamamen çevrimdışı, metin tabanlı Android uygulaması.

Üç yöntem tek bir döngüde çalışır:

- **Aralıklı tekrar (FSRS-5):** Her öbek ve cümle bir karttır; not, öğrencinin performansından otomatik çıkarılır.
- **Öbekleme:** Öğrenme birimi tek sözcük değil, `I'd like`, `on Saturday`, `although` gibi öbeklerdir. Cümle kurma taşları öbektir; seviye yükseldikçe taşlar bölünür.
- **Anlaşılabilir girdi (i+1):** Her yeni cümlede en fazla 1 yeni öbek ve 1 yeni yapı bulunur; zorluk öğrencinin Elo tarzı yetenek puanının biraz üstünde tutulur.

Kural ezberi yoktur. Hata anında, hatanın türüne ve Türkçe ile İngilizce arasındaki farka göre kademeli ipucu verilir (yönlendirme → kural → cevap).

## Yapı

| Modül | İçerik |
| --- | --- |
| `core/` | Saf Kotlin: veri modelleri, içerik ayrıştırıcı ve doğrulayıcı, FSRS, i+1 seçici, alıştırma fabrikası, cevap değerlendirici, ipucu motoru, oturum planlayıcı. Android olmadan test edilir. |
| `app/` | Android (minSdk 26): Room veritabanı, Jetpack Compose ekranları. |
| `app/src/main/assets/content/` | İçerik: `a1.json` … `c1.json` (17 ünite, 69 öbek, 86 cümle, 5 okuma metni) ve `hints.json` (31 dilbilgisi ipucu). |

## Çalıştırma

```bash
./gradlew :core:test            # algoritma ve içerik testleri (Android SDK gerekmez)
./gradlew :app:assembleDebug    # APK (Android SDK gerekir)
./gradlew -PcoreOnly :core:test # Android SDK'sı olmayan makinede yalnızca core
```

## İçerik eklemek

Yeni cümle eklerken `segments` alanları cümleyi birebir oluşturmalı ve müfredat sırasında her cümle en fazla bir yeni öbek ile bir yeni dilbilgisi etiketi getirmelidir. `ContentTest` bu kuralları ve okuma metinlerinin %95 kapsam eşiğini her derlemede denetler.

İçerik yapay zekâ yardımıyla üretilmiştir; yayından önce bir öğretmen tarafından gözden geçirilmesi önerilir.
