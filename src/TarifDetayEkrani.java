
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Numan
 */
public class TarifDetayEkrani extends JFrame {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/yemektarif?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String USER = "root";
    private static final String PASS = "kirpi1974";
    
    private String tarifAdi;
    private JLabel adLabel, kategoriLabel, hazirlamaSuresiLabel, toplamMaliyetLabel;
    private JTextArea talimatlarArea;
    private JTable malzemelerTablo;
    private DefaultTableModel malzemelerTableModel;

    public TarifDetayEkrani(String tarifAdi) {
        this.tarifAdi = tarifAdi;
        setTitle("Tarif Detayları - " + tarifAdi);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel bilgiPaneli = new JPanel(new GridLayout(5, 1));
        adLabel = new JLabel();
        kategoriLabel = new JLabel();
        hazirlamaSuresiLabel = new JLabel();
        toplamMaliyetLabel = new JLabel();

        bilgiPaneli.add(adLabel);
        bilgiPaneli.add(kategoriLabel);
        bilgiPaneli.add(hazirlamaSuresiLabel);

        talimatlarArea = new JTextArea();
        talimatlarArea.setLineWrap(true);
        talimatlarArea.setWrapStyleWord(true);
        talimatlarArea.setEditable(false);
        bilgiPaneli.add(new JScrollPane(talimatlarArea));

        bilgiPaneli.add(toplamMaliyetLabel);
        add(bilgiPaneli, BorderLayout.NORTH);

        malzemelerTableModel = new DefaultTableModel(new Object[]{"Malzeme Adı", "Gerekli Miktar", "Birim", "Toplam Miktar", "Birim Fiyat"}, 0);
        malzemelerTablo = new JTable(malzemelerTableModel);
        add(new JScrollPane(malzemelerTablo), BorderLayout.CENTER);

        tarifDetaylariniYukle();
    }

    private void tarifDetaylariniYukle() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String tarifQuery = """
                SELECT t.Kategori, t.HazirlamaSuresi, t.Talimatlar
                FROM tarifler t
                WHERE t.TarifAdi = ?
            """;

            try (PreparedStatement tarifStmt = conn.prepareStatement(tarifQuery)) {
                tarifStmt.setString(1, tarifAdi);
                try (ResultSet rs = tarifStmt.executeQuery()) {
                    if (rs.next()) {
                        adLabel.setText("Tarif Adı: " + tarifAdi);
                        kategoriLabel.setText("Kategori: " + rs.getString("Kategori"));
                        hazirlamaSuresiLabel.setText("Hazırlama Süresi: " + rs.getInt("HazirlamaSuresi") + " dk");
                        talimatlarArea.setText(rs.getString("Talimatlar"));
                    }
                }
            }

            String malzemeQuery = """
                SELECT m.MalzemeAdi, tm.MalzemeMiktar, m.ToplamMiktar, m.BirimFiyat, m.MalzemeBirim
                FROM tarifmalzeme tm
                JOIN malzemeler m ON tm.MalzemeID = m.MalzemeID
                JOIN tarifler t ON tm.TarifID = t.TarifID
                WHERE t.TarifAdi = ?
            """;

            double toplamMaliyet = 0.0;
            boolean yetersizMalzeme = false;

            try (PreparedStatement malzemeStmt = conn.prepareStatement(malzemeQuery)) {
                malzemeStmt.setString(1, tarifAdi);
                try (ResultSet rs = malzemeStmt.executeQuery()) {
                    while (rs.next()) {
                        String malzemeAdi = rs.getString("MalzemeAdi");
                        double gerekliMiktar = rs.getDouble("MalzemeMiktar");
                        double toplamMiktar = rs.getDouble("ToplamMiktar");
                        String birim = rs.getString("MalzemeBirim");
                        double birimFiyat = rs.getDouble("BirimFiyat");

                        malzemelerTableModel.addRow(new Object[]{malzemeAdi, gerekliMiktar, birim, toplamMiktar, birimFiyat});

                        if (toplamMiktar < gerekliMiktar) {
                            yetersizMalzeme = true;
                            double eksikMiktar = gerekliMiktar - toplamMiktar;
                            toplamMaliyet += eksikMiktar * birimFiyat;
                        }
                    }
                }
            }

            if (yetersizMalzeme) {
                toplamMaliyetLabel.setText("Eksik Malzemelerin Toplam Fiyatı: " + toplamMaliyet + " TL");
            } else {
                toplamMaliyetLabel.setText("Malzemeler yeterli.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Tarif bilgileri yüklenirken hata oluştu: " + e.getMessage());
        }
    }
}