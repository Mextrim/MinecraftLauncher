package com.craftlauncher;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class UITheme {

    public final String id;
    public final String name;
    public final String desc;
    public final Color bgMain;
    public final Color bgTitlebar;
    public final Color bgCard;
    public final Color bgCardAlt;
    public final Color bgInput;
    public final Color borderColor;
    public final Color accentPrimary;
    public final Color accentSecondary;
    public final Color accentHover;
    public final Color accentGlow;
    public final Color textHighlight;
    public final Color previewColor;

    public UITheme(String id, String name, String desc,
                   Color bgMain, Color bgTitlebar, Color bgCard, Color bgCardAlt, Color bgInput,
                   Color borderColor, Color accentPrimary, Color accentSecondary,
                   Color accentHover, Color textHighlight, Color previewColor) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.bgMain = bgMain;
        this.bgTitlebar = bgTitlebar;
        this.bgCard = bgCard;
        this.bgCardAlt = bgCardAlt;
        this.bgInput = bgInput;
        this.borderColor = borderColor;
        this.accentPrimary = accentPrimary;
        this.accentSecondary = accentSecondary;
        this.accentHover = accentHover;
        this.accentGlow = new Color(accentPrimary.getRed(), accentPrimary.getGreen(), accentPrimary.getBlue(), 90);
        this.textHighlight = textHighlight;
        this.previewColor = previewColor;
    }

    public static final Map<String, UITheme> ALL_THEMES = new LinkedHashMap<>();

    static {
        ALL_THEMES.put("emerald", new UITheme(
            "emerald", "Изумруд", "Классический стиль Minecraft",
            new Color(0x0B, 0x0D, 0x13), new Color(0x11, 0x13, 0x1B), new Color(0x14, 0x17, 0x22), new Color(0x0E, 0x10, 0x18),
            new Color(0x0D, 0x0F, 0x17), new Color(0x22, 0x28, 0x3A), new Color(0x22, 0xC5, 0x5E),
            new Color(0x15, 0x80, 0x3D), new Color(0x16, 0xA3, 0x4A), new Color(0x81, 0x8C, 0xF8),
            new Color(0x22, 0xC5, 0x5E)
        ));

        ALL_THEMES.put("diamond", new UITheme(
            "diamond", "Алмаз", "Кристальный неон",
            new Color(0x07, 0x0B, 0x13), new Color(0x0D, 0x12, 0x1E), new Color(0x10, 0x17, 0x26), new Color(0x0A, 0x10, 0x1B),
            new Color(0x0A, 0x10, 0x19), new Color(0x1D, 0x2B, 0x42), new Color(0x38, 0xBD, 0xF8),
            new Color(0x02, 0x84, 0xC7), new Color(0x03, 0x69, 0xA1), new Color(0x7D, 0xD3, 0xFC),
            new Color(0x38, 0xBD, 0xF8)
        ));

        ALL_THEMES.put("nether", new UITheme(
            "nether", "Незерит", "Огненный магматический стиль",
            new Color(0x11, 0x0B, 0x0A), new Color(0x1A, 0x10, 0x0E), new Color(0x20, 0x15, 0x12), new Color(0x16, 0x0D, 0x0B),
            new Color(0x14, 0x0C, 0x0B), new Color(0x3B, 0x22, 0x1D), new Color(0xF5, 0x9E, 0x0B),
            new Color(0xD9, 0x77, 0x06), new Color(0xB4, 0x53, 0x09), new Color(0xFB, 0xBF, 0x24),
            new Color(0xF5, 0x9E, 0x0B)
        ));

        ALL_THEMES.put("amethyst", new UITheme(
            "amethyst", "Аметист", "Мистический кристалл",
            new Color(0x0D, 0x09, 0x15), new Color(0x14, 0x0E, 0x1F), new Color(0x19, 0x12, 0x27), new Color(0x11, 0x0B, 0x1A),
            new Color(0x11, 0x0B, 0x1A), new Color(0x2E, 0x1E, 0x42), new Color(0xA8, 0x55, 0xF7),
            new Color(0x93, 0x33, 0xEA), new Color(0x7E, 0x22, 0xCE), new Color(0xC0, 0x84, 0xFC),
            new Color(0xA8, 0x55, 0xF7)
        ));

        ALL_THEMES.put("redstone", new UITheme(
            "redstone", "Редстоун", "Заряженная энергия",
            new Color(0x12, 0x07, 0x07), new Color(0x1A, 0x0B, 0x0B), new Color(0x22, 0x10, 0x10), new Color(0x15, 0x08, 0x08),
            new Color(0x13, 0x09, 0x09), new Color(0x3D, 0x1A, 0x1A), new Color(0xEF, 0x44, 0x44),
            new Color(0xDC, 0x26, 0x26), new Color(0xB9, 0x1C, 0x1C), new Color(0xF8, 0x71, 0x71),
            new Color(0xEF, 0x44, 0x44)
        ));
    }

    public static UITheme get(String id) {
        UITheme t = ALL_THEMES.get(id);
        return t != null ? t : ALL_THEMES.get("emerald");
    }
}
