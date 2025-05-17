package com.linkedout.recipe.service;

import com.linkedout.common.model.dto.EnrichedRequestDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import com.linkedout.common.model.dto.recipe.request.RecipeCreateDTO;
import reactor.core.publisher.Mono;

public interface RecipeService  {

	// 레시피 ID로 조회
	Mono<RecipeDTO> findById(EnrichedRequestDTO<?> requestData);

	// 일반 저장 메서드
	Mono<Void> save(EnrichedRequestDTO<RecipeCreateDTO> requestData, AuthenticationDTO accountInfo);

	// 이름으로 유사도 검색 (기본 임계값 사용)
//	Flux<Recipe> searchByNameSimilarity(String searchTerm);
//
//	// 이름으로 유사도 검색 (임계값 지정)
//	Flux<Recipe> searchByNameSimilarity(String searchTerm, float similarityThreshold);
//
//	// 정규화된 이름으로 유사도 검색 (대소문자 구분 없음)
//	Flux<Recipe> searchByNormalizedName(String searchTerm);
//
//	// 정규화된 이름으로 유사도 검색 (임계값 지정)
//	Flux<Recipe> searchByNormalizedName(String searchTerm, float similarityThreshold);
}
