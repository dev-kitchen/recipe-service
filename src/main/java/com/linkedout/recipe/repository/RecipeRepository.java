package com.linkedout.recipe.repository;

import com.linkedout.common.model.entity.Recipe;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository  extends ReactiveCrudRepository<Recipe, Long> {
}
