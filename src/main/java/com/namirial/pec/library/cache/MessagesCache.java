package com.namirial.pec.library.cache;

import com.namirial.pec.library.client.ImapService;
import com.namirial.pec.library.conf.Configuration;

import jakarta.mail.Folder;
import redis.clients.jedis.Jedis;

public class MessagesCache {
	
	private long timeToLive = Long.parseLong("3600000");
	
	private static MessagesCache single_instance = null;
	
	private static final String CONSTANT_MSGID_PREFIX = "msgId:";
	private static final String CONSTANT_FOLDER_PREFIX = "folder:";
	
	private static final String CONSTANT_ATTRIBUTE_LASTUID = "lastUID";
	private static final String CONSTANT_ATTRIBUTE_REFRESHTIME = "refreshTime";
	
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
		String[] split = Configuration.getCacheEndpoint().split(":");
        Jedis jedis = new Jedis(split[0], Integer.valueOf(split[1]));
        jedis.auth(Configuration.getCacheApikey());
        return jedis;
	}
	
	public void closeConnection(Jedis jedis) {
		jedis.close();
	}
	
	public void put (Jedis cacheConnection, String folderName, String messageId, Long UID) {
        cacheConnection.hset(CONSTANT_MSGID_PREFIX + folderName, messageId, String.valueOf(UID));
    }
	
	public Long get (Jedis cacheConnection, String folderName, String messageId) {
		if (cacheConnection.hget(CONSTANT_MSGID_PREFIX + folderName, messageId) != null)
			return Long.valueOf(cacheConnection.hget(CONSTANT_MSGID_PREFIX + folderName, messageId));
		else
			return null;
	}
	
	public void refresh (Jedis cacheConnection, Long UID, Folder folder) {
		String folderName = folder.getFullName();
		String lastUIDCache = cacheConnection.hget(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_LASTUID);
		String refreshTimeCache = cacheConnection.hget(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_REFRESHTIME);
		
		if ((lastUIDCache == null || lastUIDCache.equals("null") || Long.valueOf(lastUIDCache) < UID) && (refreshTimeCache == null
				|| refreshTimeCache.equals("null") || Long.valueOf(refreshTimeCache) < System.currentTimeMillis() + timeToLive)) {
			cacheConnection.del(CONSTANT_MSGID_PREFIX + folderName);
			cacheConnection.del(CONSTANT_FOLDER_PREFIX + folderName);
			cacheConnection.hset(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_REFRESHTIME,
					String.valueOf(System.currentTimeMillis()));
			
			Long lastUID = ImapService.getMessagesForCache(folder);
			cacheConnection.hset(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_LASTUID,  String.valueOf(lastUID));
		}
	}
}
