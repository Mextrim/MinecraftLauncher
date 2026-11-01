use std::collections::HashMap;
use std::env;
use std::fs::{self, File};
use std::io::{BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;
use rayon::prelude::*;
use serde::{Deserialize, Serialize};

const MANIFEST_URL: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
const RESOURCES_BASE_URL: &str = "https://resources.download.minecraft.net/";

#[derive(Debug, Serialize, Deserialize)]
struct VersionManifest {
    latest: LatestVersion,
    versions: Vec<VersionEntry>,
}

#[derive(Debug, Serialize, Deserialize)]
struct LatestVersion {
    release: String,
    snapshot: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct VersionEntry {
    id: String,
    #[serde(rename = "type")]
    vtype: String,
    url: String,
    #[serde(rename = "releaseTime")]
    release_time: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct VersionDetail {
    id: String,
    #[serde(rename = "type")]
    vtype: String,
    #[serde(rename = "mainClass")]
    main_class: String,
    #[serde(rename = "minecraftArguments")]
    minecraft_arguments: Option<String>,
    arguments: Option<serde_json::Value>,
    #[serde(rename = "assetIndex")]
    asset_index: Option<AssetIndexRef>,
    assets: Option<String>,
    downloads: DownloadsInfo,
    libraries: Vec<LibraryEntry>,
}

#[derive(Debug, Serialize, Deserialize)]
struct AssetIndexRef {
    id: String,
    sha1: String,
    size: u64,
    url: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct DownloadsInfo {
    client: Option<DownloadArtifact>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct DownloadArtifact {
    path: Option<String>,
    sha1: Option<String>,
    size: Option<u64>,
    url: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct LibraryEntry {
    name: String,
    downloads: Option<LibraryDownloads>,
    natives: Option<HashMap<String, String>>,
    rules: Option<Vec<RuleEntry>>,
}

#[derive(Debug, Serialize, Deserialize)]
struct LibraryDownloads {
    artifact: Option<DownloadArtifact>,
    classifiers: Option<HashMap<String, DownloadArtifact>>,
}

#[derive(Debug, Serialize, Deserialize)]
struct RuleEntry {
    action: String,
    os: Option<RuleOs>,
    features: Option<HashMap<String, bool>>,
}

#[derive(Debug, Serialize, Deserialize)]
struct RuleOs {
    name: Option<String>,
    arch: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct AssetIndexData {
    objects: HashMap<String, AssetObject>,
}

#[derive(Debug, Serialize, Deserialize)]
struct AssetObject {
    hash: String,
    size: u64,
}

#[derive(Debug, Serialize, Deserialize)]
struct JavaInfo {
    name: String,
    path: String,
    major_version: u32,
}

#[derive(Debug, Serialize)]
struct ProgressMessage {
    r#type: String,
    status: String,
    percent: f64,
}

#[derive(Debug, Serialize)]
struct LogMessage {
    r#type: String,
    line: String,
}

fn emit_progress(status: &str, percent: f64) {
    let msg = ProgressMessage {
        r#type: "progress".to_string(),
        status: status.to_string(),
        percent,
    };
    if let Ok(json) = serde_json::to_string(&msg) {
        println!("{}", json);
        let _ = std::io::stdout().flush();
    }
}

fn emit_log(line: &str) {
    let msg = LogMessage {
        r#type: "log".to_string(),
        line: line.to_string(),
    };
    if let Ok(json) = serde_json::to_string(&msg) {
        println!("{}", json);
        let _ = std::io::stdout().flush();
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        eprintln!("Usage: rust-core <command> [options]");
        eprintln!("Commands: get-manifest, detect-java, download, launch");
        return;
    }

    match args[1].as_str() {
        "get-manifest" => cmd_get_manifest(),
        "detect-java" => cmd_detect_java(),
        "download" => cmd_download(&args[2..]),
        "launch" => cmd_launch(&args[2..]),
        other => eprintln!("Unknown command: {}", other),
    }
}

fn cmd_get_manifest() {
    match ureq::get(MANIFEST_URL).call() {
        Ok(res) => {
            if let Ok(manifest) = res.into_json::<VersionManifest>() {
                if let Ok(json) = serde_json::to_string(&manifest) {
                    println!("{}", json);
                }
            } else {
                eprintln!("Failed to parse version manifest");
            }
        }
        Err(e) => eprintln!("Failed to fetch version manifest: {}", e),
    }
}

fn cmd_detect_java() {
    let mut results: Vec<JavaInfo> = Vec::new();
    let mut checked: HashMap<String, bool> = HashMap::new();

    let mut check_java = |path_str: &str, label: &str| {
        let path = Path::new(path_str);
        if path.exists() {
            let key = path_str.to_lowercase();
            if !checked.contains_key(&key) {
                checked.insert(key, true);
                let ver = get_java_version(path_str);
                let name = if label.is_empty() {
                    format!("Java {} ({})", ver, path.parent().unwrap_or(path).display())
                } else {
                    format!("{} (Java {})", label, ver)
                };
                results.push(JavaInfo {
                    name,
                    path: path_str.to_string(),
                    major_version: ver,
                });
            }
        }
    };

    // JAVA_HOME
    if let Ok(home) = env::var("JAVA_HOME") {
        check_java(&format!("{}\\bin\\javaw.exe", home), "JAVA_HOME");
        check_java(&format!("{}\\bin\\java.exe", home), "JAVA_HOME");
    }

    // Standard roots
    let roots = [
        r"C:\Program Files\Java",
        r"C:\Program Files\Microsoft",
        r"C:\Program Files\Eclipse Adoptium",
        r"C:\Program Files\BellSoft",
        r"C:\Program Files\Amazon Corretto",
        r"C:\Program Files\Zulu",
        r"C:\Program Files (x86)\Java",
    ];

    for root in &roots {
        let p = Path::new(root);
        if p.exists() {
            if let Ok(entries) = fs::read_dir(p) {
                for entry in entries.flatten() {
                    let sub = entry.path();
                    if sub.is_dir() {
                        let javaw = sub.join("bin").join("javaw.exe");
                        let java = sub.join("bin").join("java.exe");
                        if javaw.exists() {
                            check_java(&javaw.to_string_lossy(), "");
                        } else if java.exists() {
                            check_java(&java.to_string_lossy(), "");
                        }
                    }
                }
            }
        }
    }

    results.sort_by(|a, b| b.major_version.cmp(&a.major_version));
    if let Ok(json) = serde_json::to_string(&results) {
        println!("{}", json);
    }
}

fn get_java_version(java_path: &str) -> u32 {
    if let Ok(output) = Command::new(java_path).arg("-version").output() {
        let text = String::from_utf8_lossy(&output.stderr) + String::from_utf8_lossy(&output.stdout);
        for line in text.lines() {
            if line.contains("version") {
                if let Some(start) = line.find('"') {
                    if let Some(end) = line[start + 1..].find('"') {
                        let ver_str = &line[start + 1..start + 1 + end];
                        let parts: Vec<&str> = ver_str.split('.').collect();
                        if !parts.is_empty() {
                            if parts[0] == "1" && parts.len() > 1 {
                                if let Ok(v) = parts[1].parse::<u32>() {
                                    return v;
                                }
                            } else if let Ok(v) = parts[0].parse::<u32>() {
                                return v;
                            }
                        }
                    }
                }
            }
        }
    }
    0
}

fn is_rule_allowed(rules: &Option<Vec<RuleEntry>>) -> bool {
    let rules = match rules {
        Some(r) => r,
        None => return true,
    };
    if rules.is_empty() {
        return true;
    }

    let mut allowed = false;
    for rule in rules {
        let mut matches = true;
        if let Some(ref os) = rule.os {
            if let Some(ref name) = os.name {
                if !name.eq_ignore_ascii_case("windows") {
                    matches = false;
                }
            }
        }
        if matches && rule.features.is_some() {
            matches = false;
        }
        if matches {
            allowed = rule.action.eq_ignore_ascii_case("allow");
        }
    }
    allowed
}

fn maven_to_path(name: &str, classifier: Option<&str>) -> PathBuf {
    let parts: Vec<&str> = name.split(':').collect();
    if parts.len() < 3 {
        return PathBuf::from(name.replace(':', "/")).with_extension("jar");
    }
    let group = parts[0].replace('.', "/");
    let artifact = parts[1];
    let version = parts[2];
    let file_name = if let Some(c) = classifier {
        format!("{}-{}-{}.jar", artifact, version, c)
    } else if parts.len() > 3 {
        format!("{}-{}-{}.jar", artifact, version, parts[3])
    } else {
        format!("{}-{}.jar", artifact, version)
    };
    Path::new(&group).join(artifact).join(version).join(file_name)
}

fn download_file(url: &str, dest: &Path) -> Result<(), String> {
    if let Some(parent) = dest.parent() {
        let _ = fs::create_dir_all(parent);
    }
    let temp_path = dest.with_extension("tmp");
    let res = ureq::get(url).call().map_err(|e| format!("HTTP request error: {}", e))?;
    let mut reader = res.into_reader();
    let mut out = File::create(&temp_path).map_err(|e| format!("File create error: {}", e))?;
    std::io::copy(&mut reader, &mut out).map_err(|e| format!("File copy error: {}", e))?;
    let _ = fs::rename(&temp_path, dest);
    Ok(())
}

fn extract_natives(jar_path: &Path, dest_dir: &Path) {
    if let Ok(file) = File::open(jar_path) {
        if let Ok(mut archive) = zip::ZipArchive::new(file) {
            for i in 0..archive.len() {
                if let Ok(mut file) = archive.by_index(i) {
                    let name = file.name().to_string();
                    if name.starts_with("META-INF") || !name.ends_with(".dll") {
                        continue;
                    }
                    if let Some(file_name) = Path::new(&name).file_name() {
                        let out_path = dest_dir.join(file_name);
                        if let Ok(mut out_file) = File::create(out_path) {
                            let _ = std::io::copy(&mut file, &mut out_file);
                        }
                    }
                }
            }
        }
    }
}

fn cmd_download(args: &[String]) {
    let mut version_id = String::new();
    let mut version_url = String::new();
    let mut game_dir = String::new();

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--version" => { if i + 1 < args.len() { version_id = args[i + 1].clone(); i += 1; } }
            "--url" => { if i + 1 < args.len() { version_url = args[i + 1].clone(); i += 1; } }
            "--game-dir" => { if i + 1 < args.len() { game_dir = args[i + 1].clone(); i += 1; } }
            _ => {}
        }
        i += 1;
    }

    if version_id.is_empty() {
        eprintln!("Missing --version argument");
        return;
    }
    if game_dir.is_empty() {
        let appdata = env::var("APPDATA").unwrap_or_else(|_| ".".to_string());
        game_dir = format!("{}\\.minecraft", appdata);
    }

    emit_progress("Загрузка информации о версии...", 5.0);

    // Fetch version JSON
    let detail: VersionDetail = if !version_url.is_empty() {
        let res = match ureq::get(&version_url).call() {
            Ok(r) => r,
            Err(e) => { eprintln!("Failed to load version JSON: {}", e); return; }
        };
        match res.into_json() {
            Ok(d) => d,
            Err(e) => { eprintln!("Failed to parse version JSON: {}", e); return; }
        }
    } else {
        // Find in manifest
        let m_res = match ureq::get(MANIFEST_URL).call() {
            Ok(r) => r,
            Err(e) => { eprintln!("Failed to load manifest: {}", e); return; }
        };
        let manifest: VersionManifest = match m_res.into_json() {
            Ok(m) => m,
            Err(e) => { eprintln!("Failed to parse manifest: {}", e); return; }
        };
        let entry = match manifest.versions.iter().find(|v| v.id == version_id) {
            Some(e) => e,
            None => { eprintln!("Version {} not found in manifest", version_id); return; }
        };
        let v_res = match ureq::get(&entry.url).call() {
            Ok(r) => r,
            Err(e) => { eprintln!("Failed to load version details: {}", e); return; }
        };
        match v_res.into_json() {
            Ok(d) => d,
            Err(e) => { eprintln!("Failed to parse version details: {}", e); return; }
        }
    };

    let base = Path::new(&game_dir);
    let versions_dir = base.join("versions").join(&detail.id);
    let libraries_dir = base.join("libraries");
    let assets_dir = base.join("assets");
    let indexes_dir = assets_dir.join("indexes");
    let objects_dir = assets_dir.join("objects");
    let natives_dir = versions_dir.join(format!("{}-natives", detail.id));

    let _ = fs::create_dir_all(&versions_dir);
    let _ = fs::create_dir_all(&libraries_dir);
    let _ = fs::create_dir_all(&indexes_dir);
    let _ = fs::create_dir_all(&objects_dir);
    let _ = fs::create_dir_all(&natives_dir);

    // 1. Download Client JAR
    emit_progress("Загрузка игрового клиента (client.jar)...", 10.0);
    let client_jar_path = versions_dir.join(format!("{}.jar", detail.id));
    if let Some(ref client_dl) = detail.downloads.client {
        if !client_jar_path.exists() {
            let _ = download_file(&client_dl.url, &client_jar_path);
        }
    }

    // 2. Download Libraries
    emit_progress("Подготовка библиотек...", 20.0);
    let mut lib_downloads: Vec<(String, PathBuf, bool)> = Vec::new();

    for lib in &detail.libraries {
        if !is_rule_allowed(&lib.rules) {
            continue;
        }

        if let Some(ref dl) = lib.downloads {
            if let Some(ref art) = dl.artifact {
                let rel = art.path.clone().unwrap_or_else(|| maven_to_path(&lib.name, None).to_string_lossy().to_string());
                lib_downloads.push((art.url.clone(), libraries_dir.join(rel), false));
            }
            if let Some(ref classifiers) = dl.classifiers {
                if let Some(native_art) = classifiers.get("natives-windows") {
                    let rel = native_art.path.clone().unwrap_or_else(|| maven_to_path(&lib.name, Some("natives-windows")).to_string_lossy().to_string());
                    lib_downloads.push((native_art.url.clone(), libraries_dir.join(rel), true));
                }
            }
        } else {
            let rel = maven_to_path(&lib.name, None);
            let url = format!("https://libraries.minecraft.net/{}", rel.to_string_lossy().replace('\\', "/"));
            lib_downloads.push((url, libraries_dir.join(rel), false));
        }
    }

    let lib_total = lib_downloads.len();
    let lib_done = Arc::new(AtomicUsize::new(0));

    lib_downloads.par_iter().for_each(|(url, path, is_native)| {
        if !path.exists() {
            let _ = download_file(url, path);
        }
        if *is_native || path.to_string_lossy().contains("natives-windows") {
            extract_natives(path, &natives_dir);
        }
        let done = lib_done.fetch_add(1, Ordering::SeqCst) + 1;
        let pct = 20.0 + (25.0 * done as f64 / lib_total.max(1) as f64);
        emit_progress(&format!("Загрузка библиотек: {}/{}", done, lib_total), pct);
    });

    // 3. Download Assets
    emit_progress("Загрузка индекса ресурсов...", 48.0);
    if let Some(ref asset_index) = detail.asset_index {
        let index_file = indexes_dir.join(format!("{}.json", asset_index.id));
        if !index_file.exists() {
            let _ = download_file(&asset_index.url, &index_file);
        }

        if let Ok(content) = fs::read_to_string(&index_file) {
            if let Ok(idx_data) = serde_json::from_str::<AssetIndexData>(&content) {
                let mut missing: Vec<(String, PathBuf)> = Vec::new();
                for obj in idx_data.objects.values() {
                    let prefix = if obj.hash.len() >= 2 { &obj.hash[..2] } else { "00" };
                    let target = objects_dir.join(prefix).join(&obj.hash);
                    if !target.exists() {
                        let url = format!("{}{}/{}", RESOURCES_BASE_URL, prefix, obj.hash);
                        missing.push((url, target));
                    }
                }

                let asset_total = missing.len();
                let asset_done = Arc::new(AtomicUsize::new(0));

                if asset_total > 0 {
                    emit_progress(&format!("Загрузка ресурсов: 0/{}", asset_total), 50.0);
                    missing.par_iter().for_each(|(url, path)| {
                        let _ = download_file(url, path);
                        let done = asset_done.fetch_add(1, Ordering::SeqCst) + 1;
                        if done % 25 == 0 || done == asset_total {
                            let pct = 50.0 + (48.0 * done as f64 / asset_total as f64);
                            emit_progress(&format!("Загрузка ресурсов: {}/{}", done, asset_total), pct);
                        }
                    });
                }
            }
        }
    }

    emit_progress("Готово! Все файлы проверены и загружены.", 100.0);
}

fn generate_offline_uuid(name: &str) -> String {
    use md5::{Md5, Digest};
    let mut hasher = Md5::new();
    hasher.update(format!("OfflinePlayer:{}", name).as_bytes());
    let mut hash = hasher.finalize();
    hash[6] = (hash[6] & 0x0f) | 0x30;
    hash[8] = (hash[8] & 0x3f) | 0x80;
    let hex = format!("{:x}", hash);
    format!("{}-{}-{}-{}-{}", &hex[0..8], &hex[8..12], &hex[12..16], &hex[16..20], &hex[20..32])
}

fn cmd_launch(args: &[String]) {
    let mut version_id = String::new();
    let mut game_dir = String::new();
    let mut username = "Player".to_string();
    let mut ram_mb = 4096;
    let mut java_path = "java".to_string();
    let mut width = 1280;
    let mut height = 720;
    let mut fullscreen = false;

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--version" => { if i + 1 < args.len() { version_id = args[i + 1].clone(); i += 1; } }
            "--game-dir" => { if i + 1 < args.len() { game_dir = args[i + 1].clone(); i += 1; } }
            "--username" => { if i + 1 < args.len() { username = args[i + 1].clone(); i += 1; } }
            "--ram" => { if i + 1 < args.len() { ram_mb = args[i + 1].parse().unwrap_or(4096); i += 1; } }
            "--java" => { if i + 1 < args.len() { java_path = args[i + 1].clone(); i += 1; } }
            "--width" => { if i + 1 < args.len() { width = args[i + 1].parse().unwrap_or(1280); i += 1; } }
            "--height" => { if i + 1 < args.len() { height = args[i + 1].parse().unwrap_or(720); i += 1; } }
            "--fullscreen" => { fullscreen = true; }
            _ => {}
        }
        i += 1;
    }

    if game_dir.is_empty() {
        let appdata = env::var("APPDATA").unwrap_or_else(|_| ".".to_string());
        game_dir = format!("{}\\.minecraft", appdata);
    }

    let base = Path::new(&game_dir);
    let versions_dir = base.join("versions").join(&version_id);
    let client_jar = versions_dir.join(format!("{}.jar", version_id));
    let natives_dir = versions_dir.join(format!("{}-natives", version_id));
    let libraries_dir = base.join("libraries");
    let assets_dir = base.join("assets");

    emit_log(&format!("[RustCore] Запуск Minecraft {}...", version_id));
    emit_log(&format!("[RustCore] Игрок: {}", username));
    emit_log(&format!("[RustCore] Память RAM: {} MB", ram_mb));

    // Find all libraries for classpath
    let mut classpath_entries: Vec<String> = Vec::new();
    if libraries_dir.exists() {
        fn collect_jars(dir: &Path, list: &mut Vec<String>) {
            if let Ok(entries) = fs::read_dir(dir) {
                for entry in entries.flatten() {
                    let p = entry.path();
                    if p.is_dir() {
                        collect_jars(&p, list);
                    } else if p.extension().map_or(false, |ext| ext == "jar") {
                        list.push(p.to_string_lossy().to_string());
                    }
                }
            }
        }
        collect_jars(&libraries_dir, &mut classpath_entries);
    }
    classpath_entries.push(client_jar.to_string_lossy().to_string());
    let classpath = classpath_entries.join(";");

    let uuid = generate_offline_uuid(&username);

    let mut cmd = Command::new(&java_path);
    cmd.current_dir(&game_dir);
    cmd.arg(format!("-Xms512M"));
    cmd.arg(format!("-Xmx{}M", ram_mb));
    cmd.arg(format!("-Djava.library.path={}", natives_dir.display()));
    cmd.arg("-Dminecraft.launcher.brand=RustElectronLauncher");
    cmd.arg("-Dminecraft.launcher.version=2.0");
    cmd.arg("-cp");
    cmd.arg(classpath);

    cmd.arg("net.minecraft.client.main.Main");
    cmd.arg("--username").arg(&username);
    cmd.arg("--version").arg(&version_id);
    cmd.arg("--gameDir").arg(&game_dir);
    cmd.arg("--assetsDir").arg(assets_dir.to_string_lossy().to_string());
    cmd.arg("--assetIndex").arg(&version_id);
    cmd.arg("--uuid").arg(&uuid);
    cmd.arg("--accessToken").arg("0");
    cmd.arg("--userType").arg("mojang");
    cmd.arg("--versionType").arg("release");

    if fullscreen {
        cmd.arg("--fullscreen");
    } else {
        cmd.arg("--width").arg(width.to_string());
        cmd.arg("--height").arg(height.to_string());
    }

    cmd.stdout(Stdio::piped());
    cmd.stderr(Stdio::piped());

    match cmd.spawn() {
        Ok(mut child) => {
            emit_log(&format!("[RustCore] Процесс запущен с PID: {}", child.id()));

            if let Some(stdout) = child.stdout.take() {
                std::thread::spawn(move || {
                    let reader = BufReader::new(stdout);
                    for line in reader.lines().flatten() {
                        emit_log(&format!("[Minecraft] {}", line));
                    }
                });
            }

            if let Some(stderr) = child.stderr.take() {
                std::thread::spawn(move || {
                    let reader = BufReader::new(stderr);
                    for line in reader.lines().flatten() {
                        emit_log(&format!("[Minecraft ERR] {}", line));
                    }
                });
            }

            let status = child.wait();
            emit_log(&format!("[RustCore] Minecraft завершил работу: {:?}", status));
        }
        Err(e) => {
            emit_log(&format!("[RustCore Error] Не удалось запустить процесс: {}", e));
        }
    }
}
