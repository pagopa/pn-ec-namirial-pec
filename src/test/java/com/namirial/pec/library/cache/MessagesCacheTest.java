package com.namirial.pec.library.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.namirial.pec.library.client.ImapService;

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
    
    @AfterEach
    void afterEach() {
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
}
