package com.namirial.pec.library.service;

import com.namirial.pec.library.client.ImapService;
import com.namirial.pec.library.client.SmtpService;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.PnPecService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class PnPecServiceImpl implements PnPecService {

	@Override
	public Mono<String> sendMail(byte[] message) {
		
        return Mono.fromCallable(() -> {
            return SmtpService.sendMail(message);
        }).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
		
		return Mono.fromCallable(() -> {
			return ImapService.getUnreadMessages(limit);
        }).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<Void> markMessageAsRead(String messageID) {
		
		return Mono.fromCallable(() -> {
			return ImapService.markMessageAsRead(messageID);
        }).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<Integer> getMessageCount() {
		
		return Mono.fromCallable(() -> {
			return ImapService.getMessageCount();
        }).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<Void> deleteMessage(String messageID) {
		
		return Mono.fromCallable(() -> {
			return ImapService.deleteMessage(messageID);
        }).subscribeOn(Schedulers.boundedElastic());
	}
}
