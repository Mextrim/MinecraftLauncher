package com.craftlauncher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {

    private Config config;
    private UITheme currentTheme;
    private final Downloader downloader = new Downloader();
    private List<Downloader.VersionEntry> allVersions = new ArrayList<>();
    private Process runningProcess = null;

    // UI Components
    private JPanel rootPanel;
    private JPanel titlebar;
    private JButton btnTabPlay, btnTabSettings, btnTabConsole, btnTabAbout;
    private CardLayout contentLayout;
    private JPanel contentCards;

    // Play View
    private AvatarPanel avatarPanel;
    private JTextField txtNickname;
    private JComboBox<Downloader.VersionEntry> cbVersions;
    private JCheckBox chkSnapshots;
    private JButton btnRefresh;
    private JLabel lblStatus;
    private JLabel lblSummary;
    private JLabel lblPercent;
    private JProgressBar progressBar;
    private JButton btnPlay;
    private JButton btnStopGame;
    private JPanel statusBadge;
    private JLabel lblModDesc;
    private String currentModLoader = "all";

    // Settings View
    private JSlider sliderRam;
    private JLabel lblRamVal;
    private JTextField txtJavaPath;
    private DefaultListModel<JavaDetector.JavaItem> javaListModel;
    private JList<JavaDetector.JavaItem> listJavas;
    private JTextField txtGameDir;
    private JTextField txtWidth, txtHeight;
    private JCheckBox chkFullscreen;
    private JTextField txtJvmArgs;
    private JPanel themeGalleryPanel;

    // Console View
    private JTextArea consoleArea;
    private JPanel consoleStatusPill;
    private JButton btnClearLogs, btnCopyLogs, btnKillGame;

    // Window Dragging
    private Point mouseClickPoint;

    public Main() {
        this.config = Config.load();
        this.currentTheme = UITheme.get(config.theme);

        initWindow();
        initComponents();
        applyTheme(this.currentTheme);

        setSize(1040, 700);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);

        new Thread(this::loadInitialData).start();
    }

    private void initWindow() {
        setTitle("Minecraft Launcher");
        setIconImage(createLauncherIcon());
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        rootPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bgMain);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(currentTheme.borderColor);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        rootPanel.setOpaque(false);
        setContentPane(rootPanel);
    }

    private Image createLauncherIcon() {
        int s = 64;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(s, s, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int p = s / 8;
        g.setColor(new Color(134, 96, 67));
        g.fillRect(0, 0, s, s);
        g.setColor(new Color(87, 61, 38));
        g.fillRect(1 * p, 4 * p, p, p);
        g.fillRect(4 * p, 5 * p, p, p);
        g.fillRect(2 * p, 6 * p, p, p);
        g.fillRect(6 * p, 4 * p, p, p);
        g.fillRect(5 * p, 7 * p, p, p);
        g.setColor(new Color(91, 140, 50));
        g.fillRect(0, 0, s, 2 * p);
        g.fillRect(1 * p, 2 * p, p, p);
        g.fillRect(1 * p, 3 * p, p, p);
        g.fillRect(3 * p, 2 * p, p, p);
        g.fillRect(4 * p, 2 * p, p, p);
        g.fillRect(6 * p, 2 * p, p, p);
        g.fillRect(6 * p, 3 * p, p, p);
        g.setColor(new Color(115, 173, 56));
        g.fillRect(0, 0, s, 1 * p);
        g.fillRect(2 * p, 1 * p, 2 * p, p);
        g.fillRect(5 * p, 1 * p, 2 * p, p);
        g.setColor(new Color(255, 255, 255, 120));
        g.drawRect(0, 0, s - 1, s - 1);
        g.dispose();
        return img;
    }

    private void initComponents() {
        // ================= TITLEBAR =================
        titlebar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(currentTheme.bgTitlebar);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() + 10, 16, 16));
                g2.setColor(currentTheme.borderColor);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        titlebar.setPreferredSize(new Dimension(0, 50));
        titlebar.setOpaque(false);

        titlebar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseClickPoint = e.getPoint();
            }
        });
        titlebar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - mouseClickPoint.x, p.y + e.getY() - mouseClickPoint.y);
            }
        });

        // Brand Left
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        brandPanel.setOpaque(false);

        JPanel logoBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.accentPrimary);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        logoBadge.setPreferredSize(new Dimension(30, 30));
        logoBadge.setOpaque(false);
        JLabel logoPick = new JLabel("MC");
        logoPick.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoPick.setForeground(Color.WHITE);
        logoBadge.add(logoPick);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 0));
        titleBlock.setOpaque(false);
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titleRow.setOpaque(false);

        JLabel mcLabel = new JLabel("MINECRAFT");
        mcLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mcLabel.setForeground(Color.WHITE);

        JLabel lcLabel = new JLabel("LAUNCHER");
        lcLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lcLabel.setForeground(currentTheme.accentPrimary);

        titleRow.add(mcLabel);
        titleRow.add(lcLabel);

        JLabel edLabel = new JLabel("JAVA 25 EDITION");
        edLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
        edLabel.setForeground(new Color(0x64, 0x74, 0x8B));

        titleBlock.add(titleRow);
        titleBlock.add(edLabel);

        brandPanel.add(logoBadge);
        brandPanel.add(titleBlock);
        titlebar.add(brandPanel, BorderLayout.WEST);

        // Center: Navigation Tabs
        JPanel navTabsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
        navTabsPanel.setOpaque(false);

        btnTabPlay = createNavTab("ИГРАТЬ", "play", true);
        btnTabSettings = createNavTab("НАСТРОЙКИ", "settings", false);
        btnTabConsole = createNavTab("КОНСОЛЬ", "console", false);
        btnTabAbout = createNavTab("О ЛАУНЧЕРЕ", "about", false);

        navTabsPanel.add(btnTabPlay);
        navTabsPanel.add(btnTabSettings);
        navTabsPanel.add(btnTabConsole);
        navTabsPanel.add(btnTabAbout);
        titlebar.add(navTabsPanel, BorderLayout.CENTER);

        // Right: Tools, Theme Dots & Window Controls
        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBar.setOpaque(false);

        // Quick folder button
        JButton btnOpenGameDir = createSmallButton("📁 .minecraft", e -> openDirectory(config.gameDir));
        rightBar.add(btnOpenGameDir);

        // Theme Switcher dots panel
        JPanel themeDotsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 15));
        themeDotsPanel.setOpaque(false);
        for (UITheme theme : UITheme.ALL_THEMES.values()) {
            JButton dot = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(theme.accentPrimary);
                    g2.fillOval(2, 2, 14, 14);
                    if (theme.id.equalsIgnoreCase(currentTheme.id)) {
                        g2.setColor(Color.WHITE);
                        g2.drawOval(1, 1, 16, 16);
                    }
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(18, 18));
            dot.setOpaque(false);
            dot.setContentAreaFilled(false);
            dot.setBorderPainted(false);
            dot.setFocusPainted(false);
            dot.setCursor(new Cursor(Cursor.HAND_CURSOR));
            dot.setToolTipText("Стиль: " + theme.name);
            dot.addActionListener(e -> {
                applyTheme(theme);
                config.theme = theme.id;
                config.save();
            });
            themeDotsPanel.add(dot);
        }
        rightBar.add(themeDotsPanel);

        // Window Controls
        JPanel winCtrlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        winCtrlPanel.setOpaque(false);
        JButton btnMin = createWinControlBtn("—", e -> setState(Frame.ICONIFIED), false);
        JButton btnMax = createWinControlBtn("□", e -> {
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH) setExtendedState(JFrame.NORMAL);
            else setExtendedState(JFrame.MAXIMIZED_BOTH);
        }, false);
        JButton btnClose = createWinControlBtn("✕", e -> System.exit(0), true);

        winCtrlPanel.add(btnMin);
        winCtrlPanel.add(btnMax);
        winCtrlPanel.add(btnClose);
        rightBar.add(winCtrlPanel);

        titlebar.add(rightBar, BorderLayout.EAST);
        rootPanel.add(titlebar, BorderLayout.NORTH);

        // ================= CONTENT CARDS =================
        contentLayout = new CardLayout();
        contentCards = new JPanel(contentLayout);
        contentCards.setOpaque(false);
        contentCards.setBorder(new EmptyBorder(16, 20, 16, 20));

        contentCards.add(buildPlayPanel(), "play");
        contentCards.add(buildSettingsPanel(), "settings");
        contentCards.add(buildConsolePanel(), "console");
        contentCards.add(buildAboutPanel(), "about");

        rootPanel.add(contentCards, BorderLayout.CENTER);
    }

    private JButton createNavTab(String text, String cardName, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = (getForeground() == currentTheme.accentPrimary);
                if (isActive) {
                    g2.setColor(new Color(0x20, 0x27, 0x3B));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(active ? currentTheme.accentPrimary : new Color(0x94, 0xA3, 0xB8));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.addActionListener(e -> {
            contentLayout.show(contentCards, cardName);
            updateNavTabsSelection(btn);
        });
        return btn;
    }

    private void updateNavTabsSelection(JButton activeBtn) {
        for (JButton b : new JButton[]{btnTabPlay, btnTabSettings, btnTabConsole, btnTabAbout}) {
            if (b == activeBtn) {
                b.setForeground(currentTheme.accentPrimary);
            } else {
                b.setForeground(new Color(0x94, 0xA3, 0xB8));
            }
            b.repaint();
        }
    }

    private JButton createWinControlBtn(String text, ActionListener l, boolean isClose) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(48, 50));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(new Color(0x82, 0x8F, 0xA3));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(isClose ? new Color(0xE1, 0x1D, 0x48) : new Color(0x1C, 0x21, 0x30));
                btn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
                btn.setForeground(new Color(0x82, 0x8F, 0xA3));
            }
        });
        btn.addActionListener(l);
        return btn;
    }

    private JButton createSmallButton(String text, ActionListener l) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x13, 0x17, 0x24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(0x26, 0x2D, 0x40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(new Color(0xCB, 0xD5, 0xE1));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 9, 5, 9));
        btn.addActionListener(l);
        return btn;
    }

    // ================= VIEW: PLAY =================
    private JPanel buildPlayPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        // 1. Top Hero Banner
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x16, 0x1B, 0x29), getWidth(), getHeight(), new Color(0x0F, 0x12, 0x1C)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(currentTheme.borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel heroLeft = new JPanel();
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));
        heroLeft.setOpaque(false);

        JLabel tag = new JLabel("JAVA EDITION • СИНХРОНИЗАЦИЯ С MOJANG");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setForeground(currentTheme.accentPrimary);

        JLabel title = new JLabel("Быстрый запуск Minecraft (Java Core)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Поддержка всех версий, сборок OptiFine, Fabric, Forge и Forge+OptiFine. Высокая стабильность.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0x94, 0xA3, 0xB8));

        heroLeft.add(tag);
        heroLeft.add(Box.createVerticalStrut(3));
        heroLeft.add(title);
        heroLeft.add(Box.createVerticalStrut(3));
        heroLeft.add(subtitle);
        hero.add(heroLeft, BorderLayout.WEST);

        // Status Indicators on the Right
        JPanel heroRight = new JPanel();
        heroRight.setLayout(new BoxLayout(heroRight, BoxLayout.Y_AXIS));
        heroRight.setOpaque(false);

        statusBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x14, 0x53, 0x2D));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        statusBadge.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(0x4A, 0xDE, 0x80));
        JLabel statusText = new JLabel("ИГРА ЗАПУЩЕНА");
        statusText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusText.setForeground(new Color(0x4A, 0xDE, 0x80));
        statusBadge.add(dot);
        statusBadge.add(statusText);
        statusBadge.setVisible(false);
        heroRight.add(statusBadge);

        JPanel mojangPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x10, 0x13, 0x1E));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(currentTheme.borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        mojangPill.setOpaque(false);
        JLabel mDot = new JLabel("●");
        mDot.setForeground(currentTheme.accentPrimary);
        JLabel mText = new JLabel("Mojang API: Онлайн");
        mText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        mText.setForeground(new Color(0xCB, 0xD5, 0xE1));
        mojangPill.add(mDot);
        mojangPill.add(mText);
        heroRight.add(mojangPill);

        hero.add(heroRight, BorderLayout.EAST);
        panel.add(hero, BorderLayout.NORTH);

        // 2. Center Cards Grid
        JPanel cardsGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        cardsGrid.setOpaque(false);

        // ================= LEFT CARD: PLAYER PROFILE =================
        JPanel cardPlayer = createCardContainer();
        cardPlayer.setLayout(new BoxLayout(cardPlayer, BoxLayout.Y_AXIS));

        JLabel pSub = new JLabel("ПРОФИЛЬ ИГРОКА");
        pSub.setFont(new Font("Segoe UI", Font.BOLD, 10));
        pSub.setForeground(currentTheme.accentPrimary);
        JLabel pTitle = new JLabel("Персонаж и никнейм");
        pTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pTitle.setForeground(Color.WHITE);

        cardPlayer.add(pSub);
        cardPlayer.add(pTitle);
        cardPlayer.add(Box.createVerticalStrut(12));

        // Avatar + Nickname Row
        JPanel nickRow = new JPanel(new BorderLayout(12, 0));
        nickRow.setOpaque(false);
        nickRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 52));

        avatarPanel = new AvatarPanel(config.avatar);
        nickRow.add(avatarPanel, BorderLayout.WEST);

        txtNickname = new JTextField(config.nickname);
        txtNickname.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtNickname.setForeground(Color.WHITE);
        txtNickname.setCaretColor(currentTheme.accentPrimary);
        txtNickname.setBackground(currentTheme.bgInput);
        txtNickname.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.borderColor, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        txtNickname.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                config.nickname = txtNickname.getText().trim();
                config.save();
                updateSummaryLine();
            }
        });
        nickRow.add(txtNickname, BorderLayout.CENTER);
        cardPlayer.add(nickRow);
        cardPlayer.add(Box.createVerticalStrut(12));

        // Choose Avatar Chips
        JLabel lblAvTitle = new JLabel("Выберите скин/аватар:");
        lblAvTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblAvTitle.setForeground(new Color(0x94, 0xA3, 0xB8));
        cardPlayer.add(lblAvTitle);
        cardPlayer.add(Box.createVerticalStrut(4));

        JPanel avChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        avChips.setOpaque(false);
        avChips.add(createChipButton("Стив", e -> switchAvatar("steve")));
        avChips.add(createChipButton("Алекс", e -> switchAvatar("alex")));
        avChips.add(createChipButton("Крипер", e -> switchAvatar("creeper")));
        avChips.add(createChipButton("Эндермен", e -> switchAvatar("enderman")));
        cardPlayer.add(avChips);
        cardPlayer.add(Box.createVerticalStrut(12));

        // Quick Nicknames Chips
        JLabel lblQuickNick = new JLabel("Быстрый ник:");
        lblQuickNick.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblQuickNick.setForeground(new Color(0x94, 0xA3, 0xB8));
        cardPlayer.add(lblQuickNick);
        cardPlayer.add(Box.createVerticalStrut(4));

        JPanel nickChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nickChips.setOpaque(false);
        nickChips.add(createChipButton("Player", e -> setQuickNick("Player")));
        nickChips.add(createChipButton("Steve", e -> setQuickNick("Steve")));
        nickChips.add(createChipButton("Alex", e -> setQuickNick("Alex")));
        nickChips.add(createChipButton("Crafter", e -> setQuickNick("Crafter")));
        cardPlayer.add(nickChips);

        cardPlayer.add(Box.createVerticalGlue());

        // Offline Info Pill
        JPanel offlinePill = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x10, 0x12, 0x1B));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(currentTheme.borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        offlinePill.setOpaque(false);
        offlinePill.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel hint = new JLabel("<html>Офлайн-режим. Миры, инвентарь и скины привязываются локально к этому нику.</html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(0x94, 0xA3, 0xB8));
        offlinePill.add(lockIcon, BorderLayout.WEST);
        offlinePill.add(hint, BorderLayout.CENTER);
        cardPlayer.add(offlinePill);

        cardsGrid.add(cardPlayer);

        // ================= RIGHT CARD: VERSIONS & MODPACKS =================
        JPanel cardVersion = createCardContainer();
        cardVersion.setLayout(new BoxLayout(cardVersion, BoxLayout.Y_AXIS));

        // Header Row
        JPanel vHeadRow = new JPanel(new BorderLayout());
        vHeadRow.setOpaque(false);
        JPanel vTitles = new JPanel(new GridLayout(2, 1, 0, 0));
        vTitles.setOpaque(false);
        JLabel vSub = new JLabel("ВЕРСИИ И СБОРКИ");
        vSub.setFont(new Font("Segoe UI", Font.BOLD, 10));
        vSub.setForeground(currentTheme.accentPrimary);
        JLabel vTitle = new JLabel("Сборки, моды и версии");
        vTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        vTitle.setForeground(Color.WHITE);
        vTitles.add(vSub);
        vTitles.add(vTitle);
        vHeadRow.add(vTitles, BorderLayout.WEST);

        btnRefresh = createSmallButton("🔄 Обновить", e -> new Thread(this::loadInitialData).start());
        vHeadRow.add(btnRefresh, BorderLayout.EAST);
        cardVersion.add(vHeadRow);
        cardVersion.add(Box.createVerticalStrut(10));

        // Modloader Filter Chips
        JLabel lblLoader = new JLabel("Выберите сборку / модлоадер:");
        lblLoader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLoader.setForeground(new Color(0x94, 0xA3, 0xB8));
        cardVersion.add(lblLoader);
        cardVersion.add(Box.createVerticalStrut(4));

        JPanel loaderChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        loaderChips.setOpaque(false);
        loaderChips.add(createChipButton("Все", e -> filterVersionsByLoader("all")));
        loaderChips.add(createChipButton("✨ OptiFine", e -> filterVersionsByLoader("optifine")));
        loaderChips.add(createChipButton("🧵 Fabric", e -> filterVersionsByLoader("fabric")));
        loaderChips.add(createChipButton("🔨 Forge", e -> filterVersionsByLoader("forge")));
        loaderChips.add(createChipButton("⚔️ Forge+OptiFine", e -> filterVersionsByLoader("forge_optifine")));
        loaderChips.add(createChipButton("Ванилла", e -> filterVersionsByLoader("vanilla")));
        cardVersion.add(loaderChips);
        cardVersion.add(Box.createVerticalStrut(8));

        // JComboBox with clean dark rendering
        cbVersions = new JComboBox<>();
        cbVersions.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbVersions.setForeground(Color.WHITE);
        cbVersions.setBackground(currentTheme.bgInput);
        cbVersions.setMaximumSize(new Dimension(Short.MAX_VALUE, 42));
        cbVersions.setPreferredSize(new Dimension(300, 42));
        cbVersions.setRenderer(new VersionCellRenderer());
        cbVersions.addActionListener(e -> {
            Downloader.VersionEntry sel = (Downloader.VersionEntry) cbVersions.getSelectedItem();
            if (sel != null) {
                config.selectedVersion = sel.id;
                config.save();
                updateVersionDescription(sel);
                updateSummaryLine();
            }
        });
        cardVersion.add(cbVersions);
        cardVersion.add(Box.createVerticalStrut(8));

        // Quick Version Shortcuts
        JLabel lblQuickVer = new JLabel("Быстрый выбор версии:");
        lblQuickVer.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblQuickVer.setForeground(new Color(0x94, 0xA3, 0xB8));
        cardVersion.add(lblQuickVer);
        cardVersion.add(Box.createVerticalStrut(4));

        JPanel verChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        verChips.setOpaque(false);
        verChips.add(createChipButton("⭐ 1.21.4", e -> selectQuickVersion("1.21.4")));
        verChips.add(createChipButton("1.20.4", e -> selectQuickVersion("1.20.4")));
        verChips.add(createChipButton("1.16.5", e -> selectQuickVersion("1.16.5")));
        verChips.add(createChipButton("1.12.2", e -> selectQuickVersion("1.12.2")));
        verChips.add(createChipButton("Последний", e -> selectQuickVersion("latest")));
        cardVersion.add(verChips);
        cardVersion.add(Box.createVerticalStrut(8));

        // Quick Folders
        JLabel lblFolders = new JLabel("Папки модов и кастомизации:");
        lblFolders.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblFolders.setForeground(new Color(0x94, 0xA3, 0xB8));
        cardVersion.add(lblFolders);
        cardVersion.add(Box.createVerticalStrut(4));

        JPanel folderChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        folderChips.setOpaque(false);
        folderChips.add(createChipButton("📂 Папка модов (mods)", e -> openSubFolder("mods")));
        folderChips.add(createChipButton("✨ Шейдеры (shaderpacks)", e -> openSubFolder("shaderpacks")));
        folderChips.add(createChipButton("🎨 Ресурспаки", e -> openSubFolder("resourcepacks")));
        cardVersion.add(folderChips);
        cardVersion.add(Box.createVerticalStrut(8));

        // Description Box
        JPanel descBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x10, 0x12, 0x1B));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(currentTheme.borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        descBox.setOpaque(false);
        descBox.setBorder(new EmptyBorder(8, 10, 8, 10));

        lblModDesc = new JLabel("<html>✨ Выберите сборку OptiFine, Fabric или Forge для игры с модами и шейдерами.</html>");
        lblModDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblModDesc.setForeground(new Color(0x94, 0xA3, 0xB8));
        descBox.add(lblModDesc, BorderLayout.CENTER);
        cardVersion.add(descBox);

        cardVersion.add(Box.createVerticalGlue());

        // Checkbox Snapshots
        chkSnapshots = new JCheckBox("Показывать экспериментальные снапшоты и беты", config.showSnapshots);
        chkSnapshots.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkSnapshots.setForeground(new Color(0xCB, 0xD5, 0xE1));
        chkSnapshots.setOpaque(false);
        chkSnapshots.setFocusPainted(false);
        chkSnapshots.addActionListener(e -> {
            config.showSnapshots = chkSnapshots.isSelected();
            config.save();
            filterVersionsByLoader(currentModLoader);
        });
        cardVersion.add(chkSnapshots);

        cardsGrid.add(cardVersion);
        panel.add(cardsGrid, BorderLayout.CENTER);

        // ================= BOTTOM ACTION BAR =================
        JPanel actionCard = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bgCard);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(currentTheme.borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        actionCard.setOpaque(false);
        actionCard.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Left info
        JPanel actionLeft = new JPanel();
        actionLeft.setLayout(new BoxLayout(actionLeft, BoxLayout.Y_AXIS));
        actionLeft.setOpaque(false);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        lblStatus = new JLabel("Готов к запуску");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(Color.WHITE);
        lblPercent = new JLabel("");
        lblPercent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPercent.setForeground(currentTheme.accentPrimary);
        statusRow.add(lblStatus, BorderLayout.WEST);
        statusRow.add(lblPercent, BorderLayout.EAST);
        actionLeft.add(statusRow);
        actionLeft.add(Box.createVerticalStrut(6));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setPreferredSize(new Dimension(300, 8));
        progressBar.setForeground(currentTheme.accentPrimary);
        progressBar.setBackground(new Color(0x0C, 0x0E, 0x15));
        progressBar.setBorderPainted(false);
        actionLeft.add(progressBar);
        actionLeft.add(Box.createVerticalStrut(4));

        lblSummary = new JLabel("Игрок: Player • Сборка: 1.20.4 (OptiFine) • Память: 4.0 ГБ RAM");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSummary.setForeground(new Color(0x82, 0x8F, 0xA3));
        actionLeft.add(lblSummary);

        actionCard.add(actionLeft, BorderLayout.CENTER);

        // Right buttons
        JPanel actionRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRight.setOpaque(false);

        btnStopGame = new JButton("⛔ Закрыть игру");
        btnStopGame.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStopGame.setForeground(Color.WHITE);
        btnStopGame.setBackground(new Color(0xBE, 0x12, 0x3C));
        btnStopGame.setPreferredSize(new Dimension(140, 52));
        btnStopGame.setFocusPainted(false);
        btnStopGame.setBorderPainted(false);
        btnStopGame.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStopGame.setVisible(false);
        btnStopGame.addActionListener(e -> stopGame());
        actionRight.add(btnStopGame);

        btnPlay = new JButton("▶  ИГРАТЬ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, currentTheme.accentPrimary, getWidth(), getHeight(), currentTheme.accentSecondary));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnPlay.setFont(new Font("Segoe UI", Font.BOLD, 17));
        btnPlay.setForeground(Color.WHITE);
        btnPlay.setPreferredSize(new Dimension(220, 52));
        btnPlay.setFocusPainted(false);
        btnPlay.setBorderPainted(false);
        btnPlay.setContentAreaFilled(false);
        btnPlay.setOpaque(false);
        btnPlay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPlay.addActionListener(e -> launchGame());
        actionRight.add(btnPlay);

        actionCard.add(actionRight, BorderLayout.EAST);
        panel.add(actionCard, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCardContainer() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bgCard);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(currentTheme.borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        return card;
    }

    private JButton createChipButton(String text, ActionListener l) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x13, 0x17, 0x24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(0x26, 0x2D, 0x40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(new Color(0xCB, 0xD5, 0xE1));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 9, 5, 9));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(new Color(0xCB, 0xD5, 0xE1));
            }
        });
        btn.addActionListener(l);
        return btn;
    }

    // Custom dark combo renderer
    private class VersionCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout(8, 0));
            p.setBorder(new EmptyBorder(6, 10, 6, 10));
            p.setOpaque(true);

            if (isSelected) {
                p.setBackground(new Color(0x1E, 0x25, 0x38));
            } else {
                p.setBackground(currentTheme.bgInput);
            }

            if (value instanceof Downloader.VersionEntry entry) {
                JLabel title = new JLabel(entry.id);
                title.setFont(new Font("Segoe UI", Font.BOLD, 13));
                title.setForeground(Color.WHITE);

                JLabel badge = new JLabel(entry.badge);
                badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                badge.setForeground(currentTheme.accentPrimary);

                p.add(title, BorderLayout.WEST);
                p.add(badge, BorderLayout.EAST);
            } else {
                JLabel l = new JLabel(value != null ? value.toString() : "");
                l.setForeground(Color.WHITE);
                p.add(l, BorderLayout.CENTER);
            }

            return p;
        }
    }

    // Avatar drawing panel
    private class AvatarPanel extends JPanel {
        private String avatar;

        public AvatarPanel(String initialAvatar) {
            this.avatar = initialAvatar != null ? initialAvatar : "steve";
            setPreferredSize(new Dimension(52, 52));
            setMaximumSize(new Dimension(52, 52));
            setOpaque(false);
        }

        public void setAvatarType(String type) {
            this.avatar = type;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 4;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g2.setColor(new Color(0x1A, 0x20, 0x30));
            g2.fillRoundRect(x, y, size, size, 10, 10);

            int pix = (size - 8) / 8;
            int px0 = x + 4;
            int py0 = y + 4;

            if ("alex".equalsIgnoreCase(avatar)) {
                g2.setColor(new Color(0xD0, 0x66, 0x26));
                g2.fillRect(px0, py0, pix * 8, pix * 8);
                g2.setColor(new Color(0xEF, 0xC6, 0xB0));
                g2.fillRect(px0 + pix, py0 + pix * 2, pix * 6, pix * 5);
                g2.setColor(Color.WHITE);
                g2.fillRect(px0 + pix, py0 + pix * 4, pix, pix);
                g2.fillRect(px0 + pix * 6, py0 + pix * 4, pix, pix);
                g2.setColor(new Color(0x37, 0x93, 0x61));
                g2.fillRect(px0 + pix * 2, py0 + pix * 4, pix, pix);
                g2.fillRect(px0 + pix * 5, py0 + pix * 4, pix, pix);
                g2.setColor(new Color(0xC9, 0x88, 0x6D));
                g2.fillRect(px0 + pix * 3, py0 + pix * 5, pix * 2, pix);
                g2.setColor(new Color(0xB5, 0x67, 0x58));
                g2.fillRect(px0 + pix * 3, py0 + pix * 6, pix * 2, pix);
            } else if ("creeper".equalsIgnoreCase(avatar)) {
                g2.setColor(new Color(0x22, 0xC5, 0x5E));
                g2.fillRect(px0, py0, pix * 8, pix * 8);
                g2.setColor(new Color(0x0F, 0x17, 0x2A));
                g2.fillRect(px0 + pix, py0 + pix * 2, pix * 2, pix * 2);
                g2.fillRect(px0 + pix * 5, py0 + pix * 2, pix * 2, pix * 2);
                g2.fillRect(px0 + pix * 3, py0 + pix * 4, pix * 2, pix * 3);
                g2.fillRect(px0 + pix * 2, py0 + pix * 5, pix, pix * 3);
                g2.fillRect(px0 + pix * 5, py0 + pix * 5, pix, pix * 3);
            } else if ("enderman".equalsIgnoreCase(avatar)) {
                g2.setColor(new Color(0x12, 0x13, 0x1A));
                g2.fillRect(px0, py0, pix * 8, pix * 8);
                g2.setColor(new Color(0xC0, 0x84, 0xFC));
                g2.fillRect(px0, py0 + pix * 4, pix * 3, pix);
                g2.fillRect(px0 + pix * 5, py0 + pix * 4, pix * 3, pix);
                g2.setColor(new Color(0xF0, 0xAB, 0xFC));
                g2.fillRect(px0 + pix, py0 + pix * 4, pix, pix);
                g2.fillRect(px0 + pix * 6, py0 + pix * 4, pix, pix);
            } else {
                // Steve
                g2.setColor(new Color(0x4A, 0x32, 0x19));
                g2.fillRect(px0, py0, pix * 8, pix * 8);
                g2.setColor(new Color(0xBD, 0x8B, 0x72));
                g2.fillRect(px0 + pix, py0 + pix * 2, pix * 6, pix * 5);
                g2.fillRect(px0 + pix * 2, py0 + pix * 3, pix * 4, pix * 4);
                g2.setColor(Color.WHITE);
                g2.fillRect(px0 + pix, py0 + pix * 4, pix, pix);
                g2.fillRect(px0 + pix * 6, py0 + pix * 4, pix, pix);
                g2.setColor(new Color(0x28, 0x3B, 0xE0));
                g2.fillRect(px0 + pix * 2, py0 + pix * 4, pix, pix);
                g2.fillRect(px0 + pix * 5, py0 + pix * 4, pix, pix);
                g2.setColor(new Color(0x9B, 0x62, 0x45));
                g2.fillRect(px0 + pix * 3, py0 + pix * 5, pix * 2, pix);
                g2.setColor(new Color(0x4A, 0x32, 0x19));
                g2.fillRect(px0 + pix * 2, py0 + pix * 6, pix * 4, pix);
                g2.setColor(new Color(0xBD, 0x8B, 0x72));
                g2.fillRect(px0 + pix * 3, py0 + pix * 6, pix * 2, pix);
            }

            g2.setColor(currentTheme.accentPrimary);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, size, size, 10, 10);
            g2.dispose();
        }
    }

    // ================= VIEW: SETTINGS =================
    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        JLabel title = new JLabel("Параметры запуска и конфигурация");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        list.add(title);

        // 1. RAM Card
        JPanel cardRam = createCardContainer();
        cardRam.setLayout(new BoxLayout(cardRam, BoxLayout.Y_AXIS));
        JLabel rHead = new JLabel("ВЫДЕЛЕНИЕ ОПЕРАТИВНОЙ ПАМЯТИ (RAM)");
        rHead.setFont(new Font("Segoe UI", Font.BOLD, 10));
        rHead.setForeground(currentTheme.accentPrimary);
        cardRam.add(rHead);

        JPanel rRow = new JPanel(new BorderLayout());
        rRow.setOpaque(false);
        JLabel rTitle = new JLabel("Оперативная память");
        rTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        rTitle.setForeground(Color.WHITE);
        lblRamVal = new JLabel(config.ramMb + " МБ (" + (config.ramMb / 1024.0) + " ГБ)");
        lblRamVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRamVal.setForeground(currentTheme.accentPrimary);
        rRow.add(rTitle, BorderLayout.WEST);
        rRow.add(lblRamVal, BorderLayout.EAST);
        cardRam.add(rRow);
        cardRam.add(Box.createVerticalStrut(8));

        sliderRam = new JSlider(1024, 16384, config.ramMb);
        sliderRam.setOpaque(false);
        sliderRam.setMajorTickSpacing(1024);
        sliderRam.setSnapToTicks(true);
        sliderRam.addChangeListener(e -> {
            int val = sliderRam.getValue();
            config.ramMb = val;
            lblRamVal.setText(val + " МБ (" + String.format("%.1f", val / 1024.0) + " ГБ)");
            config.save();
            updateSummaryLine();
        });
        cardRam.add(sliderRam);
        list.add(cardRam);
        list.add(Box.createVerticalStrut(14));

        // 2. Java Card
        JPanel cardJava = createCardContainer();
        cardJava.setLayout(new BoxLayout(cardJava, BoxLayout.Y_AXIS));
        JLabel jHead = new JLabel("СРЕДА ВЫПОЛНЕНИЯ JAVA");
        jHead.setFont(new Font("Segoe UI", Font.BOLD, 10));
        jHead.setForeground(currentTheme.accentPrimary);
        cardJava.add(jHead);
        cardJava.add(Box.createVerticalStrut(6));

        JPanel jRow = new JPanel(new BorderLayout(8, 0));
        jRow.setOpaque(false);
        txtJavaPath = new JTextField(config.javaPath);
        txtJavaPath.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtJavaPath.setForeground(Color.WHITE);
        txtJavaPath.setBackground(currentTheme.bgInput);
        txtJavaPath.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.borderColor),
            new EmptyBorder(6, 8, 6, 8)
        ));
        jRow.add(txtJavaPath, BorderLayout.CENTER);

        JButton btnBrowseJava = createSmallButton("Обзор...", e -> {
            JFileChooser fc = new JFileChooser("C:\\Program Files");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtJavaPath.setText(fc.getSelectedFile().getAbsolutePath());
                config.javaPath = txtJavaPath.getText();
                config.save();
            }
        });
        jRow.add(btnBrowseJava, BorderLayout.EAST);
        cardJava.add(jRow);
        list.add(cardJava);
        list.add(Box.createVerticalStrut(14));

        // 3. Game Directory Card
        JPanel cardDir = createCardContainer();
        cardDir.setLayout(new BoxLayout(cardDir, BoxLayout.Y_AXIS));
        JLabel dHead = new JLabel("РАБОЧАЯ ПАПКА (.MINECRAFT)");
        dHead.setFont(new Font("Segoe UI", Font.BOLD, 10));
        dHead.setForeground(currentTheme.accentPrimary);
        cardDir.add(dHead);
        cardDir.add(Box.createVerticalStrut(6));

        JPanel dRow = new JPanel(new BorderLayout(8, 0));
        dRow.setOpaque(false);
        txtGameDir = new JTextField(config.gameDir);
        txtGameDir.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtGameDir.setForeground(Color.WHITE);
        txtGameDir.setBackground(currentTheme.bgInput);
        txtGameDir.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.borderColor),
            new EmptyBorder(6, 8, 6, 8)
        ));
        dRow.add(txtGameDir, BorderLayout.CENTER);

        JButton btnBrowseDir = createSmallButton("Обзор...", e -> {
            JFileChooser fc = new JFileChooser(txtGameDir.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtGameDir.setText(fc.getSelectedFile().getAbsolutePath());
                config.gameDir = txtGameDir.getText();
                config.save();
            }
        });
        dRow.add(btnBrowseDir, BorderLayout.EAST);
        cardDir.add(dRow);
        list.add(cardDir);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ================= VIEW: CONSOLE =================
    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel cTitle = new JLabel("Консоль вывода Minecraft");
        cTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cTitle.setForeground(Color.WHITE);
        top.add(cTitle, BorderLayout.WEST);

        JPanel cBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cBtns.setOpaque(false);
        btnCopyLogs = createSmallButton("📋 Копировать", e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(consoleArea.getText()), null);
            lblStatus.setText("Логи скопированы в буфер!");
        });
        btnClearLogs = createSmallButton("🗑 Очистить", e -> consoleArea.setText(""));
        cBtns.add(btnCopyLogs);
        cBtns.add(btnClearLogs);
        top.add(cBtns, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        consoleArea = new JTextArea();
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setBackground(new Color(0x09, 0x0B, 0x10));
        consoleArea.setForeground(new Color(0x38, 0xBD, 0xF8));
        consoleArea.setCaretColor(Color.WHITE);
        consoleArea.setEditable(false);
        consoleArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(currentTheme.borderColor));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ================= VIEW: ABOUT =================
    private JPanel buildAboutPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = createCardContainer();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Minecraft Launcher 2026 (Java Edition)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        JTextArea desc = new JTextArea(
            "• Чистая нативная реализация на OpenJDK 25 + компиляция в автономный Windows EXE через jpackage.\n" +
            "• Встроенные сборки OptiFine, Fabric, Forge и Forge + OptiFine.\n" +
            "• 5 визуальных тем оформления: Изумруд, Алмаз, Незерит, Аметист, Редстоун.\n" +
            "• Офлайн-авторизация и генерация UUID (RFC 4122 v3).\n" +
            "• Прямой запуск без внешних лаунчеров и прямая загрузка Mojang CDN.\n\n" +
            "Приятной игры!"
        );
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(new Color(0xCB, 0xD5, 0xE1));
        desc.setOpaque(false);
        desc.setEditable(false);
        card.add(desc);

        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    // ================= ACTIONS & HELPERS =================
    private void switchAvatar(String type) {
        config.avatar = type;
        config.save();
        if (avatarPanel != null) {
            avatarPanel.setAvatarType(type);
        }
    }

    private void setQuickNick(String nick) {
        config.nickname = nick;
        config.save();
        txtNickname.setText(nick);
        updateSummaryLine();
    }

    private void filterVersionsByLoader(String loader) {
        currentModLoader = loader;
        cbVersions.removeAllItems();

        List<Downloader.VersionEntry> filtered = new ArrayList<>();
        for (Downloader.VersionEntry v : allVersions) {
            if (!config.showSnapshots && !v.isRelease()) continue;

            if ("all".equalsIgnoreCase(loader)) {
                filtered.add(v);
            } else if ("optifine".equalsIgnoreCase(loader) && v.badge.contains("OPTIFINE") && !v.badge.contains("FORGE+")) {
                filtered.add(v);
            } else if ("fabric".equalsIgnoreCase(loader) && v.badge.contains("FABRIC")) {
                filtered.add(v);
            } else if ("forge".equalsIgnoreCase(loader) && v.badge.contains("FORGE") && !v.badge.contains("OPTIFINE")) {
                filtered.add(v);
            } else if ("forge_optifine".equalsIgnoreCase(loader) && v.badge.contains("FORGE+")) {
                filtered.add(v);
            } else if ("vanilla".equalsIgnoreCase(loader) && !v.badge.contains("✨") && !v.badge.contains("🧵") && !v.badge.contains("🔨") && !v.badge.contains("⚔️")) {
                filtered.add(v);
            }
        }

        for (Downloader.VersionEntry v : filtered) {
            cbVersions.addItem(v);
        }

        if (cbVersions.getItemCount() > 0) {
            cbVersions.setSelectedIndex(0);
        }
    }

    private void selectQuickVersion(String targetId) {
        if ("latest".equalsIgnoreCase(targetId)) {
            if (cbVersions.getItemCount() > 0) cbVersions.setSelectedIndex(0);
            return;
        }

        for (int i = 0; i < cbVersions.getItemCount(); i++) {
            Downloader.VersionEntry v = cbVersions.getItemAt(i);
            if (v.id.startsWith(targetId) || v.baseVersion.equalsIgnoreCase(targetId)) {
                cbVersions.setSelectedIndex(i);
                return;
            }
        }
    }

    private void updateVersionDescription(Downloader.VersionEntry entry) {
        if (lblModDesc == null || entry == null) return;

        if (entry.badge.contains("OPTIFINE") && !entry.badge.contains("FORGE+")) {
            lblModDesc.setText("<html>✨ <b>Оптимизация OptiFine HD:</b> Двукратный прирост FPS, шейдеры BSL/Complementary, зум на C.</html>");
        } else if (entry.badge.contains("FABRIC")) {
            lblModDesc.setText("<html>🧵 <b>Модлоадер Fabric:</b> Легковесный движок модов. Поддержка Sodium, Iris, Lithium.</html>");
        } else if (entry.badge.contains("FORGE") && !entry.badge.contains("OPTIFINE")) {
            lblModDesc.setText("<html>🔨 <b>Модлоадер Forge:</b> Классическая богатая база модов Create, Twilight Forest.</html>");
        } else if (entry.badge.contains("FORGE+")) {
            lblModDesc.setText("<html>⚔️ <b>Сборка Forge + OptiFine:</b> Моды Forge + реалистичные шейдеры OptiFine!</html>");
        } else {
            lblModDesc.setText("<html>⚡ <b>Официальная версия Mojang:</b> Чистый клиент без сторонних модов.</html>");
        }
    }

    private void updateSummaryLine() {
        if (lblSummary == null) return;
        Downloader.VersionEntry sel = (Downloader.VersionEntry) cbVersions.getSelectedItem();
        String ver = sel != null ? sel.id : "---";
        lblSummary.setText("Игрок: " + config.nickname + " • Сборка: " + ver + " • Память: " + String.format("%.1f", config.ramMb / 1024.0) + " ГБ RAM");
    }

    private void openDirectory(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();
            Desktop.getDesktop().open(dir);
            lblStatus.setText("Открыта папка: " + dir.getName());
        } catch (Exception e) {
            lblStatus.setText("Ошибка открытия папки: " + e.getMessage());
        }
    }

    private void openSubFolder(String sub) {
        File f = new File(config.gameDir, sub);
        openDirectory(f.getAbsolutePath());
    }

    private void applyTheme(UITheme theme) {
        this.currentTheme = theme;
        if (btnTabPlay != null) {
            btnTabPlay.setForeground(theme.accentPrimary);
        }
        repaint();
    }

    private void loadInitialData() {
        try {
            lblStatus.setText("Синхронизация версий с серверов Mojang...");
            Downloader.ManifestResult manifest = downloader.getManifest();
            allVersions = manifest.versions;

            SwingUtilities.invokeLater(() -> {
                filterVersionsByLoader("all");

                if (!config.selectedVersion.isBlank()) {
                    for (int i = 0; i < cbVersions.getItemCount(); i++) {
                        if (cbVersions.getItemAt(i).id.equalsIgnoreCase(config.selectedVersion)) {
                            cbVersions.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                lblStatus.setText("Готов к запуску");
                updateSummaryLine();
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> lblStatus.setText("Ошибка загрузки версий: " + e.getMessage()));
        }
    }

    private void launchGame() {
        Downloader.VersionEntry sel = (Downloader.VersionEntry) cbVersions.getSelectedItem();
        if (sel == null) return;

        btnPlay.setEnabled(false);
        progressBar.setValue(0);
        lblStatus.setText("Подготовка к запуску " + sel.id + "...");

        new Thread(() -> {
            try {
                downloader.downloadVersion(sel.id, sel.url, config.gameDir, report -> {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText(report.status);
                        progressBar.setValue((int) report.percent);
                        lblPercent.setText((int) report.percent + "%");
                    });
                });

                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Запуск Minecraft " + sel.id + "...");
                    progressBar.setValue(100);
                    lblPercent.setText("100%");
                });

                runningProcess = Runner.launch(sel.id, config, line -> {
                    SwingUtilities.invokeLater(() -> {
                        consoleArea.append(line + "\n");
                        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                    });
                });

                SwingUtilities.invokeLater(() -> {
                    statusBadge.setVisible(true);
                    btnStopGame.setVisible(true);
                    lblStatus.setText("Minecraft " + sel.id + " запущен!");
                });

                runningProcess.waitFor();

                SwingUtilities.invokeLater(() -> {
                    statusBadge.setVisible(false);
                    btnStopGame.setVisible(false);
                    btnPlay.setEnabled(true);
                    lblStatus.setText("Minecraft закрыт");
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnPlay.setEnabled(true);
                    lblStatus.setText("Ошибка запуска: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Не удалось запустить игру:\n" + ex.getMessage(), "Ошибка запуска", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void stopGame() {
        if (runningProcess != null && runningProcess.isAlive()) {
            runningProcess.destroyForcibly();
            lblStatus.setText("Minecraft был принудительно остановлен.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Main().setVisible(true);
        });
    }
}
