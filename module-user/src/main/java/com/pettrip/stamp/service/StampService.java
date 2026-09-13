package com.pettrip.stamp.service;

import com.pettrip.stamp.controller.StampCollectionResponse;
import com.pettrip.stamp.controller.StampResponse;
import com.pettrip.stamp.model.UserStamp;
import com.pettrip.stamp.repository.UserStampRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StampService {

  /**
   * 방문한 장소가 속한 시·도의 스탬프를 찾는다.
   *
   * <p>{@code places.area_code}가 NULL이면(동기화 전 데이터, TourAPI가 areaCode를 안 준 경우) 결과가 비고, 스탬프는 발급되지
   * 않는다. 방문 인증 자체를 막지는 않는다.
   */
  private static final String FIND_STAMP_SQL =
      """
      SELECT s.stamp_id
      FROM places p
      JOIN regions r ON p.area_code = r.area_code
      JOIN stamps s ON s.region_id = r.region_id
      WHERE p.external_place_id = :placeId
      """;

  /** 도감은 미획득 지역도 보여준다. 그래서 regions를 기준으로 LEFT JOIN 한다. */
  private static final String COLLECTION_SQL =
      """
      SELECT s.stamp_id, r.region_name, s.stamp_name, s.image_url,
             COALESCE(us.stamp_count, 0) AS stamp_count,
             us.first_acquired_at
      FROM regions r
      JOIN stamps s ON s.region_id = r.region_id
      LEFT JOIN user_stamps us ON us.stamp_id = s.stamp_id AND us.user_id = :userId
      WHERE r.area_code IS NOT NULL
      ORDER BY r.area_code
      """;

  private static final RowMapper<StampResponse> ROW_MAPPER =
      (rs, rowNum) -> {
        Timestamp acquiredAt = rs.getTimestamp("first_acquired_at");
        LocalDateTime firstAcquiredAt = null;
        if (acquiredAt != null) {
          firstAcquiredAt = acquiredAt.toLocalDateTime();
        }
        return new StampResponse(
            rs.getObject("stamp_id", UUID.class),
            rs.getString("region_name"),
            rs.getString("stamp_name"),
            rs.getString("image_url"),
            firstAcquiredAt != null,
            rs.getInt("stamp_count"),
            firstAcquiredAt);
      };

  private final UserStampRepository userStampRepository;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public StampService(
      UserStampRepository userStampRepository, NamedParameterJdbcTemplate jdbcTemplate) {
    this.userStampRepository = userStampRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 방문 인증한 장소의 지역 스탬프를 발급한다. 이미 가진 지역이면 횟수만 올린다.
   *
   * <p>지역을 알 수 없는 장소는 조용히 넘어간다. 스탬프 때문에 방문 인증이 실패하면 안 된다.
   */
  @Transactional
  public void grantForPlace(UUID userId, String externalPlaceId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("placeId", externalPlaceId);
    List<UUID> stampIds = jdbcTemplate.queryForList(FIND_STAMP_SQL, params, UUID.class);
    if (stampIds.isEmpty()) {
      return;
    }
    UUID stampId = stampIds.get(0);

    Optional<UserStamp> owned = userStampRepository.findByUserIdAndStampId(userId, stampId);
    if (owned.isPresent()) {
      UserStamp userStamp = owned.get();
      userStamp.increaseCount();
      userStampRepository.save(userStamp);
      return;
    }
    userStampRepository.save(new UserStamp(userId, stampId, LocalDateTime.now()));
  }

  @Transactional(readOnly = true)
  public StampCollectionResponse listMyStamps(UUID userId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    List<StampResponse> stamps = jdbcTemplate.query(COLLECTION_SQL, params, ROW_MAPPER);
    int acquired = (int) stamps.stream().filter(StampResponse::acquired).count();
    return new StampCollectionResponse(acquired, stamps.size(), stamps);
  }
}
