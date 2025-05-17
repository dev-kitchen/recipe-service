package com.linkedout.recipe.api.messaging;

import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageErrorHandler {
	private final ErrorResponseBuilder errorResponseBuilder;
	private final ServiceIdentifier serviceIdentifier;

	public Mono<ServiceMessageDTO<Object>> handleError(Throwable e, String correlationId, String operation) {
		log.error("서비스 요청 처리 오류: operation={}, error={}", operation, e.getMessage(), e);

		return Mono.just(ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation + "Response")
			.error(e.getMessage())
			.build());
	}
}