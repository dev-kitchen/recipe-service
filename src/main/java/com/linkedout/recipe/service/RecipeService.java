package com.linkedout.recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
  private final ObjectMapper objectMapper;
  private final ModelMapper modelMapper;

  public Mono<String> health() {
    return Mono.just("ok");
  }
}
