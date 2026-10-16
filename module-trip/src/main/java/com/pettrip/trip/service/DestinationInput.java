package com.pettrip.trip.service;

import java.math.BigDecimal;

/** 코스 도착지 입력. 사용자가 후보(POST /recommended-places)에서 고른 반려견 장소 정보. */
public record DestinationInput(
    String externalPlaceId,
    String placeName,
    String placeImageUrl,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String allowedPetSize,
    Boolean leashRequired,
    Boolean carrierRequired,
    String indoorOutdoorType,
    String placeCaution) {}
