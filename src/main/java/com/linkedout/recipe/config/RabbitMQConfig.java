package com.linkedout.recipe.config;

import com.linkedout.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Bean
  public DirectExchange serviceExchange() {
    // 첫 번째 매개변수: 교환기 이름
    // 두 번째 매개변수: durable (true로 설정하면 RabbitMQ 서버가 재시작되어도 교환기가 유지됨)
    // 세 번째 매개변수: autoDelete (자동 삭제 여부)
    return new DirectExchange(RabbitMQConstants.SERVICE_EXCHANGE, true, false);
  }

  // 소비자
  @Bean
  public Queue consumerQueue() {
    return new Queue(RabbitMQConstants.RECIPE_SERVICE_CONSUMER_QUEUE, false);
  }

  @Bean
  public Binding consumerBinding(Queue consumerQueue, DirectExchange serviceExchange) {
    return BindingBuilder.bind(consumerQueue)
        .to(serviceExchange)
        .with(RabbitMQConstants.RECIPE_CONSUMER_ROUTING_KEY);
  }

  // 리스너
  @Bean
  public Queue listenerQueue() {
    return new Queue(RabbitMQConstants.RECIPE_SERVICE_LISTENER_QUEUE, false);
  }

  @Bean
  public Binding responseBinding(Queue listenerQueue, DirectExchange serviceExchange) {
    return BindingBuilder.bind(listenerQueue)
        .to(serviceExchange)
        .with(RabbitMQConstants.RECIPE_LISTENER_ROUTING_KEY);
  }

  // JSON 메시지 변환기
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  // RabbitTemplate 설정
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    return template;
  }
}
