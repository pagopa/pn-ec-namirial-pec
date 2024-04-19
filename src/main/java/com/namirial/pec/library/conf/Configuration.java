package com.namirial.pec.library.conf;

public class Configuration {
	
    //IMAP
    private static final String DEFAULT_IMAP_FOLDER = "Search.All";
    
    //DEFAULT POOL IMAP
    private static final String DEFAULT_POOL_IMAP_MAXTOTAL = "10";
    
    private static final String DEFAULT_POOL_IMAP_MAXIDLE = "10";
    
    private static final String DEFAULT_POOL_IMAP_MINIDLE = "1";

    //DEFAULT POOL SMTP
    private static final String DEFAULT_POOL_SMTP_MAXTOTAL = "40";
    
    private static final String DEFAULT_POOL_SMTP_MAXIDLE = "40";
    
    private static final String DEFAULT_POOL_SMTP_MINIDLE = "1";
    
    //CACHE
    private static final String DEFAULT_SERVER_CACHE = "false";
    
    //IMAP
    public static String getImapHost() {
        return System.getProperty("namirial.server.imap.address");
    }

    public static String getImapPort() {
        return System.getProperty("namirial.server.imap.port");
    }
    
    public static String getImapFolder() {
        return System.getProperty("namirial.server.imap.folder", DEFAULT_IMAP_FOLDER);
    }
    
    //SMTP
    public static String getSmtpHost() {
        return System.getProperty("namirial.server.smtp.address");
    }
    
    public static String getSmtpPort() {
        return System.getProperty("namirial.server.smtp.port");
    }
    
    //CREDENZIALI PEC
    public static String getPecUsername() {
        return System.getProperty("namirial.pec.username");
    }
    
    public static String getPecPassword() {
        return System.getProperty("namirial.pec.password");
    }
    
    //POOL IMAP
    public static String getPoolImapMaxTotal() {
        return System.getProperty("namirial.pool.imap.maxtotal", DEFAULT_POOL_IMAP_MAXTOTAL);
    }
    
    public static String getPoolImapMaxIdle() {
        return System.getProperty("namirial.pool.imap.maxidle", DEFAULT_POOL_IMAP_MAXIDLE);
    }
    
    public static String getPoolImapMinIdle() {
        return System.getProperty("namirial.pool.imap.minidle", DEFAULT_POOL_IMAP_MINIDLE);
    }
    
    //POOL SMTP
    public static String getPoolSmtpMaxTotal() {
        return System.getProperty("namirial.pool.smtp.maxtotal", DEFAULT_POOL_SMTP_MAXTOTAL);
    }
    
    public static String getPoolSmtpMaxIdle() {
        return System.getProperty("namirial.pool.smtp.maxidle", DEFAULT_POOL_SMTP_MAXIDLE);
    }
    
    public static String getPoolSmtpMinIdle() {
        return System.getProperty("namirial.pool.smtp.minidle", DEFAULT_POOL_SMTP_MINIDLE);
    }
    
    //CACHE
    public static String getCache() {
        return System.getProperty("namirial.server.cache", DEFAULT_SERVER_CACHE);
    }
    
    public static String getCacheEndpoint() {
        return System.getProperty("namirial.server.cache.endpoint");
    }
    
    public static String getCacheApikey() {
        return System.getProperty("namirial.server.cache.apikey");
    }
}
