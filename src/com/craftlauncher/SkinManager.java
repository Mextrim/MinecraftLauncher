package com.craftlauncher;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class SkinManager {

    public static final Map<String, String> PRESET_BASE64 = new LinkedHashMap<>();
    public static final Map<String, String> PRESET_NAMES = new LinkedHashMap<>();

    static {
        PRESET_NAMES.put("steve", "Стив (Классический)");
        PRESET_NAMES.put("alex", "Алекс (Классическая)");
        PRESET_NAMES.put("creeper_hoodie", "Крипер в худи");
        PRESET_NAMES.put("cyberpunk", "Киберпанк (Неон)");
        PRESET_NAMES.put("military", "Военный SWAT");
        PRESET_NAMES.put("shadow", "Тёмный ассасин");

        PRESET_BASE64.put("steve", "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAEwElEQVR4Xu1av2sUURgMqCABQQVBBK0SCdooMQQD5jSFkNgpKdIEwSZoZ6GYJohNUmlhqrSxsUlhYZM/If/TmdncrLOz3+7drcneXtyB4f36bvNm3vd2l32ZmOiD+3eudMHZqWtJyTrbn189LqVfb+xAwZ17N1LxqLsBH17MZniuDFDBXn5b6+TEkxjz640dXLSmP8r/wgDd+74VILJoC5wLA3TFNRsgHqRQv/mh7+v64vkwwO8BSoiE4KLSrzd2UNF+QwQpdnn6aobs9+s1DpraFDZ183Im5ZkFSjVBf6PX0WtEcSh9PrUjmiBIIVpX8V7yd5Foxvn10efzqR0qwifMvr03y91fn9a7v7+8S8qf79eSvkfT13OxINvs0zpNYJ/Pp3ZwQjpRXUkIhWAIp3gSY5Go6JqsM55tn0/t0ImxxCMM4iACQoGXT3ZS0agDaCMGffhNdC0aGhnUCAN8hShIyRV/+/RBQs0AJUX5yuv9QQ1B2+dTOzgRlFx5PsO/v15MX2jwaPu4NJdw6fZkJoaPPGaCXtMN8LbPp3bwrU3FuwkoIRomsKRoGqS/5fX0zfDurckM2e/zGTkWHu50lfPz8wlnZmYSenwOR0fp9lBj0Ycxv77TL5fDwUE34f5+Znt6WGX4hCicRnh8DsciVbyacNoGqNEeVhk+oSoZcNYGTB4epgY0MgPOdAscC9ct8M8Z8Gxurwvij6N8vvAjJftYkjp+cXc3Q0/PVHxv4kXxYKafQpW9uLS9tZWlxg4KChzUAB0HMdlL29sJU0E9oUxP74/iMyK9j/2RARsbJ6QBHB8UZQI9O3wchJALm5sJUdcURel9Hu8muFkZ8WJGMlZkADgoygQWGaB1iiH7CfL4UJi0c2mupHg1gRwULpDtqE/HSBeUWXEzQFc/MiC39/f/3uh4s9N6RI67zkK4Ab7CkSHa9pRODegJcQM83jMkFd9r66OON1Rtq+hKj8XIAN8GZeMURXoK66p6bBTvv3VhKj4ygKXrLASFFQnsZwAmqWKSDNAVZTb0BHl8aIDED2oAykoZ4OmtwpUeR1KQiqNwF8cYj8sYIOIR4yvrBqgRlQxYWVnpgn5zczLOqaIiM1Sgx6YGiGgVD6rgoizQLTC0AXi97XQ6GVGrq6s5oYjxWNR1i6hhmjk0gsI8RrePxiATIwM0K9wAlq6zEHzHpzgXSOFRHOpqAKnbiCVX1OP9XsMY9pcJ1/3vRrjOFi1atGjRosX4AR9Ai5h5fT44+ViajrVo0aJFixYtWtQN/5Q29OGqfELTDyEe1li4AUMfr4sBmZPlcYEbUCUD+JrLDBhrAypnwHFZ6airbujXX//I6WM6zpjcgaefEbDtbAoGEcls8DEwPOJWA9wEtpsCNwClG6Dj/QxID0vcCK83BS7QRfq49iUGlJ3xl5HQTOmZ498NnKdqoItnO+rTMdLP9/XG5wce2u/zGBmKBPtq67jGu1h99Hm7kY/FSGC/LaB0A4pEs5+lz2NkcIEoIwMic8DofM/Fq/DGZYCnv6+09kfbJNrnkXDv93mMDDxJpsAi+nE7WSY+Et44A3hcrqKG+f+CyAAV7Qaw7fMYGaL/G1CBFB7FoQ5BRfeByAiO+zyq4g8lK5z2I+oYkQAAAABJRU5ErkJggg==");
        PRESET_BASE64.put("alex", "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");
        PRESET_BASE64.put("creeper_hoodie", "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");
        PRESET_BASE64.put("cyberpunk", "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");
        PRESET_BASE64.put("military", "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgCAYAAACinX6EAAAFHUlEQVR42t2Y32tcVRDHp21MuxvFKNVai1pEW6SgRR980AfBV1Ef/AtERPBntViVUPoj0SZWjSIEA5pCQLAsxvgQAxJCJEIk5CEQ8hYCIeQlBEIIgRBl7Gd253pzuzfnLrtrsj0w7PfMd/bec+b8mJkrEmifyZfaLb16QX7UDrluAkYH19/fr4ODgzo6Oqrj4+MmYHRw0uitW77Ty/KDTfxr+cUEjA5uYGBAh4aGbOKTk5MmYHRwDe+ADrlmE74ov+mnMmoCLu6Gazo8PGwTnpmZ0bm5ORMwOriGd0Cb9OsX8rNNvDv/lwkYHdzIyIhOTEzYxD+/0UfA6OAa3gGcd7Z90gHo4DjvbHuOgzsAjA7uljgCXVKwbd954A8TMDq4j+SsnpV2vSLddjEiYHRwt8Al2GvbnQlfkV9NwOjg3pEzkRPapcvEJw/X8A5gNZnsefnJfuMY7g15Sd+U9/WcXNAz0mECRgdXizsIyaqvuIXifL35LJPfyQFVO4ELjYf4LxcYvx7nQ3lAFv5o7pCePHKvcY8/+IBhdHfv36cZV7+cnbpNTeK8x/hknA/lAdXyezLO03wnhPIA5+0/JR6d74bjrXfqqWP363Onn4h4MLqanOF6xHmax3mElufWL/H50oSdxzbugHieEMojdt0B8TvA4zxtfX092sLuAOfdAc5jS3OeyfFM5x87ep8+ffKEcc8/9aRhdMda8rvvAG5jButxfmtryya0tLSk8/PzJmB0r8i7Jmk8//U8gYnjCJ7vyVQ8j0AHtyfifKd8r6urq7qxsWECXlhYsKIGAVfK80wckGsSPXKwWR+957DdFZx9MDzv3nOJzwfyoX4il0zAfX19WigUTMCh/3fJVfVdRcKEFMNrr8K9Ji9ahnhO2ixhKkqb6eDqPb5g87ze83lqeqo6JEt9T/Rgi7fL73q1+U8TMDq49+StUu3wreUKCBgdXL3H9z84oLeUDI3oV7kJE7DfB6x0p3Te0PXE8oQe02WpHerugFodgZWVlehOACePwMdyPqodwLt5BCzV5Cw69n7xnG4/q953Owqe1+VlEzDbmIGxImNjYyZgdHBp9sn3uH3a+7P2g7Nn63kZC660H3dE3IF8EJmamjIBs8pM6m159SZbhGft3yfRc7F3Dh2c98FI2vicT6khtjdCFOeQpASc7D98e05PtN5hAkYHhw3YJ4WAOZceGskPEA+ZcOXsPRT7neAhEi6pr7SfoRi6bhWaV3HJqi7U91UpXWDaLLfp4uKirq2t6ebmpgmY6g+unH25ajJ3sMhROfr4wGl9rzKTVWfQAaSlFCekwuB4n6otWcV5HxtsfVV9NbnRmXCyoYMrZ+/VIs9E/qsWe1Lfn7WfqRaIFymV9rnF+fyFgLntufmTDR2c27P6h6RJDzcduKla9GoTe9e/8OwzkYPAofG5fdABaS9A0l7gfOkF0YQQ7gW2X7JxFDiTSYd5ylyuWoSrdoGCDkh+7a207x9A/cMo29arQ/37n8gBVH+traJN0hw5DYzOq0Uqxe1fnXu02vFJOHEp6OnjD5mAy/XjVVySj19qhC5u73h1GK8u4TzpidunVYsUUjiHd6EHez/L+LANOoAb2L/seG4e7z9yV8uOPCvpYc2dkW8Ru/WXl5dNiAqzs7PR9nd7PwZd8s225/JfngEXGl+oH3QAicr09LSJJy6V9OOJkMd2sr80u7TEiUTGvyKDyeR4Vg3Gt3NLpqyV9uudClc7vtD8/wXvLJ6KaEPmBgAAAABJRU5ErkJggg==");
        PRESET_BASE64.put("shadow", "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");
    }

    public static BufferedImage getSkinImage(Config config) {
        if (config.customSkinPath != null && !config.customSkinPath.isBlank()) {
            File f = new File(config.customSkinPath);
            if (f.exists()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) return img;
                } catch (Exception ignored) {}
            }
        }

        String type = config.skinType != null ? config.skinType : "steve";
        String b64 = PRESET_BASE64.get(type);
        if (b64 != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(b64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) return img;
            } catch (Exception ignored) {}
        }

        return createFallbackSteve();
    }

    public static BufferedImage loadCustomSkin(File file) throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IllegalArgumentException("Выбранный файл не является корректным изображением!");
        }
        if ((img.getWidth() != 64 || img.getHeight() != 64) && (img.getWidth() != 64 || img.getHeight() != 32)) {
            throw new IllegalArgumentException("Размер скина должен быть 64x64 или 64x32 пикселей (текущий: " + img.getWidth() + "x" + img.getHeight() + ")");
        }
        return img;
    }

    public static BufferedImage downloadSkinByNickname(String nickname) throws Exception {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Никнейм не может быть пустым!");
        }
        String cleanNick = nickname.trim();
        String[] urls = {
            "https://minotar.net/skin/" + cleanNick,
            "https://mineskin.eu/skin/" + cleanNick
        };

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        for (String u : urls) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(u))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
                HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() == 200) {
                    BufferedImage img = ImageIO.read(res.body());
                    if (img != null && img.getWidth() == 64) {
                        return img;
                    }
                }
            } catch (Exception ignored) {}
        }
        throw new IOException("Не удалось загрузить скин для игрока: " + cleanNick + " (проверьте лицензионный ник)");
    }

    public static void applySkinToGame(BufferedImage skin, String gameDir, String nickname) {
        if (skin == null || gameDir == null) return;
        try {
            Path base = Paths.get(gameDir);
            Path skinsDir = base.resolve("skins");
            Files.createDirectories(skinsDir);

            // Save skin.png in .minecraft root
            ImageIO.write(skin, "png", base.resolve("skin.png").toFile());
            ImageIO.write(skin, "png", skinsDir.resolve("current_skin.png").toFile());

            // Build active resource pack: .minecraft/resourcepacks/MexSkinPack
            Path rpDir = base.resolve("resourcepacks").resolve("MexSkinPack");
            Path wideDir = rpDir.resolve("assets").resolve("minecraft").resolve("textures").resolve("entity").resolve("player").resolve("wide");
            Path slimDir = rpDir.resolve("assets").resolve("minecraft").resolve("textures").resolve("entity").resolve("player").resolve("slim");
            Files.createDirectories(wideDir);
            Files.createDirectories(slimDir);

            // Write pack.mcmeta
            String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"Mex Launcher - Скин для друганов\"\n  }\n}";
            Files.writeString(rpDir.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);

            // Write wide (Steve) and slim (Alex) textures
            ImageIO.write(skin, "png", wideDir.resolve("steve.png").toFile());
            ImageIO.write(skin, "png", slimDir.resolve("alex.png").toFile());

            // Also support CustomSkinLoader mod if present
            if (nickname != null && !nickname.isBlank()) {
                Path cslDir = base.resolve("CustomSkinLoader").resolve("LocalSkin").resolve("skins");
                Files.createDirectories(cslDir);
                ImageIO.write(skin, "png", cslDir.resolve(nickname + ".png").toFile());
            }

            // Ensure MexSkinPack is active in options.txt
            activateResourcePack(base.resolve("options.txt"));

        } catch (Exception e) {
            System.err.println("[SkinManager] Ошибка применения скина: " + e.getMessage());
        }
    }

    private static void activateResourcePack(Path optionsFile) {
        try {
            if (!Files.exists(optionsFile)) return;
            String content = Files.readString(optionsFile, StandardCharsets.UTF_8);
            if (!content.contains("file/MexSkinPack")) {
                if (content.contains("resourcePacks:[")) {
                    content = content.replace("resourcePacks:[", "resourcePacks:[\"file/MexSkinPack\",");
                } else {
                    content += "\nresourcePacks:[\"file/MexSkinPack\"]\n";
                }
                Files.writeString(optionsFile, content, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
    }

    public static BufferedImage renderCharacter2D(BufferedImage skin, int targetW, int targetH) {
        if (skin == null) skin = createFallbackSteve();

        BufferedImage canvas = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        // 1. Head (8, 8, 8, 8) at (4, 0)
        drawRegion(skin, g, 8, 8, 8, 8, 4, 0);
        // Head hat overlay (40, 8, 8, 8)
        drawRegion(skin, g, 40, 8, 8, 8, 4, 0);

        // 2. Torso (20, 20, 8, 12) at (4, 8)
        drawRegion(skin, g, 20, 20, 8, 12, 4, 8);
        if (skin.getHeight() >= 64) {
            drawRegion(skin, g, 20, 36, 8, 12, 4, 8); // Jacket
        }

        // 3. Right Arm (44, 20, 4, 12) at (0, 8)
        drawRegion(skin, g, 44, 20, 4, 12, 0, 8);
        if (skin.getHeight() >= 64) {
            drawRegion(skin, g, 44, 36, 4, 12, 0, 8); // Sleeve
        }

        // 4. Left Arm
        if (skin.getHeight() >= 64) {
            drawRegion(skin, g, 36, 52, 4, 12, 12, 8);
            drawRegion(skin, g, 52, 52, 4, 12, 12, 8);
        } else {
            // Flip right arm for legacy 64x32
            drawFlippedRegion(skin, g, 44, 20, 4, 12, 12, 8);
        }

        // 5. Right Leg (4, 20, 4, 12) at (4, 20)
        drawRegion(skin, g, 4, 20, 4, 12, 4, 20);
        if (skin.getHeight() >= 64) {
            drawRegion(skin, g, 4, 36, 4, 12, 4, 20); // Pants
        }

        // 6. Left Leg
        if (skin.getHeight() >= 64) {
            drawRegion(skin, g, 20, 52, 4, 12, 8, 20);
            drawRegion(skin, g, 4, 52, 4, 12, 8, 20);
        } else {
            // Flip right leg for legacy
            drawFlippedRegion(skin, g, 4, 20, 4, 12, 8, 20);
        }

        g.dispose();

        // Scale to target size with crisp nearest-neighbor interpolation
        BufferedImage result = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gRes = result.createGraphics();
        gRes.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gRes.drawImage(canvas, 0, 0, targetW, targetH, null);
        gRes.dispose();

        return result;
    }

    public static BufferedImage renderHeadAvatar(BufferedImage skin, int size) {
        if (skin == null) skin = createFallbackSteve();

        BufferedImage head = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gh = head.createGraphics();
        drawRegion(skin, gh, 8, 8, 8, 8, 0, 0);
        drawRegion(skin, gh, 40, 8, 8, 8, 0, 0);
        gh.dispose();

        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gRes = result.createGraphics();
        gRes.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gRes.drawImage(head, 0, 0, size, size, null);
        gRes.dispose();

        return result;
    }

    private static void drawRegion(BufferedImage src, Graphics2D g, int sx, int sy, int w, int h, int dx, int dy) {
        if (sx + w <= src.getWidth() && sy + h <= src.getHeight()) {
            BufferedImage sub = src.getSubimage(sx, sy, w, h);
            g.drawImage(sub, dx, dy, null);
        }
    }

    private static void drawFlippedRegion(BufferedImage src, Graphics2D g, int sx, int sy, int w, int h, int dx, int dy) {
        if (sx + w <= src.getWidth() && sy + h <= src.getHeight()) {
            BufferedImage sub = src.getSubimage(sx, sy, w, h);
            g.drawImage(sub, dx + w, dy, -w, h, null);
        }
    }

    private static BufferedImage createFallbackSteve() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0xBD, 0x8B, 0x72));
        g.fillRect(8, 8, 8, 8);
        g.setColor(new Color(0x4A, 0x32, 0x19));
        g.fillRect(8, 8, 8, 2);
        g.setColor(new Color(0x28, 0x3B, 0xE0));
        g.fillRect(10, 12, 1, 1);
        g.fillRect(13, 12, 1, 1);
        g.dispose();
        return img;
    }
}
