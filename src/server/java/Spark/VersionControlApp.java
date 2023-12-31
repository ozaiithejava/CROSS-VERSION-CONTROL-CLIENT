import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sparkjava.ResponseTransformer;
import com.sparkjava.Spark;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VersionControlApp {

    private static Map<String, Version> versions = new HashMap<>();

    public static void main(String[] args) {
        // Örnek veriler ekleyelim
        versions.put("1", new Version("1", "1.2.3", "2023-07-15", "https://example.com/downloads/1.2.3/app.zip"));

        // Spark uygulamasını başlatma
        Spark.port(4567);

        // Sürüm bilgisini getiren endpoint
        Spark.get("/version", "application/json", (request, response) -> {
            response.type("application/json");
            return versions.get("1");
        }, new JsonTransformer());

        // İndirme linkini kontrol eden endpoint
        Spark.get("/download/:versionId", (request, response) -> {
            String requestedVersionId = request.params(":versionId");
            Version requestedVersion = versions.get(requestedVersionId);

            if (requestedVersion != null) {
                response.redirect(requestedVersion.getDownloadUrl());
                return "";
            } else {
                response.status(404);
                response.type("application/json");
                return "{\"error\":\"Requested version not found.\"}";
            }
        });

        // Diğer durumlarda 404 hatası döndürür
        Spark.notFound((request, response) -> {
            response.status(404);
            response.type("application/json");
            return "{\"error\":\"Not Found\"}";
        });
    }

    static class Version {
        private String versionId;
        private String version;
        private String releaseDate;
        private String downloadUrl;

        public Version(String versionId, String version, String releaseDate, String downloadUrl) {
            this.versionId = versionId;
            this.version = version;
            this.releaseDate = releaseDate;
            this.downloadUrl = downloadUrl;
        }

        public String getVersionId() {
            return versionId;
        }

        public String getVersion() {
            return version;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }
    }

    static class JsonTransformer implements ResponseTransformer {
        private ObjectMapper objectMapper = new ObjectMapper();

        public JsonTransformer() {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        @Override
        public String render(Object model) throws IOException {
            return objectMapper.writeValueAsString(model);
        }
    }
}
