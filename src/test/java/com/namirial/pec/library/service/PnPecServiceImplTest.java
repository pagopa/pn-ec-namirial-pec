package com.namirial.pec.library.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.namirial.pec.library.client.ImapService;
import com.namirial.pec.library.client.SmtpService;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.PnPecService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

public class PnPecServiceImplTest {
	
	private String messageID = "<opec2117.20240321154004.01152.34.1.121@test.it>";
	private static PnPecService pnPecServiceImpl;
	
    @BeforeAll
    static void beforeAll() {
    	pnPecServiceImpl = new PnPecServiceImpl();
    }

    @BeforeEach
    void beforeEach() {
        VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.create();
        VirtualTimeScheduler.set(virtualTimeScheduler);
    }
    
    @AfterEach
    void afterEach() {
    	VirtualTimeScheduler.reset();
    }
    
    @Test
	void sendMail() throws IOException {
    	
		byte[] message = FileUtils.readFileToByteArray(new File("src/test/resources/emailTest.eml"));
		
		try (MockedStatic<SmtpService> smtpServiceMockStatic = Mockito.mockStatic(SmtpService.class)) {
        	smtpServiceMockStatic.when(() -> SmtpService.sendMail(message)).thenReturn(messageID);
        	Mono<String> sendMailMono = pnPecServiceImpl.sendMail(message);
        	
        	StepVerifier.create(sendMailMono).expectSubscription().expectNext(messageID).expectComplete().verify();
        }
	}
    
    @Test
	void getUnreadMessages() throws IOException, MessagingException {
    	
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
	        Message[] messagesMock = new Message[1];
	        messagesMock[0] = Mockito.mock(Message.class);
	        
	        List<byte[]> messagesList = new ArrayList<>();
	        
	        int limit = 1000;
	        
	        ByteArrayOutputStream outputStream  = new ByteArrayOutputStream();
	        
	        for (int i = 0; i < limit && i < messagesMock.length; i++) {
	        	messagesMock[i].writeTo(outputStream);
	        	messagesList.add(outputStream.toByteArray());
	        	outputStream.reset();
	        }
	        
	        PnGetMessagesResponse pnGetMessagesResponse = new PnGetMessagesResponse(new PnListOfMessages(messagesList), messagesMock.length);
	        
			imapServiceMockStatic.when(() -> ImapService.getUnreadMessages(limit)).thenReturn(pnGetMessagesResponse);
        	Mono<PnGetMessagesResponse> getUnreadMessagesMono = pnPecServiceImpl.getUnreadMessages(limit);
        	
        	StepVerifier.create(getUnreadMessagesMono).expectSubscription().expectNext(pnGetMessagesResponse).expectComplete().verify();
        }
	}
	
	@Test
	void getMessageCount() {
		
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
			int numOfMessages = 100;
			imapServiceMockStatic.when(() -> ImapService.getMessageCount()).thenReturn(numOfMessages);
        	Mono<Integer> getMessageCountMono = pnPecServiceImpl.getMessageCount();
        	
        	StepVerifier.create(getMessageCountMono).expectSubscription().expectNext(numOfMessages).expectComplete().verify();
		}
	}
	
	@Test
	void sendMail_TemporaryException() throws IOException {
    	
		byte[] message = FileUtils.readFileToByteArray(new File("src/test/resources/emailTest.eml"));
		
		try (MockedStatic<SmtpService> smtpServiceMockStatic = Mockito.mockStatic(SmtpService.class)) {
        	smtpServiceMockStatic.when(() -> SmtpService.sendMail(message)).thenThrow(new PnSpapiTemporaryErrorException("Test exception"));
        	Mono<String> sendMailMono = pnPecServiceImpl.sendMail(message);
        	
        	StepVerifier.create(sendMailMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        }
	}
    
    @Test
	void getUnreadMessages_TemporaryException() throws IOException, MessagingException {
    	
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
			int limit = 1000;
			imapServiceMockStatic.when(() -> ImapService.getUnreadMessages(limit)).thenThrow(new PnSpapiTemporaryErrorException("Test exception"));
			Mono<PnGetMessagesResponse> getUnreadMessagesMono = pnPecServiceImpl.getUnreadMessages(limit);
        	
        	StepVerifier.create(getUnreadMessagesMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        }
	}

    @Test
	void markMessageAsRead_TemporaryException() {
    	
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
			imapServiceMockStatic.when(() -> ImapService.markMessageAsRead(messageID)).thenThrow(new PnSpapiTemporaryErrorException("Test exception"));
			Mono<Void> markMessageAsReadMono = pnPecServiceImpl.markMessageAsRead(messageID);
        	
        	StepVerifier.create(markMessageAsReadMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        }
	}
	
	@Test
	void getMessageCount_TemporaryException() {
		
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
			imapServiceMockStatic.when(() -> ImapService.getMessageCount()).thenThrow(new PnSpapiTemporaryErrorException("Test exception"));
			Mono<Integer> getMessageCountMono = pnPecServiceImpl.getMessageCount();
        	
        	StepVerifier.create(getMessageCountMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        }
	}
	
	@Test
	void deleteMessage_TemporaryException() {
		
		try (MockedStatic<ImapService> imapServiceMockStatic = Mockito.mockStatic(ImapService.class)) {
			imapServiceMockStatic.when(() -> ImapService.deleteMessage(messageID)).thenThrow(new PnSpapiTemporaryErrorException("Test exception"));
			Mono<Void> deleteMessageMono = pnPecServiceImpl.deleteMessage(messageID);
        	
        	StepVerifier.create(deleteMessageMono).expectErrorMatches(t -> t instanceof PnSpapiTemporaryErrorException).verify();
        }
	}
}
