package com.pettrip.recommendation.service;

import jakarta.annotation.PostConstruct;
import java.sql.Array;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

@Service
public class PlaceRagService {

  private static final Logger log = LoggerFactory.getLogger(PlaceRagService.class);
  private static final String FALLBACK_QUERY = "반려동물 동반 즐거운 여행 좋은 장소";
  private static final String RANK_SQL =
      "SELECT r.place_id"
          + " FROM reviews r"
          + " JOIN review_embeddings re ON re.review_id = r.id"
          + " WHERE r.place_id = ANY(?)"
          + " GROUP BY r.place_id"
          + " ORDER BY MIN(re.embedding <=> ?::vector) ASC, AVG(r.rating) DESC";

  private final EmbeddingModel embeddingModel;
  private final JdbcTemplate jdbcTemplate;
  private String cachedFallbackVectorStr;

  public PlaceRagService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
    this.embeddingModel = embeddingModel;
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  void init() {
    try {
      float[] vector = embeddingModel.embed(FALLBACK_QUERY);
      cachedFallbackVectorStr = buildVectorString(vector);
      log.info("[RAG] 여행 테마 임베딩 캐싱 완료 (dim={})", vector.length);
    } catch (Exception e) {
      log.warn("[RAG] 임베딩 캐싱 실패 — RAG 랭킹 비활성화: {}", e.getMessage());
    }
  }

  public List<String> rankByReviewSimilarity(List<String> placeIds, String queryText) {
    if (placeIds.isEmpty()) {
      return placeIds;
    }
    String vectorStr = embedQuery(queryText);
    if (vectorStr == null) {
      return placeIds;
    }
    return executeRankQuery(placeIds, vectorStr);
  }

  private String embedQuery(String queryText) {
    if (queryText == null || queryText.isBlank()) {
      return cachedFallbackVectorStr;
    }
    try {
      float[] vector = embeddingModel.embed(queryText);
      return buildVectorString(vector);
    } catch (Exception e) {
      log.warn("[RAG] 동적 임베딩 실패 — fallback 사용: {}", e.getMessage());
      return cachedFallbackVectorStr;
    }
  }

  private List<String> executeRankQuery(List<String> placeIds, String vectorStr) {
    try {
      Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
      Array pgArray = conn.createArrayOf("text", placeIds.toArray(String[]::new));
      DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());

      List<String> ranked =
          jdbcTemplate.query(RANK_SQL, (rs, n) -> rs.getString("place_id"), pgArray, vectorStr);

      Set<String> seen = new LinkedHashSet<>(ranked);
      placeIds.stream().filter(id -> !seen.contains(id)).forEach(seen::add);
      return new ArrayList<>(seen);
    } catch (Exception e) {
      log.warn("[RAG] 장소 랭킹 실패 — 원본 순서 사용: {}", e.getMessage());
      return placeIds;
    }
  }

  private String buildVectorString(float[] vector) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) sb.append(",");
      sb.append(vector[i]);
    }
    sb.append("]");
    return sb.toString();
  }
}
