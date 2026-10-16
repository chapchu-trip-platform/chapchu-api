package com.pettrip.recommendation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationConfig {

  @Bean
  public ChatClient chatClient(ChatClient.Builder builder) {
    return builder.build();
  }
}
