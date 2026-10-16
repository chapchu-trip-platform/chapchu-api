package com.pettrip.user.controller;

import java.util.UUID;

/** 프로필 사진 설정. photoId를 null로 보내면 프사를 지우고 기본 이미지로 되돌린다. */
public record ProfilePhotoUpdateRequest(UUID photoId) {}
