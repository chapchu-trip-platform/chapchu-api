package com.pettrip.place.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
    Map<String, Object> body =
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
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "chapchu")
                        .queryParam("_type", "json")
                        .queryParam("contentId", contentId)
                        .build())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    List<Map<String, Object>> items = extractItems(body);
    if (items.isEmpty()) return null;
    return toPetDetailItem(items.get(0));
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
