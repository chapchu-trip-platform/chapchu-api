package com.pettrip.stamp.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.stamp.service.StampService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 지역 스탬프 도감. 미획득 지역도 함께 내려가므로 화면에서 회색 처리하면 된다. */
@RestController
@RequestMapping("/users/me/stamps")
public class StampController {

  private final StampService stampService;

  public StampController(StampService stampService) {
    this.stampService = stampService;
  }

  @GetMapping
  public StampCollectionResponse myStamps(@CurrentUserId UUID userId) {
    return stampService.listMyStamps(userId);
  }
}
