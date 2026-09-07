package com.pettrip.user.service;

import java.util.UUID;

/**
 * 프로필 사진 조회용 뷰. downloadUrl은 항상 값이 있다: 사진이 있으면 presigned GET URL(10분), 없으면 기본 이미지 URL. photoId는 사진이
 * 없으면(기본 이미지) null.
 */
public record ProfilePhotoView(UUID photoId, String downloadUrl) {}
