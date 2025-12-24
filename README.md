### 📦 Envanter Yönetim Sistemi (Inventory Management System)

📖 Genel Bakış

Envanter Yönetim Sistemi, JavaFX ve MySQL kullanılarak geliştirilmiş modern bir masaüstü uygulamasıdır.
Bu sistem; ürün yönetimi, satış takibi, rol tabanlı kullanıcı erişimi, düşük stok uyarıları, raporlama ve CSV dışa aktarma gibi temel iş süreçlerini tek bir platformda toplar.

Küçük ve orta ölçekli işletmeler için tasarlanmıştır.
Temiz ve sezgisel kullanıcı arayüzü sayesinde hem yöneticiler hem de personel tarafından kolayca kullanılabilir.

🎯 Amaç:
Stok takibini ve satış yönetimini basitleştirirken veri bütünlüğünü, güvenliği ve kullanıcı deneyimini en üst seviyede tutmak.

⚙️ Nasıl Çalışır?
🔁 Temel İş Akışı

Kullanıcı uygulamayı başlatır → Login ekranı açılır

Giriş başarılıysa → rol bazlı Dashboard yüklenir

## Admin:

- Ürün ekler, günceller, siler

- Raporları görüntüler

- CSV dışa aktarma yapar

## Personel (Staff):

- Sadece satış ekleyebilir

## Satış yapıldığında:

- Stok otomatik güncellenir

- Fiş oluşturulur ve receipts/ klasörüne kaydedilir

## Stok miktarı 10’un altına düşerse:

- Düşük stok uyarısı gösterilir

### ✨ Ana Özellikler
🔐 Kullanıcı Kimlik Doğrulama

- Kullanıcı adı ve şifre ile giriş

- Rol tabanlı erişim kontrolü

- Admin → Tam yetki

- Staff → Sadece satış ekleme

## 📦 Ürün Yönetimi

- Ürün ekleme, güncelleme ve silme

## Ürün bilgileri:

- ID

- Ad

- Kategori

- Miktar

- Fiyat

- Tedarikçi

## Düşük stok uyarıları:

Miktar < 10 → tablo üzerinde kırmızı/turuncu vurgulama

### 🧾 Satış Yönetimi

- Satış kaydı (tekli veya çoklu ürün)

- Otomatik stok düşümü

- Stoktan fazla satış yapılması engellenir

- Profesyonel fiş oluşturma

- Fişler otomatik olarak receipts/ klasörüne kaydedilir

### 📊 Raporlar & Analitik

- Günlük satış özeti (toplam miktar & gelir)

- Tarih aralığına göre satış raporu

- Düşük stok raporu

### CSV dışa aktarma:

- Ürünler

- Satışlar

- Raporlar

###🗄️ Veri Depolama

- MySQL ilişkisel veritabanı

### Tablolar:

- users

- products

- sales

### 🏗️ Sistem Mimarisi
. Katmanlı Mimari

. Sunum Katmanı
JavaFX GUI (Dashboard, tablolar, diyaloglar, menüler)

. İş Mantığı Katmanı
ProductService, SaleService, ReportService

. Veri Erişim Katmanı
DBConnection + MySQL JDBC

. Model Katmanı
Product, Sale, ReportEntry

## 📌 Not:
UI katmanı doğrudan veritabanına erişmez. Temiz mimari prensipleri uygulanmıştır.



## 🗃️ Veritabanı Şeması
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(255),
    role ENUM('admin', 'staff')
);

CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    quantity INT,
    price DECIMAL(10,2),
    supplier VARCHAR(100)
);

CREATE TABLE sales (
    sale_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT,
    user_id INT,
    quantity_sold INT,
    sale_date DATE,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

### 🛠️ Kullanılan Teknolojiler

- Java 21

- JavaFX 21

- MySQL 8+

- JDBC (mysql-connector-j)

- Eclipse 

- CSV Export

### 🚀 Kurulum ve Çalıştırma
## Gereksinimler

- Java 21+

- MySQL 8+

- Eclipse IDE

### Çalıştırma

- MySQL veritabanını oluştur

- Projeyi Eclipse’e import et

- MySQL Connector JAR ekle

- DBConnection.java içinde bağlantı bilgilerini düzenle

- Main.java → Run as Java Application

## 🔮 Gelecek Geliştirmeler

- Şifre hash’leme (bcrypt)

- Barkod tarama desteği

- PDF fiş oluşturma

- Kullanıcı aktivite logları

- Modern JavaFX CSS tema

- Çoklu dil desteği

- Bulut yedekleme

## 🤝 Katkıda Bulunma

- Repo’yu forkla

- Yeni bir branch oluştur

- Değişiklikleri commit et

- Pull Request gönder

## 📄 Lisans

Bu proje MIT Lisansı ile lisanslanmıştır.
Detaylar için LICENSE dosyasına bakınız.

### “Bu proje, JavaFX ve MySQL kullanılarak geliştirilmiş, rol tabanlı erişime sahip, profesyonel fiş üretimi ve raporlama özellikleri sunan tam kapsamlı bir masaüstü envanter yönetim sistemidir.”

## 📬 İletişim

Sorular ve öneriler için:

GitHub Issues

Proje deposu üzerinden iletişim
