package services;
import product.ShopCurrency;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class Currencies {
    private static final Map<String,ShopCurrency> ALL=load();
    private Currencies() { }
    private static Map<String,ShopCurrency> load(){
        Map<String,ShopCurrency> rows=new TreeMap<>();
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(Currencies.class.getResourceAsStream("/currencies/catalogue.tsv")),StandardCharsets.UTF_8))){
            for(String line; (line=in.readLine())!=null;){String[] f=line.split("\t");rows.put(f[0],new ShopCurrency(f[0],f[1],f[2],Integer.parseInt(f[3])));}
        }catch(IOException ex){throw new IllegalStateException(ex);}return Collections.unmodifiableMap(rows);
    }
    public static List<ShopCurrency> all(){return List.copyOf(ALL.values());}
    public static ShopCurrency get(String code){ShopCurrency c=ALL.get(code);if(c==null)throw AuthService.failure("currency.invalid");return c;}
    public static ShopCurrency snapshot(String code,int minor){ShopCurrency known=ALL.get(code==null?"":code);return new ShopCurrency(code==null?"?":code,known==null?"":known.name(),known==null?"":known.symbol(),minor);}
}
