package com.namirial.pec.library.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.namirial.pec.library.conf.Configuration;
import com.namirial.pec.library.pool.ImapConnectionPool;

import com.sun.mail.util.MessageRemovedIOException;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessageRemovedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.MessageIDTerm;
import jakarta.mail.search.SearchTerm;

public class ImapService {
	
	private static final String CONSTANT_FOLDER = Configuration.getImapFolder();
    private static final String CONSTANT_HASH_FOLDER = "INBOX.";
	
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
            
            for (int i = 0; messagesList.size() < limit && i < messages.length; i++) {
            	try {
	            	messages[i].writeTo(outputStream);
	            	messagesList.add(outputStream.toByteArray());
	            	outputStream.reset();
            	} catch (MessageRemovedIOException | MessageRemovedException e) {
            		//Queste eccezioni vengono generate sempre quando il messaggio è cancellato. Il messaggio verrà saltato.
            		continue;
            	}
            }
            
            folderInbox.close(false);
            
            return new PnGetMessagesResponse(new PnListOfMessages(messagesList), messages.length);
        } catch (MessagingException | IOException e) {
            throw new PnSpapiTemporaryErrorException ("getUnreadMessages: " + e.getClass() + " " + e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Void markMessageAsRead (String messageID) {
		
		ImapConnectionPool imapConnectionPool = ImapConnectionPool.getInstance();
        
        Store store = imapConnectionPool.getImapConnection();
        
        messageID = trimMessageID(messageID);
        
        try {
            Folder folderInbox = store.getFolder(CONSTANT_HASH_FOLDER + getHashFolder(messageID));
            folderInbox.open(Folder.READ_WRITE);
            
            Message[] messages = getMessagesByMessageID(folderInbox, messageID, false);
            
            for (Message message : messages) {
            	message.setFlag(Flags.Flag.SEEN, true);
            }
            
            folderInbox.close(false);
            
            return null;
        } catch (MessagingException e) {
        	throw new PnSpapiTemporaryErrorException ("markMessageAsRead: " + e.getClass() + " " + e.getMessage());
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
        	throw new PnSpapiTemporaryErrorException ("getMessageCount: " + e.getClass() + " " + e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Void deleteMessage (String messageID) {
		
		ImapConnectionPool imapConnectionPool = ImapConnectionPool.getInstance();
        
        Store store = imapConnectionPool.getImapConnection();
        
        messageID = trimMessageID(messageID);
        
        try {
            Folder folderInbox = store.getFolder(CONSTANT_HASH_FOLDER + getHashFolder(messageID));
            folderInbox.open(Folder.READ_WRITE);
            
            Message[] messages = getMessagesByMessageID(folderInbox, messageID, true);
            
            for (Message message : messages) {
            	message.setFlag(Flags.Flag.DELETED, true);
            }
            
            folderInbox.close(false);
            
            return null;
        } catch (MessagingException e) {
        	throw new PnSpapiTemporaryErrorException ("deleteMessage: " + e.getClass() + " " + e.getMessage());
        } finally {
        	imapConnectionPool.releaseImapConnection(store);
        }
	}
	
	public static Message[] getMessagesByMessageID(Folder folderInbox, String messageID, boolean delete)
			throws PnSpapiTemporaryErrorException, PnSpapiPermanentErrorException {
		
		try {
			SearchTerm searchTerm;
			if (delete) {
				MessageIDTerm searchTerm1 = new MessageIDTerm(messageID);
				FlagTerm searchTerm2 = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
				searchTerm = new AndTerm(searchTerm1, searchTerm2);
			} else
				searchTerm = new MessageIDTerm(messageID);
	        Message[] messages = folderInbox.search(searchTerm);
	        
	        if (messages.length > 1)
	        	throw new PnSpapiPermanentErrorException ("The number of messages returned is > 1");
	        
	        return messages;
		} catch (MessagingException e) {
			throw new PnSpapiTemporaryErrorException ("getMessagesByMessageID: " + e.getClass() + " " + e.getMessage());
		}
	}
    
    public static String getHashFolder (String messageID) {
        String sha1MessageID = DigestUtils.sha1Hex(messageID);
        return sha1MessageID.substring(0, 1);
    }
    
    private static String trimMessageID (String messageID) {
        if (messageID.startsWith("<"))
            messageID = messageID.substring(1);
        
        if (messageID.endsWith(">"))
            messageID = messageID.substring(0, messageID.length()-1);
        
        return messageID;
    }
}
