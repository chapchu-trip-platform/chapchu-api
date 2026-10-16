package com.pettrip.review.service;

import com.pettrip.review.model.Review;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReviewEmbeddingService {

  private static final Logger log = LoggerFactory.getLogger(ReviewEmbeddingService.class);

  private final EmbeddingModel embeddingModel;
  private final JdbcTemplate jdbcTemplate;

  public ReviewEmbeddingService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
    this.embeddingModel = embeddingModel;
    this.jdbcTemplate = jdbcTemplate;
  }

  public void generateAndSave(Review review) {
    String text = buildEmbeddingText(review);
    log.info("[임베딩] 생성 시작 — reviewId={}, 텍스트={}자", review.getId(), text.length());
    long start = System.currentTimeMillis();
    try {
      float[] vector = embeddingModel.embed(text);
      String vectorStr = buildVectorString(vector);
      jdbcTemplate.update(
          "INSERT INTO review_embeddings (review_id, embedding) VALUES (?::uuid, ?::vector)"
              + " ON CONFLICT (review_id) DO UPDATE SET embedding = EXCLUDED.embedding",
          review.getId().toString(),
          vectorStr);
      log.info(
          "[임베딩] 저장 완료 — reviewId={}, 소요={}ms", review.getId(), System.currentTimeMillis() - start);
    } catch (Exception e) {
      log.error(
          "[임베딩] 생성 실패 — RAG 검색 누락 대상. reviewId={}, 소요={}ms, 원인={}",
          review.getId(),
          System.currentTimeMillis() - start,
          e.getMessage());
    }
  }

  private String buildEmbeddingText(Review review) {
    String weather = Objects.requireNonNullElse(review.getWeather(), "정보없음");
    return String.format(
        "날씨: %s\n별점: %d점\n리뷰: %s", weather, review.getRating(), review.getContents());
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
