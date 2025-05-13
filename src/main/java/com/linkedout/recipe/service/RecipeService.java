package com.linkedout.recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
	private final ObjectMapper objectMapper;
	private final ModelMapper modelMapper;

	public String test() {
		log.info("서비스로직 진입");
		return "ok";
	}
}
