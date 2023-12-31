# Java-version-control
if relased new version check and dowland

### Api Json 
```json
{
  "id": "1",
  "version": "1.0.0",
  "releaseDate": "2023-01-01"
}
```

## Usage:
```Java
/**
 * Uygulamanın sürüm kontrolünü gerçekleştiren ve güncelleme işlemlerini yöneten ana sınıf.
 */
public class VersionControlMain {

    /**
     * Uygulamanın ana giriş noktası. Sürüm kontrolü yapılır ve gerekirse güncelleme işlemi başlatılır.
     *
     * @param args Komut satırı argümanları (kullanılmıyor).
     */
    public static void main(String[] args) {
        // VersionControlClient sınıfının bir örneğini oluşturuyoruz.
        VersionControlClient versionControlClient = new VersionControlClient();

        // Örnek kullanım: "1" sürümünü kontrol eder ve güncelleme işlemi başlatır.
        versionControlClient.checkAndUpdateVersion("1");
    }
}
```
