package com.pettrip.album.controller;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 앨범 사진 1장. 실제 이미지는 프론트가 {@code GET /photos/{photoId}}로 받아간다.
 *
 * @param petId 사진 속 반려동물. 이 아이의 {@code isDie}로 앨범/추억앨범이 갈렸다
 */
public record AlbumPhotoItem(
    UUID photoId, String photoKey, UUID petId, String petName, LocalDate takenAt) {}
