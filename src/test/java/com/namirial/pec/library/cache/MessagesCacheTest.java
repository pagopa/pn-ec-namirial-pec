package com.namirial.pec.library.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.namirial.pec.library.client.ImapService;
import com.namirial.pec.library.conf.Configuration;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.Folder;
import redis.clients.jedis.Jedis;

public class MessagesCacheTest {
	
	private String messageId = "<opec2117.20240321154004.01152.34.1.121@test.it>";
	private String folderName = "INBOX";
	private String uidString = "100";
	private String jedisReturn = "1";
	private static final String CONSTANT_MSGID_PREFIX = "msgId:";
	private static final String CONSTANT_FOLDER_PREFIX = "folder:";
	private static final String CONSTANT_ATTRIBUTE_LASTUID = "lastUID";
	private static final String CONSTANT_ATTRIBUTE_REFRESHTIME = "refreshTime";
	private MessagesCache messagesCache;
	private Jedis jedisMock;
	private Folder folderMock;
	
    @BeforeEach
    void beforeEach() {
    	messagesCache = MessagesCache.getInstance();
    	jedisMock = Mockito.mock(Jedis.class);
    	folderMock = Mockito.mock(Folder.class);
    }
    
    @Test
    public void getConnectionSuccess() {
    	
		try (MockedStatic<Configuration> configurationMockStatic = Mockito.mockStatic(Configuration.class)) {
	        try (MockedConstruction<Jedis> jedisMockedConstruction = Mockito.mockConstruction(Jedis.class)) {
	        	configurationMockStatic.when(() -> Configuration.getCacheEndpoint()).thenReturn("localhost:8080");
	        	
	        	Jedis jedis = new Jedis();
	        	
	        	Mockito.when(jedis.auth("password")).thenReturn("OK");
	        	
	        	Jedis returnedJedis = messagesCache.getConnection();
	        	
	        	jedis.close();
	        	
	            assertEquals("OK", jedis.auth("password"));
	            assertEquals(jedis.getClass(), returnedJedis.getClass());
	        }
		}
    }
    
    @Test
	void putSuccess() {
    	
    	Mockito.when(jedisMock.hset(CONSTANT_MSGID_PREFIX + folderName, messageId, uidString)).thenReturn(Long.parseLong(jedisReturn));
    	
        messagesCache.put(jedisMock, folderName, messageId, Long.valueOf(uidString));
        
    	Mockito.verify(jedisMock, times(1)).hset(CONSTANT_MSGID_PREFIX + folderName, messageId, uidString);
    }
    
    @Test
	void getSuccess_cacheHit() {
    	
    	Mockito.when(jedisMock.hget(CONSTANT_MSGID_PREFIX + folderName, messageId)).thenReturn(uidString);
    	
        Long returnedValue = messagesCache.get(jedisMock, folderName, messageId);
        
        assertEquals(Long.valueOf(uidString), returnedValue);
    }
    
    @Test
	void getSuccess_cacheMiss() {
    	
    	Mockito.when(jedisMock.hget(CONSTANT_MSGID_PREFIX + folderName, messageId)).thenReturn(null);
    	
        Long returnedValue = messagesCache.get(jedisMock, folderName, messageId);
        
        assertEquals(null, returnedValue);
    }
    
    @Test
	void refreshSuccess() {
    	
    	Mockito.when(jedisMock.hget(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_LASTUID)).thenReturn(null);
    	Mockito.when(jedisMock.hget(CONSTANT_FOLDER_PREFIX + folderName, CONSTANT_ATTRIBUTE_REFRESHTIME)).thenReturn(null);
    	
    	Mockito.when(jedisMock.del(CONSTANT_MSGID_PREFIX + folderName)).thenReturn(Long.parseLong(jedisReturn));
    	Mockito.when(jedisMock.del(CONSTANT_FOLDER_PREFIX + folderName)).thenReturn(Long.parseLong(jedisReturn));
    	
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
			imapServiceMockStatic.when(() -> ImapService.getMessagesForCache(folderMock)).thenReturn(Long.parseLong(uidString));
        	messagesCache.refresh(jedisMock, Long.valueOf("1"), folderMock);
        	
        	Mockito.verify(jedisMock, times(2)).del(Mockito.anyString());
        	Mockito.verify(jedisMock, times(1)).hset(Mockito.anyString(), Mockito.eq(CONSTANT_ATTRIBUTE_REFRESHTIME), Mockito.anyString());
        	Mockito.verify(jedisMock, times(1)).hset(Mockito.anyString(), Mockito.eq(CONSTANT_ATTRIBUTE_LASTUID), Mockito.anyString());
		}
    }
    
    @Test
    public void getConnection_TemporaryException() {
    	
		try (MockedStatic<Configuration> configurationMockStatic = Mockito.mockStatic(Configuration.class)) {
        	configurationMockStatic.when(() -> Configuration.getCacheEndpoint()).thenReturn("localhost:8080");
        	
        	configurationMockStatic.when(() -> Configuration.getCacheApikey()).thenReturn("password");
        	
            assertThrows(PnSpapiTemporaryErrorException.class, () -> {
            	messagesCache.getConnection();
            });
		}
    }
    
    @Test
    public void put_TemporaryException() {
    	
    	Mockito.doThrow(new PnSpapiTemporaryErrorException("Test exception")).
    		when(jedisMock).hset(CONSTANT_MSGID_PREFIX + folderName, messageId, uidString);
    	
        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
        	messagesCache.put(jedisMock, folderName, messageId, Long.valueOf(uidString));
        });
    }
    
    @Test
    public void get_TemporaryException() {
    	
    	Mockito.doThrow(new PnSpapiTemporaryErrorException("Test exception")).
    		when(jedisMock).hget(CONSTANT_MSGID_PREFIX + folderName, messageId);
    	
        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
        	messagesCache.get(jedisMock, folderName, messageId);
        });
    }
}
