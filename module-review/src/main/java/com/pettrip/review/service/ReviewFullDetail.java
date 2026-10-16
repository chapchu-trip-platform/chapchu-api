package com.pettrip.review.service;

import com.pettrip.review.model.Review;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 리뷰 한 건의 완전한 정보. 작성자·반려동물·장소 이름까지 채워 준다.
 *
 * <p>{@link ReviewDetail}은 리뷰 + 사진만 담아 목록 응답에 쓴다. 카드처럼 사람이 읽는 화면은 {@code petId} 같은 UUID가 아니라 이름이
 * 필요해 이 타입을 쓴다.
 *
 * <p>{@code course}는 코스 스탑에 달린 리뷰에만 있다. 장소에 바로 쓴 리뷰는 null이다.
 */
public record ReviewFullDetail(
    Review review,
    Author author,
    PetInfo pet,
    PlaceInfo place,
    CourseInfo course,
    List<ReviewPhotoView> photos) {

  /** 작성자. 탈퇴하면 리뷰의 user_id가 NULL이 되므로 통째로 null일 수 있다. */
  public record Author(UUID userId, String nickname, PhotoRef profilePhoto) {}

  public record PetInfo(UUID petId, String petName, String breedName, PhotoRef profilePhoto) {}

  /** places에 없는 장소면 placeName·address가 null이고 externalPlaceId만 남는다. */
  public record PlaceInfo(String externalPlaceId, String placeName, String address) {}

  public record CourseInfo(UUID courseId, LocalDate travelDate) {}

  /** 프로필 사진 참조. downloadUrl은 presigned GET URL(10분 유효). */
  public record PhotoRef(UUID photoId, String downloadUrl) {}
}
