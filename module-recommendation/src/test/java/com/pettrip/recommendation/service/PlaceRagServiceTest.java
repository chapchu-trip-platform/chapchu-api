package com.pettrip.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaceRagServiceTest {

  @Mock private EmbeddingModel embeddingModel;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private Array pgArray;

  private PlaceRagService placeRagService;

  @BeforeEach
  void setUp() throws Exception {
    when(embeddingModel.embed(any(String.class))).thenReturn(new float[1536]);
    when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createArrayOf(any(), any())).thenReturn(pgArray);
    placeRagService = new PlaceRagService(embeddingModel, jdbcTemplate);
    placeRagService.init();
  }

  @Test
  @SuppressWarnings("unchecked")
  void 리뷰_있는_장소가_없는_장소보다_앞에_랭크된다() {
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(), any()))
        .thenReturn(List.of("p1", "p3"));

    List<String> result = placeRagService.rankByReviewSimilarity(List.of("p1", "p2", "p3"), null);

    assertThat(result).containsExactly("p1", "p3", "p2");
  }

  @Test
  @SuppressWarnings("unchecked")
  void 모든_장소에_리뷰_없으면_원본_순서_반환된다() {
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(), any()))
        .thenReturn(List.of());

    List<String> result = placeRagService.rankByReviewSimilarity(List.of("p1", "p2"), null);

    assertThat(result).containsExactly("p1", "p2");
  }

  @Test
  @SuppressWarnings("unchecked")
  void 예외_발생시_원본_목록_그대로_반환된다() throws SQLException {
    when(connection.createArrayOf(any(), any())).thenThrow(new SQLException("DB error"));

    List<String> result = placeRagService.rankByReviewSimilarity(List.of("p1", "p2"), null);

    assertThat(result).containsExactly("p1", "p2");
  }

  @Test
  void 빈_목록이면_즉시_반환된다() {
    List<String> result = placeRagService.rankByReviewSimilarity(List.of(), null);

    assertThat(result).isEmpty();
  }
}
