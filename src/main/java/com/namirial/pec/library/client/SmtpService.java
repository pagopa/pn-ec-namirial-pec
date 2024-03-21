package com.namirial.pec.library.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.namirial.pec.library.pool.SmtpConnectionPool;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.ParseException;

public class SmtpService {
	
	public static String sendMail (byte[] message) {
		
		Session session = Session.getDefaultInstance(new Properties());
		
		SmtpConnectionPool smtpConnectionPool = SmtpConnectionPool.getInstance();
        
        Transport transport = smtpConnectionPool.getSmtpConnection();
        
        try {
        	InputStream inputStream = new ByteArrayInputStream(message);
        	
            MimeMessage mimeMessage = new MimeMessage(session, inputStream);
            
	        transport.sendMessage(mimeMessage, mimeMessage.getRecipients(Message.RecipientType.TO));
	        
	        String messageIdInput = mimeMessage.getHeader("Message-ID")[0];
	        
	        return messageIdInput.substring(1, messageIdInput.length()-1);
        } catch (ParseException e) {
        	throw new PnSpapiPermanentErrorException(e.getMessage());
        } catch (MessagingException e) {
            throw new PnSpapiTemporaryErrorException(e.getMessage());
        } finally {
        	smtpConnectionPool.releaseSmtpConnection(transport);
        }
	}
}
