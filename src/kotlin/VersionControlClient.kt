import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.*
import java.net.URL
import java.nio.file.*
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class VersionControlClient {

    private val REMOTE_API_URL = "https://example.com/version-control-api"
    private val DOWNLOAD_URL = "https://example.com/download/"

    /**
     * Uygulamanın yerel sürümünü kontrol eder ve gerekirse günceller.
     *
     * @param localVersionId Uygulamanın yerel sürümünün kimliği.
     */
    fun checkAndUpdateVersion(localVersionId: String) {
        try {
            val localVersion = loadLocalVersion(localVersionId)
            val remoteVersion = getRemoteVersion(localVersionId)

            if (remoteVersion != null && isNewVersionAvailable(localVersion, remoteVersion)) {
                downloadAndExtractNewVersion(remoteVersion)
                updateLocalVersion(remoteVersion)
                notifyUpdateCompleted()
            } else {
                println("Uygulama güncel.")
            }
        } catch (e: IOException) {
            handleException(e)
        }
    }

    /**
     * Yerel sürüm bilgisini yükler.
     *
     * @param localVersionId Uygulamanın yerel sürümünün kimliği.
     * @return Yerel sürüm bilgisi.
     * @throws IOException Dosya okuma hatası durumunda fırlatılır.
     */
    private fun loadLocalVersion(localVersionId: String): Version {
        val localVersionFile = File("localVersion.json")
        return if (localVersionFile.exists()) {
            val objectMapper = ObjectMapper()
            objectMapper.readValue(localVersionFile, Version::class.java)
        } else {
            Version()
        }
    }

    /**
     * Uzaktaki sürüm bilgisini alır.
     *
     * @param localVersionId Uygulamanın yerel sürümünün kimliği.
     * @return Uzaktaki sürüm bilgisi.
     * @throws IOException API isteği veya JSON dönüşüm hatası durumunda fırlatılır.
     */
    private fun getRemoteVersion(localVersionId: String): Version? {
        val remoteApiUrl = "$REMOTE_API_URL/$localVersionId"
        val json = apiGetRequest(remoteApiUrl)
        return convertJsonToVersion(json)
    }

    /**
     * Yerel sürüm ile uzaktaki sürümü karşılaştırır.
     *
     * @param localVersion  Yerel sürüm bilgisi.
     * @param remoteVersion Uzaktaki sürüm bilgisi.
     * @return Yeni sürüm bulunuyorsa true, aksi takdirde false.
     */
    private fun isNewVersionAvailable(localVersion: Version, remoteVersion: Version): Boolean {
        return localVersion.version != remoteVersion.version
    }

    /**
     * Yeni bir sürüm indirir ve çıkartır.
     *
     * @param remoteVersion İndirilecek ve çıkartılacak yeni sürüm bilgisi.
     * @throws IOException İndirme veya dosya işleme hatası durumunda fırlatılır.
     */
    private fun downloadAndExtractNewVersion(remoteVersion: Version) {
        val downloadUrl = "$DOWNLOAD_URL${remoteVersion.id}.zip"
        val zipContent = downloadFile(downloadUrl)

        val tempDirectory = Files.createTempDirectory("update_temp")
        val zipFilePath = tempDirectory.resolve("${remoteVersion.id}.zip")
        Files.write(zipFilePath, zipContent)
        unzip(zipFilePath.toString(), tempDirectory.toString())
        Files.delete(zipFilePath)

        moveFiles(tempDirectory.toString(), "${System.getProperty("user.home")}/Desktop/NewVersion")
    }

    /**
     * Dosyaları taşır.
     *
     * @param sourceDirectory      Kaynak dizin.
     * @param destinationDirectory Hedef dizin.
     * @throws IOException Taşıma işlemi sırasında hata durumunda fırlatılır.
     */
    private fun moveFiles(sourceDirectory: String, destinationDirectory: String) {
        Files.walk(Paths.get(sourceDirectory))
             .forEach { source ->
                 val destination = Paths.get(destinationDirectory, source.toString().substring(sourceDirectory.length))
                 try {
                     Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
                 } catch (e: IOException) {
                     handleException(e)
                 }
             }
    }

    /**
     * Yerel sürüm dosyasını günceller.
     *
     * @param remoteVersion Güncellenecek sürüm bilgisi.
     * @throws IOException Dosya yazma hatası durumunda fırlatılır.
     */
    private fun updateLocalVersion(remoteVersion: Version) {
        val localVersionFile = File("localVersion.json")
        val objectMapper = ObjectMapper()
        objectMapper.writeValue(localVersionFile, remoteVersion)
    }

    /**
     * Güncelleme tamamlandı mesajı verir.
     */
    private fun notifyUpdateCompleted() {
        println("Güncelleme tamamlandı.")
    }

    /**
     * API'ye GET isteği gönderir ve cevabı alır.
     *
     * @param url API URL'si.
     * @return API'den alınan JSON cevabı.
     * @throws IOException API isteği veya dosya işleme hatası durumunda fırlatılır.
     */
    private fun apiGetRequest(url: String): String {
        try (reader: BufferedReader = BufferedReader(InputStreamReader(URL(url).openStream()))) {
            val content = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                content.append(line)
            }
            return content.toString()
        }
    }

    /**
     * Dosya indirir.
     *
     * @param url İndirilecek dosyanın URL'si.
     * @return İndirilen dosyanın içeriği.
     * @throws IOException İndirme işlemi sırasında hata durumunda fırlatılır.
     */
    private fun downloadFile(url: String): ByteArray {
        try (inStream: InputStream = URL(url).openStream()) {
            val outStream = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                outStream.write(buffer, 0, bytesRead)
            }
            return outStream.toByteArray()
        }
    }

    /**
     * JSON formatındaki metni Version nesnesine çevirir.
     *
     * @param json JSON formatındaki metin.
     * @return Version nesnesi.
     * @throws IOException JSON dönüşüm hatası durumunda fırlatılır.
     */
    private fun convertJsonToVersion(json: String): Version {
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(json, Version::class.java)
    }

    /**
     * ZIP dosyasını çıkartır.
     *
     * @param zipFilePath      ZIP dosyasının yolu.
     * @param destDirectory    Çıkartılacak dosyaların hedef dizini.
     * @throws IOException Çıkartma işlemi sırasında hata durumunda fırlatılır.
     */
    private fun unzip(zipFilePath: String, destDirectory: String) {
        ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val filePath = "$destDirectory${File.separator}${entry!!.name}"
                if (!entry!!.isDirectory) {
                    extractFile(zipIn, filePath)
                } else {
                    File(filePath).mkdir()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
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
    private fun extractFile(zipIn: ZipInputStream, filePath: String) {
        BufferedOutputStream(FileOutputStream(filePath)).use { bos ->
            val bytesIn = ByteArray(4096)
            var read: Int
            while (zipIn.read(bytesIn).also { read = it } != -1) {
                bos.write(bytesIn, 0, read)
            }
        }
    }

    /**
     * Hata durumunu işler.
     *
     * @param e Fırlatılan hata.
     */
    private fun handleException(e: Exception) {
        System.err.println("Error: ${e.message}")
    }

}

data class Version(
    val id: String = "",
    val version: String = "",
    @JsonFormat(pattern = "yyyy-MM-dd")
    val releaseDate: LocalDate = LocalDate.now()
)
