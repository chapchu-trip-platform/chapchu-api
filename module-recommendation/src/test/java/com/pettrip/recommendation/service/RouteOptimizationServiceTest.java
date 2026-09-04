package com.pettrip.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationServiceTest {

  @Mock ChatClient chatClient;
  @Mock ChatClient.ChatClientRequestSpec requestSpec;
  @Mock ChatClient.CallResponseSpec callResponseSpec;

  RouteOptimizationService service;

  @BeforeEach
  void setUp() {
    service = new RouteOptimizationService(chatClient, new ObjectMapper());
  }

  @Test
  void AI응답으로_최적화된_순서를_반환한다() {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("[\"p2\",\"p1\"]");

    List<PlaceInfo> places =
        List.of(
            new PlaceInfo(
                "p1",
                "장소A",
                "주소A",
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "관광지",
                "OUTDOOR"),
            new PlaceInfo(
                "p2",
                "장소B",
                "주소B",
                new BigDecimal("37.6"),
                new BigDecimal("127.1"),
                "음식점",
                "INDOOR"));

    List<String> result = service.optimizeOrder(places, "소형", 3, "맑음", (short) 25);

    assertThat(result).containsExactly("p2", "p1");
  }

  @Test
  void AI_실패시_원래_순서로_폴백한다() {
    when(chatClient.prompt()).thenThrow(new RuntimeException("API error"));

    List<PlaceInfo> places =
        List.of(
            new PlaceInfo("p1", "장소A", "주소A", BigDecimal.ONE, BigDecimal.ONE, null, null),
            new PlaceInfo("p2", "장소B", "주소B", BigDecimal.ONE, BigDecimal.ONE, null, null));

    List<String> result = service.optimizeOrder(places, null, null, null, null);

    assertThat(result).containsExactly("p1", "p2");
  }

  @Test
  void AI가_잘못된_응답_반환시_원래_순서로_폴백한다() {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("죄송합니다, 순서를 결정할 수 없습니다.");

    List<PlaceInfo> places =
        List.of(
            new PlaceInfo("p1", "장소A", "주소A", BigDecimal.ONE, BigDecimal.ONE, null, null),
            new PlaceInfo("p2", "장소B", "주소B", BigDecimal.ONE, BigDecimal.ONE, null, null));

    List<String> result = service.optimizeOrder(places, null, null, null, null);

    assertThat(result).containsExactly("p1", "p2");
  }

  @Test
  void 장소가_하나면_AI_호출_없이_그대로_반환한다() {
    List<PlaceInfo> places =
        List.of(new PlaceInfo("p1", "장소A", "주소A", BigDecimal.ONE, BigDecimal.ONE, null, null));

    List<String> result = service.optimizeOrder(places, null, null, null, null);

    assertThat(result).containsExactly("p1");
  }
}
