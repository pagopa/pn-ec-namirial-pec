package com.namirial.pec.library.client;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.namirial.pec.library.pool.ImapConnectionPool;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;

class ImapServiceTest {
	
	private String messageID = "<opec2117.20240321154004.01152.34.1.121@test.it>";
	private ImapConnectionPool imapConnectionPoolMock;
	private Store storeMock;
	private Folder folderMock;
	
    @BeforeAll
    static void beforeAll() throws MessagingException {
    	 Mockito.mockStatic(ImapConnectionPool.class);
    }
	
    @BeforeEach
    void beforeEach() throws MessagingException {
    	imapConnectionPoolMock = Mockito.mock(ImapConnectionPool.class);
    	storeMock = Mockito.mock(Store.class);
    	Mockito.when(imapConnectionPoolMock.getImapConnection()).thenReturn(storeMock);
    	folderMock = Mockito.mock(Folder.class);
        Mockito.when(storeMock.getFolder(Mockito.anyString())).thenReturn(folderMock);
        Mockito.when(ImapConnectionPool.getInstance()).thenReturn(imapConnectionPoolMock);
    }
    
    @Test
    public void getUnreadMessagesSuccess() throws MessagingException, IOException {
    	
        Message[] messagesMock = new Message[1];
        messagesMock[0] = Mockito.mock(Message.class);
        
        Mockito.doNothing().when(folderMock).open(Mockito.anyInt());
        
        Mockito.when(folderMock.search(Mockito.any(FlagTerm.class))).thenReturn(messagesMock);
        
        List<byte[]> messagesList = new ArrayList<>();
        
        int limit = 1000;
        
        ByteArrayOutputStream outputStream  = new ByteArrayOutputStream();
        
        for (int i = 0; i < limit && i < messagesMock.length; i++) {
        	messagesMock[i].writeTo(outputStream);
        	messagesList.add(outputStream.toByteArray());
        	outputStream.reset();
        }
        
        PnGetMessagesResponse pnGetMessagesResponse = new PnGetMessagesResponse(new PnListOfMessages(messagesList), messagesMock.length);
        
        PnGetMessagesResponse returnedPnGetMessagesResponse = ImapService.getUnreadMessages(limit);
        assertEquals(pnGetMessagesResponse.getNumOfMessages(), returnedPnGetMessagesResponse.getNumOfMessages());
    }
    
    @Test
    public void markMessageAsReadSuccess() throws MessagingException {
        
        Message[] messagesMock = new Message[1];
        messagesMock[0] = Mockito.mock(Message.class);
        
        Mockito.doNothing().when(folderMock).open(Mockito.anyInt());
        
        Mockito.when(folderMock.search(Mockito.any(SearchTerm.class))).thenReturn(messagesMock);
        
    	ImapService.markMessageAsRead(messageID);
    	
        Mockito.verify(messagesMock[0]).setFlag(Flags.Flag.SEEN, true);
        Mockito.verify(folderMock).close(false);
    }
    
    @Test
    public void getMessageCountSuccess() throws MessagingException {
    	
        Message[] messagesMock = new Message[1];
        messagesMock[0] = Mockito.mock(Message.class);
        
        Mockito.doNothing().when(folderMock).open(Mockito.anyInt());
        
        int numOfMessages = 100;
        Mockito.when(folderMock.getMessageCount()).thenReturn(numOfMessages);
        
        Integer returnedNumOfMessages = ImapService.getMessageCount();
        assertEquals(numOfMessages, returnedNumOfMessages);
    }
    
    @Test
    public void deleteMessageSuccess() throws MessagingException {
    	
        Message[] messagesMock = new Message[1];
        messagesMock[0] = Mockito.mock(Message.class);
        
        Mockito.doNothing().when(folderMock).open(Mockito.anyInt());
        
        Mockito.when(folderMock.search(Mockito.any(SearchTerm.class))).thenReturn(messagesMock);
        
        ImapService.deleteMessage(messageID);
        
        Mockito.verify(messagesMock[0]).setFlag(Flags.Flag.DELETED, true);
        Mockito.verify(folderMock).close(false);
    }
    
    @Test
    public void getUnreadMessages_TemporaryException() throws MessagingException {
    	
        Mockito.doThrow(new MessagingException("Test exception")).when(folderMock).open(Folder.READ_ONLY);

        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
            ImapService.getUnreadMessages(100);
        });
    }
    
    @Test
    public void markMessageAsRead_TemporaryException() throws MessagingException {
    	
        Mockito.doThrow(new MessagingException("Test exception")).when(folderMock).open(Folder.READ_WRITE);

        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
            ImapService.markMessageAsRead(messageID);
        });
    }
    
    @Test
    public void getMessageCount_TemporaryException() throws MessagingException {
    	
        Mockito.doThrow(new MessagingException("Test exception")).when(folderMock).open(Folder.READ_ONLY);

        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
            ImapService.getMessageCount();
        });
    }
    
    @Test
    public void deleteMessage_TemporaryException() throws MessagingException {
    	
        Mockito.doThrow(new MessagingException("Test exception")).when(folderMock).open(Folder.READ_WRITE);

        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
            ImapService.deleteMessage(messageID);
        });
    }
    
    @Test
    public void getMessagesByMessageIDSuccess() throws MessagingException {
        
        Message[] messagesMock = new Message[1];
        messagesMock[0] = Mockito.mock(Message.class);
        
        Mockito.when(folderMock.search(Mockito.any(SearchTerm.class))).thenReturn(messagesMock);
        
        Mockito.when(ImapService.getMessagesByMessageID(folderMock, messageID, false)).thenReturn(messagesMock);
        
        Message[] returnedMessages = ImapService.getMessagesByMessageID(folderMock, messageID, false);
        
        assertEquals(messagesMock.length, returnedMessages.length);
    }
}
