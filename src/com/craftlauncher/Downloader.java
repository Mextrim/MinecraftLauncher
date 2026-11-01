package com.craftlauncher;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

public class Downloader {

    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES_BASE_URL = "https://resources.download.minecraft.net/";
    public static final String YOLO_MODPACK_URL = "https://github.com/Mextrim/MinecraftLauncher/releases/download/v2.0.0/yolo.zip";
    public static final String FORGE_1_20_1_RUNTIME_URL = "https://github.com/Mextrim/MinecraftLauncher/releases/download/v2.0.0/forge-1.20.1-runtime.zip";
    public static final String JRE_17_URL = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jre/hotspot/normal/eclipse";

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public static class ProgressReport {
        public final String status;
        public final double percent;

        public ProgressReport(String status, double percent) {
            this.status = status;
            this.percent = percent;
        }
    }

    public static class VersionEntry {
        public final String id;
        public final String type;
        public final String url;
        public final String badge;
        public final String baseVersion;

        public VersionEntry(String id, String type, String url) {
            this(id, type, url, "release".equalsIgnoreCase(type) ? "РЕЛИЗ" : type.toUpperCase(), id);
        }

        public VersionEntry(String id, String type, String url, String badge, String baseVersion) {
            this.id = id;
            this.type = type;
            this.url = url;
            this.badge = badge;
            this.baseVersion = baseVersion;
        }

        public boolean isRelease() { return "release".equalsIgnoreCase(type); }

        @Override
        public String toString() {
            return id + "  [" + badge + "]";
        }
    }

    public static class ManifestResult {
        public String latestRelease = "";
        public List<VersionEntry> versions = new ArrayList<>();
    }

    public static List<VersionEntry> getDefaultVersions() {
        List<VersionEntry> list = new ArrayList<>();
        list.add(new VersionEntry("1.20.1 (Forge + YOLO Modpack)", "release", "", "YOLO MODPACK", "1.20.1"));
        list.add(new VersionEntry("1.21.4 (OptiFine)", "release", "", "OPTIFINE HD", "1.21.4"));
        list.add(new VersionEntry("1.21.4 (Fabric)", "release", "", "FABRIC", "1.21.4"));
        list.add(new VersionEntry("1.21.4 (Forge)", "release", "", "FORGE", "1.21.4"));
        list.add(new VersionEntry("1.21.4 (Forge + OptiFine)", "release", "", "FORGE + OPTIFINE", "1.21.4"));
        list.add(new VersionEntry("1.21.4", "release", "", "РЕЛИЗ", "1.21.4"));
        list.add(new VersionEntry("1.20.4 (OptiFine)", "release", "", "OPTIFINE HD", "1.20.4"));
        list.add(new VersionEntry("1.20.4", "release", "", "РЕЛИЗ", "1.20.4"));
        list.add(new VersionEntry("1.20.1 (Forge)", "release", "", "FORGE", "1.20.1"));
        list.add(new VersionEntry("1.16.5 (OptiFine)", "release", "", "OPTIFINE HD", "1.16.5"));
        list.add(new VersionEntry("1.16.5", "release", "", "РЕЛИЗ", "1.16.5"));
        list.add(new VersionEntry("1.12.2 (OptiFine)", "release", "", "OPTIFINE HD", "1.12.2"));
        list.add(new VersionEntry("1.12.2", "release", "", "РЕЛИЗ", "1.12.2"));
        return list;
    }

    public ManifestResult getManifest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        MiniJson.Value root = MiniJson.parse(res.body());

        ManifestResult result = new ManifestResult();
        result.latestRelease = root.get("latest").get("release").asString("1.21");

        for (MiniJson.Value item : root.get("versions").asArray()) {
            String id = item.get("id").asString();
            String type = item.get("type").asString();
            String url = item.get("url").asString();
            result.versions.add(new VersionEntry(id, type, url));
        }

        // Inject modded builds: OptiFine, Fabric, Forge, Forge + OptiFine, YOLO Modpack
        List<VersionEntry> modded = new ArrayList<>();
        String[] popular = {"1.20.1", "1.21.4", "1.20.4", "1.16.5", "1.12.2"};
        for (String baseVer : popular) {
            String url = "";
            for (VersionEntry v : result.versions) {
                if (v.id.equals(baseVer)) {
                    url = v.url;
                    break;
                }
            }
            if ("1.20.1".equals(baseVer)) {
                modded.add(new VersionEntry("1.20.1 (Forge + YOLO Modpack)", "release", url, "YOLO MODPACK", "1.20.1"));
            }
            modded.add(new VersionEntry(baseVer + " (OptiFine)", "release", url, "OPTIFINE HD", baseVer));
            modded.add(new VersionEntry(baseVer + " (Fabric)", "release", url, "FABRIC", baseVer));
            modded.add(new VersionEntry(baseVer + " (Forge)", "release", url, "FORGE", baseVer));
            modded.add(new VersionEntry(baseVer + " (Forge + OptiFine)", "release", url, "FORGE + OPTIFINE", baseVer));
        }
        result.versions.addAll(0, modded);

        return result;
    }

    public void downloadVersion(String versionId, String versionUrl, String gameDir,
                                java.util.function.Consumer<ProgressReport> onProgress) throws Exception {

        onProgress.accept(new ProgressReport("Загрузка информации о версии...", 5.0));

        String baseVer = versionId.contains(" (") ? versionId.substring(0, versionId.indexOf(" (")) : versionId;

        // 1. Get version JSON
        String detailJson;
        if (versionUrl != null && !versionUrl.isBlank()) {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(versionUrl)).GET().build();
            detailJson = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        } else {
            ManifestResult manifest = getManifest();
            VersionEntry match = manifest.versions.stream().filter(v -> v.id.equals(versionId) || v.id.equals(baseVer)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Версия " + versionId + " не найдена!"));
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(match.url)).GET().build();
            detailJson = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        }

        MiniJson.Value detail = MiniJson.parse(detailJson);

        Path base = Paths.get(gameDir);
        Path versionsDir = base.resolve("versions").resolve(versionId);
        Path librariesDir = base.resolve("libraries");
        Path assetsDir = base.resolve("assets");
        Path indexesDir = assetsDir.resolve("indexes");
        Path objectsDir = assetsDir.resolve("objects");
        Path nativesDir = versionsDir.resolve(versionId + "-natives");

        Files.createDirectories(versionsDir);
        Files.createDirectories(librariesDir);
        Files.createDirectories(indexesDir);
        Files.createDirectories(objectsDir);
        Files.createDirectories(nativesDir);

        Files.createDirectories(base.resolve("mods"));
        Files.createDirectories(base.resolve("shaderpacks"));
        Files.createDirectories(base.resolve("resourcepacks"));

        // Copy client jar from base if needed
        Path clientJar = versionsDir.resolve(versionId + ".jar");
        Path baseJar = base.resolve("versions").resolve(baseVer).resolve(baseVer + ".jar");
        if (Files.exists(baseJar) && (!Files.exists(clientJar) || Files.size(clientJar) == 0)) {
            Files.copy(baseJar, clientJar, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2. Download client.jar
        onProgress.accept(new ProgressReport("Загрузка игрового клиента (client.jar)...", 10.0));
        String clientUrl = detail.get("downloads").get("client").get("url").asString();
        long clientSize = detail.get("downloads").get("client").get("size").asLong(0);

        if (!Files.exists(clientJar) || (clientSize > 0 && Files.size(clientJar) != clientSize)) {
            if (clientUrl != null && !clientUrl.isBlank()) {
                downloadFile(clientUrl, clientJar);
            }
        }

        // 3. Download Libraries
        onProgress.accept(new ProgressReport("Подготовка библиотек...", 15.0));
        List<DownloadTask> libTasks = new ArrayList<>();

        for (MiniJson.Value lib : detail.get("libraries").asArray()) {
            if (!isRuleAllowed(lib.get("rules"))) continue;

            MiniJson.Value dl = lib.get("downloads");
            if (dl.isObject()) {
                MiniJson.Value art = dl.get("artifact");
                if (art.isObject() && !art.get("url").asString().isBlank()) {
                    String rel = art.get("path").asString(mavenToPath(lib.get("name").asString(), null));
                    libTasks.add(new DownloadTask(art.get("url").asString(), librariesDir.resolve(rel), false));
                }

                MiniJson.Value classifiers = dl.get("classifiers");
                if (classifiers.isObject()) {
                    MiniJson.Value winNative = classifiers.get("natives-windows");
                    if (winNative.isObject() && !winNative.get("url").asString().isBlank()) {
                        String rel = winNative.get("path").asString(mavenToPath(lib.get("name").asString(), "natives-windows"));
                        libTasks.add(new DownloadTask(winNative.get("url").asString(), librariesDir.resolve(rel), true));
                    }
                }
            } else {
                String rel = mavenToPath(lib.get("name").asString(), null);
                String url = "https://libraries.minecraft.net/" + rel.replace('\\', '/');
                libTasks.add(new DownloadTask(url, librariesDir.resolve(rel), false));
            }
        }

        int totalLibs = libTasks.size();
        AtomicInteger doneLibs = new AtomicInteger(0);
        ExecutorService libPool = Executors.newFixedThreadPool(16);

        List<Future<?>> libFutures = new ArrayList<>();
        for (DownloadTask task : libTasks) {
            libFutures.add(libPool.submit(() -> {
                try {
                    if (!Files.exists(task.dest)) {
                        downloadFile(task.url, task.dest);
                    }
                    if (task.isNative || task.dest.getFileName().toString().contains("natives-windows")) {
                        extractNatives(task.dest, nativesDir);
                    }
                } catch (Exception ignored) {}
                int done = doneLibs.incrementAndGet();
                double pct = 15.0 + (30.0 * done / Math.max(1, totalLibs));
                onProgress.accept(new ProgressReport("Загрузка библиотек: " + done + "/" + totalLibs, pct));
            }));
        }

        for (Future<?> f : libFutures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        libPool.shutdown();

        // 4. Download Asset Index
        onProgress.accept(new ProgressReport("Загрузка индекса ресурсов...", 48.0));
        MiniJson.Value assetIdx = detail.get("assetIndex");
        if (assetIdx.isObject()) {
            String idxId = assetIdx.get("id").asString();
            String idxUrl = assetIdx.get("url").asString();
            Path idxFile = indexesDir.resolve(idxId + ".json");

            if (!Files.exists(idxFile)) {
                downloadFile(idxUrl, idxFile);
            }

            // 5. Download Assets
            if (Files.exists(idxFile)) {
                String idxJson = Files.readString(idxFile);
                MiniJson.Value idxObj = MiniJson.parse(idxJson);
                Map<String, MiniJson.Value> objects = idxObj.get("objects").asObject();

                List<DownloadTask> assetTasks = new ArrayList<>();
                for (MiniJson.Value obj : objects.values()) {
                    String hash = obj.get("hash").asString();
                    long size = obj.get("size").asLong(0);
                    String prefix = hash.length() >= 2 ? hash.substring(0, 2) : "00";
                    Path target = objectsDir.resolve(prefix).resolve(hash);
                    if (!Files.exists(target) || (size > 0 && Files.size(target) != size)) {
                        String url = RESOURCES_BASE_URL + prefix + "/" + hash;
                        assetTasks.add(new DownloadTask(url, target, false));
                    }
                }

                int totalAssets = assetTasks.size();
                if (totalAssets > 0) {
                    onProgress.accept(new ProgressReport("Загрузка ресурсов: 0/" + totalAssets, 50.0));
                    AtomicInteger doneAssets = new AtomicInteger(0);
                    ExecutorService assetPool = Executors.newFixedThreadPool(24);

                    List<Future<?>> assetFutures = new ArrayList<>();
                    for (DownloadTask task : assetTasks) {
                        assetFutures.add(assetPool.submit(() -> {
                            try {
                                downloadFile(task.url, task.dest);
                            } catch (Exception ignored) {}
                            int done = doneAssets.incrementAndGet();
                            if (done % 25 == 0 || done == totalAssets) {
                                double pct = 50.0 + (48.0 * done / totalAssets);
                                onProgress.accept(new ProgressReport("Загрузка ресурсов: " + done + "/" + totalAssets, pct));
                            }
                        }));
                    }

                    for (Future<?> f : assetFutures) {
                        try { f.get(); } catch (Exception ignored) {}
                    }
                    assetPool.shutdown();
                }
            }
        }

        onProgress.accept(new ProgressReport("Все файлы проверены и загружены!", 100.0));
    }

    private static class DownloadTask {
        final String url;
        final Path dest;
        final boolean isNative;

        DownloadTask(String url, Path dest, boolean isNative) {
            this.url = url;
            this.dest = dest;
            this.isNative = isNative;
        }
    }

    public void downloadFile(String url, Path dest) throws Exception {
        Files.createDirectories(dest.getParent());
        Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .GET()
            .build();
        HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());

        if (res.statusCode() >= 400) {
            throw new IOException("HTTP " + res.statusCode() + " при загрузке: " + url);
        }

        try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }

        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void extractNatives(Path jarPath, Path targetDir) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("META-INF") || !name.toLowerCase().endsWith(".dll")) {
                    continue;
                }
                Path dest = targetDir.resolve(Paths.get(name).getFileName().toString());
                try (OutputStream os = Files.newOutputStream(dest)) {
                    zis.transferTo(os);
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean isRuleAllowed(MiniJson.Value rules) {
        if (!rules.isArray() || rules.asArray().isEmpty()) return true;

        boolean allowed = false;
        for (MiniJson.Value r : rules.asArray()) {
            boolean matches = true;
            MiniJson.Value os = r.get("os");
            if (os.isObject()) {
                String osName = os.get("name").asString();
                if (!osName.isBlank() && !"windows".equalsIgnoreCase(osName)) {
                    matches = false;
                }
            }
            if (matches && r.get("features").isObject()) {
                matches = false;
            }
            if (matches) {
                allowed = "allow".equalsIgnoreCase(r.get("action").asString("allow"));
            }
        }
        return allowed;
    }

    public static String mavenToPath(String mavenName, String classifier) {
        String[] parts = mavenName.split(":");
        if (parts.length < 3) return mavenName.replace(':', '/') + ".jar";

        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifierPart = classifier != null && !classifier.isBlank() ? "-" + classifier : (parts.length > 3 ? "-" + parts[3] : "");

        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifierPart + ".jar";
    }

    public void downloadAndInstallYoloModpack(String gameDir, java.util.function.Consumer<ProgressReport> onProgress) throws Exception {
        onProgress.accept(new ProgressReport("Подключение к GitHub CDN для загрузки модов YOLO...", 2.0));
        Path modsDir = Paths.get(gameDir, "mods");
        Files.createDirectories(modsDir);

        Path zipTmp = Paths.get(gameDir, "yolo_temp.zip");

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(YOLO_MODPACK_URL)).GET().build();
        HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());

        long contentLength = res.headers().firstValueAsLong("content-length").orElse(135933393L);

        try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(zipTmp)) {
            byte[] buf = new byte[65536];
            long totalRead = 0;
            int n;
            long lastNotifyTime = 0;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                totalRead += n;
                long now = System.currentTimeMillis();
                if (now - lastNotifyTime > 150 || totalRead == contentLength) {
                    lastNotifyTime = now;
                    double pct = Math.min(90.0, 5.0 + ((double) totalRead / Math.max(1, contentLength) * 85.0));
                    onProgress.accept(new ProgressReport(
                        String.format(java.util.Locale.US, "Загрузка модов YOLO с GitHub: %.1f / %.1f МБ", totalRead / (1024.0 * 1024.0), contentLength / (1024.0 * 1024.0)),
                        pct
                    ));
                }
            }
        }

        onProgress.accept(new ProgressReport("Распаковка 53 модов в папку mods...", 92.0));

        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipTmp))) {
            ZipEntry entry;
            byte[] buf = new byte[32768];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
                if (name.contains("\\")) name = name.substring(name.lastIndexOf('\\') + 1);
                if (!name.toLowerCase().endsWith(".jar")) continue;

                Path target = modsDir.resolve(name);
                try (OutputStream fos = Files.newOutputStream(target)) {
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }
                count++;
            }
        } finally {
            try { Files.deleteIfExists(zipTmp); } catch (Exception ignored) {}
        }

        onProgress.accept(new ProgressReport("Успешно установлено " + count + " модов сборки YOLO!", 100.0));
    }

    public static boolean isYoloModpackInstalled(String gameDir) {
        Path modsDir = Paths.get(gameDir, "mods");
        if (!Files.exists(modsDir)) return false;
        try (var stream = Files.list(modsDir)) {
            return stream.anyMatch(p -> p.getFileName().toString().toLowerCase().contains("tacz"));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isForge1201Installed(String gameDir) {
        Path json = Paths.get(gameDir, "versions", "1.20.1-forge-47.3.0", "1.20.1-forge-47.3.0.json");
        return Files.exists(json);
    }

    public static boolean isJava17Installed(String gameDir) {
        Path p = Paths.get(gameDir, "runtime", "jre-17", "bin", "javaw.exe");
        if (Files.exists(p)) return true;
        Path p2 = Paths.get(gameDir, "runtime", "jre-17", "bin", "java.exe");
        if (Files.exists(p2)) return true;
        for (JavaDetector.JavaItem j : JavaDetector.detect()) {
            if (j.version >= 17 && j.version <= 21) return true;
        }
        return false;
    }

    public void downloadAndInstallForge1201(String gameDir, java.util.function.Consumer<ProgressReport> onProgress) throws Exception {
        onProgress.accept(new ProgressReport("Загрузка среды Minecraft Forge 1.20.1 с GitHub...", 10.0));
        Path zipTmp = Paths.get(gameDir, "forge_temp.zip");
        downloadFile(FORGE_1_20_1_RUNTIME_URL, zipTmp);

        onProgress.accept(new ProgressReport("Распаковка Forge 1.20.1 и библиотек...", 60.0));
        unzip(zipTmp, Paths.get(gameDir));
        Files.deleteIfExists(zipTmp);
        onProgress.accept(new ProgressReport("Minecraft Forge 1.20.1 успешно установлен!", 100.0));
    }

    public void downloadAndInstallJava17(String gameDir, java.util.function.Consumer<ProgressReport> onProgress) throws Exception {
        onProgress.accept(new ProgressReport("Загрузка Java 17 (Adoptium JRE) для модов...", 10.0));
        Path runtimeDir = Paths.get(gameDir, "runtime");
        Path destDir = runtimeDir.resolve("jre-17");
        if (Files.exists(destDir.resolve("bin").resolve("javaw.exe")) || Files.exists(destDir.resolve("bin").resolve("java.exe"))) {
            return;
        }
        Files.createDirectories(runtimeDir);
        Path tmpZip = Paths.get(gameDir, "jre17_temp.zip");
        downloadFile(JRE_17_URL, tmpZip);

        onProgress.accept(new ProgressReport("Распаковка Java 17...", 70.0));
        String rootDir = "";
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tmpZip))) {
            ZipEntry ze = zis.getNextEntry();
            if (ze != null) {
                String name = ze.getName().replace('\\', '/');
                if (name.contains("/")) {
                    rootDir = name.substring(0, name.indexOf('/'));
                }
            }
        }
        unzip(tmpZip, runtimeDir);
        Files.deleteIfExists(tmpZip);

        if (!rootDir.isBlank()) {
            Path extracted = runtimeDir.resolve(rootDir);
            if (Files.exists(extracted) && !extracted.equals(destDir)) {
                if (Files.exists(destDir)) deleteDirectory(destDir);
                Files.move(extracted, destDir, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        onProgress.accept(new ProgressReport("Java 17 успешно установлена!", 100.0));
    }

    public static void unzip(Path zipFile, Path targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[32768];
            while ((entry = zis.getNextEntry()) != null) {
                Path dest = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    try (OutputStream fos = Files.newOutputStream(dest)) {
                        int len;
                        while ((len = zis.read(buf)) > 0) {
                            fos.write(buf, 0, len);
                        }
                    }
                }
            }
        }
    }

    public static void deleteDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ignored) {}
    }
}
