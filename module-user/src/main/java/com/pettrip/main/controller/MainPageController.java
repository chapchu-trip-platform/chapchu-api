package com.pettrip.main.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.main.service.MainPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
public class MainPageController {

  private final MainPageService mainPageService;

  public MainPageController(MainPageService mainPageService) {
    this.mainPageService = mainPageService;
  }

  @GetMapping
  public MainPageResponse getMainPage() {
    return MainPageResponse.from(mainPageService.getMainPage(TempAuthContext.TEMP_USER_ID));
  }
}
