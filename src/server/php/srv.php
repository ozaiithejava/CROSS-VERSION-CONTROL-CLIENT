<?php

$versions = [
    [
        "versionId" => "1",
        "version" => "1.2.3",
        "releaseDate" => "2023-07-15",
        "downloadUrl" => "https://example.com/downloads/1.2.3/app.zip"
    ]
];

// Sürüm bilgisini getiren endpoint
if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['action']) && $_GET['action'] === 'version') {
    echo json_encode($versions[0]);
    exit;
}

// İndirme linkini kontrol eden endpoint
if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['action']) && $_GET['action'] === 'download') {
    if (isset($_GET['versionId'])) {
        $requestedVersionId = $_GET['versionId'];
        foreach ($versions as $version) {
            if ($version['versionId'] === $requestedVersionId) {
                header("Location: " . $version['downloadUrl']);
                exit;
            }
        }
    }

    // Eğer belirtilen sürüm bulunamazsa 404 hatası döndürür
    http_response_code(404);
    echo json_encode(["error" => "Requested version not found."]);
    exit;
}

// Diğer durumlarda 404 hatası döndürür
http_response_code(404);
echo json_encode(["error" => "Not Found"]);
exit;
