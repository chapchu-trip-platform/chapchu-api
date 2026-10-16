package com.pettrip.user.controller;

import com.pettrip.user.model.Region;
import com.pettrip.user.model.Theme;
import com.pettrip.user.model.TransportMethod;
import java.util.List;
import java.util.UUID;

public record PreferenceOptionsResponse(
    List<Option> regions, List<Option> themes, List<Option> transportMethods) {

  public record Option(UUID id, String name) {}

  public static PreferenceOptionsResponse of(
      List<Region> regions, List<Theme> themes, List<TransportMethod> transportMethods) {
    return new PreferenceOptionsResponse(
        regions.stream().map(r -> new Option(r.getId(), r.getRegionName())).toList(),
        themes.stream().map(t -> new Option(t.getId(), t.getThemeName())).toList(),
        transportMethods.stream()
            .map(t -> new Option(t.getId(), t.getTransportMethodName()))
            .toList());
  }
}
