package com.pettrip.review.controller;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pettrip.config.SecurityConfig;
import com.pettrip.review.model.Review;
import com.pettrip.review.service.ReviewService;
import java.util.List;
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
@WebMvcTest(PlaceReviewController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs(outputDir = "app/build/generated-snippets")
class PlaceReviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReviewService reviewService;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void 장소별_리뷰_목록을_조회한다() throws Exception {
    String placeId = "place-abc";
    when(reviewService.listPlaceReviews(placeId))
        .thenReturn(
            List.of(
                new Review(
                    placeId, UUID.randomUUID(), UUID.randomUUID(), (short) 4, "좋았어요", "SUNNY")));

    mockMvc
        .perform(get("/places/{placeId}/reviews", placeId))
        .andExpect(status().isOk())
        .andDo(
            document(
                "place-review-list",
                pathParameters(parameterWithName("placeId").description("장소 외부 ID")),
                responseFields(
                    fieldWithPath("[].id").description("리뷰 ID"),
                    fieldWithPath("[].placeId").description("장소 외부 ID"),
                    fieldWithPath("[].petId").description("동행한 반려동물 ID"),
                    fieldWithPath("[].rating").description("별점").type(JsonFieldType.NUMBER),
                    fieldWithPath("[].contents").description("리뷰 내용"),
                    fieldWithPath("[].weather")
                        .description("날씨 (SUNNY·CLOUDY·RAINY·SNOWY, 없으면 null)")
                        .type(JsonFieldType.STRING)
                        .optional(),
                    fieldWithPath("[].recommendationCount").description("추천 수"),
                    fieldWithPath("[].createdAt").description("작성일시"),
                    fieldWithPath("[].coursePlaceId")
                        .description("코스 방문 장소 ID. 코스 외 단독 리뷰면 null")
                        .type(JsonFieldType.STRING)
                        .optional())));
  }
}
