package com.pettrip.place.controller;

import com.pettrip.place.model.Place;
import com.pettrip.place.service.PlaceService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/places")
public class PlaceController {

  private final PlaceService placeService;

  public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
  }

  @GetMapping("/{externalPlaceId}")
  public PlaceResponse getPlace(@PathVariable String externalPlaceId) {
    Place place = placeService.getPlace(externalPlaceId);
    return PlaceResponse.from(place);
  }

  @GetMapping("/nearby")
  public List<PlaceResponse> searchNearby(
      @RequestParam BigDecimal lat,
      @RequestParam BigDecimal lng,
      @RequestParam(defaultValue = "5000") int radiusMeters) {
    return placeService.searchNearby(lat, lng, radiusMeters).stream()
        .map(PlaceResponse::from)
        .toList();
  }
}
