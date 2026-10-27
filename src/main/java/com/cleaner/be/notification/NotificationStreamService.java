package com.cleaner.be.notification;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 회원별 SSE 연결을 관리하고 알림 트랜잭션 커밋 후에만 브라우저로 이벤트를 보냅니다. */
@Component
public class NotificationStreamService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationStreamService.class);
	private static final long STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L;

	private final ConcurrentMap<Long, Set<SseEmitter>> emittersByMember = new ConcurrentHashMap<>();

	/** 인증된 회원의 연결을 등록하고 브라우저가 스트림을 식별할 수 있도록 연결 이벤트를 보냅니다. */
	public SseEmitter subscribe(Long memberId) throws IOException {
		SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
		emittersByMember.computeIfAbsent(memberId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
		emitter.onCompletion(() -> remove(memberId, emitter));
		emitter.onTimeout(() -> {
			remove(memberId, emitter);
			emitter.complete();
		});
		emitter.onError(exception -> remove(memberId, emitter));
		try {
			emitter.send(SseEmitter.event().name("connected").data("connected"));
		} catch (IOException | IllegalStateException exception) {
			remove(memberId, emitter);
			emitter.completeWithError(exception);
			throw exception;
		}
		return emitter;
	}

	/** 커밋된 알림만 같은 회원의 모든 열린 브라우저 탭에 전달합니다. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publish(NotificationCreatedEvent event) {
		Set<SseEmitter> emitters = emittersByMember.get(event.memberId());
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
					.id(event.notification().id().toString())
					.name("notification")
					.data(event.notification()));
			} catch (IOException | IllegalStateException exception) {
				remove(event.memberId(), emitter);
				emitter.completeWithError(exception);
				LOGGER.debug("Removing disconnected notification stream for member {}", event.memberId(), exception);
			}
		}
	}

	/** 구독 종료·시간 초과·전송 실패 시 빈 회원별 연결 집합까지 정리합니다. */
	private void remove(Long memberId, SseEmitter emitter) {
		emittersByMember.computeIfPresent(memberId, (ignored, emitters) -> {
			emitters.remove(emitter);
			return emitters.isEmpty() ? null : emitters;
		});
	}

	/** 애플리케이션 종료 시 열린 HTTP 스트림을 닫아 컨테이너 자원이 남지 않게 합니다. */
	@PreDestroy
	public void closeAll() {
		emittersByMember.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
		emittersByMember.clear();
	}
}
