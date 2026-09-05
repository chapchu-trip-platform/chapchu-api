package com.pettrip.recommendation.service;

import java.math.BigDecimal;

public record PlaceInfo(
    String id,
    String name,
    String address,
    BigDecimal lat,
    BigDecimal lng,
    String categoryLabel,
    String indoorOutdoor,
    PlaceGroup group) {

  public enum PlaceGroup {
    START,
    MIDDLE,
    END
  }
}
