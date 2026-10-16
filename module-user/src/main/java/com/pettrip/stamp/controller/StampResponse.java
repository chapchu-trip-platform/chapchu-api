package com.pettrip.stamp.controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도감의 칸 하나. 미획득 지역도 내려간다.
 *
 * @param stampName 시·도 이름 (서울·부산·대구·인천·광주·대전·울산·세종·경기·강원·충북·충남·전북·전남·경북·경남·제주)
 * @param acquired false면 화면에서 회색 처리한다
 * @param firstAcquiredAt 처음 획득한 시각. 미획득이면 null
 */
public record StampResponse(
    UUID stampId,
    String stampName,
    boolean acquired,
    int stampCount,
    LocalDateTime firstAcquiredAt) {}
