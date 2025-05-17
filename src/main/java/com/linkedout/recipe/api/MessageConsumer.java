package com.linkedout.recipe.api;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.recipe.api.messaging.MessageErrorHandler;
import com.linkedout.recipe.api.messaging.MessageProcessor;
import com.linkedout.recipe.api.messaging.MessageSender;
import com.linkedout.recipe.api.messaging.ResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 마이크로서비스 간 통신을 위한 메시지 소비자 클래스입니다. 이 클래스는 특정 큐를 리스닝하고 {@link ServiceMessageDTO} 구조에 부합하는 메시지를
 * 처리합니다. 수행되는 작업은 수신 메시지의 operation 필드에 의해 정의되며, 작업 유형에 따라 {@link }의 적절한 메서드로 비즈니스 로직을 위임합니다.
 *
 * <p>이 클래스의 책임: - 다른 마이크로서비스로부터의 메시지 리스닝 - 지정된 작업에 따른 수신 서비스 요청 처리 - {@link ErrorResponseBuilder}를
 * 사용한 오류 처리 및 오류 응답 구성 - `replyTo` 필드에 지정된 응답 큐로 성공 또는 오류 응답 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {
	private final MessageProcessor messageProcessor;
	private final ResponseFactory responseFactory;
	private final MessageErrorHandler errorHandler;
	private final MessageSender messageSender;


	/**
	 * 지정된 큐에서 수신되는 서비스 요청을 처리합니다. 이 메서드는 메시지를 수신하여
	 * 상관관계 ID, 작업 유형, 발신자 서비스, 응답 대상, 페이로드, 인증 정보와 같은
	 * 관련 정보를 추출하고 작업 처리를 {@code messageProcessor}에 위임합니다.
	 * 처리 결과나 오류를 기반으로 응답을 생성하여 `replyTo` 주소로 전송합니다.
	 *
	 * @param requestMessage 실행할 작업, 발신자 서비스 정보, 페이로드, 인증 정보 등을
	 *                       포함하는 서비스 메시지
	 */
	@RabbitListener(queues = RabbitMQConstants.RECIPE_SERVICE_CONSUMER_QUEUE)
	public void processServiceRequest(ServiceMessageDTO<?> requestMessage) {
		String correlationId = requestMessage.getCorrelationId();
		String operation = requestMessage.getOperation();
		String senderService = requestMessage.getSenderService();
		String replyTo = requestMessage.getReplyTo();
		Object payload = requestMessage.getPayload();
		AuthenticationDTO accountInfo = requestMessage.getAuthentication();

		log.debug(
			"서비스 요청 수신: correlationId={}, operation={}, sender={}, replyTo={}, authenticationDTO={}, payload={}",
			correlationId,
			operation,
			senderService,
			replyTo,
			accountInfo,
			payload);

		Mono<ServiceMessageDTO<Object>> responseMono = messageProcessor
			.processOperation(operation, requestMessage, accountInfo)
			.map(result -> responseFactory.createSuccessResponse(correlationId, operation, result))
			.switchIfEmpty(Mono.just(responseFactory.createEmptyResponse(correlationId, operation)))
			.onErrorResume(e -> errorHandler.handleError(e, correlationId, operation));

		processResponse(responseMono, correlationId, replyTo);
	}

	/**
	 * 서비스 요청에 대해 생성된 응답을 처리합니다. 주어진 {@code Mono}를 구독하여
	 * 해결된 응답을 발신자 서비스로 전송하고 처리 중 발생하는 오류를 처리합니다.
	 *
	 * @param responseMono  전송할 응답 데이터를 포함하는 리액티브 발행자
	 * @param correlationId 요청과 응답을 연결하는 고유 식별자
	 * @param replyTo       응답을 전송할 주소나 큐
	 */
	private void processResponse(Mono<ServiceMessageDTO<Object>> responseMono, String correlationId, String replyTo) {
		responseMono
			.subscribeOn(Schedulers.boundedElastic())
			.subscribe(
				response -> messageSender.sendResponse(response, correlationId, replyTo),
				error -> {
					log.error("응답 처리 중 오류 발생: {}", error.getMessage(), error);
					ServiceMessageDTO<Object> errorResponse = responseFactory.createInternalErrorResponse(correlationId, error);
					messageSender.sendResponse(errorResponse, correlationId, replyTo);
				});
	}
}
