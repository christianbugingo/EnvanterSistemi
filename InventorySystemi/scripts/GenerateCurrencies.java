import java.util.*;
import java.nio.file.*;
import javax.xml.parsers.*;
/** Build-time only. Currency metadata comes from Java; ISO's current list excludes retired codes. */
public class GenerateCurrencies {
    public static void main(String[] args) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var doc=factory.newDocumentBuilder().parse(Path.of(args[0]).toFile());var entries=doc.getElementsByTagName("CcyNtry");
        var rows=new TreeMap<String,String>();
        for(int i=0;i<entries.getLength();i++){
            var e=(org.w3c.dom.Element)entries.item(i);var codes=e.getElementsByTagName("Ccy");var minors=e.getElementsByTagName("CcyMnrUnts");
            if(codes.getLength()==0||minors.getLength()==0)continue;String code=codes.item(0).getTextContent(),minor=minors.item(0).getTextContent();if(!minor.matches("[0-4]"))minor="-1";
            String name=e.getElementsByTagName("CcyNm").item(0).getTextContent(),symbol=code;
            try{Currency c=Currency.getInstance(code);name=c.getDisplayName(Locale.ENGLISH);symbol=c.getSymbol(Locale.ENGLISH);if(c.getDefaultFractionDigits()!=Integer.parseInt(minor))System.err.println("ISO minor-unit update: "+code);}catch(IllegalArgumentException ex){System.err.println("New ISO code not in this JDK: "+code);}
            rows.put(code,code+"\t"+name+"\t"+symbol+"\t"+minor);
        }
        Files.write(Path.of(args[1]),rows.values(),java.nio.charset.StandardCharsets.UTF_8);
    }
}
