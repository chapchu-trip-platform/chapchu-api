package com.pettrip.stamp.controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도감의 칸 하나. 미획득 지역도 내려간다.
 *
 * @param acquired false면 화면에서 회색 처리한다
 * @param imageUrl 지자체 마스코트. 아직 등록 전이면 null
 * @param firstAcquiredAt 처음 획득한 시각. 미획득이면 null
 */
public record StampResponse(
    UUID stampId,
    String regionName,
    String stampName,
    String imageUrl,
    boolean acquired,
    int stampCount,
    LocalDateTime firstAcquiredAt) {}
