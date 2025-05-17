package com.linkedout.recipe.service.helper;

import com.linkedout.common.messaging.serviceClient.ServiceMessageClient;
import com.linkedout.common.model.dto.account.AccountDTO;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import com.linkedout.common.model.entity.Recipe;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RecipeQueryHelper {
	private final ModelMapper modelMapper;
	private final ServiceMessageClient messageClient;

	public Mono<RecipeDTO> convertAndAttachAuthorInfo(Mono<Recipe> recipeMono) {
		return recipeMono.flatMap(recipe -> {
			RecipeDTO recipeDTO = modelMapper.map(recipe, RecipeDTO.class);

			return messageClient.sendMessage("account", "getFindById", recipe.getAuthorId(), AccountDTO.class)
				.map(author -> {
					recipeDTO.setAuthor(author);
					return recipeDTO;
				})
				.onErrorResume(e -> Mono.just(recipeDTO));
		});
	}
}
