package com.pettrip.place.service;

import com.pettrip.common.service.ExternalApiException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TourApiClient {

  private final RestClient restClient;
  private final String serviceKey;

  public TourApiClient(
      RestClient.Builder builder,
      @Value("${app.tour-api.base-url}") String baseUrl,
      @Value("${app.tour-api.key}") String serviceKey) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.serviceKey = serviceKey;
  }

  public List<NearbyItem> fetchNearby(BigDecimal lat, BigDecimal lng, int radiusMeters) {
    requireServiceKey();

    Map<String, Object> body =
        call(
            () ->
                restClient
                    .get()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path("/locationBasedList2")
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("numOfRows", 20)
                                .queryParam("pageNo", 1)
                                .queryParam("MobileOS", "ETC")
                                .queryParam("MobileApp", "chapchu")
                                .queryParam("_type", "json")
                                .queryParam("mapX", lng)
                                .queryParam("mapY", lat)
                                .queryParam("radius", radiusMeters)
                                .queryParam("contentTypeId", 12)
                                .queryParam("petTour", "Y")
                                .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));

    return extractItems(body).stream().map(this::toNearbyItem).toList();
  }

  public PetDetailItem fetchPetDetail(String contentId) {
    requireServiceKey();

    Map<String, Object> body =
        call(
            () ->
                restClient
                    .get()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path("/detailPetTour2")
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("MobileOS", "ETC")
                                .queryParam("MobileApp", "chapchu")
                                .queryParam("_type", "json")
                                .queryParam("contentId", contentId)
                                .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));

    List<Map<String, Object>> items = extractItems(body);
    if (items.isEmpty()) return null;
    return toPetDetailItem(items.get(0));
  }

  /**
   * TOUR_API_KEY가 주입되지 않으면 TourAPI가 인증 오류 본문을 돌려주는데, 그게 파싱 단계에서 엉뚱한 예외로 터져 원인을 알기 어렵다. 호출 전에 먼저 걸러
   * 무엇이 빠졌는지 분명히 알린다.
   */
  private void requireServiceKey() {
    if (serviceKey == null || serviceKey.isBlank()) {
      throw new ExternalApiException("TourAPI 서비스 키가 설정되지 않았습니다. (TOUR_API_KEY)", null);
    }
  }

  /** 외부 호출 실패를 {@link ExternalApiException}으로 감싼다. 그대로 두면 500 raw 응답이 나간다. */
  private <T> T call(Supplier<T> request) {
    try {
      return request.get();
    } catch (RestClientException e) {
      throw new ExternalApiException("TourAPI 호출에 실패했습니다.", e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> extractItems(Map<String, Object> body) {
    if (body == null) return Collections.emptyList();
    try {
      Map<String, Object> response = (Map<String, Object>) body.get("response");
      Map<String, Object> responseBody = (Map<String, Object>) response.get("body");
      Map<String, Object> items = (Map<String, Object>) responseBody.get("items");
      Object item = items.get("item");
      if (item instanceof List<?> list) {
        return (List<Map<String, Object>>) list;
      }
      if (item instanceof Map<?, ?> single) {
        return List.of((Map<String, Object>) single);
      }
    } catch (Exception ignored) {
    }
    return Collections.emptyList();
  }

  private NearbyItem toNearbyItem(Map<String, Object> map) {
    return new NearbyItem(
        str(map, "contentid"),
        str(map, "title"),
        str(map, "firstimage"),
        str(map, "addr1"),
        decimal(map, "mapy"),
        decimal(map, "mapx"));
  }

  private PetDetailItem toPetDetailItem(Map<String, Object> map) {
    return new PetDetailItem(
        str(map, "acmpyTypeCd"),
        str(map, "acmpyPsblCpam"),
        str(map, "acmpyNeedMtr"),
        str(map, "etcAcmpyInfo"),
        str(map, "relaPrkge"));
  }

  private String str(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : v.toString();
  }

  private BigDecimal decimal(Map<String, Object> map, String key) {
    String v = str(map, key);
    if (v == null || v.isBlank()) return null;
    try {
      return new BigDecimal(v);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public record NearbyItem(
      String contentId,
      String title,
      String firstImage,
      String addr1,
      BigDecimal lat,
      BigDecimal lng) {}

  public record PetDetailItem(
      String acmpyTypeCd,
      String acmpyPsblCpam,
      String acmpyNeedMtr,
      String etcAcmpyInfo,
      String relaPrkge) {}
}
