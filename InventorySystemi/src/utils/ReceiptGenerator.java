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
    public static String render(Checkout checkout) {return render(checkout.lines(),checkout.username(),checkout);}
    private static String render(List<Sale> items,String user,Checkout checkout) {
        ShopCurrency currency=checkout==null?(items.isEmpty()?services.Currencies.snapshot(null,2):items.getFirst().getCurrency()):checkout.currency();if(currency==null)currency=services.Currencies.snapshot(null,2);
        StringBuilder out=new StringBuilder(LocaleManager.get("receipt.title")).append('\n');
        out.append(checkout==null?LocalDateTime.now():checkout.createdAt()).append(" | ").append(user).append('\n');
        if(checkout!=null)out.append(LocaleManager.get("checkout.id")).append(": ").append(checkout.id()).append('\n');
        out.append("=".repeat(60)).append('\n');
        for(Sale s:items)out.append('#').append(s.getSaleId()).append(" | ").append(s.getProductName()).append(" | ")
                .append(Quantities.format(s.getQuantitySold(),s.getUnit())).append(" × ")
                .append(currency.format(s.getPrice())).append(" / ").append(s.getUnit().label()).append(" = ").append(currency.format(s.getTotal()))
                .append(s.isVoided()?" ["+LocaleManager.get("sale.voided")+"]":"").append('\n');
        BigDecimal total=checkout==null?items.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add):checkout.total();
        if(checkout!=null&&checkout.taxRate().signum()>0){out.append(LocaleManager.get("amount.subtotal",currency.format(checkout.subtotal()))).append('\n');out.append(LocaleManager.get("amount.tax",checkout.taxRate().stripTrailingZeros().toPlainString(),currency.format(checkout.taxAmount()))).append('\n');}
        out.append(LocaleManager.get("sale.cart.total",currency.format(total))).append('\n');
        if(checkout!=null)out.append(LocaleManager.get("payment.method")).append(": ").append(checkout.method()).append('\n');
        out.append(LocaleManager.get("receipt.tax.note")).append('\n');
        return out.toString();
    }
    private static Path generate(List<Sale> items,String user,Checkout checkout) {
        String out=render(items,user,checkout);
        try {
            Files.createDirectories(receiptsDirectory());
            Path file=Files.createTempFile(receiptsDirectory(),"receipt_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+"_",".txt");
            Files.writeString(file,out,java.nio.charset.StandardCharsets.UTF_8);return file;
        } catch(IOException e) {throw new UncheckedIOException(e);}
    }
}
