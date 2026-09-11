package com.pettrip.album.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * @param petId 사진 속 반려동물. 이 값이 있어야 앨범/추억앨범을 가를 수 있어 필수다
 * @param photoIds 담을 사진. {@code POST /photos}로 먼저 등록한 뒤 그 id를 보낸다
 */
public record AlbumPhotoAddRequest(
    @NotNull UUID petId, @NotEmpty @Size(max = 30) List<UUID> photoIds) {}
