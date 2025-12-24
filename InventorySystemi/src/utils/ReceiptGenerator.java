package utils;

import product.Sale;
import gui.LocaleManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptGenerator {

    public static void generateAndSaveReceipt(List<Sale> salesList, String currentUser) {
        try {
            // Generate receipt content
            String receiptContent = generateReceiptContent(salesList, currentUser);

            // Create receipts directory if it doesn't exist
            File receiptsDir = new File("receipts");
            if (!receiptsDir.exists()) {
                receiptsDir.mkdir();
            }

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "receipts/satis_fisi_" + timestamp + ".txt";
            File receiptFile = new File(fileName);

            // Save to file
            try (FileWriter writer = new FileWriter(receiptFile)) {
                writer.write(receiptContent);
            }

            System.out.println("Fiş kaydedildi: " + receiptFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Fiş kaydedilemedi: " + e.getMessage());
        }
    }

    private static String generateReceiptContent(List<Sale> salesList, String currentUser) {
        StringBuilder receipt = new StringBuilder();

        // 1. PROFESSIONAL RECEIPT FORMATTING
        receipt.append("=".repeat(50)).append("\n");
        receipt.append(String.format("%30s\n", "ENVANTER SİSTEMİ"));
        receipt.append(String.format("%32s\n", "SATIŞ FİŞİ"));
        receipt.append("=".repeat(50)).append("\n\n");

        // Sale info
        receipt.append(String.format("%-20s: %s\n", "Tarih", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))));
        receipt.append(String.format("%-20s: %s\n", "Satış Yapan", currentUser));
        receipt.append("-".repeat(50)).append("\n");

        // 2. ITEMIZED LIST
        receipt.append(String.format("%-5s %-20s %-8s %-10s %-10s\n",
            "No", "Ürün Adı", "Adet", "Birim Fiyat", "Toplam"));
        receipt.append("-".repeat(50)).append("\n");

        // Items
        double totalAmount = 0;
        int itemNumber = 1;

        for (Sale sale : salesList) {
            double itemTotal = sale.getPrice() * sale.getQuantitySold();
            totalAmount += itemTotal;

            // Truncate long product names
            String productName = sale.getProductName();
            if (productName.length() > 20) {
                productName = productName.substring(0, 17) + "...";
            }

            receipt.append(String.format("%-5d %-20s %-8d ₺%-9.2f ₺%-9.2f\n",
                itemNumber++,
                productName,
                sale.getQuantitySold(),
                sale.getPrice(),
                itemTotal
            ));
        }

        receipt.append("=".repeat(50)).append("\n");

        // 3. CALCULATIONS
        double kdv = totalAmount * 0.18;
        double genelToplam = totalAmount * 1.18;

        receipt.append(String.format("%-35s ₺%.2f\n", "ARA TOPLAM:", totalAmount));
        receipt.append(String.format("%-35s ₺%.2f\n", "KDV (%18):", kdv));
        receipt.append(String.format("%-35s ₺%.2f\n", "GENEL TOPLAM:", genelToplam));
        receipt.append("=".repeat(50)).append("\n\n");

        // Footer
        receipt.append("TEŞEKKÜR EDERİZ\n");
        receipt.append("İADE: 7 GÜN İÇİNDE FİŞ İLE\n");
        receipt.append("=".repeat(50)).append("\n");

        return receipt.toString();
    }
}