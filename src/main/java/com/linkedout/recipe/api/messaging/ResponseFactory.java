package com.linkedout.recipe.api.messaging;


import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseFactory {
	private final ServiceIdentifier serviceIdentifier;

	public ServiceMessageDTO<Object> createSuccessResponse(String correlationId, String operation, Object result) {
		log.info("서비스로직 완료, 응답생성");
		return ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation + "Response")
			.payload(result)
			.build();
	}

	public ServiceMessageDTO<Object> createEmptyResponse(String correlationId, String operation) {
		log.info("빈 결과값, 응답생성");
		return ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation + "Response")
			.payload(null)
			.build();
	}

	public ServiceMessageDTO<Object> createErrorResponse(String correlationId, String operation, String errorMessage) {
		return ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation + "Response")
			.error(errorMessage)
			.build();
	}

	public ServiceMessageDTO<Object> createInternalErrorResponse(String correlationId, Throwable error) {
		return ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation("internalErrorResponse")
			.error("내부 서버 오류: " + error.getMessage())
			.build();
	}
}