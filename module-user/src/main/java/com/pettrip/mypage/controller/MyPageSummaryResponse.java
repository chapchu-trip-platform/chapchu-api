package com.pettrip.mypage.controller;

import com.pettrip.mypage.service.MyPageSummary;

public record MyPageSummaryResponse(String nickname, String email, long petCount) {

  public static MyPageSummaryResponse from(MyPageSummary summary) {
    return new MyPageSummaryResponse(summary.nickname(), summary.email(), summary.petCount());
  }
}
