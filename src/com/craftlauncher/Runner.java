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
        Path versionsDir = base.resolve("versions").resolve(versionId);
        Path clientJar = versionsDir.resolve(versionId + ".jar");
        Path nativesDir = versionsDir.resolve(versionId + "-natives");
        Path librariesDir = base.resolve("libraries");
        Path assetsDir = base.resolve("assets");

        if (!Files.exists(clientJar) && versionId.contains(" (")) {
            String baseVer = versionId.substring(0, versionId.indexOf(" ("));
            Path baseJar = base.resolve("versions").resolve(baseVer).resolve(baseVer + ".jar");
            if (Files.exists(baseJar)) {
                Files.createDirectories(versionsDir);
                Files.copy(baseJar, clientJar, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        if (!Files.exists(clientJar)) {
            throw new FileNotFoundException("Файл клиента не найден: " + clientJar);
        }

        String javaExecutable = config.javaPath;
        if (javaExecutable == null || javaExecutable.isBlank() || !new File(javaExecutable).exists()) {
            List<JavaDetector.JavaItem> javas = JavaDetector.detect();
            javaExecutable = !javas.isEmpty() ? javas.get(0).path : "java";
        }

        onLog.accept("[JavaRunner] Используется Java: " + javaExecutable);
        onLog.accept("[JavaRunner] Рабочая папка: " + base);
        onLog.accept("[JavaRunner] Версия игры: " + versionId);

        // Collect classpath
        List<String> classPaths = new ArrayList<>();
        if (Files.exists(librariesDir)) {
            try (var stream = Files.walk(librariesDir)) {
                stream.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .forEach(p -> classPaths.add(p.toAbsolutePath().toString()));
            }
        }
        classPaths.add(clientJar.toAbsolutePath().toString());
        String combinedClasspath = String.join(";", classPaths);

        String offlineUuid = generateOfflineUuid(config.nickname);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-Xms512M");
        command.add("-Xmx" + config.ramMb + "M");
        command.add("-Djava.library.path=" + nativesDir.toAbsolutePath());
        command.add("-Dminecraft.launcher.brand=CraftLauncherJava");
        command.add("-Dminecraft.launcher.version=2.0");

        if (config.customJvmArgs != null && !config.customJvmArgs.isBlank()) {
            for (String arg : config.customJvmArgs.split(" ")) {
                if (!arg.isBlank()) command.add(arg);
            }
        }

        command.add("-cp");
        command.add(combinedClasspath);
        command.add("net.minecraft.client.main.Main");

        command.add("--username"); command.add(config.nickname);
        command.add("--version"); command.add(versionId);
        command.add("--gameDir"); command.add(base.toAbsolutePath().toString());
        command.add("--assetsDir"); command.add(assetsDir.toAbsolutePath().toString());
        command.add("--assetIndex"); command.add(versionId);
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

        onLog.accept("[JavaRunner] Запуск процесса Minecraft...");

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
