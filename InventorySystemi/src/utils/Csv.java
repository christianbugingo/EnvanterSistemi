package utils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class Csv {
    public static void write(Path path,List<List<?>> rows) throws IOException {
        try(BufferedWriter out=Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {
            for(List<?> row:rows) {
                List<String> cells=new ArrayList<>();
                for(Object value:row) {
                    String text=String.valueOf(value);
                    String leading=text.stripLeading();
                    if(!(value instanceof Number) && !leading.isEmpty() && "=+-@".indexOf(leading.charAt(0))>=0) text="'"+text;
                    cells.add("\""+text.replace("\"","\"\"")+"\"");
                }
                out.write(String.join(",",cells)); out.newLine();
            }
        }
    }
}
