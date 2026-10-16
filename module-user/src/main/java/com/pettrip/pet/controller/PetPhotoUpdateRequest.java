package com.pettrip.pet.controller;

import java.util.UUID;

/**
 * 반려동물 프로필 사진 설정.
 *
 * @param photoId {@code POST /photos}로 등록한 사진 id. null로 보내면 사진을 뗀다
 */
public record PetPhotoUpdateRequest(UUID photoId) {}
