package com.linkedout.recipe.config;

import com.linkedout.common.messaging.ServiceIdentifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
  @Value("${service.name}")
  private String serviceName;

  @Bean
  public ServiceIdentifier serviceIdentifier() {
    return new ServiceIdentifier(serviceName);
  }
}
