package com.pettrip.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    appendGroup(sb, places);
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

  public List<String> selectAndOrder(
      List<PlaceInfo> startGroup,
      List<List<PlaceInfo>> middleGroups,
      List<PlaceInfo> endGroup,
      int n,
      BigDecimal startLat,
      BigDecimal startLng,
      BigDecimal endLat,
      BigDecimal endLng,
      String petSizeLabel,
      Integer petAge,
      String weatherStatus,
      Short temperature) {
    if (startGroup.isEmpty() || endGroup.isEmpty()) {
      return fallbackSelection(startGroup, middleGroups, endGroup);
    }

    List<List<PlaceInfo>> nonEmptyMiddle = middleGroups.stream().filter(g -> !g.isEmpty()).toList();
    int actualMiddleCount = nonEmptyMiddle.size();

    try {
      String prompt =
          buildSelectPrompt(
              startGroup,
              nonEmptyMiddle,
              endGroup,
              actualMiddleCount,
              startLat,
              startLng,
              endLat,
              endLng,
              petSizeLabel,
              petAge,
              weatherStatus,
              temperature);
      String response = chatClient.prompt().user(prompt).call().content();
      return parseAndValidateSelection(
          response, startGroup, nonEmptyMiddle, endGroup, actualMiddleCount);
    } catch (Exception e) {
      return fallbackSelection(startGroup, middleGroups, endGroup);
    }
  }

  private String buildSelectPrompt(
      List<PlaceInfo> startGroup,
      List<List<PlaceInfo>> middleGroups,
      List<PlaceInfo> endGroup,
      int n,
      BigDecimal startLat,
      BigDecimal startLng,
      BigDecimal endLat,
      BigDecimal endLng,
      String petSizeLabel,
      Integer petAge,
      String weatherStatus,
      Short temperature) {
    StringBuilder sb = new StringBuilder();
    sb.append("반려동물과 함께하는 하루 여행 코스를 만들어줘.\n");
    sb.append("출발지에서 시작해 도착지에서 끝나며, 중간에 들를 장소를 골라 방문 순서를 정한다.\n\n");
    sb.append(String.format("[출발지 좌표] 위도 %s, 경도 %s%n", startLat, startLng));
    sb.append(String.format("[도착지 좌표] 위도 %s, 경도 %s%n%n", endLat, endLng));
    sb.append(String.format("[반려동물 정보] %s, %s살%n", nullToEmpty(petSizeLabel), nullToEmpty(petAge)));
    sb.append(
        String.format(
            "[날씨] %s, %s도. 날씨에 맞게 실내/실외 비중 조절해줘.%n%n",
            nullToEmpty(weatherStatus), nullToEmpty(temperature)));
    sb.append("아래 그룹에서 각각 정확히 1개씩 선택해:\n\n");

    sb.append("## 출발지 그룹\n");
    appendGroup(sb, startGroup);

    for (int i = 0; i < middleGroups.size(); i++) {
      sb.append(String.format("## 중간 그룹 %d%n", i + 1));
      appendGroup(sb, middleGroups.get(i));
    }

    sb.append("## 도착지 그룹\n");
    appendGroup(sb, endGroup);

    sb.append("\n선택 기준:\n");
    sb.append("- 동선이 출발지 → 중간 → 도착지로 자연스럽게 이어지도록\n");
    sb.append("- 식당(음식점)은 점심·저녁 시간대에 배치\n");
    sb.append("- 날씨가 나쁘면 실내 장소 우선\n\n");
    sb.append("반드시 아래 형식으로 id만 방문 순서대로 JSON 배열 출력:\n");
    sb.append(String.format("- 배열 길이 정확히 %d%n", n + 2));
    sb.append("- 첫 번째 id는 반드시 출발지 그룹\n");
    sb.append("- 마지막 id는 반드시 도착지 그룹\n");
    sb.append("- 가운데 id들은 각 중간 그룹에서 순서대로 하나씩, 중복 없음\n");
    sb.append("- 설명 없이 배열만 출력\n\n");
    sb.append("예시: [\"id1\",\"id2\",\"id3\",\"id4\"]\n");
    return sb.toString();
  }

  private void appendGroup(StringBuilder sb, List<PlaceInfo> group) {
    for (PlaceInfo p : group) {
      String categoryLabel = nullToEmpty(p.categoryLabel());
      String indoorOutdoor = "BOTH";
      if (p.indoorOutdoor() != null) {
        indoorOutdoor = p.indoorOutdoor();
      }
      sb.append(
          String.format(
              "{\"id\":\"%s\",\"name\":\"%s\",\"address\":\"%s\","
                  + "\"lat\":%s,\"lng\":%s,\"category\":\"%s\",\"indoorOutdoor\":\"%s\"}%n",
              p.id(), p.name(), p.address(), p.lat(), p.lng(), categoryLabel, indoorOutdoor));
    }
  }

  private String nullToEmpty(String s) {
    if (s == null) {
      return "";
    }
    return s;
  }

  private String nullToEmpty(Object o) {
    if (o == null) {
      return "";
    }
    return o.toString();
  }

  private List<String> parseAndValidateSelection(
      String response,
      List<PlaceInfo> startGroup,
      List<List<PlaceInfo>> middleGroups,
      List<PlaceInfo> endGroup,
      int n) {
    try {
      String json = response.trim();
      int start = json.indexOf('[');
      int end = json.lastIndexOf(']');
      if (start == -1 || end == -1) {
        return fallbackSelection(startGroup, middleGroups, endGroup);
      }
      List<String> ids =
          objectMapper.readValue(
              json.substring(start, end + 1), new TypeReference<List<String>>() {});
      if (ids.size() != n + 2) {
        return fallbackSelection(startGroup, middleGroups, endGroup);
      }
      Set<String> startIds = idSet(startGroup);
      Set<String> endIds = idSet(endGroup);
      if (!startIds.contains(ids.get(0))) {
        return fallbackSelection(startGroup, middleGroups, endGroup);
      }
      if (!endIds.contains(ids.get(ids.size() - 1))) {
        return fallbackSelection(startGroup, middleGroups, endGroup);
      }
      for (int i = 0; i < middleGroups.size(); i++) {
        if (!idSet(middleGroups.get(i)).contains(ids.get(i + 1))) {
          return fallbackSelection(startGroup, middleGroups, endGroup);
        }
      }
      if (Set.copyOf(ids).size() != ids.size()) {
        return fallbackSelection(startGroup, middleGroups, endGroup);
      }
      return ids;
    } catch (Exception e) {
      return fallbackSelection(startGroup, middleGroups, endGroup);
    }
  }

  private Set<String> idSet(List<PlaceInfo> group) {
    return group.stream().map(PlaceInfo::id).collect(Collectors.toSet());
  }

  private List<String> fallbackSelection(
      List<PlaceInfo> startGroup, List<List<PlaceInfo>> middleGroups, List<PlaceInfo> endGroup) {
    List<String> ids = new ArrayList<>();
    if (!startGroup.isEmpty()) {
      ids.add(startGroup.get(0).id());
    }
    for (List<PlaceInfo> group : middleGroups) {
      if (group.isEmpty()) {
        continue;
      }
      ids.add(group.get(0).id());
    }
    if (!endGroup.isEmpty()) {
      ids.add(endGroup.get(0).id());
    }
    return ids;
  }
}
