package database;
public final class BuildInfo {
    private static final java.util.Properties DATA=new java.util.Properties();
    static {try(var in=BuildInfo.class.getResourceAsStream("/build.properties")){if(in!=null)DATA.load(in);}catch(java.io.IOException ex){org.slf4j.LoggerFactory.getLogger(BuildInfo.class).warn("Build metadata unavailable",ex);}}
    private BuildInfo() { }
    public static String version(){return DATA.getProperty("version","1.3.0");}
    public static String date(){return DATA.getProperty("build.date","development");}
}
