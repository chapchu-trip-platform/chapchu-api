package com.pettrip.trip.service;

import com.pettrip.photo.service.PhotoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 앨범 = 여행 사진(course_place로 코스에 연결된 내 사진)을 코스 단위로 묶어 준다.
 *
 * <p>리뷰에 연결된 사진은 공개(isPublic=true), 아니면 개인. 비공개 버킷이라 presigned downloadUrl(10분)로 내려준다. 사진→코스→펫
 * 경로(travel_courses.pet_id)로 펫별 조회도 지원한다.
 */
@Service
public class AlbumService {

  private static final String BASE_SQL =
      """
      SELECT tc.course_id, tc.travel_date, tc.pet_id,
             p.photo_id, p.photo_url AS photo_key, p.taken_at, p.created_at,
             cp.external_place_id,
             EXISTS(SELECT 1 FROM review_photos rp WHERE rp.photo_id = p.photo_id) AS is_public
      FROM photos p
      JOIN course_places cp ON cp.course_place_id = p.course_place_id
      JOIN travel_courses tc ON tc.course_id = cp.course_id
      WHERE p.user_id = :userId
      """;

  private static final String ORDER_BY =
      " ORDER BY tc.travel_date DESC NULLS LAST, tc.course_id, p.created_at";

  private static final RowMapper<AlbumRow> ROW_MAPPER =
      (rs, n) ->
          new AlbumRow(
              rs.getObject("course_id", UUID.class),
              rs.getObject("travel_date", LocalDate.class),
              rs.getObject("pet_id", UUID.class),
              rs.getObject("photo_id", UUID.class),
              rs.getString("photo_key"),
              rs.getObject("taken_at", LocalDate.class),
              rs.getTimestamp("created_at").toLocalDateTime(),
              rs.getString("external_place_id"),
              rs.getBoolean("is_public"));

  private final PhotoService photoService;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public AlbumService(PhotoService photoService, NamedParameterJdbcTemplate jdbcTemplate) {
    this.photoService = photoService;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** 내 앨범: 코스에 연결된 내 사진 전부를 코스 단위로 묶는다. */
  public List<CourseAlbum> getMyAlbum(UUID userId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    return group(jdbcTemplate.query(BASE_SQL + ORDER_BY, params, ROW_MAPPER));
  }

  /** 펫별 앨범: 그 펫과 함께한 코스의 내 사진을 코스 단위로 묶는다(펫 사망 후 추모용). */
  public List<CourseAlbum> getPetAlbum(UUID userId, UUID petId) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("petId", petId);
    return group(
        jdbcTemplate.query(BASE_SQL + " AND tc.pet_id = :petId" + ORDER_BY, params, ROW_MAPPER));
  }

  private List<CourseAlbum> group(List<AlbumRow> rows) {
    Map<UUID, CourseAlbumAcc> byCourse = new LinkedHashMap<>();
    for (AlbumRow row : rows) {
      CourseAlbumAcc acc =
          byCourse.computeIfAbsent(
              row.courseId(),
              key -> new CourseAlbumAcc(row.travelDate(), row.petId(), new ArrayList<>()));
      acc.photos()
          .add(
              new AlbumPhoto(
                  row.photoId(),
                  photoService.issueDownloadUrl(row.photoKey()).toString(),
                  row.takenAt(),
                  row.createdAt(),
                  row.externalPlaceId(),
                  row.isPublic()));
    }
    List<CourseAlbum> result = new ArrayList<>();
    byCourse.forEach(
        (courseId, acc) ->
            result.add(new CourseAlbum(courseId, acc.travelDate(), acc.petId(), acc.photos())));
    return result;
  }

  record AlbumRow(
      UUID courseId,
      LocalDate travelDate,
      UUID petId,
      UUID photoId,
      String photoKey,
      LocalDate takenAt,
      LocalDateTime createdAt,
      String externalPlaceId,
      boolean isPublic) {}

  private record CourseAlbumAcc(LocalDate travelDate, UUID petId, List<AlbumPhoto> photos) {}

  /** 코스 1개의 앨범: 코스 정보 + 그 코스에서 찍은 사진들. */
  public record CourseAlbum(
      UUID courseId, LocalDate travelDate, UUID petId, List<AlbumPhoto> photos) {}

  /**
   * 앨범 사진 1장. isPublic = 리뷰에 등록돼 공개된 사진인지.
   *
   * <p>takenAt은 DATE(일 단위)라 같은 날 사진끼리 순서를 가릴 수 없다. createdAt(업로드 시각, 초 단위)을 함께 내려 FE가 시간순 정밀 정렬을 할
   * 수 있게 한다. 서버 정렬 기준도 동일하게 created_at이다.
   */
  public record AlbumPhoto(
      UUID photoId,
      String downloadUrl,
      LocalDate takenAt,
      LocalDateTime createdAt,
      String externalPlaceId,
      boolean isPublic) {}
}
