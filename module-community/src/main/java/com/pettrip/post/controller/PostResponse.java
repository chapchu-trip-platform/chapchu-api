package com.pettrip.post.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @param photoId 대표 사진(첫 장). 마이페이지 목록 등 단일 썸네일용으로 유지한다
 * @param photoUrl 대표 사진의 S3 키
 * @param photoCount 첨부된 사진 수. {@code photos.size()}와 같지만 목록 응답과 필드를 맞춘다
 * @param photos 첨부된 사진 전체. downloadUrl은 FE가 {@code GET /photos/{photoId}}로 발급받는다
 * @param recommended 요청한 사용자가 이 글을 추천했는지. 추천 취소 버튼을 그리려면 필요하다
 * @param bookmarked 요청한 사용자가 이 글을 북마크했는지
 */
public record PostResponse(
    UUID id,
    UUID petId,
    UUID photoId,
    UUID courseId,
    String title,
    String content,
    int viewCount,
    int recommendationCount,
    int commentCount,
    boolean recommended,
    boolean bookmarked,
    String nickname,
    String photoUrl,
    int photoCount,
    List<PhotoView> photos,
    LocalDateTime createdAt) {

  /** 사진 목록을 채운 복사본을 만든다. 목록/상세 조회는 글을 먼저 읽고 사진을 배치로 붙인다. */
  public PostResponse withPhotos(List<PhotoView> photos) {
    return new PostResponse(
        id,
        petId,
        photoId,
        courseId,
        title,
        content,
        viewCount,
        recommendationCount,
        commentCount,
        recommended,
        bookmarked,
        nickname,
        photoUrl,
        photos.size(),
        photos,
        createdAt);
  }

  /** 게시글 사진 1장. */
  public record PhotoView(UUID photoId, String photoKey) {}
}
