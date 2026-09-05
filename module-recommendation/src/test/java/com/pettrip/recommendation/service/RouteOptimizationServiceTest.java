package com.pettrip.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
                "OUTDOOR",
                PlaceInfo.PlaceGroup.MIDDLE),
            new PlaceInfo(
                "p2",
                "장소B",
                "주소B",
                new BigDecimal("37.6"),
                new BigDecimal("127.1"),
                "음식점",
                "INDOOR",
                PlaceInfo.PlaceGroup.MIDDLE));

    List<String> result = service.optimizeOrder(places, "소형", 3, "맑음", (short) 25);

    assertThat(result).containsExactly("p2", "p1");
  }

  @Test
  void AI_실패시_원래_순서로_폴백한다() {
    when(chatClient.prompt()).thenThrow(new RuntimeException("API error"));

    List<PlaceInfo> places =
        List.of(
            new PlaceInfo(
                "p1",
                "장소A",
                "주소A",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE),
            new PlaceInfo(
                "p2",
                "장소B",
                "주소B",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE));

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
            new PlaceInfo(
                "p1",
                "장소A",
                "주소A",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE),
            new PlaceInfo(
                "p2",
                "장소B",
                "주소B",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE));

    List<String> result = service.optimizeOrder(places, null, null, null, null);

    assertThat(result).containsExactly("p1", "p2");
  }

  @Test
  void 중간그룹이_비어있으면_건너뛰고_나머지로_코스를_구성한다() {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("[\"s1\",\"m2\",\"e1\"]");

    List<PlaceInfo> startGroup =
        List.of(
            new PlaceInfo(
                "s1",
                "출발",
                "주소",
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "관광지",
                "OUTDOOR",
                PlaceInfo.PlaceGroup.START));
    List<List<PlaceInfo>> middleGroups =
        List.of(
            List.of(),
            List.of(
                new PlaceInfo(
                    "m2",
                    "중간",
                    "주소",
                    new BigDecimal("37.55"),
                    new BigDecimal("127.05"),
                    "음식점",
                    "INDOOR",
                    PlaceInfo.PlaceGroup.MIDDLE)));
    List<PlaceInfo> endGroup =
        List.of(
            new PlaceInfo(
                "e1",
                "도착",
                "주소",
                new BigDecimal("37.6"),
                new BigDecimal("127.1"),
                "관광지",
                "OUTDOOR",
                PlaceInfo.PlaceGroup.END));

    List<String> result =
        service.selectAndOrder(
            startGroup,
            middleGroups,
            endGroup,
            2,
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            new BigDecimal("37.6"),
            new BigDecimal("127.1"),
            "소형",
            3,
            "맑음",
            (short) 25);

    assertThat(result).containsExactly("s1", "m2", "e1");
  }

  @Test
  void selectAndOrder_프롬프트에_반려동물_날씨_그룹_정보가_포함된다() {
    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("[\"s1\",\"m1\",\"e1\"]");

    List<PlaceInfo> startGroup =
        List.of(
            new PlaceInfo(
                "s1",
                "출발장소",
                "주소",
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "관광지",
                "OUTDOOR",
                PlaceInfo.PlaceGroup.START));
    List<List<PlaceInfo>> middleGroups =
        List.of(
            List.of(),
            List.of(
                new PlaceInfo(
                    "m1",
                    "중간장소",
                    "주소",
                    new BigDecimal("37.55"),
                    new BigDecimal("127.05"),
                    "음식점",
                    "INDOOR",
                    PlaceInfo.PlaceGroup.MIDDLE)));
    List<PlaceInfo> endGroup =
        List.of(
            new PlaceInfo(
                "e1",
                "도착장소",
                "주소",
                new BigDecimal("37.6"),
                new BigDecimal("127.1"),
                "관광지",
                "OUTDOOR",
                PlaceInfo.PlaceGroup.END));

    service.selectAndOrder(
        startGroup,
        middleGroups,
        endGroup,
        2,
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        new BigDecimal("37.6"),
        new BigDecimal("127.1"),
        "소형",
        3,
        "맑음",
        (short) 25);

    verify(requestSpec).user(promptCaptor.capture());
    String prompt = promptCaptor.getValue();

    assertThat(prompt).contains("소형");
    assertThat(prompt).contains("3");
    assertThat(prompt).contains("맑음");
    assertThat(prompt).contains("25");
    assertThat(prompt).contains("s1");
    assertThat(prompt).contains("출발장소");
    assertThat(prompt).contains("e1");
    assertThat(prompt).contains("도착장소");
    assertThat(prompt).contains("m1");
    assertThat(prompt).contains("중간장소");
    assertThat(prompt).contains("배열 길이 정확히 3");
  }

  @Test
  void 장소가_하나면_AI_호출_없이_그대로_반환한다() {
    List<PlaceInfo> places =
        List.of(
            new PlaceInfo(
                "p1",
                "장소A",
                "주소A",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE));

    List<String> result = service.optimizeOrder(places, null, null, null, null);

    assertThat(result).containsExactly("p1");
  }
}
