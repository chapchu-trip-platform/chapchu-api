package com.pettrip.review.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.review.service.ReviewService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 앨범 = 내가 쓴 리뷰들의 사진 모아보기. review_photos에서 파생한다. */
@RestController
@RequestMapping("/users/me/album")
public class AlbumController {

  private final ReviewService reviewService;

  public AlbumController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping
  public List<AlbumPhotoResponse> myAlbum(@CurrentUserId UUID userId) {
    return reviewService.getMyAlbum(userId).stream().map(AlbumPhotoResponse::of).toList();
  }
}
