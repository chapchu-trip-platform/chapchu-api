package com.pettrip.trip.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.trip.service.AlbumService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 앨범 = 여행 사진을 코스 단위로 묶어 보여준다. 리뷰 등록 사진은 공개, 나머지는 개인(isPublic). */
@RestController
public class AlbumController {

  private final AlbumService albumService;

  public AlbumController(AlbumService albumService) {
    this.albumService = albumService;
  }

  @GetMapping("/users/me/album")
  public List<CourseAlbumResponse> myAlbum(@CurrentUserId UUID userId) {
    return albumService.getMyAlbum(userId).stream().map(CourseAlbumResponse::from).toList();
  }

  @GetMapping("/users/me/pets/{petId}/album")
  public List<CourseAlbumResponse> petAlbum(@CurrentUserId UUID userId, @PathVariable UUID petId) {
    return albumService.getPetAlbum(userId, petId).stream().map(CourseAlbumResponse::from).toList();
  }
}
