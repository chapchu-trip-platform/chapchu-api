package com.pettrip.recommendation.service;

/** AI가 큐레이션한 코스 스탑 1개: 장소 id + 선택 이유(왜 이 곳). */
public record SelectedPlace(String id, String reason) {}
