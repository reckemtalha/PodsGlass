# PodsGlass — Samsung için AirPods Pro 3 yardımcısı (root'suz)

## Neler yapıyor
- **iPhone tarzı açılır pencere:** Kutunun kapağını telefonun yanında açınca alttan yaylanarak çıkan cam görünümlü pencere, titreşim, sol/sağ/kutu pil halkaları. Arka plan One UI'ın gerçek pencere bulanıklığıyla buzlanır.
- **Canlı pil:** Bildirimde sürekli "Sol %80 · Sağ %90 · Kutu %60".
- **Son görülen konum:** Kulaklık bağlantısı koptuğu an telefonun GPS konumu kaydedilir; "Haritada Aç" ile Google Haritalar'da gösterilir.
- **Kişiselleştirme:** Kulaklık adı, 6 vurgu rengi, cam yoğunluğu, açılır pencereyi aç/kapat.
- **Kontrol rehberi:** Sap hareketleri ve Bluetooth ayarlarına kısayol.

## Root olmadan yapılamayanlar
- Gürültü engelleme modunu uygulamadan değiştirme, kulak algılama, basılı tutma hareketini değiştirme, telefonu Apple cihazı gibi tanıtma.
- Gerçek Find My (Apple ağı) — uygulama bunun yerine "son görülen konum" tutar.

## Kurulum (Android Studio)
1. Bilgisayara **Android Studio** kur (ücretsiz).
2. `PodsGlass` klasörünü aç: *File → Open*. Gradle senkronizasyonunun bitmesini bekle.
3. Telefonda *Ayarlar → Telefon hakkında → Yazılım bilgisi → Yapım numarası*na 7 kez dokun, ardından *Geliştirici seçenekleri → USB hata ayıklama*yı aç.
4. Telefonu USB ile bağla, Android Studio'da yeşil ▶ **Run** düğmesine bas.
5. Uygulamada **Kurulum** kartındaki üç izni ver (Bluetooth/konum, açılır pencere, pil kısıtlaması).
6. Konumun her zaman kaydedilmesi için: *Ayarlar → Uygulamalar → PodsGlass → İzinler → Konum → Her zaman izin ver* (önerilir).

## İpuçları
- Pencere, etraftaki başka AirPods'lara tepki vermesin diye yalnızca çok yakın sinyalde açılır (kutuyu telefona yakın aç).
- Pil değerleri Apple yayınında %10'luk adımlarla gelir.
