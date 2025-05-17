package com.linkedout.recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.messaging.ServiceMessageClient;
import com.linkedout.common.model.dto.EnrichedRequestDTO;
import com.linkedout.common.model.dto.account.AccountDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import com.linkedout.common.model.dto.recipe.request.RecipeCreateDTO;
import com.linkedout.common.model.entity.Recipe;
import com.linkedout.common.util.MonoPipe;
import com.linkedout.recipe.repository.RecipeRepository;
import com.linkedout.recipe.service.helper.RecipeQueryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {
  private final ObjectMapper objectMapper;
  private final ModelMapper modelMapper;
	private final RecipeRepository recipeRepository;
	private final RecipeQueryHelper queryHelper;


	@Override
	public Mono<RecipeDTO> findById(EnrichedRequestDTO<?> request) {
		return MonoPipe.of(request.extractIdFromPath())
			.then(recipeRepository::findById)
			.thenFlatMap(queryHelper::convertAndAttachAuthorInfo)
			.handleError(error -> log.error("DB 쿼리 중 오류 발생: {}", error.getMessage(), error))
			.handleEmpty(Mono::empty)
			.result();
	}


	@Override
	public Mono<Void> save(EnrichedRequestDTO<RecipeCreateDTO> request, AuthenticationDTO accountInfo) {
		Recipe recipe = modelMapper.map(request.getBody(), Recipe.class);
		recipe.setAuthorId(Long.valueOf(accountInfo.getPrincipal()));
		return recipeRepository.save(recipe).then();
	}

}
