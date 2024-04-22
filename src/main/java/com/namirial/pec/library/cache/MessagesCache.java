package com.namirial.pec.library.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namirial.pec.library.client.ImapService;
import com.namirial.pec.library.conf.Configuration;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.Folder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class MessagesCache {
	
	private long timeToLive = Long.parseLong("3600000");
	
	private static MessagesCache single_instance = null;
	
	private static final String CONSTANT_MSGID_PREFIX = "msgId:";
	private static final String CONSTANT_FOLDER_PREFIX = "folder:";
	
	private static final String CONSTANT_ATTRIBUTE_LASTUID = "lastUID";
	private static final String CONSTANT_ATTRIBUTE_REFRESHTIME = "refreshTime";
	
	private static final Logger log = LoggerFactory.getLogger(MessagesCache.class);
	
    private MessagesCache() { 
    	
    } 
    
    public static MessagesCache getInstance() {
    	synchronized (MessagesCache.class) {
    		if (single_instance == null)
    			single_instance = new MessagesCache(); 
    		return single_instance; 
    	}
    }
    
	public Jedis getConnection() {
		try {
			String[] split = Configuration.getCacheEndpoint().split(":");
	        Jedis jedis = new Jedis(split[0], Integer.valueOf(split[1]));
	        jedis.auth(Configuration.getCacheApikey());
	        return jedis;
		} catch (IllegalStateException | JedisConnectionException e) {
			throw new PnSpapiTemporaryErrorException ("getConnection: " + e.getClass() + " " + e.getMessage());
		}
	}
	
	public void closeConnection(Jedis jedis) {
		jedis.close();
	}
	
	public void put (Jedis cacheConnection, String folderName, String messageId, Long uid) {
		try {
        	cacheConnection.hset(CONSTANT_MSGID_PREFIX + folderName, messageId, String.valueOf(uid));
		} catch (IllegalStateException | JedisConnectionException e) {
			throw new PnSpapiTemporaryErrorException ("put: " + e.getClass() + " " + e.getMessage());
		}
    }
	
	public Long get (Jedis cacheConnection, String folderName, String messageId) {
		try {
			if (cacheConnection.hget(CONSTANT_MSGID_PREFIX + folderName, messageId) != null) {
				log.info("MessagesCache-messageId {} cached", messageId);
				return Long.valueOf(cacheConnection.hget(CONSTANT_MSGID_PREFIX + folderName, messageId));
			} else {
				log.info("MessagesCache-messageId {} not cached", messageId);
				return null;
			}
		} catch (IllegalStateException | JedisConnectionException e) {
			throw new PnSpapiTemporaryErrorException ("get: " + e.getClass() + " " + e.getMessage());
		}
	}
	
	public void refresh (Jedis cacheConnection, Long uid, Folder folder) {
		String folderName = folder.getFullName();
		String lastUIDCache;
		String refreshTimeCache;
		boolean refresh = false;
		
		try {
	    	synchronized (this) {
	    		lastUIDCache = cacheConnection.hget(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_LASTUID);
	    		refreshTimeCache = cacheConnection.hget(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_REFRESHTIME);
	    		
	    		if ((lastUIDCache == null && refreshTimeCache == null) ||
	    				(lastUIDCache != null && Long.valueOf(lastUIDCache) < uid) ||
	    				(refreshTimeCache != null && System.currentTimeMillis() > Long.parseLong(refreshTimeCache) + timeToLive)) {
	    			log.info("MessagesCache-refresh for folder {}", folderName);
	    			refresh = true;
	    			cacheConnection.del(CONSTANT_MSGID_PREFIX + folderName);
	    			cacheConnection.del(CONSTANT_FOLDER_PREFIX + folderName);
	    			cacheConnection.hset(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_REFRESHTIME,
	    					String.valueOf(System.currentTimeMillis()));
	    		}
	    	}
	    	
			if (refresh) {
				Long lastUID = ImapService.getMessagesForCache(folder);
				cacheConnection.hset(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_LASTUID, String.valueOf(lastUID));
			}
		} catch (IllegalStateException | JedisConnectionException e) {
			throw new PnSpapiTemporaryErrorException ("refresh: " + e.getClass() + " " + e.getMessage());
		}
	}
}
