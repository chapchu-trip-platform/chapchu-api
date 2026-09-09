package com.pettrip.place.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.place.model.Place;
import com.pettrip.place.service.PlaceService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장소 조회는 공개 엔드포인트다(SecurityConfig.PUBLIC_GET_PATHS). 그래서 유저 id를 {@code Optional}로 받는다. {@code
 * UUID}로 받으면 토큰 없는 요청에서 예외가 나 401로 떨어진다.
 */
@RestController
@RequestMapping("/places")
public class PlaceController {

  private final PlaceService placeService;

  public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
  }

  @GetMapping("/{externalPlaceId}")
  public PlaceResponse getPlace(
      @CurrentUserId Optional<UUID> userId, @PathVariable String externalPlaceId) {
    Place place = placeService.getPlace(externalPlaceId);
    Set<String> wishlisted = placeService.findWishlistedPlaceIds(userId, List.of(externalPlaceId));
    return PlaceResponse.from(place, wishlisted.contains(externalPlaceId));
  }

  @GetMapping("/nearby")
  public List<PlaceResponse> searchNearby(
      @CurrentUserId Optional<UUID> userId,
      @RequestParam BigDecimal lat,
      @RequestParam BigDecimal lng,
      @RequestParam(defaultValue = "5000") int radiusMeters) {
    List<Place> places = placeService.searchNearby(lat, lng, radiusMeters);
    Set<String> wishlisted =
        placeService.findWishlistedPlaceIds(
            userId, places.stream().map(Place::getExternalPlaceId).toList());
    return places.stream()
        .map(place -> PlaceResponse.from(place, wishlisted.contains(place.getExternalPlaceId())))
        .toList();
  }
}
