package com.pettrip.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.review.service.ReviewService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(ReviewRecommendationController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class ReviewRecommendationControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReviewService reviewService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 리뷰를_추천한다() throws Exception {
    UUID reviewId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/reviews/{reviewId}/recommendations", reviewId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "review-recommendation-create",
                pathParameters(parameterWithName("reviewId").description("리뷰 ID"))));

    verify(reviewService).recommend(any(), eq(reviewId));
  }

  @Test
  void 리뷰_추천을_취소한다() throws Exception {
    UUID reviewId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/reviews/{reviewId}/recommendations", reviewId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "review-recommendation-delete",
                pathParameters(parameterWithName("reviewId").description("리뷰 ID"))));

    verify(reviewService).cancelRecommendation(any(), eq(reviewId));
  }
}
