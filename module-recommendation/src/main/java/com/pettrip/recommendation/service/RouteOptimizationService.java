package com.pettrip.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RouteOptimizationService {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final String routeOrderTemplate;
  private final String courseSelectTemplate;

  public RouteOptimizationService(
      ChatClient chatClient,
      ObjectMapper objectMapper,
      @Value("classpath:/prompts/route-order.st") Resource routeOrderTemplate,
      @Value("classpath:/prompts/course-select.st") Resource courseSelectTemplate) {
    this.chatClient = chatClient;
    this.objectMapper = objectMapper;
    this.routeOrderTemplate = loadTemplate(routeOrderTemplate);
    this.courseSelectTemplate = loadTemplate(courseSelectTemplate);
  }

  private static String loadTemplate(Resource resource) {
    try {
      return resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("프롬프트 템플릿 로드 실패: " + resource.getFilename(), e);
    }
  }

  /** {@code {{key}}} 마커를 값으로 치환한다. 프롬프트의 JSON 예시 중괄호와 충돌하지 않게 이중 중괄호를 쓴다. */
  private static String render(String template, Map<String, String> vars) {
    String result = template;
    for (Map.Entry<String, String> entry : vars.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
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
    StringBuilder placesBlock = new StringBuilder();
    appendGroup(placesBlock, places);

    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("petInfoLine", buildPetInfoLine(petSizeLabel, petAge));
    vars.put("weatherLine", buildWeatherLine(weatherStatus, temperature));
    vars.put("placesBlock", placesBlock.toString());
    return render(routeOrderTemplate, vars);
  }

  private String buildPetInfoLine(String petSizeLabel, Integer petAge) {
    if (petSizeLabel == null && petAge == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder("반려동물 정보: ");
    if (petSizeLabel != null) sb.append(petSizeLabel).append("견 ");
    if (petAge != null) sb.append(petAge).append("살. ");
    return sb.append("\n").toString();
  }

  private String buildWeatherLine(String weatherStatus, Short temperature) {
    if (weatherStatus == null && temperature == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder("날씨: ");
    if (weatherStatus != null) sb.append(weatherStatus).append(" ");
    if (temperature != null) sb.append(temperature).append("도. ");
    return sb.append("날씨에 맞게 실내/실외 비중을 조절해줘.\n").toString();
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
    StringBuilder groups = new StringBuilder();
    groups.append("## 출발지 그룹\n");
    appendGroup(groups, startGroup);
    for (int i = 0; i < middleGroups.size(); i++) {
      groups.append(String.format("## 중간 그룹 %d%n", i + 1));
      appendGroup(groups, middleGroups.get(i));
    }
    groups.append("## 도착지 그룹\n");
    appendGroup(groups, endGroup);

    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("startLat", String.valueOf(startLat));
    vars.put("startLng", String.valueOf(startLng));
    vars.put("endLat", String.valueOf(endLat));
    vars.put("endLng", String.valueOf(endLng));
    vars.put("petSize", nullToEmpty(petSizeLabel));
    vars.put("petAge", nullToEmpty(petAge));
    vars.put("weatherStatus", nullToEmpty(weatherStatus));
    vars.put("temperature", nullToEmpty(temperature));
    vars.put("groupsBlock", groups.toString());
    vars.put("arrayLength", String.valueOf(n + 2));
    return render(courseSelectTemplate, vars);
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
