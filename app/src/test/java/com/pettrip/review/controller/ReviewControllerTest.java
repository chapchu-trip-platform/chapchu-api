package com.pettrip.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrip.config.SecurityConfig;
import com.pettrip.review.model.Review;
import com.pettrip.review.service.ReviewService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class ReviewControllerTest {

  private static final UUID USER_ID = UUID.fromString("0198f3a0-1234-7000-8000-000000000001");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ReviewService reviewService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 리뷰를_작성한다() throws Exception {
    UUID petId = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest(
            "place-abc", petId, (short) 4, "강아지랑 산책하기 딱 좋은 곳이에요", "SUNNY", null);
    Review review =
        new Review("place-abc", USER_ID, petId, (short) 4, "강아지랑 산책하기 딱 좋은 곳이에요", "SUNNY");
    when(reviewService.createReview(any(), any())).thenReturn(review);

    mockMvc
        .perform(
            post("/reviews")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "review-create",
                requestFields(
                    fieldWithPath("placeId").description("장소 외부 ID"),
                    fieldWithPath("petId").description("동행한 반려동물 ID"),
                    fieldWithPath("rating").description("별점 (1~5)").type(JsonFieldType.NUMBER),
                    fieldWithPath("contents").description("리뷰 내용"),
                    fieldWithPath("weather")
                        .description("날씨 (SUNNY·CLOUDY·RAINY·SNOWY). 생략 가능")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("coursePlaceId")
                        .description("코스 방문 장소 ID. 코스 여행 중 작성 시 연결. 생략 가능")
                        .type(JsonFieldType.STRING)
                        .optional()),
                responseFields(
                    fieldWithPath("id").description("리뷰 ID"),
                    fieldWithPath("placeId").description("장소 외부 ID"),
                    fieldWithPath("petId").description("동행한 반려동물 ID"),
                    fieldWithPath("rating").description("별점").type(JsonFieldType.NUMBER),
                    fieldWithPath("contents").description("리뷰 내용"),
                    fieldWithPath("weather")
                        .description("날씨")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("recommendationCount").description("추천 수"),
                    fieldWithPath("createdAt").description("작성일시"),
                    fieldWithPath("coursePlaceId")
                        .description("코스 방문 장소 ID. 코스 외 단독 리뷰면 null")
                        .type(JsonFieldType.STRING)
                        .optional())));
  }

  @Test
  void 내_리뷰를_삭제한다() throws Exception {
    UUID reviewId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/reviews/{reviewId}", reviewId)
                .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "review-delete",
                pathParameters(parameterWithName("reviewId").description("삭제할 리뷰 ID"))));

    verify(reviewService).deleteReview(any(), eq(reviewId));
  }
}
