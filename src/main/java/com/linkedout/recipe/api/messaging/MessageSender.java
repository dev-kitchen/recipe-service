package com.linkedout.recipe.api.messaging;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {
	private final RabbitTemplate rabbitTemplate;

	public void sendResponse(ServiceMessageDTO<Object> response, String correlationId, String replyTo) {
		log.info(
			"서비스 응답 전송: correlationId={}, replyTo={}, 응답타입={}",
			correlationId,
			replyTo,
			(response.getError() != null ? "오류" : "성공"));

		rabbitTemplate.convertAndSend(
			RabbitMQConstants.SERVICE_EXCHANGE,
			replyTo,
			response);

		log.info("서비스 응답 전송 완료: correlationId={}", correlationId);
	}
}