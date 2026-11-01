package com.craftlauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;

public class Icons {

    private static Font fontAwesome;

    static {
        try (InputStream is = Icons.class.getResourceAsStream("/com/craftlauncher/fa-solid-900.ttf")) {
            if (is != null) {
                fontAwesome = Font.createFont(Font.TRUETYPE_FONT, is);
            } else {
                File f = new File("src/com/craftlauncher/fa-solid-900.ttf");
                if (f.exists()) {
                    fontAwesome = Font.createFont(Font.TRUETYPE_FONT, f);
                } else {
                    fontAwesome = new Font("Dialog", Font.PLAIN, 12);
                }
            }
        } catch (Exception e) {
            fontAwesome = new Font("Dialog", Font.PLAIN, 12);
        }
    }

    public static Font getFont(float size) {
        return fontAwesome.deriveFont(size);
    }

    // Creates an Icon from FontAwesome unicode glyph
    public static Icon fa(String code, float size, Color color) {
        int isize = Math.round(size);
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Font f = fontAwesome.deriveFont(size);
                g2.setFont(f);
                g2.setColor(color != null ? color : (c != null ? c.getForeground() : Color.WHITE));
                FontMetrics fm = g2.getFontMetrics(f);
                int stringW = fm.stringWidth(code);
                int ox = x + (isize - stringW) / 2;
                int oy = y + (isize - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(code, ox, oy);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return isize; }

            @Override
            public int getIconHeight() { return isize; }
        };
    }

    // Glyph constants from FontAwesome 6 Free Solid
    public static final String GAMEPAD = "\uf11b";
    public static final String GEAR = "\uf013";
    public static final String TERMINAL = "\uf120";
    public static final String INFO = "\uf05a";
    public static final String PLAY = "\uf04b";
    public static final String STOP = "\uf04d";
    public static final String FOLDER = "\uf07b";
    public static final String FOLDER_OPEN = "\uf07c";
    public static final String SYNC = "\uf021";
    public static final String SPARKLES = "\uf0d0";
    public static final String CUBES = "\uf1b3";
    public static final String HAMMER = "\uf6e3";
    public static final String SHIELD = "\uf3ed";
    public static final String CUBE = "\uf1b2";
    public static final String STAR = "\uf005";
    public static final String LOCK = "\uf023";
    public static final String USER = "\uf007";
    public static final String BOX_OPEN = "\uf49e";
    public static final String SUN = "\uf185";
    public static final String PALETTE = "\uf53f";
    public static final String COPY = "\uf0c5";
    public static final String TRASH = "\uf1f8";
    public static final String CHECK = "\uf00c";
    public static final String MICROCHIP = "\uf2db";
    public static final String COFFEE = "\uf0f4";

    // Standard Icon helpers
    public static Icon tabPlay(float size, Color color) { return fa(GAMEPAD, size, color); }
    public static Icon tabSettings(float size, Color color) { return fa(GEAR, size, color); }
    public static Icon tabConsole(float size, Color color) { return fa(TERMINAL, size, color); }
    public static Icon tabAbout(float size, Color color) { return fa(INFO, size, color); }

    public static Icon play(float size, Color color) { return fa(PLAY, size, color); }
    public static Icon stop(float size, Color color) { return fa(STOP, size, color); }
    public static Icon folder(float size, Color color) { return fa(FOLDER_OPEN, size, color); }
    public static Icon refresh(float size, Color color) { return fa(SYNC, size, color); }
    public static Icon optifine(float size, Color color) { return fa(SPARKLES, size, color); }
    public static Icon fabric(float size, Color color) { return fa(CUBES, size, color); }
    public static Icon forge(float size, Color color) { return fa(HAMMER, size, color); }
    public static Icon forgeOptifine(float size, Color color) { return fa(SHIELD, size, color); }
    public static Icon vanilla(float size, Color color) { return fa(CUBE, size, color); }
    public static Icon star(float size, Color color) { return fa(STAR, size, color); }
    public static Icon lock(float size, Color color) { return fa(LOCK, size, color); }
    public static Icon user(float size, Color color) { return fa(USER, size, color); }
    public static Icon mods(float size, Color color) { return fa(BOX_OPEN, size, color); }
    public static Icon shader(float size, Color color) { return fa(SUN, size, color); }
    public static Icon palette(float size, Color color) { return fa(PALETTE, size, color); }
    public static Icon copy(float size, Color color) { return fa(COPY, size, color); }
    public static Icon trash(float size, Color color) { return fa(TRASH, size, color); }
    public static Icon ram(float size, Color color) { return fa(MICROCHIP, size, color); }
    public static Icon java(float size, Color color) { return fa(COFFEE, size, color); }

    // Native Status Dot
    public static Icon dot(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : new Color(0x4A, 0xDE, 0x80));
                g2.fillOval(x + 2, y + 2, size - 4, size - 4);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }
}
