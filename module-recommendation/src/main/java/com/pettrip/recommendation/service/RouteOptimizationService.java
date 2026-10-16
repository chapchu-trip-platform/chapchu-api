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

  /**
   * 후보 풀에서 출발→도착 흐름·취향·날씨를 보고 중간 스탑을 최대 maxStops개 큐레이션한다(도착지 제외, 이유 포함). 실패 시 풀 상위 maxStops개로 폴백.
   */
  public List<SelectedPlace> curateCourse(
      List<PlaceInfo> pool,
      int maxStops,
      BigDecimal startLat,
      BigDecimal startLng,
      String destinationName,
      BigDecimal destLat,
      BigDecimal destLng,
      String petSizeLabel,
      Integer petAge,
      List<String> petActivities,
      String weatherStatus,
      Short temperature) {
    if (pool.isEmpty() || maxStops <= 0) {
      return List.of();
    }
    try {
      String prompt =
          buildCuratePrompt(
              pool,
              maxStops,
              startLat,
              startLng,
              destinationName,
              destLat,
              destLng,
              petSizeLabel,
              petAge,
              petActivities,
              weatherStatus,
              temperature);
      String response = chatClient.prompt().user(prompt).call().content();
      return parseCuration(response, pool, maxStops);
    } catch (Exception e) {
      return curateFallback(pool, maxStops);
    }
  }

  private String buildCuratePrompt(
      List<PlaceInfo> pool,
      int maxStops,
      BigDecimal startLat,
      BigDecimal startLng,
      String destinationName,
      BigDecimal destLat,
      BigDecimal destLng,
      String petSizeLabel,
      Integer petAge,
      List<String> petActivities,
      String weatherStatus,
      Short temperature) {
    StringBuilder poolBlock = new StringBuilder();
    appendGroup(poolBlock, pool);

    String activities = "";
    if (petActivities != null && !petActivities.isEmpty()) {
      activities = String.join(", ", petActivities);
    }

    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("petSize", nullToEmpty(petSizeLabel));
    vars.put("petAge", nullToEmpty(petAge));
    vars.put("petActivities", activities);
    vars.put("weatherStatus", nullToEmpty(weatherStatus));
    vars.put("temperature", nullToEmpty(temperature));
    vars.put("startLat", String.valueOf(startLat));
    vars.put("startLng", String.valueOf(startLng));
    vars.put("destName", nullToEmpty(destinationName));
    vars.put("destLat", String.valueOf(destLat));
    vars.put("destLng", String.valueOf(destLng));
    vars.put("maxStops", String.valueOf(maxStops));
    vars.put("poolBlock", poolBlock.toString());
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

  private List<SelectedPlace> parseCuration(String response, List<PlaceInfo> pool, int maxStops) {
    try {
      String json = response.trim();
      int start = json.indexOf('[');
      int end = json.lastIndexOf(']');
      if (start == -1 || end == -1) {
        return curateFallback(pool, maxStops);
      }
      List<Map<String, String>> raw =
          objectMapper.readValue(
              json.substring(start, end + 1), new TypeReference<List<Map<String, String>>>() {});
      Set<String> poolIds = pool.stream().map(PlaceInfo::id).collect(Collectors.toSet());
      List<SelectedPlace> selected = new ArrayList<>();
      Set<String> seen = new java.util.HashSet<>();
      for (Map<String, String> item : raw) {
        String id = item.get("id");
        if (id == null || !poolIds.contains(id) || !seen.add(id)) {
          continue;
        }
        selected.add(new SelectedPlace(id, item.get("reason")));
        if (selected.size() >= maxStops) {
          break;
        }
      }
      if (selected.isEmpty()) {
        return curateFallback(pool, maxStops);
      }
      return selected;
    } catch (Exception e) {
      return curateFallback(pool, maxStops);
    }
  }

  private List<SelectedPlace> curateFallback(List<PlaceInfo> pool, int maxStops) {
    return pool.stream().limit(maxStops).map(p -> new SelectedPlace(p.id(), null)).toList();
  }
}
