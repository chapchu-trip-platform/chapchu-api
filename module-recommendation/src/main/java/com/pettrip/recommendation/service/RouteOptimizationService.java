package com.pettrip.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RouteOptimizationService {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;

  public RouteOptimizationService(ChatClient chatClient, ObjectMapper objectMapper) {
    this.chatClient = chatClient;
    this.objectMapper = objectMapper;
  }

  public List<String> optimizeOrder(
      List<PlaceInfo> places,
      String petSizeLabel,
      Integer petAge,
      String weatherStatus,
      Short temperature) {
    if (places.size() <= 1) {
      return places.stream().map(PlaceInfo::id).toList();
    }
    try {
      String prompt = buildPrompt(places, petSizeLabel, petAge, weatherStatus, temperature);
      String response = chatClient.prompt().user(prompt).call().content();
      return parseAndValidate(response, places);
    } catch (Exception e) {
      return fallback(places);
    }
  }

  private String buildPrompt(
      List<PlaceInfo> places,
      String petSizeLabel,
      Integer petAge,
      String weatherStatus,
      Short temperature) {
    StringBuilder sb = new StringBuilder();
    sb.append("다음 장소들을 반려동물 동반 하루 여행에 최적화된 방문 순서로 정렬해줘.\n");
    sb.append("기준: 이동 동선 최소화(위경도 기반), 식당은 점심·저녁 시간대 배치.\n");

    if (petSizeLabel != null || petAge != null) {
      sb.append("반려동물 정보: ");
      if (petSizeLabel != null) sb.append(petSizeLabel).append("견 ");
      if (petAge != null) sb.append(petAge).append("살. ");
      sb.append("\n");
    }

    if (weatherStatus != null || temperature != null) {
      sb.append("날씨: ");
      if (weatherStatus != null) sb.append(weatherStatus).append(" ");
      if (temperature != null) sb.append(temperature).append("도. ");
      sb.append("날씨에 맞게 실내/실외 비중을 조절해줘.\n");
    }

    sb.append("반드시 JSON 배열로 id만 반환해. 예: [\"id1\",\"id2\"]\n\n");
    sb.append("장소:\n");
    for (PlaceInfo p : places) {
      sb.append(
          String.format(
              "{\"id\":\"%s\",\"name\":\"%s\",\"address\":\"%s\","
                  + "\"lat\":%s,\"lng\":%s,\"category\":\"%s\",\"indoorOutdoor\":\"%s\"}\n",
              p.id(),
              p.name(),
              p.address(),
              p.lat(),
              p.lng(),
              p.categoryLabel() != null ? p.categoryLabel() : "",
              p.indoorOutdoor() != null ? p.indoorOutdoor() : "BOTH"));
    }
    return sb.toString();
  }

  private List<String> parseAndValidate(String response, List<PlaceInfo> places) {
    try {
      String json = response.trim();
      int start = json.indexOf('[');
      int end = json.lastIndexOf(']');
      if (start == -1 || end == -1) {
        return fallback(places);
      }
      List<String> ids =
          objectMapper.readValue(
              json.substring(start, end + 1), new TypeReference<List<String>>() {});
      Set<String> validIds = places.stream().map(PlaceInfo::id).collect(Collectors.toSet());
      if (ids.size() == places.size() && ids.stream().allMatch(validIds::contains)) {
        return ids;
      }
      return fallback(places);
    } catch (Exception e) {
      return fallback(places);
    }
  }

  private List<String> fallback(List<PlaceInfo> places) {
    return places.stream().map(PlaceInfo::id).toList();
  }
}
