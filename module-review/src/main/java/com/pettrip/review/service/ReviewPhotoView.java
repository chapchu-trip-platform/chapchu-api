package com.pettrip.review.service;

import java.time.LocalDate;
import java.util.UUID;

/** 리뷰에 붙은 사진 1장의 조회용 뷰. downloadUrl은 presigned GET URL(10분 유효). */
public record ReviewPhotoView(UUID photoId, String downloadUrl, LocalDate takenAt) {}
