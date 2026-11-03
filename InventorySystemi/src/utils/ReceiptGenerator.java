package utils;

import product.Sale;
import gui.LocaleManager;
import java.nio.file.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptGenerator {
    public static Path receiptsDirectory() { return Path.of(System.getProperty("user.home"),".inventorysystem","receipts"); }
    public static Path generateAndSaveReceipt(List<Sale> items, String user) {
        StringBuilder out=new StringBuilder(LocaleManager.get("receipt.title")).append('\n');
        out.append(LocalDateTime.now()).append(" | ").append(user).append('\n').append("=".repeat(60)).append('\n');
        for(Sale s:items) out.append('#').append(s.getSaleId()).append(" | ").append(s.getProductName()).append(" | ")
                .append(LocaleManager.quantity(s.getQuantitySold())).append(' ').append(s.getUnit().label()).append(" × ")
                .append(LocaleManager.money(s.getPrice())).append(" = ").append(LocaleManager.money(s.getTotal())).append('\n');
        out.append(LocaleManager.get("sale.cart.total",LocaleManager.money(items.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add)))).append('\n');
        out.append(LocaleManager.get("receipt.tax.note")).append('\n');
        try {
            Files.createDirectories(receiptsDirectory());
            Path file=Files.createTempFile(receiptsDirectory(),"receipt_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+"_",".txt");
            Files.writeString(file,out,java.nio.charset.StandardCharsets.UTF_8); return file;
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
}
