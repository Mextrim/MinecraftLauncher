package com.craftlauncher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;

public class Runner {

    private static Process currentProcess;

    public static synchronized Process launch(
        String versionId,
        Config config,
        Consumer<String> onLog
    ) throws Exception {

        Path base = Paths.get(config.gameDir);
        String baseVer = versionId.contains(" (") ? versionId.substring(0, versionId.indexOf(" (")) : versionId;

        boolean isForge = versionId.contains("Forge") || versionId.contains("YOLO");

        // Target version directories & JSON
        String targetVerId = versionId;
        Path targetVersionJson = null;

        if (isForge && "1.20.1".equals(baseVer)) {
            targetVerId = "1.20.1-forge-47.3.0";
            targetVersionJson = base.resolve("versions").resolve(targetVerId).resolve(targetVerId + ".json");
        } else {
            Path directJson = base.resolve("versions").resolve(versionId).resolve(versionId + ".json");
            if (Files.exists(directJson)) {
                targetVersionJson = directJson;
            } else {
                Path baseJson = base.resolve("versions").resolve(baseVer).resolve(baseVer + ".json");
                if (Files.exists(baseJson)) {
                    targetVersionJson = baseJson;
                }
            }
        }

        Path baseClientJar = base.resolve("versions").resolve(baseVer).resolve(baseVer + ".jar");
        Path versionsDir = base.resolve("versions").resolve(versionId);
        Path clientJar = baseClientJar;

        if (!Files.exists(clientJar)) {
            clientJar = versionsDir.resolve(versionId + ".jar");
        }

        Path nativesDir = versionsDir.resolve(versionId + "-natives");
        if (!Files.exists(nativesDir)) {
            nativesDir = base.resolve("versions").resolve(baseVer).resolve(baseVer + "-natives");
        }

        Path librariesDir = base.resolve("libraries");
        Path assetsDir = base.resolve("assets");

        // Determine Java executable
        String javaExecutable = config.javaPath;
        if (javaExecutable == null || javaExecutable.isBlank() || !new File(javaExecutable).exists()) {
            javaExecutable = findBestJava(base, isForge, baseVer);
        } else if (isForge && "1.20.1".equals(baseVer)) {
            // Check if user selected Java is incompatible with Mixin/ASM in 1.20.1 Forge
            int ver = JavaDetector.parseVersion(javaExecutable);
            if (ver > 21 || ver < 17) {
                String safeJava = findBestJava(base, true, baseVer);
                if (safeJava != null && !safeJava.isBlank()) {
                    onLog.accept("[JavaRunner] Внимание: Java " + ver + " несовместима с модами 1.20.1. Переключение на: " + safeJava);
                    javaExecutable = safeJava;
                }
            }
        }

        onLog.accept("[JavaRunner] Используется Java: " + javaExecutable);
        onLog.accept("[JavaRunner] Рабочая папка: " + base);
        onLog.accept("[JavaRunner] Версия игры: " + versionId + (isForge ? " (Forge Runtime: " + targetVerId + ")" : ""));

        // Build classpath with forward slashes
        Set<String> classPaths = new LinkedHashSet<>();

        if (targetVersionJson != null && Files.exists(targetVersionJson)) {
            addClasspathFromVersionJson(targetVersionJson, librariesDir, classPaths);
        }
        Path baseJson = base.resolve("versions").resolve(baseVer).resolve(baseVer + ".json");
        if (Files.exists(baseJson)) {
            addClasspathFromVersionJson(baseJson, librariesDir, classPaths);
        }

        if (classPaths.isEmpty() && Files.exists(librariesDir)) {
            try (var stream = Files.walk(librariesDir)) {
                stream.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .forEach(p -> classPaths.add(p.toAbsolutePath().toString().replace('\\', '/')));
            }
        }

        if (Files.exists(clientJar)) {
            classPaths.add(clientJar.toAbsolutePath().toString().replace('\\', '/'));
        } else if (Files.exists(baseClientJar)) {
            classPaths.add(baseClientJar.toAbsolutePath().toString().replace('\\', '/'));
        }

        // Write classpath to argument file (using forward slashes to avoid escape char issues)
        Path argFile = base.resolve("minecraft_args.txt");
        String combinedClasspath = String.join(";", classPaths);
        Files.writeString(argFile, "-cp\n\"" + combinedClasspath + "\"\n", StandardCharsets.UTF_8);

        String offlineUuid = generateOfflineUuid(config.nickname);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-Xms512M");
        command.add("-Xmx" + config.ramMb + "M");
        command.add("-Djava.library.path=" + nativesDir.toAbsolutePath());

        if (config.customJvmArgs != null && !config.customJvmArgs.isBlank()) {
            for (String arg : config.customJvmArgs.split(" ")) {
                if (!arg.isBlank()) command.add(arg);
            }
        }

        String mainClass = "net.minecraft.client.main.Main";
        List<String> forgeGameArgs = new ArrayList<>();

        if (isForge && targetVersionJson != null && Files.exists(targetVersionJson)) {
            MiniJson.Value forgeJson = MiniJson.parse(Files.readString(targetVersionJson));
            mainClass = forgeJson.get("mainClass").asString("cpw.mods.bootstraplauncher.BootstrapLauncher");

            // JVM arguments from Forge JSON
            MiniJson.Value jvmArr = forgeJson.get("arguments").get("jvm");
            if (jvmArr.isArray()) {
                String libDirStr = librariesDir.toAbsolutePath().toString().replace('\\', '/');
                for (MiniJson.Value v : jvmArr.asArray()) {
                    String arg = v.asString();
                    arg = arg.replace("${library_directory}", libDirStr);
                    arg = arg.replace("${classpath_separator}", ";");
                    arg = arg.replace("${version_name}", baseVer);
                    command.add(arg);
                }
            }

            // Game arguments from Forge JSON
            MiniJson.Value gameArr = forgeJson.get("arguments").get("game");
            if (gameArr.isArray()) {
                for (MiniJson.Value v : gameArr.asArray()) {
                    forgeGameArgs.add(v.asString());
                }
            }
        } else {
            command.add("-Dminecraft.launcher.brand=CraftLauncherJava");
            command.add("-Dminecraft.launcher.version=2.0");
        }

        // Argument file for classpath (circumvents Windows 32767 char cmd limit)
        command.add("@" + argFile.toAbsolutePath().toString().replace('\\', '/'));

        // Main class
        command.add(mainClass);

        // Forge specific game arguments (e.g. --launchTarget forgeclient --fml.*)
        command.addAll(forgeGameArgs);

        // Standard Minecraft game arguments
        command.add("--username"); command.add(config.nickname);
        command.add("--version"); command.add(targetVerId);
        command.add("--gameDir"); command.add(base.toAbsolutePath().toString());
        command.add("--assetsDir"); command.add(assetsDir.toAbsolutePath().toString());
        command.add("--assetIndex"); command.add(baseVer);
        command.add("--uuid"); command.add(offlineUuid);
        command.add("--accessToken"); command.add("0");
        command.add("--userType"); command.add("mojang");
        command.add("--versionType"); command.add("release");

        if (config.fullscreen) {
            command.add("--fullscreen");
        } else {
            command.add("--width"); command.add(String.valueOf(config.windowWidth));
            command.add("--height"); command.add(String.valueOf(config.windowHeight));
        }

        onLog.accept("[JavaRunner] Запуск процесса Minecraft (" + mainClass + ")...");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(base.toFile());

        Process process = pb.start();
        currentProcess = process;

        // Stream stdout
        Thread outThread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    onLog.accept("[Minecraft] " + line);
                }
            } catch (Exception ignored) {}
        });
        outThread.setDaemon(true);
        outThread.start();

        // Stream stderr
        Thread errThread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    onLog.accept("[Minecraft ERR] " + line);
                }
            } catch (Exception ignored) {}
        });
        errThread.setDaemon(true);
        errThread.start();

        return process;
    }

    private static void addClasspathFromVersionJson(Path jsonPath, Path librariesDir, Set<String> classPaths) {
        try {
            MiniJson.Value root = MiniJson.parse(Files.readString(jsonPath));
            MiniJson.Value libs = root.get("libraries");
            if (libs.isArray()) {
                for (MiniJson.Value lib : libs.asArray()) {
                    MiniJson.Value art = lib.get("downloads").get("artifact");
                    if (art.isObject()) {
                        String relPath = art.get("path").asString();
                        if (!relPath.isBlank()) {
                            Path p = librariesDir.resolve(relPath.replace('/', File.separatorChar));
                            if (Files.exists(p)) {
                                classPaths.add(p.toAbsolutePath().toString().replace('\\', '/'));
                            }
                        }
                    } else if (lib.get("name").isString()) {
                        String relPath = Downloader.mavenToPath(lib.get("name").asString(), null);
                        Path p = librariesDir.resolve(relPath.replace('/', File.separatorChar));
                        if (Files.exists(p)) {
                            classPaths.add(p.toAbsolutePath().toString().replace('\\', '/'));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static String findBestJava(Path base, boolean isForge, String baseVer) {
        // 1. Check local Minecraft runtime jre-17
        Path jre17 = base.resolve("runtime").resolve("jre-17").resolve("bin").resolve("javaw.exe");
        if (Files.exists(jre17)) {
            return jre17.toAbsolutePath().toString();
        }
        Path jre17Java = base.resolve("runtime").resolve("jre-17").resolve("bin").resolve("java.exe");
        if (Files.exists(jre17Java)) {
            return jre17Java.toAbsolutePath().toString();
        }

        // 2. Search installed Java versions
        List<JavaDetector.JavaItem> javas = JavaDetector.detect();
        if (isForge && "1.20.1".equals(baseVer)) {
            for (JavaDetector.JavaItem j : javas) {
                if (j.version >= 17 && j.version <= 21) {
                    return j.path;
                }
            }
        }
        return !javas.isEmpty() ? javas.get(0).path : "java";
    }

    public static synchronized void killGame() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
        }
    }

    public static String generateOfflineUuid(String username) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x30); // version 3
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); // IETF variant

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String hex = sb.toString();
            return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" +
                   hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20, 32);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
