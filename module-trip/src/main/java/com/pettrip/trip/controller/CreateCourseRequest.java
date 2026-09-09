package com.pettrip.trip.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 코스 생성 요청. 중간 개수는 지정하지 않는다(AI가 취향·거리·날씨로 큐레이션). 도착지는 사용자가 후보(POST /recommended-places)에서 고른 반려견
 * 장소를 통째로 담아 보낸다 → 서버가 Place로 upsert 후 고정 도착 스탑으로 사용.
 */
public record CreateCourseRequest(
    @NotNull UUID petId,
    @NotNull LocalDate travelDate,
    @NotBlank String startLocation,
    @NotNull BigDecimal startLat,
    @NotNull BigDecimal startLng,
    @NotNull @Valid Destination destination,
    Short temperature,
    Short humidity,
    String weatherStatus) {

  /** 사용자가 고른 최종 도착지(반려견 장소). recommend 응답 항목을 그대로 담는다. */
  public record Destination(
      @NotBlank String externalPlaceId,
      @NotBlank String placeName,
      String placeImageUrl,
      @NotNull BigDecimal latitude,
      @NotNull BigDecimal longitude,
      String address,
      String categoryLabel,
      String indoorOutdoorType,
      String allowedPetSize,
      Boolean leashRequired,
      Boolean carrierRequired,
      String placeCaution) {}
}
