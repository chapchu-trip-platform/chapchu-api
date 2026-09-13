package com.pettrip.stamp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.stamp.controller.StampCollectionResponse;
import com.pettrip.stamp.controller.StampResponse;
import com.pettrip.stamp.model.UserStamp;
import com.pettrip.stamp.repository.UserStampRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class StampServiceTest {

  @Mock private UserStampRepository userStampRepository;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private StampService stampService;

  @BeforeEach
  void setUp() {
    stampService = new StampService(userStampRepository, jdbcTemplate);
  }

  @Test
  void grantForPlace는_지역을_알_수_없는_장소면_아무것도_하지_않는다() {
    UUID userId = UUID.randomUUID();
    when(jdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class), any()))
        .thenReturn(List.of());

    stampService.grantForPlace(userId, "126508");

    verify(userStampRepository, never()).save(any());
  }

  @Test
  void grantForPlace는_처음_방문이면_스탬프를_발급한다() {
    UUID userId = UUID.randomUUID();
    UUID stampId = UUID.randomUUID();
    when(jdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class), any()))
        .thenReturn(List.of(stampId));
    when(userStampRepository.findByUserIdAndStampId(userId, stampId)).thenReturn(Optional.empty());

    stampService.grantForPlace(userId, "126508");

    ArgumentCaptor<UserStamp> captor = ArgumentCaptor.forClass(UserStamp.class);
    verify(userStampRepository).save(captor.capture());
    assertThat(captor.getValue().getStampCount()).isEqualTo(1);
    assertThat(captor.getValue().getFirstAcquiredAt()).isNotNull();
  }

  @Test
  void grantForPlace는_재방문이면_횟수만_올린다() {
    UUID userId = UUID.randomUUID();
    UUID stampId = UUID.randomUUID();
    UserStamp existing = new UserStamp(userId, stampId, LocalDateTime.now());
    when(jdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class), any()))
        .thenReturn(List.of(stampId));
    when(userStampRepository.findByUserIdAndStampId(userId, stampId))
        .thenReturn(Optional.of(existing));

    stampService.grantForPlace(userId, "126508");

    assertThat(existing.getStampCount()).isEqualTo(2);
    verify(userStampRepository).save(existing);
  }

  @Test
  void listMyStamps는_미획득_지역도_포함해_돌려준다() {
    UUID userId = UUID.randomUUID();
    List<StampResponse> rows =
        List.of(
            new StampResponse(
                UUID.randomUUID(),
                "강원",
                "강원 스탬프",
                "https://example.com/m.png",
                true,
                2,
                LocalDateTime.of(2026, 9, 1, 10, 0)),
            new StampResponse(UUID.randomUUID(), "경기", "경기 스탬프", null, false, 0, null));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(rows);

    StampCollectionResponse result = stampService.listMyStamps(userId);

    assertThat(result.stamps()).hasSize(2);
    assertThat(result.totalCount()).isEqualTo(2);
    assertThat(result.acquiredCount()).isEqualTo(1);
  }
}
