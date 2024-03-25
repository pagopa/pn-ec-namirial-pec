package com.namirial.pec.library.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.namirial.pec.library.pool.ImapConnectionPool;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.MessageIDTerm;
import jakarta.mail.search.SearchTerm;

public class ImapService {
	
	private static final String CONSTANT_FOLDER = "INBOX";
	
	public static PnGetMessagesResponse getUnreadMessages (int limit) {
		
		ImapConnectionPool imapConnectionPool = ImapConnectionPool.getInstance();
        
        Store store = imapConnectionPool.getImapConnection();
        
        try {
            Folder folderInbox = store.getFolder(CONSTANT_FOLDER);
            folderInbox.open(Folder.READ_ONLY);
            
            Message[] messages = folderInbox.search(
            		new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            
            List<byte[]> messagesList = new ArrayList<>();
            
            ByteArrayOutputStream outputStream  = new ByteArrayOutputStream();
            
            for (int i = 0; i < limit && i < messages.length; i++) {
            	messages[i].writeTo(outputStream);
            	messagesList.add(outputStream.toByteArray());
            	outputStream.reset();
            }
            
            folderInbox.close(false);
            
            return new PnGetMessagesResponse(new PnListOfMessages(messagesList), messages.length);
        } catch (MessagingException | IOException e) {
            throw new PnSpapiTemporaryErrorException (e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Void markMessageAsRead (String messageID) {
		
		ImapConnectionPool imapConnectionPool = ImapConnectionPool.getInstance();
        
        Store store = imapConnectionPool.getImapConnection();
        
        try {
            Folder folderInbox = store.getFolder(CONSTANT_FOLDER);
            folderInbox.open(Folder.READ_WRITE);
            
            Message[] messages = getMessagesByMessageID(folderInbox, messageID);
            
            for (Message message : messages) {
            	message.setFlag(Flags.Flag.SEEN, true);
            }
            
            folderInbox.close(false);
            
            return null;
        } catch (MessagingException e) {
        	throw new PnSpapiTemporaryErrorException (e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Integer getMessageCount () {
		
		ImapConnectionPool imapConnectionPool = ImapConnectionPool.getInstance();
        
        Store store = imapConnectionPool.getImapConnection();
        
        try {
            Folder folderInbox = store.getFolder(CONSTANT_FOLDER);
            folderInbox.open(Folder.READ_ONLY);
            
            return folderInbox.getMessageCount();
        } catch (MessagingException e) {
        	throw new PnSpapiTemporaryErrorException (e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Void deleteMessage (String messageID) {
		
		ImapConnectionPool imapConnectionPool = ImapConnectionPool.getInstance();
        
        Store store = imapConnectionPool.getImapConnection();
        
        try {
            Folder folderInbox = store.getFolder(CONSTANT_FOLDER);
            folderInbox.open(Folder.READ_WRITE);
            
            Message[] messages = getMessagesByMessageID(folderInbox, messageID);
            
            for (Message message : messages) {
            	message.setFlag(Flags.Flag.DELETED, true);
            }
            
            folderInbox.close(true);
            
            return null;
        } catch (MessagingException e) {
        	throw new PnSpapiTemporaryErrorException (e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Message[] getMessagesByMessageID(Folder folderInbox, String messageID)
			throws PnSpapiTemporaryErrorException, PnSpapiPermanentErrorException {
		
		try {
			SearchTerm searchTerm = new MessageIDTerm(messageID);
	        Message[] messages = folderInbox.search(searchTerm);
	        
	        if (messages.length > 1)
	        	throw new PnSpapiPermanentErrorException ("The number of messages returned is > 1");
	        
	        return messages;
		} catch (MessagingException e) {
			throw new PnSpapiTemporaryErrorException (e.getMessage());
		}
	}
}
