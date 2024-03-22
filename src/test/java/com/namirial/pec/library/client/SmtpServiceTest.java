package com.namirial.pec.library.client;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.namirial.pec.library.pool.SmtpConnectionPool;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class SmtpServiceTest {

	private byte[] message;
	private SmtpConnectionPool smtpConnectionPoolMock;
	private Transport transportMock;
	private MimeMessage mimeMessageMock;
	private Address[] addressesMock;

	@BeforeEach
	void beforeEach() throws IOException {
		message = FileUtils.readFileToByteArray(new File("src/test/resources/emailTest.eml"));
		smtpConnectionPoolMock = Mockito.mock(SmtpConnectionPool.class);
		transportMock = Mockito.mock(Transport.class);
		Mockito.when(smtpConnectionPoolMock.getSmtpConnection()).thenReturn(transportMock);
		mimeMessageMock = Mockito.mock(MimeMessage.class);
		addressesMock = new Address[1];
        addressesMock[0] = Mockito.mock(Address.class);
	}

	@Test
	void sendEmailSuccess() throws MessagingException {
		
        Mockito.mockStatic(SmtpConnectionPool.class);
        Mockito.when(SmtpConnectionPool.getInstance()).thenReturn(smtpConnectionPoolMock);	

        String messageId = null;
        
		Mockito.doNothing().when(transportMock).sendMessage(mimeMessageMock, addressesMock);
		InputStream inputStream = new ByteArrayInputStream(message);
		Session session = Session.getDefaultInstance(new Properties());
		MimeMessage mimeMessage = new MimeMessage(session, inputStream);
        
        String messageIdInput = mimeMessage.getHeader("Message-ID")[0];
        messageId = messageIdInput.substring(1, messageIdInput.length()-1);
		
		String returnedMessageId = SmtpService.sendMail(message);
		assertEquals(messageId, returnedMessageId);
	}
	
    @Test
    public void sendMail_TemporaryException() throws MessagingException {
    	
        Mockito.doThrow(new MessagingException("Test exception")).when(transportMock).sendMessage(mimeMessageMock, addressesMock);
        
        assertThrows(PnSpapiTemporaryErrorException.class, () -> {
            SmtpService.sendMail(message);
        });
    }
}
