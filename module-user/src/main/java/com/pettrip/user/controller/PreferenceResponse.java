package com.pettrip.user.controller;

import com.pettrip.user.model.User;
import java.util.List;
import java.util.UUID;

public record PreferenceResponse(
    List<CodeItem> regions, List<CodeItem> themes, List<CodeItem> transportMethods) {

  public record CodeItem(UUID id, String name) {}

  public static PreferenceResponse from(User user) {
    return new PreferenceResponse(
        user.getPreferredRegions().stream()
            .map(region -> new CodeItem(region.getId(), region.getRegionName()))
            .toList(),
        user.getPreferredThemes().stream()
            .map(theme -> new CodeItem(theme.getId(), theme.getThemeName()))
            .toList(),
        user.getPreferredTransportMethods().stream()
            .map(
                transportMethod ->
                    new CodeItem(transportMethod.getId(), transportMethod.getTransportMethodName()))
            .toList());
  }
}
