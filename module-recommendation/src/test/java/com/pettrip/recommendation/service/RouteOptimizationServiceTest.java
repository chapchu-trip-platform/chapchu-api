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
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationServiceTest {

  @Mock ChatClient chatClient;
  @Mock ChatClient.ChatClientRequestSpec requestSpec;
  @Mock ChatClient.CallResponseSpec callResponseSpec;

  RouteOptimizationService service;

  @BeforeEach
  void setUp() {
    service =
        new RouteOptimizationService(
            chatClient,
            new ObjectMapper(),
            new ClassPathResource("prompts/route-order.st"),
            new ClassPathResource("prompts/course-select.st"));
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
  void 프롬프트가_템플릿에서_렌더되고_미치환마커가_없다() {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("[\"p1\",\"p2\"]");

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

    service.optimizeOrder(places, "소형", 3, "맑음", (short) 25);

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    verify(requestSpec).user(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertThat(prompt).contains("장소:").contains("장소A").contains("반려동물 정보: 소형견 3살");
    assertThat(prompt).doesNotContain("{{").doesNotContain("}}");
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
  void curateCourse는_풀에서_maxStops이하를_이유와_함께_고른다() {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("[{\"id\":\"m2\",\"reason\":\"물놀이 좋아요\"}]");

    List<PlaceInfo> pool =
        List.of(
            new PlaceInfo(
                "m1",
                "장소A",
                "주소",
                new BigDecimal("37.51"),
                new BigDecimal("127.01"),
                "관광지",
                "OUTDOOR",
                PlaceInfo.PlaceGroup.MIDDLE),
            new PlaceInfo(
                "m2",
                "장소B",
                "주소",
                new BigDecimal("37.55"),
                new BigDecimal("127.05"),
                "음식점",
                "INDOOR",
                PlaceInfo.PlaceGroup.MIDDLE));

    List<SelectedPlace> result =
        service.curateCourse(
            pool,
            3,
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "도착장소",
            new BigDecimal("37.6"),
            new BigDecimal("127.1"),
            "소형",
            3,
            List.of("수영"),
            "맑음",
            (short) 25);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo("m2");
    assertThat(result.get(0).reason()).isEqualTo("물놀이 좋아요");
  }

  @Test
  void curateCourse_프롬프트에_취향_날씨_후보가_담기고_미치환마커가_없다() {
    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("[{\"id\":\"m1\",\"reason\":\"좋아요\"}]");

    List<PlaceInfo> pool =
        List.of(
            new PlaceInfo(
                "m1",
                "중간장소",
                "주소",
                new BigDecimal("37.55"),
                new BigDecimal("127.05"),
                "음식점",
                "INDOOR",
                PlaceInfo.PlaceGroup.MIDDLE));

    service.curateCourse(
        pool,
        3,
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        "도착장소",
        new BigDecimal("37.6"),
        new BigDecimal("127.1"),
        "소형",
        3,
        List.of("수영"),
        "맑음",
        (short) 25);

    verify(requestSpec).user(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertThat(prompt).contains("수영");
    assertThat(prompt).contains("맑음");
    assertThat(prompt).contains("중간장소");
    assertThat(prompt).contains("도착장소");
    assertThat(prompt).contains("최대 3");
    assertThat(prompt).doesNotContain("{{").doesNotContain("}}");
  }

  @Test
  void curateCourse_파싱실패시_풀_상위_maxStops로_폴백한다() {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("죄송합니다, 고를 수 없습니다.");

    List<PlaceInfo> pool =
        List.of(
            new PlaceInfo(
                "m1",
                "A",
                "주소",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE),
            new PlaceInfo(
                "m2",
                "B",
                "주소",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE),
            new PlaceInfo(
                "m3",
                "C",
                "주소",
                BigDecimal.ONE,
                BigDecimal.ONE,
                null,
                null,
                PlaceInfo.PlaceGroup.MIDDLE));

    List<SelectedPlace> result =
        service.curateCourse(
            pool,
            2,
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "도착",
            new BigDecimal("37.6"),
            new BigDecimal("127.1"),
            null,
            null,
            List.of(),
            null,
            null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).id()).isEqualTo("m1");
    assertThat(result.get(0).reason()).isNull();
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
