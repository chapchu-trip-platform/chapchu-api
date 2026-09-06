package com.pettrip.place.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

@Component
public class TourApiClient {

  private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

  private final RestClient restClient;
  private final String serviceKey;

  public TourApiClient(
      RestClient.Builder builder,
      @Value("${app.tour-api.base-url}") String baseUrl,
      @Value("${app.tour-api.key}") String serviceKey) {
    // 공공데이터포털 serviceKey는 이미 퍼센트 인코딩된 형태(%2B 등)로 발급된다.
    // 기본 인코딩 모드는 이 '%'를 '%25'로 이중 인코딩해 인증을 깨뜨린다.
    DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(baseUrl);
    uriFactory.setEncodingMode(EncodingMode.VALUES_ONLY);
    this.restClient = builder.uriBuilderFactory(uriFactory).build();
    this.serviceKey = serviceKey.strip();
  }

  public List<NearbyItem> fetchNearby(BigDecimal lat, BigDecimal lng, int radiusMeters) {
    Map<String, Object> body =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/locationBasedList2")
                        .query("serviceKey=" + serviceKey)
                        .queryParam("numOfRows", 20)
                        .queryParam("pageNo", 1)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "chapchu")
                        .queryParam("_type", "json")
                        .queryParam("mapX", "{mapX}")
                        .queryParam("mapY", "{mapY}")
                        .queryParam("radius", "{radius}")
                        .queryParam("arrange", "E")
                        .build(
                            Map.of(
                                "mapX", lng,
                                "mapY", lat,
                                "radius", radiusMeters)))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    return extractItems(body).stream().map(this::toNearbyItem).toList();
  }

  public PetDetailItem fetchPetDetail(String contentId) {
    Map<String, Object> body =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/detailPetTour2")
                        .query("serviceKey=" + serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "chapchu")
                        .queryParam("_type", "json")
                        .queryParam("contentId", "{contentId}")
                        .build(Map.of("contentId", contentId)))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    List<Map<String, Object>> items = extractItems(body);
    if (items.isEmpty()) return null;
    return toPetDetailItem(items.get(0));
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> extractItems(Map<String, Object> body) {
    if (body == null) {
      log.warn("TourAPI 응답 본문이 null 입니다");
      return Collections.emptyList();
    }

    String resultCode = resultCode(body);
    if (resultCode != null && !"0000".equals(resultCode)) {
      log.warn("TourAPI 인증/처리 실패 resultCode={} body={}", resultCode, body);
      return Collections.emptyList();
    }

    try {
      if (!(body.get("response") instanceof Map<?, ?> response)) {
        return Collections.emptyList();
      }
      if (!(((Map<String, Object>) response).get("body") instanceof Map<?, ?> responseBody)) {
        return Collections.emptyList();
      }
      if (!(((Map<String, Object>) responseBody).get("items") instanceof Map<?, ?> items)) {
        return Collections.emptyList();
      }
      Object item = ((Map<String, Object>) items).get("item");
      if (item instanceof List<?> list) {
        return (List<Map<String, Object>>) list;
      }
      if (item instanceof Map<?, ?> single) {
        return List.of((Map<String, Object>) single);
      }
    } catch (Exception e) {
      log.warn("TourAPI 응답 파싱 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
    }
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  private String resultCode(Map<String, Object> body) {
    try {
      Map<String, Object> response = (Map<String, Object>) body.get("response");
      Map<String, Object> header = (Map<String, Object>) response.get("header");
      Object code = header.get("resultCode");
      if (code == null) return null;
      return code.toString();
    } catch (Exception e) {
      log.warn("TourAPI resultCode 파싱 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  private NearbyItem toNearbyItem(Map<String, Object> map) {
    return new NearbyItem(
        str(map, "contentid"),
        str(map, "contenttypeid"),
        str(map, "title"),
        str(map, "firstimage"),
        str(map, "addr1"),
        decimal(map, "mapy"),
        decimal(map, "mapx"),
        decimal(map, "dist"));
  }

  private PetDetailItem toPetDetailItem(Map<String, Object> map) {
    return new PetDetailItem(
        str(map, "acmpyTypeCd"),
        str(map, "acmpyPsblCpam"),
        str(map, "acmpyNeedMtr"),
        str(map, "etcAcmpyInfo"),
        str(map, "relaAcdntRiskMtr"));
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
      String contentTypeId,
      String title,
      String firstImage,
      String addr1,
      BigDecimal lat,
      BigDecimal lng,
      BigDecimal dist) {}

  public record PetDetailItem(
      String acmpyTypeCd,
      String acmpyPsblCpam,
      String acmpyNeedMtr,
      String etcAcmpyInfo,
      String relaAcdntRiskMtr) {}
}
