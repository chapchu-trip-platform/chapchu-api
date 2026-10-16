package com.pettrip.main.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.main.service.MainPageService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class MainPageController {

  private final MainPageService mainPageService;

  public MainPageController(MainPageService mainPageService) {
    this.mainPageService = mainPageService;
  }

  @GetMapping
  public MainPageResponse getMainPage(@CurrentUserId UUID userId) {
    return MainPageResponse.from(mainPageService.getMainPage(userId));
  }
}
