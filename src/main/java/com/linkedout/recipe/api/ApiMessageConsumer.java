package com.linkedout.recipe.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;
import com.linkedout.common.dto.ServiceMessageDTO;
import com.linkedout.common.exception.BaseException;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.util.converter.PayloadConverter;
import com.linkedout.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;

/**
 * API Gateway로부터 수신한 API 요청을 처리하는 소비자 클래스입니다. 이 클래스는 특정 큐를 리스닝하고 {@link ServiceMessageDTO} 구조에 부합하는
 * 메시지를 처리합니다. 수행되는 작업은 수신 메시지의 operation 필드에 의해 정의되며, 작업 유형에 따라 서비스의 적절한 메서드로 비즈니스 로직을 위임합니다.
 *
 * <p>이 클래스의 책임: - API Gateway로부터의 메시지 리스닝 - 지정된 작업에 따른 수신 서비스 요청 처리 - {@link ErrorResponseBuilder}를
 * 사용한 오류 처리 및 오류 응답 구성 - `replyTo` 필드에 지정된 응답 큐로 성공 또는 오류 응답 전송
 *
 * <p>의존성: - {@link RabbitTemplate} : 응답 메시지 전송용 - {@link RecipeService} : 계정 기반 작업 수행용 - {@link
 * ErrorResponseBuilder} : 구조화된 오류 응답 생성용 - {@link ServiceIdentifier} : 응답을 보내는 서비스 이름 식별용 - {@link
 * ObjectMapper} : JSON 처리용 - {@link PayloadConverter} : 페이로드 데이터를 특정 객체 타입으로 변환용
 *
 * <p>이 컴포넌트의 어노테이션: - {@link Component} : 스프링 관리 컴포넌트 표시 - {@link Slf4j} : 클래스 내 로깅 활성화 - {@link
 * RequiredArgsConstructor} : final 필드의 의존성 주입
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiMessageConsumer {

  private final RabbitTemplate rabbitTemplate;
  private final RecipeService recipeService;
  private final ErrorResponseBuilder errorResponseBuilder;
  private final ServiceIdentifier serviceIdentifier;
  private final ObjectMapper objectMapper;
  private final PayloadConverter payloadConverter;

  /**
   * RabbitMQ 큐를 통해 들어오는 API 요청을 처리하는 리스너 메서드입니다.
   *
   * <p>이 메서드는 다음과 같은 작업을 수행합니다: <br>
   * 1. 큐에서 수신한 메시지를 ApiRequestData 객체로 변환 <br>
   * 2. HTTP 메서드와 경로를 분석하여 적절한 서비스 메서드로 라우팅 <br>
   * 3. 비즈니스 로직 실행 후 결과를 ApiResponseData 형태로 변환 <br>
   * 4. 오류 발생시 적절한 에러 응답 구성 <br>
   * 5. correlationId를 사용하여 요청-응답 추적 <br>
   * 6. 처리된 응답을 지정된 응답 큐로 전송
   *
   * @param request HTTP 메서드, URL 경로, 요청 본문 등을 포함하는 API 요청 객체
   * @param correlationId 요청과 응답을 매칭하기 위한 고유 식별자. 메시지 헤더에서 추출됨
   */
  @RabbitListener(queues = RabbitMQConstants.RECIPE_API_QUEUE)
  public void processApiRequest(
      ApiRequestData request, @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
    log.info("받은 요청: {}, correlationId: {}", request, correlationId);

    // 요청 처리를 Mono로 래핑
    Mono<ApiResponseData> responseMono =
        Mono.defer(
            () -> {
              String path = request.getPath();
              String method = request.getMethod();
              String requestKey = method + " " + path;

              try {
                // 스위치 문으로 다양한 경로 매칭

                return switch (requestKey) {
                  case "GET /api/recipes/health" -> recipeService.health(request, correlationId);

                  default -> {
                    ApiResponseData errorResponse = ApiResponseData.create(correlationId);
                    errorResponseBuilder.populateErrorResponse(
                        errorResponse, 404, "지원하지 않는 작업: " + requestKey);
                    yield Mono.just(errorResponse);
                  }
                };
              } catch (BaseException ex) {
                ApiResponseData errorResponse = ApiResponseData.create(correlationId);
                errorResponseBuilder.populateErrorResponse(
                    errorResponse, ex.getStatusCode(), ex.getMessage());
                return errorResponse.toMono();
              }
            });

    responseMono
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            completedResponse -> {
              // 응답 전송
              rabbitTemplate.convertAndSend(
                  RabbitMQConstants.API_EXCHANGE,
                  RabbitMQConstants.API_GATEWAY_ROUTING_KEY,
                  completedResponse);
              log.info("응답 전송: {}", completedResponse);
            });
  }
}
