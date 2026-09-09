package com.pettrip.wishlist.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 찜한 장소 하나. 목록 화면을 그리는 데 필요한 장소 정보를 함께 담는다.
 *
 * <p>{@code placeId}만 내려주면 프론트가 항목마다 {@code GET /places/{id}}를 따로 불러야 한다.
 *
 * @param createdAt 찜한 시각. 장소 등록 시각이 아니다
 */
public record WishlistResponse(
    String placeId,
    String placeName,
    String placeImageUrl,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Short rating,
    Integer reviewNum,
    LocalDateTime createdAt) {}
