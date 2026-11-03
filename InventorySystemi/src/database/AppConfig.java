package database;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** A small literal key=value reader. It never executes shell code. JVM > environment > file. */
public final class AppConfig {
    private static final Map<String,String> FILE=load();
    private AppConfig() { }
    public static String get(String key, String fallback) {
        String prop=System.getProperty(key); if(prop!=null) return prop;
        String env=System.getenv(key); if(env!=null) return env;
        return FILE.getOrDefault(key,fallback);
    }
    private static Map<String,String> load() {
        String explicit=System.getenv("INVENTORY_ENV_FILE");
        List<Path> paths=explicit!=null ? List.of(Path.of(explicit)) : List.of(
                Path.of(System.getProperty("user.home"),".inventorysystem",".env"),Path.of(".env"),Path.of("InventorySystemi",".env"));
        for(Path path:paths) {
            if(!Files.isRegularFile(path)) continue;
            try {
                Map<String,String> result=new HashMap<>();
                for(String line:Files.readAllLines(path)) {
                    line=line.trim(); if(line.isEmpty() || line.startsWith("#")) continue;
                    if(line.startsWith("export ")) line=line.substring(7).trim();
                    int equals=line.indexOf('='); if(equals<1) throw new IOException("Invalid configuration entry");
                    String key=line.substring(0,equals).trim(), value=line.substring(equals+1).trim();
                    if(value.length()>=2 && ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\"")))) value=value.substring(1,value.length()-1);
                    if(!key.matches("[A-Z_]+")) throw new IOException("Invalid configuration key");
                    result.put(key,value);
                }
                return Map.copyOf(result);
            } catch(IOException e) { throw new IllegalStateException("Cannot read configuration: "+path,e); }
        }
        if(explicit!=null) throw new IllegalStateException("Configuration file not found: "+explicit);
        return Map.of();
    }
}
