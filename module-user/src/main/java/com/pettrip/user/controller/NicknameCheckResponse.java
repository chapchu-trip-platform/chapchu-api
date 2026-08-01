package com.pettrip.user.controller;

public record NicknameCheckResponse(boolean available) {
  public static NicknameCheckResponse of(boolean available) {
    return new NicknameCheckResponse(available);
  }
}
