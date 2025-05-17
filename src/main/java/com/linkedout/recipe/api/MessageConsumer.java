package com.linkedout.recipe.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.common.util.converter.PayloadConverter;
import com.linkedout.recipe.api.messaging.MessageErrorHandler;
import com.linkedout.recipe.api.messaging.MessageProcessor;
import com.linkedout.recipe.api.messaging.MessageSender;
import com.linkedout.recipe.api.messaging.ResponseFactory;
import com.linkedout.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
	 * ACCOUNT_SERVICE_CONSUMER_QUEUE로부터 서비스 요청 메시지를 소비하고 처리합니다.
	 *
	 * <p>메서드 처리 순서: <br>
	 * 1. 수신된 메시지에서 correlationId, operation, senderService, replyTo 필드를 추출 <br>
	 * 2. operation타입에 따라 적절한 AccountService 메서드를 호출하여 요청된 작업을 수행 <br>
	 * 3. 처리 결과를 ServiceMessageDTO에 담아 송신 서비스의 응답 큐로 전송
	 *
	 * <p>오류 처리: - 작업 처리 중 발생한 예외는 오류 메시지와 함께 응답 DTO에 포함 - 지원하지 않는 operation인 경우
	 * UnsupportedOperationException 발생 - 모든 오류는 로깅되며 클라이언트에게 적절한 오류 응답 전송
	 *
	 * @param requestMessage 처리할 서비스 요청 메시지로 다음 필드들을 포함: - correlationId: 요청-응답 매칭을 위한 고유 식별자 -
	 *                       senderService: 요청을 보낸 서비스의 식별자 - operation: 수행할 작업 유형 (test/findByEmail/createAccount) -
	 *                       replyTo: 응답을 전송할 큐 이름 - payload: 작업에 필요한 데이터
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

		// 비동기 응답 처리
		processResponse(responseMono, correlationId, replyTo);
	}

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