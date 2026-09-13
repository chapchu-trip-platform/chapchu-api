package com.pettrip.post.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @param photoId 대표 사진(첫 장). 마이페이지 목록 등 단일 썸네일용으로 유지한다
 * @param photoUrl 대표 사진의 S3 키
 * @param photos 첨부된 사진 전체. 각 항목에 presigned {@code downloadUrl}(10분)이 포함된다
 * @param recommended 요청한 사용자가 이 글을 추천했는지. 추천 취소 버튼을 그리려면 필요하다
 * @param bookmarked 요청한 사용자가 이 글을 북마크했는지
 * @param authorProfilePhotoUrl 작성자 프로필 사진 presigned URL(10분). 없으면 null. 비공개 버킷이라 서버가 발급한다
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
    String authorProfilePhotoUrl,
    String photoUrl,
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
        authorProfilePhotoUrl,
        photoUrl,
        photos,
        createdAt);
  }

  /**
   * 게시글 사진 1장.
   *
   * @param photoKey S3 경로(원본 식별용)
   * @param downloadUrl presigned GET URL(10분). 비공개 버킷이라 이 URL로만 열람 가능
   */
  public record PhotoView(UUID photoId, String photoKey, String downloadUrl) {}
}
