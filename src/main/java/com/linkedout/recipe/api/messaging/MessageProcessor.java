package com.linkedout.recipe.api.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.linkedout.common.model.dto.EnrichedRequestDTO;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.common.model.dto.recipe.request.RecipeCreateDTO;
import com.linkedout.common.util.converter.PayloadConverter;
import com.linkedout.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProcessor {
	private final RecipeService recipeService;
	private final PayloadConverter payloadConverter;

	public Mono<?> processOperation(String operation, ServiceMessageDTO<?> requestMessage, AuthenticationDTO accountInfo) {
		return switch (operation) {
			case "getById" -> {
				EnrichedRequestDTO<?> requestData = payloadConverter.convert(
					requestMessage.getPayload(), EnrichedRequestDTO.class);
				yield recipeService.findById(requestData);
			}
			case "postRecipes" -> {
				EnrichedRequestDTO<RecipeCreateDTO> requestData = payloadConverter.convert(
					requestMessage.getPayload(), new TypeReference<>() {});
				yield recipeService.save(requestData, accountInfo);
			}
			default -> Mono.error(new UnsupportedOperationException("지원하지 않는 작업: " + operation));
		};
	}
}