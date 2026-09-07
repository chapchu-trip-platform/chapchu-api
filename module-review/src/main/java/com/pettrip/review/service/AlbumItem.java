package com.pettrip.review.service;

import java.time.LocalDate;
import java.util.UUID;

/** 앨범 항목: 내 리뷰에 붙은 사진 1장 + 어느 리뷰·장소인지. downloadUrl은 presigned GET(10분). */
public record AlbumItem(
    UUID photoId, String downloadUrl, LocalDate takenAt, UUID reviewId, String placeId) {}
