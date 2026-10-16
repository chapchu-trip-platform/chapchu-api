package com.pettrip.pet.service;

import java.util.UUID;

/**
 * 반려동물 사진 조회용 뷰. 프로필 사진과 배경화면에 함께 쓴다.
 *
 * <p>사진이 없으면 이 객체 자체가 {@code null}이다. 기본 이미지는 프론트가 처리한다.
 *
 * @param downloadUrl presigned GET URL(10분). 비공개 버킷이라 이 URL로만 열람할 수 있다
 */
public record PetPhotoView(UUID photoId, String downloadUrl) {}
