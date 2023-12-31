import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper

import java.io._
import java.net.URL
import java.nio.file._
import java.time.LocalDate
import java.util.zip.{ZipEntry, ZipInputStream}

object VersionControlClient {

  private val REMOTE_API_URL = "https://example.com/version-control-api"
  private val DOWNLOAD_URL = "https://example.com/download/"

  def main(args: Array[String]): Unit = {
    val versionControlClient = new VersionControlClient
    // Örnek kullanım
    versionControlClient.checkAndUpdateVersion("1")
  }
}

class VersionControlClient {

  /**
   * Uygulamanın yerel sürümünü kontrol eder ve gerekirse günceller.
   *
   * @param localVersionId Uygulamanın yerel sürümünün kimliği.
   */
  def checkAndUpdateVersion(localVersionId: String): Unit = {
    try {
      val localVersion = loadLocalVersion(localVersionId)
      val remoteVersion = getRemoteVersion(localVersionId)

      if (remoteVersion.nonEmpty && isNewVersionAvailable(localVersion, remoteVersion.get)) {
        downloadAndExtractNewVersion(remoteVersion.get)
        updateLocalVersion(remoteVersion.get)
        notifyUpdateCompleted()
      } else {
        println("Uygulama güncel.")
      }
    } catch {
      case e: IOException => handleException(e)
    }
  }

  /**
   * Yerel sürüm bilgisini yükler.
   *
   * @param localVersionId Uygulamanın yerel sürümünün kimliği.
   * @return Yerel sürüm bilgisi.
   * @throws IOException Dosya okuma hatası durumunda fırlatılır.
   */
  private def loadLocalVersion(localVersionId: String): Version = {
    val localVersionFile = new File("localVersion.json")
    if (localVersionFile.exists()) {
      val objectMapper = new ObjectMapper
      objectMapper.readValue(localVersionFile, classOf[Version])
    } else {
      new Version
    }
  }

  /**
   * Uzaktaki sürüm bilgisini alır.
   *
   * @param localVersionId Uygulamanın yerel sürümünün kimliği.
   * @return Uzaktaki sürüm bilgisi.
   * @throws IOException API isteği veya JSON dönüşüm hatası durumunda fırlatılır.
   */
  private def getRemoteVersion(localVersionId: String): Option[Version] = {
    val remoteApiUrl = s"$REMOTE_API_URL/$localVersionId"
    val json = apiGetRequest(remoteApiUrl)
    convertJsonToVersion(json)
  }

  /**
   * Yerel sürüm ile uzaktaki sürümü karşılaştırır.
   *
   * @param localVersion  Yerel sürüm bilgisi.
   * @param remoteVersion Uzaktaki sürüm bilgisi.
   * @return Yeni sürüm bulunuyorsa true, aksi takdirde false.
   */
  private def isNewVersionAvailable(localVersion: Version, remoteVersion: Version): Boolean = {
    localVersion.version != remoteVersion.version
  }

  /**
   * Yeni bir sürüm indirir ve çıkartır.
   *
   * @param remoteVersion İndirilecek ve çıkartılacak yeni sürüm bilgisi.
   * @throws IOException İndirme veya dosya işleme hatası durumunda fırlatılır.
   */
  private def downloadAndExtractNewVersion(remoteVersion: Version): Unit = {
    val downloadUrl = s"$DOWNLOAD_URL${remoteVersion.id}.zip"
    val zipContent = downloadFile(downloadUrl)

    val tempDirectory = Files.createTempDirectory("update_temp")
    val zipFilePath = tempDirectory.resolve(s"${remoteVersion.id}.zip")
    Files.write(zipFilePath, zipContent)
    unzip(zipFilePath.toString, tempDirectory.toString)
    Files.delete(zipFilePath)

    moveFiles(tempDirectory.toString, s"${System.getProperty("user.home")}/Desktop/NewVersion")
  }

  /**
   * Dosyaları taşır.
   *
   * @param sourceDirectory      Kaynak dizin.
   * @param destinationDirectory Hedef dizin.
   * @throws IOException Taşıma işlemi sırasında hata durumunda fırlatılır.
   */
  private def moveFiles(sourceDirectory: String, destinationDirectory: String): Unit = {
    Files.walk(Paths.get(sourceDirectory)).forEach { source =>
      val destination = Paths.get(destinationDirectory, source.toString.substring(sourceDirectory.length))
      try {
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
      } catch {
        case e: IOException => handleException(e)
      }
    }
  }

  /**
   * Yerel sürüm dosyasını günceller.
   *
   * @param remoteVersion Güncellenecek sürüm bilgisi.
   * @throws IOException Dosya yazma hatası durumunda fırlatılır.
   */
  private def updateLocalVersion(remoteVersion: Version): Unit = {
    val localVersionFile = new File("localVersion.json")
    val objectMapper = new ObjectMapper
    objectMapper.writeValue(localVersionFile, remoteVersion)
  }

  /**
   * Güncelleme tamamlandı mesajı verir.
   */
  private def notifyUpdateCompleted(): Unit = {
    println("Güncelleme tamamlandı.")
  }

  /**
   * API'ye GET isteği gönderir ve cevabı alır.
   *
   * @param url API URL'si.
   * @return API'den alınan JSON cevabı.
   * @throws IOException API isteği veya dosya işleme hatası durumunda fırlatılır.
   */
  private def apiGetRequest(url: String): String = {
    val reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))
    try {
      val content = new StringBuilder
      var line: String = reader.readLine()
      while (line != null) {
        content.append(line)
        line = reader.readLine()
      }
      content.toString
    } finally {
      reader.close()
    }
  }

  /**
   * Dosya indirir.
   *
   * @param url İndirilecek dosyanın URL'si.
   * @return İndirilen dosyanın içeriği.
   * @throws IOException İndirme işlemi sırasında hata durumunda fırlatılır.
   */
  private def downloadFile(url: String): Array[Byte] = {
    val inStream = new URL(url).openStream()
    try {
      val outStream = new ByteArrayOutputStream
      val buffer = new Array[Byte](4096)
      var bytesRead: Int = inStream.read(buffer)
      while (bytesRead != -1) {
        outStream.write(buffer, 0, bytesRead)
        bytesRead = inStream.read(buffer)
      }
      outStream.toByteArray
    } finally {
      inStream.close()
    }
  }

  /**
   * JSON formatındaki metni Version nesnesine çevirir.
   *
   * @param json JSON formatındaki metin.
   * @return Version nesnesi.
   * @throws IOException JSON dönüşüm hatası durumunda fırlatılır.
   */
  private def convertJsonToVersion(json: String): Option[Version] = {
    val objectMapper = new ObjectMapper
    try {
      Some(objectMapper.readValue(json, classOf[Version]))
    } catch {
      case e: IOException => None
    }
  }

  /**
   * ZIP dosyasını çıkartır.
   *
   * @param zipFilePath      ZIP dosyasının yolu.
   * @param destDirectory    Çıkartılacak dosyaların hedef dizini.
   * @throws IOException Çıkartma işlemi sırasında hata durumunda fırlatılır.
   */
  private def unzip(zipFilePath: String, destDirectory: String): Unit = {
    val zipIn = new ZipInputStream(new FileInputStream(zipFilePath))
    try {
      var entry: ZipEntry = zipIn.getNextEntry
      while (entry != null) {
        val filePath = destDirectory + File.separator + entry.getName
        if (!entry.isDirectory) {
          extractFile(zipIn, filePath)
        } else {
          new File(filePath).mkdir()
        }
        zipIn.closeEntry()
        entry = zipIn.getNextEntry
      }
    } finally {
      zipIn.close()
    }
  }

  /**
   * ZIP dosyasındaki bir dosyayı çıkartır.
   *
   * @param zipIn     ZIP dosyasının giriş akışı.
   * @param filePath  Çıkartılacak dosyanın yolu.
   * @throws IOException Çıkartma işlemi sırasında hata durumunda fırlatılır.
   */
  private def extractFile(zipIn: ZipInputStream, filePath: String): Unit = {
    val bos = new BufferedOutputStream(new FileOutputStream(filePath))
    try {
      val bytesIn = new Array[Byte](4096)
      var read: Int = zipIn.read(bytesIn)
      while (read != -1) {
        bos.write(bytesIn, 0, read)
        read = zipIn.read(bytesIn)
      }
    } finally {
      bos.close()
    }
  }

  /**
   * Hata durumunu işler.
   *
   * @param e Fırlatılan hata.
   */
  private def handleException(e: Exception): Unit = {
    System.err.println(s"Error: ${e.getMessage}")
  }
}

case class Version(
  id: String = "",
  version: String = "",
  @JsonFormat(pattern = "yyyy-MM-dd")
  releaseDate: LocalDate = LocalDate.now
)
