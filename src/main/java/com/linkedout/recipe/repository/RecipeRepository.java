package com.linkedout.recipe.repository;

import com.linkedout.recipe.entity.Recipe;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository  extends ReactiveCrudRepository<Recipe, Long> {
}
