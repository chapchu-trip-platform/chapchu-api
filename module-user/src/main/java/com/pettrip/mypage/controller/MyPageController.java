package com.pettrip.mypage.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.mypage.service.MyPageService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/mypage")
public class MyPageController {

  private final MyPageService myPageService;

  public MyPageController(MyPageService myPageService) {
    this.myPageService = myPageService;
  }

  @GetMapping
  public MyPageSummaryResponse getSummary(@CurrentUserId UUID userId) {
    return MyPageSummaryResponse.from(myPageService.getSummary(userId));
  }
}
