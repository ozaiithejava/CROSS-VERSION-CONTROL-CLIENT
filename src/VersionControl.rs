use serde::{Deserialize, Serialize};
use std::fs::{self, File};
use std::io::{self, BufRead, BufReader, Write};
use std::net::TcpStream;
use std::path::{Path, PathBuf};
use std::str;
use std::thread;

const REMOTE_API_URL: &str = "https://example.com/version-control-api";
const DOWNLOAD_URL: &str = "https://example.com/download/";

#[derive(Debug, Serialize, Deserialize)]
struct Version {
    id: String,
    version: String,
    #[serde(with = "my_date_format")]
    release_date: chrono::NaiveDate,
}

mod my_date_format {
    use chrono::NaiveDate;
    use serde::{self, Deserialize, Deserializer, Serializer};

    const FORMAT: &str = "yyyy-MM-dd";

    pub fn serialize<S>(date: &NaiveDate, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let s = date.format(FORMAT).to_string();
        serializer.serialize_str(&s)
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<NaiveDate, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s = String::deserialize(deserializer)?;
        NaiveDate::parse_from_str(&s, FORMAT).map_err(serde::de::Error::custom)
    }
}

struct VersionControlClient;

impl VersionControlClient {
    fn check_and_update_version(&self, local_version_id: &str) {
        if let Ok(local_version) = self.load_local_version(local_version_id) {
            if let Some(remote_version) = self.get_remote_version(local_version_id) {
                if self.is_new_version_available(&local_version, &remote_version) {
                    if let Ok(new_version_dir) = self.download_and_extract_new_version(&remote_version) {
                        self.move_files(&new_version_dir, &self.new_version_destination());
                        self.update_local_version(&remote_version);
                        self.notify_update_completed();
                    } else {
                        eprintln!("Failed to download and extract new version.");
                    }
                } else {
                    println!("Uygulama güncel.");
                }
            } else {
                eprintln!("Failed to get remote version information.");
            }
        } else {
            eprintln!("Failed to load local version information.");
        }
    }

    fn load_local_version(&self, local_version_id: &str) -> io::Result<Version> {
        let local_version_path = Path::new("localVersion.json");
        if local_version_path.exists() {
            let file = File::open(local_version_path)?;
            let reader = BufReader::new(file);
            Ok(serde_json::from_reader(reader)?)
        } else {
            Ok(Version::default())
        }
    }

    fn get_remote_version(&self, local_version_id: &str) -> Option<Version> {
        let remote_api_url = format!("{}/{}", REMOTE_API_URL, local_version_id);
        if let Ok(response) = reqwest::blocking::get(&remote_api_url) {
            if let Ok(json) = response.text() {
                return serde_json::from_str(&json).ok();
            }
        }
        None
    }

    fn is_new_version_available(&self, local_version: &Version, remote_version: &Version) -> bool {
        local_version.version != remote_version.version
    }

    fn download_and_extract_new_version(&self, remote_version: &Version) -> io::Result<PathBuf> {
        let download_url = format!("{}{}.zip", DOWNLOAD_URL, remote_version.id);
        let zip_content = reqwest::blocking::get(&download_url)?.bytes()?;
        let temp_dir = self.new_version_temp_directory();
        let zip_file_path = temp_dir.join(format!("{}.zip", remote_version.id));
        fs::write(&zip_file_path, &zip_content)?;

        self.unzip(&zip_file_path, &temp_dir)?;

        Ok(temp_dir)
    }

    fn move_files(&self, source_directory: &Path, destination_directory: &Path) {
        fs::create_dir_all(destination_directory).ok();
        if let Ok(entries) = fs::read_dir(source_directory) {
            for entry in entries {
                if let Ok(entry) = entry {
                    let destination = destination_directory.join(entry.file_name());
                    fs::rename(entry.path(), &destination).ok();
                }
            }
        }
    }

    fn update_local_version(&self, remote_version: &Version) {
        let local_version_path = Path::new("localVersion.json");
        let file = File::create(local_version_path).expect("Failed to create local version file");
        serde_json::to_writer(file, remote_version).expect("Failed to write local version file");
    }

    fn notify_update_completed(&self) {
        println!("Güncelleme tamamlandı.");
    }

    fn new_version_temp_directory(&self) -> PathBuf {
        let temp_dir = env::temp_dir().join("update_temp");
        fs::create_dir_all(&temp_dir).expect("Failed to create temp directory");
        temp_dir
    }

    fn new_version_destination(&self) -> PathBuf {
        let desktop_dir = dirs::desktop_dir().expect("Failed to get desktop directory");
        desktop_dir.join("NewVersion")
    }

    fn unzip(&self, zip_file_path: &Path, dest_directory: &Path) -> io::Result<()> {
        let file = File::open(zip_file_path)?;
        let reader = BufReader::new(file);
        let mut archive = zip::ZipArchive::new(reader)?;

        for i in 0..archive.len() {
            let mut file = archive.by_index(i)?;
            let file_path = dest_directory.join(file.sanitized_name());

            if file.is_dir() {
                fs::create_dir_all(&file_path)?;
            } else {
                if let Some(parent) = file_path.parent() {
                    fs::create_dir_all(parent)?;
                }
                let mut new_file = File::create(&file_path)?;
                io::copy(&mut file, &mut new_file)?;
            }
        }

        Ok(())
    }
}

fn main() {
    let client = VersionControlClient;
    let local_version_id = "your_local_version_id_here";
    client.check_and_update_version(local_version_id);
}
