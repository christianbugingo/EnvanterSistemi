package utils;

import product.*;
import gui.LocaleManager;
import java.nio.file.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptGenerator {
    public static Path receiptsDirectory() {return Path.of(System.getProperty("user.home"),".inventorysystem","receipts");}
    public static Path generateAndSaveReceipt(List<Sale> items,String user) {return generate(items,user,null);}
    public static Path generateAndSaveReceipt(Checkout checkout) {return generate(checkout.lines(),checkout.username(),checkout);}
    private static Path generate(List<Sale> items,String user,Checkout checkout) {
        StringBuilder out=new StringBuilder(LocaleManager.get("receipt.title")).append('\n');
        out.append(checkout==null?LocalDateTime.now():checkout.createdAt()).append(" | ").append(user).append('\n');
        if(checkout!=null)out.append(LocaleManager.get("checkout.id")).append(": ").append(checkout.id()).append('\n');
        out.append("=".repeat(60)).append('\n');
        for(Sale s:items)out.append('#').append(s.getSaleId()).append(" | ").append(s.getProductName()).append(" | ")
                .append(LocaleManager.quantity(s.getQuantitySold())).append(' ').append(s.getUnit().label()).append(" × ")
                .append(LocaleManager.money(s.getPrice())).append(" = ").append(LocaleManager.money(s.getTotal()))
                .append(s.isVoided()?" ["+LocaleManager.get("sale.voided")+"]":"").append('\n');
        BigDecimal total=checkout==null?items.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add):checkout.total();
        out.append(LocaleManager.get("receipt.original.total",LocaleManager.money(total))).append('\n');
        if(checkout!=null && checkout.method()!=PaymentMethod.UNRECORDED) {
            out.append(LocaleManager.get("payment.method")).append(": ").append(checkout.method()).append('\n');
            if(checkout.method()==PaymentMethod.CASH) {
                out.append(LocaleManager.get("payment.received")).append(": ").append(LocaleManager.money(checkout.cashReceived())).append('\n');
                out.append(LocaleManager.get("payment.change",LocaleManager.money(checkout.change()))).append('\n');
            } else out.append(LocaleManager.get("payment.card.note")).append('\n');
        }
        out.append(LocaleManager.get("receipt.tax.note")).append('\n');
        try {
            Files.createDirectories(receiptsDirectory());
            Path file=Files.createTempFile(receiptsDirectory(),"receipt_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+"_",".txt");
            Files.writeString(file,out,java.nio.charset.StandardCharsets.UTF_8);return file;
        } catch(IOException e) {throw new UncheckedIOException(e);}
    }
}
