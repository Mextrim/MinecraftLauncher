package com.craftlauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Icons {

    @FunctionalInterface
    public interface Painter {
        void paint(Graphics2D g, int w, int h);
    }

    public static Icon create(int width, int height, Painter painter) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                painter.paint(g2, width, height);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return width; }

            @Override
            public int getIconHeight() { return height; }
        };
    }

    // Play icon (Triangle)
    public static Icon play(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : Color.WHITE);
            int pad = (int) (w * 0.15);
            int[] xs = {pad, w - pad, pad};
            int[] ys = {pad, h / 2, h - pad};
            g.fillPolygon(xs, ys, 3);
        });
    }

    // Folder icon
    public static Icon folder(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0xFB, 0xBF, 0x24));
            int tabW = (int) (w * 0.45);
            int tabH = (int) (h * 0.28);
            g.fillRoundRect(1, 1, tabW, tabH + 2, 3, 3);
            int bodyY = (int) (h * 0.28);
            g.fillRoundRect(1, bodyY, w - 2, h - bodyY - 1, 4, 4);
            g.setColor(new Color(255, 255, 255, 60));
            g.drawLine(2, bodyY + 1, w - 3, bodyY + 1);
        });
    }

    // Refresh icon (Circular arrow)
    public static Icon refresh(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x38, 0xBD, 0xF8));
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int pad = 2;
            g.drawArc(pad, pad, w - pad * 2, h - pad * 2, 45, 270);
            int[] xs = {w - pad - 4, w - pad + 1, w - pad - 2};
            int[] ys = {pad, pad, pad + 4};
            g.fillPolygon(xs, ys, 3);
        });
    }

    // OptiFine Sparkle Star
    public static Icon optifine(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x38, 0xBD, 0xF8));
            int cx = w / 2, cy = h / 2;
            int r = w / 2 - 1;
            int inner = Math.max(2, r / 3);
            int[] xs = {cx, cx + inner, cx + r, cx + inner, cx, cx - inner, cx - r, cx - inner};
            int[] ys = {cy - r, cy - inner, cy, cy + inner, cy + r, cy + inner, cy, cy - inner};
            g.fillPolygon(xs, ys, 8);
        });
    }

    // Fabric Woven Diamond
    public static Icon fabric(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0xF5, 0x9E, 0x0B));
            int cx = w / 2, cy = h / 2;
            int r = w / 2 - 1;
            int[] xs = {cx, cx + r, cx, cx - r};
            int[] ys = {cy - r, cy, cy + r, cy};
            g.setStroke(new BasicStroke(1.8f));
            g.drawPolygon(xs, ys, 4);
            g.drawLine(cx - r / 2, cy, cx + r / 2, cy);
            g.drawLine(cx, cy - r / 2, cx, cy + r / 2);
        });
    }

    // Forge Anvil
    public static Icon forge(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0xEF, 0x44, 0x44));
            int topY = (int) (h * 0.22);
            int topH = (int) (h * 0.26);
            g.fillRoundRect(1, topY, w - 2, topH, 2, 2);
            int midW = (int) (w * 0.38);
            g.fillRect((w - midW) / 2, topY + topH, midW, (int) (h * 0.25));
            int baseH = (int) (h * 0.22);
            g.fillRoundRect(2, h - baseH - 1, w - 4, baseH, 2, 2);
        });
    }

    // Forge + OptiFine Shield / Star
    public static Icon forgeOptifine(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0xA8, 0x55, 0xF7));
            int[] xs = {2, w - 3, w - 3, w / 2, 2};
            int[] ys = {2, 2, (int) (h * 0.62), h - 2, (int) (h * 0.62)};
            g.fillPolygon(xs, ys, 5);
            g.setColor(Color.WHITE);
            int cx = w / 2, cy = (int) (h * 0.45);
            g.fillRect(cx - 1, 4, 2, (int) (h * 0.6));
            g.fillRect(4, cy - 1, w - 8, 2);
        });
    }

    // Vanilla Isometric Cube
    public static Icon vanilla(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            int cx = w / 2, cy = h / 2;
            int r = w / 2 - 1;
            g.setColor(new Color(0x22, 0xC5, 0x5E));
            int[] tx = {cx, cx + r, cx, cx - r};
            int[] ty = {cy - r + 2, cy - r / 2 + 2, cy + 2, cy - r / 2 + 2};
            g.fillPolygon(tx, ty, 4);

            g.setColor(new Color(0x15, 0x80, 0x3D));
            int[] lx = {cx - r, cx, cx, cx - r};
            int[] ly = {cy - r / 2 + 2, cy + 2, cy + r - 1, cy + r / 2 - 1};
            g.fillPolygon(lx, ly, 4);

            g.setColor(new Color(0x16, 0x65, 0x34));
            int[] rx = {cx, cx + r, cx + r, cx};
            int[] ry = {cy + 2, cy - r / 2 + 2, cy + r / 2 - 1, cy + r - 1};
            g.fillPolygon(rx, ry, 4);
        });
    }

    // 5-Point Star
    public static Icon star(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0xFA, 0xCC, 0x15));
            int cx = w / 2, cy = h / 2;
            int r = w / 2 - 1;
            int inner = (int) (r * 0.45);
            int[] xs = new int[10];
            int[] ys = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                int radius = (i % 2 == 0) ? r : inner;
                xs[i] = cx + (int) (radius * Math.cos(angle));
                ys[i] = cy + (int) (radius * Math.sin(angle));
            }
            g.fillPolygon(xs, ys, 10);
        });
    }

    // Padlock
    public static Icon lock(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x94, 0xA3, 0xB8));
            int shackleW = (int) (w * 0.52);
            int shackleH = (int) (h * 0.44);
            int shackleX = (w - shackleW) / 2;
            g.setStroke(new BasicStroke(1.8f));
            g.drawArc(shackleX, 2, shackleW, shackleH * 2, 0, 180);
            int bodyY = (int) (h * 0.42);
            g.fillRoundRect(shackleX - 2, bodyY, shackleW + 4, h - bodyY - 1, 3, 3);
            g.setColor(new Color(0x10, 0x12, 0x1B));
            g.fillOval(w / 2 - 2, bodyY + 3, 4, 4);
            g.fillRect(w / 2 - 1, bodyY + 5, 2, 3);
        });
    }

    // Shader Sparkle/Sun
    public static Icon shader(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0xE8, 0x79, 0xF9));
            int cx = w / 2, cy = h / 2;
            int r = w / 2 - 2;
            g.fillOval(cx - 3, cy - 3, 6, 6);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(cx, 1, cx, h - 2);
            g.drawLine(1, cy, w - 2, cy);
            g.drawLine(cx - r / 2, cy - r / 2, cx + r / 2, cy + r / 2);
            g.drawLine(cx - r / 2, cy + r / 2, cx + r / 2, cy - r / 2);
        });
    }

    // Art Palette
    public static Icon palette(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x38, 0xBD, 0xF8));
            g.fillOval(1, 1, w - 2, h - 2);
            g.setColor(new Color(0x13, 0x17, 0x24));
            g.fillOval(w - 7, h - 7, 5, 5);
            g.setColor(new Color(0xEF, 0x44, 0x44)); g.fillOval(3, 4, 3, 3);
            g.setColor(new Color(0xFA, 0xCC, 0x15)); g.fillOval(7, 3, 3, 3);
            g.setColor(new Color(0x22, 0xC5, 0x5E)); g.fillOval(10, 6, 3, 3);
        });
    }

    // Copy Logs
    public static Icon copy(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x94, 0xA3, 0xB8));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(1, 1, w - 5, h - 5, 2, 2);
            g.drawRoundRect(4, 4, w - 5, h - 5, 2, 2);
        });
    }

    // Trash Can
    public static Icon trash(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x94, 0xA3, 0xB8));
            g.fillRect(2, 2, w - 4, 2);
            g.fillRect(w / 2 - 2, 0, 4, 2);
            g.drawRect(3, 5, w - 7, h - 7);
            g.drawLine(w / 2 - 2, 7, w / 2 - 2, h - 4);
            g.drawLine(w / 2 + 1, 7, w / 2 + 1, h - 4);
        });
    }

    // Stop Square
    public static Icon stop(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : Color.WHITE);
            g.fillRect(3, 3, w - 6, h - 6);
        });
    }

    // Status Dot
    public static Icon dot(int size, Color color) {
        return create(size, size, (g, w, h) -> {
            g.setColor(color != null ? color : new Color(0x4A, 0xDE, 0x80));
            g.fillOval(2, 2, w - 4, h - 4);
        });
    }
}
