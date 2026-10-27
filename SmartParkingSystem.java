import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class SmartParkingSystem extends JFrame {
    private int totalSlots = 10;
    private int occupiedSlots = 0;
    private JLabel statusLabel;
    private JLabel occupiedLabel;
    private JLabel rateLabel;
    private JTextArea historyArea;
    private java.util.Map<String, Long> parkedCars = new HashMap<>(); // plate -> entry time
    private java.util.List<String> parkingHistory = new ArrayList<>();
    private final double hourlyRate = 30000.0;

    // ===== Palet warna UI =====
    private static final Color NAVY      = new Color(0x0B, 0x25, 0x45);
    private static final Color BLUE      = new Color(0x1E, 0x6F, 0xD9);
    private static final Color LIGHT_BG  = new Color(0xF1, 0xF5, 0xF9);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color BORDER_C  = new Color(0xD9, 0xE2, 0xEC);
    private static final Color TEXT_DARK = new Color(0x1F, 0x2A, 0x37);
    private static final Color TEXT_GRAY = new Color(0x64, 0x74, 0x8B);
    private static final Color GREEN     = new Color(0x16, 0xA3, 0x4A);
    private static final Color RED       = new Color(0xDC, 0x26, 0x26);
    private static final Color CYAN      = new Color(0x08, 0x91, 0xB2);

    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_SUB     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_CAPTION = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_VALUE   = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);

    // ===== Komponen kustom (hanya tampilan) =====
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final Color line;

        RoundedPanel(int radius, Color fill, Color line) {
            this.radius = radius;
            this.fill = fill;
            this.line = line;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private final Color base;
        private final Color hoverColor;
        private final Color pressColor;
        private boolean hover = false;
        private boolean pressed = false;

        RoundedButton(String text, Color base) {
            super(text);
            this.base = base;
            this.hoverColor = base.brighter();
            this.pressColor = base.darker();
            setFont(FONT_BUTTON);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(130, 46));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(pressed ? pressColor : (hover ? hoverColor : base));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public SmartParkingSystem() {
        setTitle("Smart Parking System");
        setSize(760, 620);
        setMinimumSize(new Dimension(680, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ----- Label informasi -----
        statusLabel = new JLabel(getStatusText(), SwingConstants.CENTER);
        statusLabel.setFont(FONT_VALUE);

        occupiedLabel = new JLabel("", SwingConstants.CENTER);
        occupiedLabel.setFont(FONT_VALUE);
        occupiedLabel.setForeground(NAVY);

        rateLabel = new JLabel("Rp " + String.format("%,.0f", hourlyRate).replace(',', '.') + " / jam",
                SwingConstants.CENTER);
        rateLabel.setFont(FONT_VALUE);
        rateLabel.setForeground(BLUE);

        updateStatusColor();

        // ----- Tombol -----
        JButton parkButton = new RoundedButton("Parkir Mobil", GREEN);
        JButton exitButton = new RoundedButton("Keluar Mobil", RED);
        JButton checkButton = new RoundedButton("Cek Status", BLUE);
        JButton searchButton = new RoundedButton("Cari No Plat", CYAN);

        parkButton.addActionListener(e -> parkCar());
        exitButton.addActionListener(e -> exitCar());
        checkButton.addActionListener(e -> showStatus());
        searchButton.addActionListener(e -> searchPlate());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(parkButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(checkButton);
        buttonPanel.add(searchButton);

        // ----- Area riwayat -----
        historyArea = new JTextArea(10, 50);
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        historyArea.setForeground(TEXT_DARK);
        historyArea.setBackground(CARD_BG);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        historyArea.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_C));
        scrollPane.getViewport().setBackground(CARD_BG);

        // ----- Header -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        JLabel titleLabel = new JLabel("Smart Parking System");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        JLabel subtitleLabel = new JLabel("Kelola kendaraan masuk, keluar, dan riwayat parkir");
        subtitleLabel.setFont(FONT_SUB);
        subtitleLabel.setForeground(new Color(0xA9, 0xC7, 0xF0));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(titleLabel);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitleLabel);
        header.add(titleBox, BorderLayout.WEST);

        // ----- Kartu statistik -----
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        statsPanel.setOpaque(false);
        statsPanel.add(makeCard("SLOT PARKIR", statusLabel));
        statsPanel.add(makeCard("SLOT TERPAKAI", occupiedLabel));
        statsPanel.add(makeCard("TARIF PARKIR", rateLabel));

        JPanel topArea = new JPanel();
        topArea.setOpaque(false);
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.add(statsPanel);
        topArea.add(Box.createVerticalStrut(14));
        topArea.add(buttonPanel);

        // ----- Kartu riwayat -----
        RoundedPanel historyCard = new RoundedPanel(16, CARD_BG, BORDER_C);
        historyCard.setLayout(new BorderLayout(0, 8));
        historyCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel historyTitle = new JLabel("Riwayat Parkir");
        historyTitle.setFont(FONT_SECTION);
        historyTitle.setForeground(NAVY);
        historyCard.add(historyTitle, BorderLayout.NORTH);
        historyCard.add(scrollPane, BorderLayout.CENTER);

        // ----- Susunan utama -----
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(LIGHT_BG);
        body.setBorder(new EmptyBorder(18, 20, 20, 20));
        body.add(topArea, BorderLayout.NORTH);
        body.add(historyCard, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(LIGHT_BG);
        content.add(header, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel makeCard(String caption, JLabel valueLabel) {
        RoundedPanel card = new RoundedPanel(16, CARD_BG, BORDER_C);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(14, 12, 14, 12));
        JLabel captionLabel = new JLabel(caption, SwingConstants.CENTER);
        captionLabel.setFont(FONT_CAPTION);
        captionLabel.setForeground(TEXT_GRAY);
        card.add(captionLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private String getStatusText() {
        return "Slot tersedia: " + (totalSlots - occupiedSlots) + "/" + totalSlots;
    }

    private void updateStatusColor() {
        if (occupiedSlots == totalSlots) {
            statusLabel.setForeground(RED);
        } else {
            statusLabel.setForeground(GREEN.darker());
        }
        statusLabel.setText(getStatusText());
        if (occupiedLabel != null) {
            occupiedLabel.setText(occupiedSlots + " mobil");
        }
    }

    private void parkCar() {
        if (occupiedSlots < totalSlots) {
            String plateNumber = JOptionPane.showInputDialog(this, "Masukkan nomor plat:");
            if (plateNumber != null && !plateNumber.trim().isEmpty()) {
                long entryTime = System.currentTimeMillis();
                parkedCars.put(plateNumber, entryTime);
                occupiedSlots++;
                parkingHistory.add("[MASUK] " + plateNumber + " | Masuk: " + new Date(entryTime));
                updateStatusColor();
                updateHistoryArea();
                JOptionPane.showMessageDialog(this, "Mobil berhasil diparkir.");
            } else {
                JOptionPane.showMessageDialog(this, "Nomor plat tidak boleh kosong!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Parkiran penuh!", "Peringatan", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exitCar() {
        if (occupiedSlots > 0) {
            String plateNumber = JOptionPane.showInputDialog(this, "Masukkan nomor plat keluar:");
            if (plateNumber != null && !plateNumber.trim().isEmpty()) {
                Long entryTime = parkedCars.remove(plateNumber);
                if (entryTime != null) {
                    long exitTime = System.currentTimeMillis();
                    double hours = Math.ceil((exitTime - entryTime) / (100.0 * 60 * 60));
                    double payment = hours * hourlyRate;
                    occupiedSlots--;
                    parkingHistory.add("[KELUAR] " + plateNumber + " | Keluar: " + new Date(exitTime) + " | Durasi: " + (int) hours + "JAM | Bayar: Rp " + payment);
                    updateStatusColor();
                    updateHistoryArea();
                    JOptionPane.showMessageDialog(this, "Mobil keluar. Durasi: " + (int) hours+ " JAM, Total bayar: Rp " + payment);
                } else {
                    JOptionPane.showMessageDialog(this, "Nomor plat tidak ditemukan di parkiran!");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Nomor plat tidak boleh kosong!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Tidak ada mobil yang keluar!", "Peringatan", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void searchPlate() {
        String searchPlate = JOptionPane.showInputDialog(this, "Masukkan nomor plat untuk dicari:");
        if (searchPlate != null && !searchPlate.trim().isEmpty()) {
            StringBuilder result = new StringBuilder();
            for (String record : parkingHistory) {
                if (record.contains(searchPlate)) {
                    result.append(record).append("\n");
                }
            }
            if (result.length() > 0) {
                JOptionPane.showMessageDialog(this, "Hasil pencarian untuk " + searchPlate + ":\n" + result);
            } else {
                JOptionPane.showMessageDialog(this, "Nomor plat tidak ditemukan di riwayat.");
            }
        }
    }

    private void showStatus() {
        StringBuilder masuk = new StringBuilder();
        StringBuilder keluar = new StringBuilder();
        
        for (String record : parkingHistory) {
            if (record.startsWith("[MASUK]")) {
                masuk.append(record).append("\n");
            } else if (record.startsWith("[KELUAR]")) {
                keluar.append(record).append("\n");
            }
        }

        String statusMessage = "Status Parkiran:\n" +
                "Slot Terpakai: " + occupiedSlots + "\n" +
                "Slot Tersedia: " + (totalSlots - occupiedSlots) + "\n\n" +
                "Mobil yang Telah Masuk:\n" + masuk.toString() + "\n" +
                "Mobil yang Telah Keluar:\n" + keluar.toString();

        JOptionPane.showMessageDialog(this, statusMessage, "Status Lengkap", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateHistoryArea() {
        StringBuilder sb = new StringBuilder();
        for (String record : parkingHistory) {
            sb.append(record).append("\n");
        }
        historyArea.setText(sb.toString());
    }

    // Tema dialog (JOptionPane) agar serasi dengan jendela utama
    private static void applyDialogTheme() {
        Font dialogFont = new Font("Segoe UI", Font.PLAIN, 14);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT_DARK);
        UIManager.put("OptionPane.messageFont", dialogFont);
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("TextField.font", dialogFont);
        UIManager.put("Button.background", BLUE);
        UIManager.put("Button.foreground", Color.WHITE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyDialogTheme();
            SmartParkingSystem frame = new SmartParkingSystem();
            frame.setVisible(true);
        });
    }
}
