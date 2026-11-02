package com.craftlauncher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Config {

    public String nickname = "Player";
    public String selectedVersion = "";
    public boolean showSnapshots = false;
    public int ramMb = 4096;
    public String javaPath = "";
    public String gameDir = "";
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public boolean fullscreen = false;
    public String customJvmArgs = "-XX:+UseG1GC";
    public String theme = "emerald";
    public String avatar = "steve";
    public String modLoader = "all";
    public String skinType = "steve";
    public String customSkinPath = "";
    public String skinName = "Стив (Классический)";

    public Config() {
        String appdata = System.getenv("APPDATA");
        if (appdata == null || appdata.isBlank()) {
            appdata = System.getProperty("user.home");
        }
        this.gameDir = Paths.get(appdata, ".minecraft").toString();
    }

    private static Path getConfigPath() {
        String appdata = System.getenv("APPDATA");
        if (appdata == null || appdata.isBlank()) {
            appdata = System.getProperty("user.home");
        }
        Path dir = Paths.get(appdata, ".minecraft");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        Path mexPath = dir.resolve("mex_launcher_config.json");
        if (!Files.exists(mexPath)) {
            Path oldPath = dir.resolve("craft_launcher_java.json");
            if (Files.exists(oldPath)) return oldPath;
        }
        return mexPath;
    }

    public static Config load() {
        Config cfg = new Config();
        Path p = getConfigPath();
        if (Files.exists(p)) {
            try {
                String json = Files.readString(p, StandardCharsets.UTF_8);
                MiniJson.Value v = MiniJson.parse(json);
                cfg.nickname = v.get("nickname").asString(cfg.nickname);
                cfg.selectedVersion = v.get("selectedVersion").asString(cfg.selectedVersion);
                cfg.showSnapshots = v.get("showSnapshots").asBoolean(cfg.showSnapshots);
                cfg.ramMb = v.get("ramMb").asInt(cfg.ramMb);
                cfg.javaPath = v.get("javaPath").asString(cfg.javaPath);
                cfg.gameDir = v.get("gameDir").asString(cfg.gameDir);
                cfg.windowWidth = v.get("windowWidth").asInt(cfg.windowWidth);
                cfg.windowHeight = v.get("windowHeight").asInt(cfg.windowHeight);
                cfg.fullscreen = v.get("fullscreen").asBoolean(cfg.fullscreen);
                cfg.customJvmArgs = v.get("customJvmArgs").asString(cfg.customJvmArgs);
                cfg.theme = v.get("theme").asString(cfg.theme);
                cfg.avatar = v.get("avatar").asString(cfg.avatar);
                cfg.modLoader = v.get("modLoader").asString(cfg.modLoader);
                cfg.skinType = v.get("skinType").asString(cfg.skinType);
                cfg.customSkinPath = v.get("customSkinPath").asString(cfg.customSkinPath);
                cfg.skinName = v.get("skinName").asString(cfg.skinName);
            } catch (Exception e) {
                System.err.println("Config load error: " + e.getMessage());
            }
        }
        return cfg;
    }

    public void save() {
        try {
            Path p = getConfigPath();
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"nickname\": \"").append(escape(nickname)).append("\",\n");
            sb.append("  \"selectedVersion\": \"").append(escape(selectedVersion)).append("\",\n");
            sb.append("  \"showSnapshots\": ").append(showSnapshots).append(",\n");
            sb.append("  \"ramMb\": ").append(ramMb).append(",\n");
            sb.append("  \"javaPath\": \"").append(escape(javaPath)).append("\",\n");
            sb.append("  \"gameDir\": \"").append(escape(gameDir)).append("\",\n");
            sb.append("  \"windowWidth\": ").append(windowWidth).append(",\n");
            sb.append("  \"windowHeight\": ").append(windowHeight).append(",\n");
            sb.append("  \"fullscreen\": ").append(fullscreen).append(",\n");
            sb.append("  \"customJvmArgs\": \"").append(escape(customJvmArgs)).append("\",\n");
            sb.append("  \"theme\": \"").append(escape(theme)).append("\",\n");
            sb.append("  \"avatar\": \"").append(escape(avatar)).append("\",\n");
            sb.append("  \"modLoader\": \"").append(escape(modLoader)).append("\",\n");
            sb.append("  \"skinType\": \"").append(escape(skinType)).append("\",\n");
            sb.append("  \"customSkinPath\": \"").append(escape(customSkinPath)).append("\",\n");
            sb.append("  \"skinName\": \"").append(escape(skinName)).append("\"\n");
            sb.append("}\n");
            Files.writeString(p, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Config save error: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
