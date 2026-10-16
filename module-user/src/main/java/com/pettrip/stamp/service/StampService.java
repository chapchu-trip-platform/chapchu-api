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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StampService {

  /**
   * 방문한 장소가 속한 도의 스탬프를 찾는다.
   *
   * <p>스탬프는 17개 광역자치단체(시·도) 단위다.
   *
   * <p>{@code places.area_code}가 NULL이면(동기화 전 데이터, TourAPI가 areaCode를 안 준 경우) 결과가 비고, 스탬프는 발급되지
   * 않는다. 방문 인증 자체를 막지는 않는다.
   */
  private static final String FIND_STAMP_SQL =
      """
      SELECT sac.stamp_id
      FROM places p
      JOIN stamp_area_codes sac ON sac.area_code = p.area_code
      WHERE p.external_place_id = :placeId
      """;

  /** areaCode로 바로 시·도 스탬프를 찾는다. 코스 완료 시 도착지 areaCode로 발급할 때 쓴다. */
  private static final String FIND_STAMP_BY_AREA_SQL =
      "SELECT stamp_id FROM stamp_area_codes WHERE area_code = :areaCode";

  /** 도감은 미획득 지역도 보여준다. 그래서 stamps를 기준으로 LEFT JOIN 한다. */
  private static final String COLLECTION_SQL =
      """
      SELECT s.stamp_id, s.stamp_name,
             COALESCE(us.stamp_count, 0) AS stamp_count,
             us.first_acquired_at
      FROM stamps s
      LEFT JOIN user_stamps us ON us.stamp_id = s.stamp_id AND us.user_id = :userId
      ORDER BY s.stamp_name
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
            rs.getString("stamp_name"),
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
    grantStamp(userId, stampIds.get(0));
  }

  /**
   * 완료한 코스의 도착지 시·도 스탬프를 발급한다. 이미 가진 지역이면 횟수만 올린다.
   *
   * <p>{@code areaCode}가 null이거나 매핑이 없으면 조용히 넘어간다. 스탬프 발급이 코스 완료를 막으면 안 되므로 별도 트랜잭션(REQUIRES_NEW)에서
   * 처리한다 — 여기서 실패해도 호출부의 완료 트랜잭션은 커밋된다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void grantForArea(UUID userId, Short areaCode) {
    if (areaCode == null) {
      return;
    }
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("areaCode", areaCode);
    List<UUID> stampIds = jdbcTemplate.queryForList(FIND_STAMP_BY_AREA_SQL, params, UUID.class);
    if (stampIds.isEmpty()) {
      return;
    }
    grantStamp(userId, stampIds.get(0));
  }

  private void grantStamp(UUID userId, UUID stampId) {
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
