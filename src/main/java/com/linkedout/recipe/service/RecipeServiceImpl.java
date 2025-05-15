package com.linkedout.recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.model.dto.EnrichedRequestData;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import com.linkedout.common.model.dto.recipe.request.RecipeCreateDTO;
import com.linkedout.common.model.entity.Recipe;
import com.linkedout.recipe.repository.RecipeRepository;
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


	@Override
  public Mono<String> health() {
    return Mono.just("ok");
  }



	@Override
	public Mono<RecipeDTO> findById(EnrichedRequestData<?> request) {
		Long id = request.extractIdFromPath();
		if (id == null) {
			return Mono.error(new IllegalArgumentException("Invalid ID"));
		}

		return recipeRepository.findById(id)
			.doOnError(err -> log.error("DB 쿼리 중 오류 발생: {}", err.getMessage(), err))
			.mapNotNull(recipe -> modelMapper.map(recipe, RecipeDTO.class))
			.switchIfEmpty(Mono.defer(() -> {
				log.warn("ID {}인 레시피를 찾을 수 없음", id);
				return Mono.empty();
			}));
	}

	@Override
	public Mono<RecipeDTO> save(RecipeCreateDTO recipeCreateDTO) {
		Recipe recipe = modelMapper.map(recipeCreateDTO, Recipe.class);
		return recipeRepository.save(recipe)
			.map(savedRecipe ->  modelMapper.map(savedRecipe, RecipeDTO.class));
	}
}
