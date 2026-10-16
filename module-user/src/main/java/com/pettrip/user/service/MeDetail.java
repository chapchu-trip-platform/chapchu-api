package com.pettrip.user.service;

import com.pettrip.user.model.User;

/** 내 정보 + 프로필 사진 뷰(기본 이미지 포함). 컨트롤러가 응답 DTO로 매핑한다. */
public record MeDetail(User user, ProfilePhotoView profilePhoto) {}
