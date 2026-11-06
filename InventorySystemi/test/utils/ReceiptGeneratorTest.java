package utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import product.Sale;
import static org.junit.jupiter.api.Assertions.*;

class ReceiptGeneratorTest {
    @Test
    void receiptsUseWritableUserDirectoryAndNeverOverwrite(@TempDir Path directory) throws Exception {
        String previous = System.getProperty("user.home");
        try {
            System.setProperty("user.home", directory.toString());
            Sale sale = new Sale(0, 1, 1, 2, LocalDate.now());
            sale.setProductName("Türkçe ürün");
            sale.setPrice(new java.math.BigDecimal("12.50"));
            ReceiptGenerator.generateAndSaveReceipt(List.of(sale), "tester");
            ReceiptGenerator.generateAndSaveReceipt(List.of(sale), "tester");
            try (var files = Files.list(directory.resolve(".inventorysystem/receipts"))) {
                var receipts = files.toList();
                assertEquals(2, receipts.size());
                for (Path receipt : receipts) assertTrue(Files.readString(receipt).contains("Türkçe ürün"));
            }
        } finally {
            System.setProperty("user.home", previous);
        }
    }
}
