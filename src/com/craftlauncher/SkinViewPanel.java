package com.craftlauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SkinViewPanel extends JPanel {

    private BufferedImage skinImage;
    private BufferedImage character2D;
    private BufferedImage headAvatar;
    private String skinName = "Стив (Классический)";
    private UITheme currentTheme;

    public SkinViewPanel(Config config, UITheme theme) {
        this.currentTheme = theme;
        setPreferredSize(new Dimension(130, 200));
        setMinimumSize(new Dimension(110, 180));
        setOpaque(false);
        refreshFromConfig(config);
    }

    public void setTheme(UITheme theme) {
        this.currentTheme = theme;
        repaint();
    }

    public void refreshFromConfig(Config config) {
        this.skinImage = SkinManager.getSkinImage(config);
        this.character2D = SkinManager.renderCharacter2D(skinImage, 80, 160);
        this.headAvatar = SkinManager.renderHeadAvatar(skinImage, 48);
        this.skinName = config.skinName != null ? config.skinName : "Стив (Классический)";
        repaint();
    }

    public void setCustomSkin(BufferedImage skin, String name) {
        this.skinImage = skin;
        this.skinName = name;
        this.character2D = SkinManager.renderCharacter2D(skin, 80, 160);
        this.headAvatar = SkinManager.renderHeadAvatar(skin, 48);
        repaint();
    }

    public BufferedImage getSkinImage() {
        return skinImage;
    }

    public BufferedImage getHeadAvatar() {
        return headAvatar;
    }

    public String getSkinName() {
        return skinName;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background podium card
        Color bg = currentTheme != null ? currentTheme.bgCard : new Color(0x13, 0x1A, 0x2A);
        Color border = currentTheme != null ? currentTheme.borderColor : new Color(0x2A, 0x35, 0x4B);

        g2.setColor(bg);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 14, 14);

        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(2, 2, w - 4, h - 4, 14, 14);

        // Character shadow ellipse on the floor
        int shadowW = 68;
        int shadowH = 12;
        int shadowX = (w - shadowW) / 2;
        int shadowY = h - 26;
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(shadowX, shadowY, shadowW, shadowH);

        // Draw 2D character
        if (character2D != null) {
            int charW = character2D.getWidth();
            int charH = character2D.getHeight();
            // Scale to fit nicely
            int drawH = Math.min(charH, h - 40);
            int drawW = (int) (charW * ((double) drawH / charH));
            int drawX = (w - drawW) / 2;
            int drawY = 12;

            g2.drawImage(character2D, drawX, drawY, drawW, drawH, null);
        }

        // Active skin badge at the bottom
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        String text = skinName;
        if (text.length() > 16) text = text.substring(0, 14) + "..";
        int textW = fm.stringWidth(text);
        int badgeW = textW + 12;
        int badgeH = 16;
        int badgeX = (w - badgeW) / 2;
        int badgeY = h - 22;

        Color accent = currentTheme != null ? currentTheme.accentPrimary : new Color(0x10, 0xB9, 0x81);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);

        g2.setColor(accent);
        g2.drawString(text, badgeX + 6, badgeY + 12);

        g2.dispose();
    }
}
