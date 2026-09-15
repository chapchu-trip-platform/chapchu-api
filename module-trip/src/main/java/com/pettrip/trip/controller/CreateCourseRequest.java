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

  /**
   * 사용자가 임의로 고른 최종 도착지(고정 끝점). 우리 펫장소 DB와 무관하며 places/RAG에 축적하지 않는다(decisions/045).
   *
   * <p>서버는 {@code placeName·latitude·longitude}만 사용한다. {@code externalPlaceId}·정책 필드(allowedPetSize
   * 등)는 보내도 무시하며 저장하지 않는다.
   */
  public record Destination(
      String externalPlaceId,
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
