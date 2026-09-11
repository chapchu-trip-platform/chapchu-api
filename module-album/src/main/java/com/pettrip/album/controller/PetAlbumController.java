package com.pettrip.album.controller;

import com.pettrip.album.service.AlbumService;
import com.pettrip.common.service.CurrentUserId;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 앨범은 유저당 하나다. 사진 속 반려동물의 {@code isDie}로 앨범과 추억앨범이 갈린다.
 *
 * <p>{@code GET /users/me/album}(리뷰 사진 모아보기)과는 다른 기능이다. 그쪽은 review 도메인 소관이다.
 */
@RestController
@RequestMapping("/albums")
public class PetAlbumController {

  private final AlbumService albumService;

  public PetAlbumController(AlbumService albumService) {
    this.albumService = albumService;
  }

  @GetMapping
  public AlbumResponse getMyAlbum(@CurrentUserId UUID userId) {
    return albumService.getMyAlbum(userId);
  }

  @PatchMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rename(@CurrentUserId UUID userId, @RequestBody @Valid AlbumRenameRequest request) {
    albumService.rename(userId, request.albumName());
  }

  @PostMapping("/photos")
  @ResponseStatus(HttpStatus.CREATED)
  public void addPhotos(
      @CurrentUserId UUID userId, @RequestBody @Valid AlbumPhotoAddRequest request) {
    albumService.addPhotos(userId, request.petId(), request.photoIds());
  }

  @DeleteMapping("/photos/{photoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removePhoto(@CurrentUserId UUID userId, @PathVariable UUID photoId) {
    albumService.removePhoto(userId, photoId);
  }
}
