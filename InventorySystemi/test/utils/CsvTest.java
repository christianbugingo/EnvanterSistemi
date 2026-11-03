package utils;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class CsvTest {
    @Test void exportsCommasQuotesNewlinesUtf8AndLocaleIndependentDecimals(@TempDir Path dir) throws Exception {
        Path path=dir.resolve("products.csv");
        gui.LocaleManager.setLanguage("tr");
        try {
            Csv.write(path,List.of(List.of("Türkçe, \"ürün\"\nname",new BigDecimal("12.50"),"=1+1")));
            String content=Files.readString(path);
            assertTrue(content.contains("\"Türkçe, \"\"ürün\"\"\nname\""));
            assertTrue(content.contains("\"12.50\""));assertFalse(content.contains("12,50"));
            assertTrue(content.contains("\"'=1+1\""));
        } finally {gui.LocaleManager.setLanguage("en");}
    }
}
