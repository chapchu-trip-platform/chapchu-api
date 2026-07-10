package com.pettrip.main.controller;

import com.pettrip.main.service.MainPageSummary;
import java.util.List;

public record MainPageResponse(String nickname, List<String> petNames) {

  public static MainPageResponse from(MainPageSummary summary) {
    return new MainPageResponse(summary.nickname(), summary.petNames());
  }
}
