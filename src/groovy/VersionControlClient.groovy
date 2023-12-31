@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import groovyx.net.http.RESTClient

class VersionControlClient {

    private static final String REMOTE_API_URL = "https://example.com/version-control-api"
    private static final String DOWNLOAD_URL = "https://example.com/download/"

    void checkAndUpdateVersion(String localVersionId) {
        try {
            def localVersion = loadLocalVersion(localVersionId)
            def remoteVersion = getRemoteVersion(localVersionId)

            if (remoteVersion != null && isNewVersionAvailable(localVersion, remoteVersion)) {
                downloadAndExtractNewVersion(remoteVersion)
                updateLocalVersion(remoteVersion)
                notifyUpdateCompleted()
            } else {
                println("Uygulama güncel.")
            }
        } catch (Exception e) {
            handleException(e)
        }
    }

    def loadLocalVersion(String localVersionId) {
        def localVersionFile = new File("localVersion.json")
        if (localVersionFile.exists()) {
            def objectMapper = new groovy.json.JsonSlurper()
            return objectMapper.parse(localVersionFile)
        } else {
            return [:]
        }
    }

    def getRemoteVersion(String localVersionId) {
        def remoteApiUrl = "${REMOTE_API_URL}/${localVersionId}"
        def json = apiGetRequest(remoteApiUrl)
        return convertJsonToVersion(json)
    }

    boolean isNewVersionAvailable(localVersion, remoteVersion) {
        return localVersion.version != remoteVersion.version
    }

    void downloadAndExtractNewVersion(remoteVersion) {
        def downloadUrl = "${DOWNLOAD_URL}${remoteVersion.id}.zip"
        def zipContent = downloadFile(downloadUrl)

        def tempDirectory = Files.createTempDirectory("update_temp")
        def zipFilePath = tempDirectory.resolve("${remoteVersion.id}.zip")
        Files.write(zipFilePath, zipContent.bytes)
        unzip(zipFilePath.toString(), tempDirectory.toString())
        Files.delete(zipFilePath)

        moveFiles(tempDirectory.toString(), "${System.getProperty('user.home')}/Desktop/NewVersion")
    }

    void moveFiles(sourceDirectory, destinationDirectory) {
        def sourceDir = new File(sourceDirectory)
        sourceDir.eachFile { file ->
            def dest = new File("${destinationDirectory}/${file.name}")
            file.renameTo(dest)
        }
    }

    void updateLocalVersion(remoteVersion) {
        def localVersionFile = new File("localVersion.json")
        def objectMapper = new groovy.json.JsonBuilder(remoteVersion)
        localVersionFile.text = objectMapper.toPrettyString()
    }

    void notifyUpdateCompleted() {
        println("Güncelleme tamamlandı.")
    }

    def apiGetRequest(url) {
        def restClient = new RESTClient(url)
        def response = restClient.get()
        return response.data.text
    }

    def downloadFile(url) {
        new URL(url).getText()
    }

    void handleException(Exception e) {
        println("Error: ${e.message}")
    }

    static void main(String[] args) {
        def versionControlClient = new VersionControlClient()

        // Örnek kullanım
        versionControlClient.checkAndUpdateVersion("1")
    }
}
