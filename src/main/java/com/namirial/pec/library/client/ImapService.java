package com.namirial.pec.library.client;

import com.namirial.pec.library.cache.MessagesCache;
import com.namirial.pec.library.conf.Configuration;
import com.namirial.pec.library.pool.ImapConnectionPool;
import com.sun.mail.util.MessageRemovedIOException;
import it.pagopa.pn.commons.utils.metrics.cloudwatch.CloudWatchMetricHandler;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import jakarta.mail.*;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.MessageIDTerm;
import jakarta.mail.search.SearchTerm;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ImapService {
	private static final Logger log = LoggerFactory.getLogger(ImapService.class);
	
	private static final String CONSTANT_FOLDER = Configuration.getImapFolder();
    private static final String CONSTANT_HASH_FOLDER = "INBOX.";
	private static CloudWatchMetricHandler cloudWatchMetricHandler;

    public ImapService(CloudWatchMetricHandler cloudWatchMetricHandler) {
        this.cloudWatchMetricHandler = new CloudWatchMetricHandler(CloudWatchAsyncClient.create());
    }

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
		
    	MessagesCache messagesCache = null;
    	Jedis cacheConnection = null;
		
		try {
			Long UID = null;
			
			if (Boolean.valueOf(Configuration.getCache())) {
				messagesCache = MessagesCache.getInstance();
				cacheConnection = messagesCache.getConnection();
				UID = messagesCache.get(cacheConnection, folderInbox.getFullName(),messageID);
			}
			
			Message[] messages = new Message[0];
			
			if (UID != null) {
				UIDFolder uf = (UIDFolder) folderInbox;
				
				if (uf.getMessageByUID(UID) != null) {
					messages = new Message[1];
					messages[0] = uf.getMessageByUID(UID);
				}
			} else {
				SearchTerm searchTerm;
				if (delete) {
					MessageIDTerm searchTerm1 = new MessageIDTerm(messageID);
					FlagTerm searchTerm2 = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
					searchTerm = new AndTerm(searchTerm1, searchTerm2);
				} else
					searchTerm = new MessageIDTerm(messageID);
		        messages = folderInbox.search(searchTerm);

				if (messages.length > 1) {
					String duplicatedMessageId = messages[0].getHeader("Message-ID")[0];
					log.info("More than one message found with the same messageID {}", duplicatedMessageId);

					Dimension dimension = Dimension.builder()
							.name("DimensionMessageName")
							.value(Configuration.getMetricDuplicateMessagesName())
							.build();

					cloudWatchMetricHandler.sendMetric(
							Configuration.getMessagesMetricNamespace(),
							dimension,
							Configuration.getMetricDuplicateMessagesName(),
							(double) messages.length-1);
				}

		        if (messagesCache != null && messages.length != 0) {
			        UIDFolder uf = (UIDFolder) folderInbox;
			        messagesCache.refresh(cacheConnection, Long.valueOf(uf.getUID(messages[0])), folderInbox);
		        }
			}
			
	        return messages;
		} catch (MessagingException e) {
			throw new PnSpapiTemporaryErrorException ("getMessagesByMessageID: " + e.getClass() + " " + e.getMessage());
        } finally {
        	if (messagesCache != null && cacheConnection != null)
        		messagesCache.closeConnection(cacheConnection);
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
    
    public static Long getMessagesForCache (Folder folder) {
    	
    	int limit = 1000;
    	MessagesCache messagesCache = null;
    	Jedis cacheConnection = null;
		
        try {
            UIDFolder uf = (UIDFolder) folder;
            Message[] messages = folder.getMessages();
            Long lastUID = null;
            
            messagesCache = MessagesCache.getInstance();
            cacheConnection = messagesCache.getConnection();
            
            for (int i = 0; i < limit && i < messages.length; i++) {
        		Message message = messages[i];
        		lastUID = Long.valueOf(uf.getUID(message));
            	messagesCache.put(cacheConnection, folder.getFullName(), trimMessageID(message.getHeader("Message-ID")[0]), lastUID);
            }
            return lastUID;
        } catch (MessagingException e) {
            throw new PnSpapiTemporaryErrorException ("getMessagesForCache: " + e.getClass() + " " + e.getMessage());
        } finally {
        	if (messagesCache != null && cacheConnection != null)
        		messagesCache.closeConnection(cacheConnection);
        }
	}
}
