package com.pettrip.place.controller;

import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PlaceResponse registerPlace(@RequestBody @Valid PlaceRegisterRequest request) {
    Place place =
        placeService.upsertPlace(
            request.externalPlaceId(),
            request.themeId(),
            request.placeName(),
            request.placeImageUrl(),
            request.address(),
            request.latitude(),
            request.longitude(),
            request.businessHours(),
            request.phoneNumber(),
            request.rating());
    return PlaceResponse.from(place);
  }

  @PutMapping("/{externalPlaceId}/policy")
  @ResponseStatus(HttpStatus.OK)
  public PlacePetPolicyResponse upsertPetPolicy(
      @PathVariable String externalPlaceId, @RequestBody PlacePetPolicyRequest request) {
    PlacePetPolicy policy =
        placeService.upsertPetPolicy(
            externalPlaceId,
            request.allowedPetSize(),
            request.leashRequired(),
            request.carrierRequired(),
            request.indoorOutdoorType(),
            request.parking(),
            request.placeCaution());
    return PlacePetPolicyResponse.from(policy);
  }
}
