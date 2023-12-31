import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionControlClient {

    private static final String REMOTE_API_URL = "https://example.com/version-control-api";
    private static final String DOWNLOAD_URL = "https://example.com/download/";

    /**
     * Uygulamanın yerel sürümünü kontrol eder ve gerekirse günceller.
     *
     * @param localVersionId Uygulamanın yerel sürümünün kimliği.
     */
    public void checkAndUpdateVersion(String localVersionId) {
        try {
            Version localVersion = loadLocalVersion(localVersionId);
            Version remoteVersion = getRemoteVersion(localVersionId);

            if (remoteVersion != null && isNewVersionAvailable(localVersion, remoteVersion)) {
                downloadAndExtractNewVersion(remoteVersion);
                updateLocalVersion(remoteVersion);
                notifyUpdateCompleted();
            } else {
                System.out.println("Uygulama güncel.");
            }
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Yerel sürüm bilgisini yükler.
     *
     * @param localVersionId Uygulamanın yerel sürümünün kimliği.
     * @return Yerel sürüm bilgisi.
     * @throws IOException Dosya okuma hatası durumunda fırlatılır.
     */
    private Version loadLocalVersion(String localVersionId) throws IOException {
        File localVersionFile = new File("localVersion.json");
        if (localVersionFile.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(localVersionFile, Version.class);
        } else {
            return new Version();
        }
    }

    /**
     * Uzaktaki sürüm bilgisini alır.
     *
     * @param localVersionId Uygulamanın yerel sürümünün kimliği.
     * @return Uzaktaki sürüm bilgisi.
     * @throws IOException API isteği veya JSON dönüşüm hatası durumunda fırlatılır.
     */
    private Version getRemoteVersion(String localVersionId) throws IOException {
        String remoteApiUrl = REMOTE_API_URL + "/" + localVersionId;
        String json = apiGetRequest(remoteApiUrl);
        return convertJsonToVersion(json);
    }

    /**
     * Yerel sürüm ile uzaktaki sürümü karşılaştırır.
     *
     * @param localVersion  Yerel sürüm bilgisi.
     * @param remoteVersion Uzaktaki sürüm bilgisi.
     * @return Yeni sürüm bulunuyorsa true, aksi takdirde false.
     */
    private boolean isNewVersionAvailable(Version localVersion, Version remoteVersion) {
        return !localVersion.getVersion().equals(remoteVersion.getVersion());
    }

    /**
     * Yeni bir sürüm indirir ve çıkartır.
     *
     * @param remoteVersion İndirilecek ve çıkartılacak yeni sürüm bilgisi.
     * @throws IOException İndirme veya dosya işleme hatası durumunda fırlatılır.
     */
    private void downloadAndExtractNewVersion(Version remoteVersion) throws IOException {
        String downloadUrl = DOWNLOAD_URL + remoteVersion.getId() + ".zip";
        byte[] zipContent = downloadFile(downloadUrl);

        Path tempDirectory = Files.createTempDirectory("update_temp");
        Path zipFilePath = tempDirectory.resolve(remoteVersion.getId() + ".zip");
        Files.write(zipFilePath, zipContent);
        unzip(zipFilePath.toString(), tempDirectory.toString());
        Files.delete(zipFilePath);

        moveFiles(tempDirectory.toString(), System.getProperty("user.home") + "/Desktop/NewVersion");
    }

    /**
     * Dosyaları taşır.
     *
     * @param sourceDirectory      Kaynak dizin.
     * @param destinationDirectory Hedef dizin.
     * @throws IOException Taşıma işlemi sırasında hata durumunda fırlatılır.
     */
    private void moveFiles(String sourceDirectory, String destinationDirectory) throws IOException {
        Files.walk(Paths.get(sourceDirectory))
             .forEach(source -> {
                 Path destination = Paths.get(destinationDirectory, source.toString().substring(sourceDirectory.length()));
                 try {
                     Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
                 } catch (IOException e) {
                     handleException(e);
                 }
             });
    }

    /**
     * Yerel sürüm dosyasını günceller.
     *
     * @param remoteVersion Güncellenecek sürüm bilgisi.
     * @throws IOException Dosya yazma hatası durumunda fırlatılır.
     */
    private void updateLocalVersion(Version remoteVersion) throws IOException {
        File localVersionFile = new File("localVersion.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(localVersionFile, remoteVersion);
    }

    /**
     * Güncelleme tamamlandı mesajı verir.
     */
    private void notifyUpdateCompleted() {
        System.out.println("Güncelleme tamamlandı.");
    }

    /**
     * API'ye GET isteği gönderir ve cevabı alır.
     *
     * @param url API URL'si.
     * @return API'den alınan JSON cevabı.
     * @throws IOException API isteği veya dosya işleme hatası durumunda fırlatılır.
     */
    private String apiGetRequest(String url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        }
    }

    /**
     * Dosya indirir.
     *
     * @param url İndirilecek dosyanın URL'si.
     * @return İndirilen dosyanın içeriği.
     * @throws IOException İndirme işlemi sırasında hata durumunda fırlatılır.
     */
    private byte[] downloadFile(String url) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    /**
     * JSON formatındaki metni Version nesnesine çevirir.
     *
     * @param json JSON formatındaki metin.
     * @return Version nesnesi.
     * @throws IOException JSON dönüşüm hatası durumunda fırlatılır.
     */
    private Version convertJsonToVersion(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Version.class);
    }

    /**
     * ZIP dosyasını çıkartır.
     *
     * @param zipFilePath      ZIP dosyasının yolu.
     * @param destDirectory    Çıkartılacak dosyaların hedef dizini.
     * @throws IOException Çıkartma işlemi sırasında hata durumunda fırlatılır.
     */
    private void unzip(String zipFilePath, String destDirectory) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    /**
     * ZIP dosyasındaki bir dosyayı çıkartır.
     *
     * @param zipIn     ZIP dosyasının giriş akışı.
     * @param filePath  Çıkartılacak dosyanın yolu.
     * @throws IOException Çıkartma işlemi sırasında hata durumunda fırlatılır.
     */
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    /**
     * Hata durumunu işler.
     *
     * @param e Fırlatılan hata.
     */
    private void handleException(Exception e) {
        System.err.println("Error: " + e.getMessage());
    }
}
